package dev.toka.pl.tokaPortal.utils;

import cn.nukkit.Server;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.utils.ConfigSection;

import java.util.function.Function;

public class Utils {
    /** 世界名稱對世界；測試可替換，避免載入整個伺服器。 */
    static Function<String, Level> levels = name -> {
        Server server = Server.getInstance();
        return server == null ? null : server.getLevelByName(name);
    };

    public static void setLevelLookup(Function<String, Level> lookup) {
        levels = lookup;
    }

    public static Location parseLocation(ConfigSection data) {
        return new Location(data.getDouble("x"), data.getDouble("y"), data.getDouble("z"),
                data.getDouble("yaw"), data.getDouble("pitch"), levels.apply(data.getString("level")));
    }

}
