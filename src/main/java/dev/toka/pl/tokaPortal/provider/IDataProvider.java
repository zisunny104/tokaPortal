package dev.toka.pl.tokaPortal.provider;

import cn.nukkit.level.Location;
import dev.toka.pl.tokaPortal.point.HomePoint;

public interface IDataProvider {

    int addHomePoint(String name, String creator, Location loc);

    HomePoint getHomePoint(int id);

    HomePoint getHomePoint(String name);

    boolean delHomePoint(Object val);

    IDataProvider save();

    IDataProvider close(); // save=true

    IDataProvider close(boolean save);

    IDataProvider reload(); // save=false

    IDataProvider reload(boolean save);

    String getName();
}
