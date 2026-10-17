package dev.toka.pl.tokaPortal.provider;

import cn.nukkit.level.Location;
import dev.toka.pl.tokaPortal.point.HomePoint;


public interface IDataProvider {

    int addHomePoint(String name, Object creator, Location loc);

    HomePoint getHomePoint(int id);

    HomePoint getHomePoint(String name);

    HomePoint[] getHomePointsByCreator(Object creator);

    boolean delHomePoint(Object val);

    IDataProvider save();

    IDataProvider close();

    IDataProvider close(boolean save);

    IDataProvider reload();

    IDataProvider reload(boolean save);

    String getName();
}
