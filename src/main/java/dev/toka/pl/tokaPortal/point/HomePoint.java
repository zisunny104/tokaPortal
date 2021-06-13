package dev.toka.pl.tokaPortal.point;

import cn.nukkit.Server;
import cn.nukkit.level.Location;

public class HomePoint implements BasePoint {

    int id = -1, x, y, z, yaw, pitch;
    String name, type, creator, level;
    Location loc;

    HomePoint(int id, String name, String type, String creator, Location loc) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.creator = creator;
        this.x = (int) loc.x;
        this.y = (int) loc.y;
        this.z = (int) loc.z;
        this.yaw = (int) loc.yaw;
        this.pitch = (int) loc.getPitch();
        this.level = loc.getLevelName();
        this.loc = loc;
    }

    //TODO:PROVIDER

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getCreator() {
        return creator;
    }

    @Override
    public Location getLocation() {
        return loc;
    }
}
