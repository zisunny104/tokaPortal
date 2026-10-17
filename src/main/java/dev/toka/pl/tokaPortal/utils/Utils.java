package dev.toka.pl.tokaPortal.utils;

import cn.nukkit.Server;
import cn.nukkit.level.Location;
import cn.nukkit.utils.ConfigSection;

public class Utils {


    public static Location parseLocation(ConfigSection data) {
        return new Location(data.getDouble("x"), data.getDouble("y"), data.getDouble("z"),
                data.getDouble("yaw"), data.getDouble("pitch"),
                Server.getInstance().getLevelByName(data.getString("level")));
    }

}
