package dev.toka.pl.tokaPortal.provider;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.plugin.Plugin;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.point.PublicPoints;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

/**
 * 住家與公共傳送點擁有者遷移：把只有名稱的項目對應到 UUID。順序為線上玩家、tokaProfiles、tokaEconomy、伺服器的離線查詢；
 * 前三者不讀磁碟，在主執行緒做，離線查詢（讀玩家檔）在背景執行緒做，結果回主執行緒套用。
 * tokaProfiles、tokaEconomy 以反射軟相依，未安裝或 API 不可用就略過。
 */
public final class OwnerMigration {
    private final Main plugin;
    private final ExecutorService lookup = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "tokaPortal-lookup");
        thread.setDaemon(true);
        return thread;
    });

    public OwnerMigration(Main plugin) {
        this.plugin = plugin;
    }

    public void stop() {
        lookup.shutdownNow();
    }

    /** 不讀磁碟的名稱解析：線上玩家、tokaProfiles、tokaEconomy。必須在主執行緒呼叫。 */
    public Optional<UUID> resolveFast(String name) {
        Server server = plugin.getServer();
        for (Player player : server.getOnlinePlayers().values()) {
            if (player.getName().equalsIgnoreCase(name)) return Optional.of(player.getUniqueId());
        }
        Optional<UUID> found = profiles(server, name);
        return found.isPresent() ? found : economy(server, name);
    }

    /** 先快速解析，沒有再到背景執行緒查伺服器的玩家檔；回呼在主執行緒。 */
    public void resolve(String name, Consumer<Optional<UUID>> done) {
        Optional<UUID> fast = resolveFast(name);
        if (fast.isPresent()) {
            done.accept(fast);
            return;
        }
        lookupAsync(List.of(name), found -> done.accept(Optional.ofNullable(found.get(name.toLowerCase(Locale.ROOT)))));
    }

    /** 啟動時對 legacy 住家與公共傳送點做一次遷移，結果寫 log。points 可為 null。 */
    public void start(IDataProvider provider, PublicPoints points) {
        Map<String, String> homeNames = new LinkedHashMap<>();
        for (IDataProvider.LegacyGroup group : provider.legacyGroups()) homeNames.put(group.name().toLowerCase(Locale.ROOT), group.name());
        Map<String, String> pointNames = new LinkedHashMap<>();
        if (points != null) for (IDataProvider.LegacyGroup group : points.legacyGroups()) pointNames.put(group.name().toLowerCase(Locale.ROOT), group.name());
        if (homeNames.isEmpty() && pointNames.isEmpty()) return;
        Map<String, String> names = new LinkedHashMap<>(homeNames);
        pointNames.forEach(names::putIfAbsent);
        Map<String, UUID> resolved = new HashMap<>();
        List<String> remaining = new ArrayList<>();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            Optional<UUID> id = resolveFast(entry.getValue());
            if (id.isPresent()) resolved.put(entry.getKey(), id.get());
            else remaining.add(entry.getValue());
        }
        lookupAsync(remaining, found -> {
            resolved.putAll(found);
            // 背景查詢期間 provider 可能已換掉或關閉，套用時只看目前的
            IDataProvider current = Main.getProvider();
            if (current != null && !homeNames.isEmpty()) {
                IDataProvider.Assigned result = current.assignOwners(resolved);
                int left = current.legacyGroups().stream().mapToInt(IDataProvider.LegacyGroup::homes).sum();
                plugin.getLogger().info(Main.text("log.homes.migrated", homeNames.size(), result.assigned(), left, result.disputed()));
                for (String name : result.disputedNames()) plugin.getLogger().warning(Main.text("log.homes.dispute", name));
            }
            PublicPoints currentPoints = plugin.getPublicPoints();
            if (currentPoints != null && !pointNames.isEmpty()) {
                IDataProvider.Assigned result = currentPoints.assignOwners(resolved);
                int left = currentPoints.legacyGroups().stream().mapToInt(IDataProvider.LegacyGroup::homes).sum();
                plugin.getLogger().info(Main.text("log.points.migrated", pointNames.size(), result.assigned(), left, result.disputed()));
                for (String name : result.disputedNames()) plugin.getLogger().warning(Main.text("log.points.dispute", name));
            }
        });
    }

    /** 背景查詢離線玩家；回呼在主執行緒，鍵為小寫名稱，查不到的不放入。 */
    private void lookupAsync(List<String> names, Consumer<Map<String, UUID>> done) {
        if (names.isEmpty()) {
            done.accept(Map.of());
            return;
        }
        try {
            lookup.execute(() -> {
                Map<String, UUID> found = new HashMap<>();
                for (String name : names) {
                    try { plugin.getServer().lookupName(name).ifPresent(id -> found.put(name.toLowerCase(Locale.ROOT), id)); }
                    catch (RuntimeException ignored) { /* 查不到就維持 legacy */ }
                }
                try { plugin.getServer().getScheduler().scheduleTask(plugin, () -> done.accept(found)); }
                catch (RuntimeException ignored) { /* 插件已停用 */ }
            });
        } catch (RejectedExecutionException e) {
            // 插件已停用
        }
    }

    private static Optional<UUID> profiles(Server server, String name) {
        try {
            Plugin plugin = server.getPluginManager().getPlugin("tokaProfiles");
            if (plugin == null || !plugin.isEnabled()) return Optional.empty();
            Object repository = plugin.getClass().getMethod("profiles").invoke(plugin);
            Object ids = repository.getClass().getMethod("resolveName", String.class).invoke(repository, name);
            if (ids instanceof Set<?> set && set.size() == 1 && set.iterator().next() instanceof UUID id) return Optional.of(id);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // 未安裝、尚未就緒或 API 不相容：略過
        }
        return Optional.empty();
    }

    private static Optional<UUID> economy(Server server, String name) {
        try {
            Plugin plugin = server.getPluginManager().getPlugin("tokaEconomy");
            if (plugin == null || !plugin.isEnabled()) return Optional.empty();
            Class<?> api = Class.forName("dev.toka.economy.Economy", false, plugin.getClass().getClassLoader());
            if (api.getMethod("resolve", String.class).invoke(null, name) instanceof Optional<?> found
                    && found.orElse(null) instanceof UUID id) return Optional.of(id);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // 未安裝或 API 不相容：略過
        }
        return Optional.empty();
    }
}
