package dev.toka.pl.tokaPortal.utils;

import cn.nukkit.Server;
import cn.nukkit.level.Location;
import cn.nukkit.utils.ConfigSection;

public class Utils {

    public static final String CONFIG_VERSION = "1.0.4";

    public static ConfigSection encodeLocation(final Location location) {
        return new ConfigSection() {
            {
                this.set("x", location.x);
                this.set("y", location.y);
                this.set("z", location.z);
                this.set("yaw", location.yaw);
                this.set("pitch", location.pitch);
                this.set("level", location.level.getName());
            }
        };
    }

    public static Location parseLocation(ConfigSection data) {
        return new Location(data.getDouble("x"), data.getDouble("y"), data.getDouble("z"),
                data.getDouble("yaw"), data.getDouble("pitch"),
                Server.getInstance().getLevelByName(data.getString("level")));
    }

}
