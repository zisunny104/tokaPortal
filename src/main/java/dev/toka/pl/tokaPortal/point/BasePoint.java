package dev.toka.pl.tokaPortal.point;

import cn.nukkit.level.Location;

public interface BasePoint {

    String getName();

    String getType();

    String getCreator();

    Location getLocation();

    boolean isCreator(Object creator);
}
