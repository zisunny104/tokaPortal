package dev.toka.pl.tokaPortal.provider;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.point.HomePoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class BaseDataProvider implements IDataProvider {

    public Main main;

    public static HashMap<Integer, HomePoint> homes = new HashMap<>();
    //TODO:WARP MAP

    public BaseDataProvider(Main main) {
        this.main = main;
        reload();
    }

    @Override
    public int addHomePoint(String name, Object creator, Location loc) {
        int id = -1;
        if (creator instanceof Player) {
            creator = ((Player) creator).getName();
        }
        if ((creator instanceof String)) {
            id = homes.size();
            homes.put(id, new HomePoint(this, id, name, "home", (String) creator, loc));
            save();
        }
        return id;
    }

    @Override
    public HomePoint getHomePoint(int id) {
        return homes.getOrDefault(id, null);
    }

    @Override
    public HomePoint getHomePoint(String name) {
        for (HomePoint home : homes.values()) {
            if (home.getName().equals(name)) {
                return home;
            }
        }
        return null;
    }

    @Override
    public HomePoint[] getHomePointsByCreator(Object creator) {
        List<HomePoint> result = new ArrayList<>();
        for (HomePoint home : homes.values()) {
            if (home.isCreator(creator)) result.add(home);
        }
        return result.toArray(new HomePoint[result.size()]);
    }

    @Override
    public boolean delHomePoint(Object val) {
        int id = -1;
        if (val instanceof HomePoint) {
            val = ((HomePoint) val).getId();
        }
        if (!(val instanceof Integer)) {
            throw new IllegalArgumentException("Invalid val,this function only accepts HomePoint/int.");
        }
        id = (int) val;
        if (!homes.containsKey(id)) {
            return false;
        }
        homes.remove(id);
        save();
        return true;
    }


    public IDataProvider reload() {
        return reload(false);
    }

    @Override
    public abstract IDataProvider reload(boolean save);

    @Override
    public abstract IDataProvider save();

    public IDataProvider close() {
        return close(true);
    }

    @Override
    public abstract IDataProvider close(boolean save);

    @Override
    public abstract String getName();
}
