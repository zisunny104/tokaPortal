package dev.toka.pl.tokaPortal.provider;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.level.Location;
import cn.nukkit.utils.ConfigSection;
import dev.toka.pl.tokaPortal.point.HomePoint;
import dev.toka.pl.tokaPortal.provider.storage.HomeStorage;
import dev.toka.pl.tokaPortal.provider.storage.WriteQueue;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class BaseDataProvider implements IDataProvider {
    /** Main-thread only: publishes counts without exposing names, positions or mutable HomePoint objects. */
    public Map<UUID, Long> planOwnerCounts() {
        Map<UUID, Long> counts = new HashMap<>();
        for (HomePoint home : homes.values()) if (home.getOwner() != null) counts.merge(home.getOwner(), 1L, Long::sum);
        return Map.copyOf(counts);
    }
    public long planTotalCount() { return homes.size(); }
    private final HomeStorage storage;
    private final WriteQueue writer;
    private final String name;
    private Map<Integer, HomePoint> homes = new LinkedHashMap<>();
    private ConfigSection document;
    /** 線上玩家名稱對 UUID；測試可替換。 */
    private Function<String, UUID> online = name -> {
        Server server = Server.getInstance();
        Player player = server == null ? null : server.getPlayerExact(name);
        return player == null ? null : player.getUniqueId();
    };

    public void setOnlineLookup(Function<String, UUID> lookup) { this.online = lookup; }

    public BaseDataProvider(HomeStorage storage, WriteQueue writer, String name) {
        this.storage = storage;
        this.writer = writer;
        this.name = name;
        try { reload(); }
        catch (RuntimeException e) {
            try { storage.close(); } catch (IOException close) { e.addSuppressed(close); }
            throw e;
        }
    }

    @Override
    public int addHomePoint(String name, Object creator, Location loc) {
        UUID owner;
        String shown;
        if (creator instanceof Player player) { owner = player.getUniqueId(); shown = player.getName(); }
        else if (creator instanceof UUID uuid) { owner = uuid; shown = knownName(uuid); }
        else if (creator instanceof String text) { owner = uuidOfName(text); shown = text; }
        else return -1;
        if (getHomePoint(name) != null) throw new IllegalArgumentException("Duplicate home name.");
        int id = homes.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
        Map<Integer, HomePoint> candidate = new LinkedHashMap<>(homes);
        candidate.put(id, new HomePoint(id, name, "home", owner, shown, loc));
        persist(candidate);
        homes = candidate;
        return id;
    }

    /** 名稱對 UUID：線上玩家，否則已知擁有者的最後名稱（只有一人時）；查不到為 null（legacy）。 */
    private UUID uuidOfName(String name) {
        UUID id = online.apply(name);
        if (id != null) return id;
        Set<UUID> owners = new HashSet<>();
        for (HomePoint home : homes.values()) {
            if (home.getOwner() != null && home.getOwnerName().equalsIgnoreCase(name)) owners.add(home.getOwner());
        }
        return owners.size() == 1 ? owners.iterator().next() : null;
    }

    private String knownName(UUID id) {
        for (HomePoint home : homes.values()) if (id.equals(home.getOwner())) return home.getOwnerName();
        return id.toString();
    }

    @Override public HomePoint getHomePoint(int id) { return homes.get(id); }
    @Override public HomePoint getHomePoint(String name) {
        return homes.values().stream().filter(home -> home.getName().equals(name)).findFirst().orElse(null);
    }
    @Override public HomePoint[] getHomePointsByCreator(Object creator) {
        if (creator instanceof Player player) creator = player.getUniqueId();
        if (creator instanceof UUID uuid) return getHomePointsByOwner(uuid);
        if (!(creator instanceof String name)) return new HomePoint[0];
        // 名稱版：名稱是線上玩家就以 UUID 比對（改名後被別人用的舊名稱不會誤認）；其餘比對最後已知名稱與 legacy 住家
        UUID id = online.apply(name);
        return homes.values().stream().filter(home -> home.getOwner() != null && id != null
                ? home.getOwner().equals(id) : home.isCreator(name)).toArray(HomePoint[]::new);
    }

    @Override public HomePoint[] getHomePointsByOwner(UUID owner) {
        return homes.values().stream().filter(home -> owner != null && owner.equals(home.getOwner())).toArray(HomePoint[]::new);
    }

    @Override public List<LegacyGroup> legacyGroups() {
        Map<String, int[]> count = new TreeMap<>();
        Map<String, String> shown = new HashMap<>();
        for (HomePoint home : homes.values()) {
            if (!home.isLegacy()) continue;
            String key = lower(home.getOwnerName());
            count.computeIfAbsent(key, k -> new int[1])[0]++;
            shown.putIfAbsent(key, home.getOwnerName());
        }
        List<LegacyGroup> groups = new ArrayList<>();
        count.forEach((key, n) -> groups.add(new LegacyGroup(shown.get(key), n[0], isDisputed(key, null))));
        return groups;
    }

    private static String lower(String name) { return name == null ? "" : name.toLowerCase(Locale.ROOT); }

    /** 已有「不同 UUID」的住家使用同一個名稱；candidate 為 null 時只要有任何 UUID 持有就算。 */
    private boolean isDisputed(String lowerName, UUID candidate) {
        return homes.values().stream().anyMatch(home -> home.getOwner() != null
                && !home.getOwner().equals(candidate) && lower(home.getOwnerName()).equals(lowerName));
    }

    @Override public Assigned assignOwners(Map<String, UUID> resolved) {
        int assigned = 0, disputed = 0;
        List<String> disputedNames = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (HomePoint home : homes.values()) {
            if (!home.isLegacy()) continue;
            String key = lower(home.getOwnerName());
            UUID id = resolved.get(key);
            if (id == null) continue;
            if (isDisputed(key, id)) {
                if (seen.add(key)) { disputed++; disputedNames.add(home.getOwnerName()); }
                continue;
            }
            home.claim(id, home.getOwnerName());
            assigned++;
        }
        if (assigned > 0) persist(homes);
        return new Assigned(assigned, disputed, disputedNames);
    }

    @Override public int claimLegacy(String name, UUID owner, String ownerName) {
        int claimed = 0;
        for (HomePoint home : homes.values()) {
            if (home.isLegacy() && lower(home.getOwnerName()).equals(lower(name))) { home.claim(owner, ownerName); claimed++; }
        }
        if (claimed > 0) persist(homes);
        return claimed;
    }

    @Override public Joined onPlayerJoin(UUID id, String name) {
        int renamed = 0, claimed = 0;
        boolean disputed = false;
        String key = lower(name);
        boolean conflict = isDisputed(key, id);
        for (HomePoint home : homes.values()) {
            if (id.equals(home.getOwner())) {
                if (!name.equals(home.getOwnerName())) { home.setOwnerName(name); renamed++; }
            } else if (home.isLegacy() && lower(home.getOwnerName()).equals(key)) {
                if (conflict) disputed = true;
                else { home.claim(id, name); claimed++; }
            }
        }
        if (renamed > 0 || claimed > 0) persist(homes);
        return new Joined(renamed, claimed, disputed);
    }

    @Override public boolean delHomePoint(Object value) {
        if (value instanceof HomePoint home) value = home.getId();
        if (!(value instanceof Integer id)) throw new IllegalArgumentException("A home or a home id is required.");
        if (!homes.containsKey(id)) return false;
        Map<Integer, HomePoint> candidate = new LinkedHashMap<>(homes);
        candidate.remove(id);
        persist(candidate);
        homes = candidate;
        return true;
    }

    private void persist(Map<Integer, HomePoint> candidate) {
        ConfigSection next = new ConfigSection(document);
        next.set("homes", new ArrayList<>(candidate.values().stream().map(HomePoint::getRawData).toList()));
        ConfigSection validated;
        try { validated = HomeStorage.validate(next); }
        catch (IOException e) { throw new IllegalStateException("Home data could not be saved.", e); }
        // 記憶體立即更新，寫入排入背景佇列（驗證後為獨立副本）
        writer.submit("log.homes.saveFailed", () -> storage.save(validated));
        document = next;
    }

    @Override public IDataProvider reload() { return reload(false); }
    @Override public IDataProvider reload(boolean save) {
        if (save) save();
        try {
            ConfigSection loaded = storage.load();
            Map<Integer, HomePoint> next = new LinkedHashMap<>();
            for (Object item : loaded.getList("homes")) {
                ConfigSection home = new ConfigSection(HomeStorage.toMap((Map<?, ?>) item));
                int id = next.size();
                next.put(id, new HomePoint(id, home));
            }
            homes = next;
            document = loaded;
            return this;
        } catch (IOException e) { throw new IllegalStateException("Home data could not be loaded.", e); }
    }
    @Override public IDataProvider save() { persist(homes); return this; }
    @Override public IDataProvider close() { return close(true); }
    @Override public IDataProvider close(boolean save) {
        try { if (save) save(); }
        finally {
            writer.drain(10_000);
            try { storage.close(); }
            catch (IOException e) { throw new IllegalStateException("Home data could not be closed.", e); }
        }
        return this;
    }
    @Override public String getName() { return name; }
}
