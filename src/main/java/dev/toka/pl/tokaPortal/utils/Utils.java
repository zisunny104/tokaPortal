package dev.toka.pl.tokaPortal.utils;

import cn.nukkit.Server;
import cn.nukkit.level.Location;
import cn.nukkit.utils.ConfigSection;

public class Utils {

    public static final String CONFIG_VERSION = "1.0.4";

    public static final String TITLE_PORTAL_MAIN = "=傳送系統=";
    public static final String TITLE_PORTAL_TELEPORT_LIST = "=傳送點列表=";
    public static final String TITLE_PORTAL_SHOPBOX_LIST = "=商店列表=";
    public static final String TITLE_PORTAL_REGION_LIST = "=區域列表=";
    public static final String TITLE_PORTAL_LAND_LIST = "=領地列表=";
    public static final String TITLE_PORTAL_HOME_LIST = "=住家列表=";
    public static final String TITLE_PORTAL_HOME_SET = "=設定住家=";
    public static final String TITLE_PORTAL_HOME_DEL = "=刪除住家=";
    public static final String TITLE_PORTAL_HOME_EDIT = "=編輯住家=";
    public static final String TITLE_PORTAL_PLAYER_LIST = "=玩家列表=";
    public static final String TITLE_PORTAL_PLAYER_NONE = "=現無玩家=";
    public static final String TITLE_PORTAL_TPA_ACCEPT = "=傳送同意=";

    public static final String BUTTON_PORTAL_TELEPORT_LIST = "§e>>§8傳送點列表  ";
    public static final String BUTTON_PORTAL_SHOPBOX_LIST = "§e>>§8商店傳送列表";
    public static final String BUTTON_PORTAL_REGION_LIST = "§e>>§8區域傳送列表";
    public static final String BUTTON_PORTAL_LAND_LIST = "§e>>§8領地傳送列表";
    public static final String BUTTON_PORTAL_HOME_LIST = "§e>>§8住家傳送列表";
    public static final String BUTTON_PORTAL_PLAYER_LIST = "§e>>§8線上玩家傳送";

    public static final String BUTTON_BACK = "§l§e>返回前頁<";
    public static final String BUTTON_PASS = "§l§b>進入城市通<";
    public static final String BUTTON_CLOSE = "§l§c>關閉<";
    public static final String BUTTON_YES = "§l§a>同意<";
    public static final String BUTTON_NO = "§l§c>取消<";

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
