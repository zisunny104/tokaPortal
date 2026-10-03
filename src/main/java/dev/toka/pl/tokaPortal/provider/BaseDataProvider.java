package dev.toka.pl.tokaPortal.provider;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import cn.nukkit.utils.ConfigSection;
import dev.toka.pl.tokaPortal.point.HomePoint;
import dev.toka.pl.tokaPortal.provider.storage.HomeStorage;

import java.io.IOException;
import java.util.*;

public class BaseDataProvider implements IDataProvider {
    private final HomeStorage storage;
    private final String name;
    private Map<Integer, HomePoint> homes = new LinkedHashMap<>();
    private ConfigSection document;

    public BaseDataProvider(HomeStorage storage, String name) {
        this.storage = storage;
        this.name = name;
        try { reload(); }
        catch (RuntimeException e) {
            try { storage.close(); } catch (IOException close) { e.addSuppressed(close); }
            throw e;
        }
    }

    @Override
    public int addHomePoint(String name, Object creator, Location loc) {
        if (creator instanceof Player player) creator = player.getName();
        if (!(creator instanceof String owner)) return -1;
        if (getHomePoint(name) != null) throw new IllegalArgumentException("住家名稱重複。");
        int id = homes.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
        Map<Integer, HomePoint> candidate = new LinkedHashMap<>(homes);
        candidate.put(id, new HomePoint(id, name, "home", owner, loc));
        persist(candidate);
        homes = candidate;
        return id;
    }

    @Override public HomePoint getHomePoint(int id) { return homes.get(id); }
    @Override public HomePoint getHomePoint(String name) {
        return homes.values().stream().filter(home -> home.getName().equals(name)).findFirst().orElse(null);
    }
    @Override public HomePoint[] getHomePointsByCreator(Object creator) {
        return homes.values().stream().filter(home -> home.isCreator(creator)).toArray(HomePoint[]::new);
    }

    @Override public boolean delHomePoint(Object value) {
        if (value instanceof HomePoint home) value = home.getId();
        if (!(value instanceof Integer id)) throw new IllegalArgumentException("必須指定住家或住家編號。");
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
        try { storage.save(next); }
        catch (IOException e) { throw new IllegalStateException("住家資料儲存失敗。", e); }
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
        } catch (IOException e) { throw new IllegalStateException("住家資料載入失敗。", e); }
    }
    @Override public IDataProvider save() { persist(homes); return this; }
    @Override public IDataProvider close() { return close(true); }
    @Override public IDataProvider close(boolean save) {
        try { if (save) save(); }
        finally {
            try { storage.close(); }
            catch (IOException e) { throw new IllegalStateException("住家資料關閉失敗。", e); }
        }
        return this;
    }
    @Override public String getName() { return name; }
}
