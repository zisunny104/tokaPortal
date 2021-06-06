package dev.toka.pl.tokaPortal;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowSimple;
import prj.toka.zero.player.PlayerInfo;
import prj.toka.zero.ser.chat.Chat;
import prj.toka.zero.ser.land.Land;
import prj.toka.zero.ser.portal.teleportPoint.teleportPoint;
import prj.toka.zero.ser.region.Region;

import static dev.toka.pl.tokaPortal.Portal.*;
import static prj.toka.zero.player.Players.getPlayerInfo;
import static prj.toka.zero.player.citypass.CityPass.sendCityPassForm;
import static prj.toka.zero.utils.Utils.getPlayerNameList;

public class PortalWindow implements Listener {

    private static final String TITLE_PORTAL_MAIN = "=傳送系統=";
    private static final String TITLE_PORTAL_TELEPORT_LIST = "=傳送點列表=";
    private static final String TITLE_PORTAL_SHOPBOX_LIST = "=商店列表=";
    private static final String TITLE_PORTAL_REGION_LIST = "=區域列表=";
    private static final String TITLE_PORTAL_LAND_LIST = "=領地列表=";
    private static final String TITLE_PORTAL_PLAYER_LIST = "=玩家列表=";
    private static final String TITLE_PORTAL_PLAYER_NONE = "=現無玩家=";
    private static final String TITLE_PORTAL_TPA_ACCEPT = "=傳送同意=";

    private static final String BUTTON_PORTAL_TELEPORT_LIST = "§e>>§8傳送點列表  ";
    private static final String BUTTON_PORTAL_SHOPBOX_LIST = "§e>>§8商店傳送列表";
    private static final String BUTTON_PORTAL_REGION_LIST = "§e>>§8區域傳送列表";
    private static final String BUTTON_PORTAL_LAND_LIST = "§e>>§8領地傳送列表";
    private static final String BUTTON_PORTAL_PLAYER_LIST = "§e>>§8線上玩家傳送";

    private static final String BUTTON_BACK = "§l§e>返回前頁<";
    private static final String BUTTON_PASS = "§l§b>進入城市通<";
    private static final String BUTTON_CLOSE = "§l§c>關閉<";
    private static final String BUTTON_YES = "§l§a>同意<";
    private static final String BUTTON_NO = "§l§c>取消<";

    public PortalWindow() {
    }

    public static void sendPortalMainWindow(Player player) {
        FormWindowSimple window = new FormWindowSimple(TITLE_PORTAL_MAIN, "請選擇傳送目的");
        window.addButton(new ElementButton(BUTTON_PORTAL_REGION_LIST));
        window.addButton(new ElementButton(BUTTON_PORTAL_LAND_LIST));
        window.addButton(new ElementButton(BUTTON_PORTAL_PLAYER_LIST));
        window.addButton(new ElementButton(BUTTON_PORTAL_SHOPBOX_LIST));
        window.addButton(new ElementButton(BUTTON_PORTAL_TELEPORT_LIST));
        window.addButton((new ElementButton(BUTTON_PASS)));
        window.addButton(new ElementButton(BUTTON_CLOSE));
        player.showFormWindow(window);
    }

    private static void sendPortalTeleportPointListWindow(Player player) {
        FormWindowSimple FormWindow = new FormWindowSimple(TITLE_PORTAL_TELEPORT_LIST, "傳送點列表 \n點擊進行傳送");
        FormWindow.addButton(new ElementButton(BUTTON_BACK));
        for (teleportPoint point : teleportPoint.getPoints().values()) {
            if (point.getType().equals("point")) {
                FormWindow.addButton(new ElementButton(point.getInfo()));
            }
        }
        player.showFormWindow(FormWindow);
    }

    private static void sendPortalShopBoxListWindow(Player player) {
        FormWindowSimple FormWindow = new FormWindowSimple(TITLE_PORTAL_SHOPBOX_LIST, "點擊進行傳送");
        FormWindow.addButton(new ElementButton(BUTTON_BACK));
        for (teleportPoint point : teleportPoint.getPoints().values()) {
            if (point.getType().equals("shopbox")) {
                FormWindow.addButton(new ElementButton(point.getInfo()));
            }
        }
        player.showFormWindow(FormWindow);
    }

    private static void sendPortalRegionListWindow(Player player) {
        FormWindowSimple FormWindow = new FormWindowSimple(TITLE_PORTAL_REGION_LIST, "點擊按鈕 進行傳送");
        FormWindow.addButton(new ElementButton(BUTTON_BACK));
        for (Region region : Region.getRegions().values()) {
            FormWindow.addButton(new ElementButton(region.getInfo()));
        }

        player.showFormWindow(FormWindow);
    }

    public static void sendPortalLandListWindow(Player player) {
        FormWindowCustom window = new FormWindowCustom(TITLE_PORTAL_LAND_LIST);
        if (Land.getLandNameListByOwner(player).size() > 0) {
            window.addElement(new ElementLabel("選取需要傳送的領地後\n點擊下方'送出'按鈕 進行傳送"));
            window.addElement(new ElementDropdown("領地傳送列表", Land.getLandNameListByOwner(player)));
        } else {
            window.addElement(new ElementLabel("您沒有可傳送的領地"));
        }

        player.showFormWindow(window);
    }

    private static void sendPortalPlayerListWindow(Player player) {
        FormWindowCustom window;
        if (Server.getInstance().getOnlinePlayers().size() > 1) {
            window = new FormWindowCustom(TITLE_PORTAL_PLAYER_LIST);
            window.addElement(new ElementLabel("選取需要傳送的玩家後\n點擊下方'送出'按鈕 進行傳送"));
            window.addElement(new ElementDropdown("線上玩家列表", getPlayerNameList(player)));
        } else {
            window = new FormWindowCustom(TITLE_PORTAL_PLAYER_NONE);
            window.addElement(new ElementLabel("現在沒有其他線上玩家可以傳送喔.."));
        }

        player.showFormWindow(window);
    }

    public static void sendPortalTpaAcceptWindow(Player player) {
        String text = "玩家%pl想要傳送到你身邊".replace("%pl", Portal.getTpaFrom(player).getName());
        FormWindowSimple FormWindow = new FormWindowSimple(TITLE_PORTAL_TPA_ACCEPT, text);
        FormWindow.addButton(new ElementButton(BUTTON_YES));
        FormWindow.addButton(new ElementButton(BUTTON_NO));

        player.showFormWindow(FormWindow);
    }

    @EventHandler
    public void PortalRespond(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        FormWindow window = event.getWindow();
        if (event.getResponse() != null) {
            if (!event.wasClosed()) {
                String title;
                if (window instanceof FormWindowSimple) {
                    title = ((FormWindowSimple) event.getWindow()).getTitle();
                    String button = ((FormResponseSimple) event.getResponse()).getClickedButton().getText();

                    switch (title) {
                        case TITLE_PORTAL_MAIN: {
                            switch (button) {
                                case BUTTON_PORTAL_TELEPORT_LIST: {
                                    sendPortalTeleportPointListWindow(player);
                                    break;
                                }

                                case BUTTON_PORTAL_SHOPBOX_LIST: {
                                    sendPortalShopBoxListWindow(player);
                                    break;
                                }

                                case BUTTON_PORTAL_REGION_LIST: {
                                    sendPortalRegionListWindow(player);
                                    break;
                                }

                                case BUTTON_PORTAL_PLAYER_LIST: {
                                    sendPortalPlayerListWindow(player);
                                    break;
                                }

                                case BUTTON_PORTAL_LAND_LIST: {
                                    sendPortalLandListWindow(player);
                                    break;
                                }

                                case BUTTON_PASS:
                                    sendCityPassForm(player);
                                    break;

                                default: {
                                    break;
                                }
                            }
                        }

                        case TITLE_PORTAL_TELEPORT_LIST: {
                            if (button.equals(BUTTON_BACK)) {
                                sendPortalMainWindow(player);
                            }
                            for (teleportPoint point : teleportPoint.getPoints().values()) {
                                if (button.equals(point.getInfo())) {
                                    Portal.toPoint(point, event.getPlayer());
                                }
                            }
                            break;
                        }

                        case TITLE_PORTAL_SHOPBOX_LIST: {
                            if (button.equals(BUTTON_BACK)) {
                                sendPortalMainWindow(player);
                            }
                            for (teleportPoint point : teleportPoint.getPoints().values()) {
                                if (button.equals(point.getInfo())) {
                                    Portal.toPoint(point, event.getPlayer());
                                }
                            }
                            break;
                        }

                        case TITLE_PORTAL_REGION_LIST: {
                            if (button.equals(BUTTON_BACK)) {
                                sendPortalMainWindow(player);
                            }
                            for (Region region : Region.getRegions().values()) {
                                if (button.equals(region.getInfo())) {
                                    toRegion(region, event.getPlayer());
                                }
                            }
                            break;
                        }

                        case TITLE_PORTAL_TPA_ACCEPT: {
                            Player Fplayer = Portal.getTpaFrom(player);
                            PlayerInfo Fpli = getPlayerInfo(Fplayer);
                            if (button.equals(BUTTON_YES)) {
                                if (Fplayer.isOnline()) {
                                    portal(Fplayer, player.getLocation(), player.getName());
                                    Fpli.sendText("§a[傳送]§b您已傳送到玩家§e %1 §b身旁§r".replace("%1", player.getName()));
                                    delTpaMap(player);
                                }
                            } else if (button.equals(BUTTON_NO)) {
                                Fpli.sendText("§a[傳送]§b玩家§e %1 §b取消你的傳送要求§r".replace("%1", player.getName()));
                                Chat.sendBroadcast("[DEBUG]玩家" + Fplayer.getName() + "的傳送要求已被" + player.getName() + "取消");
                                delTpaMap(player);
                            }
                        }
                        break;
                    }
                } else if (window instanceof FormWindowCustom) {
                    if (!event.wasClosed()) {
                        title = ((FormWindowCustom) event.getWindow()).getTitle();
                        FormResponseCustom response = ((FormWindowCustom) window).getResponse();
                        switch (title) {
                            case TITLE_PORTAL_PLAYER_NONE:
                                break;

                            case TITLE_PORTAL_PLAYER_LIST:
                                String toPl = response.getDropdownResponse(1).getElementContent();
                                if (toPl == null) {
                                    break;
                                }

                                Player TPlayer = Server.getInstance().getPlayer(toPl);
                                Portal.Tpa(player, TPlayer);
                                break;

                            case TITLE_PORTAL_LAND_LIST:
                                if (response.getDropdownResponse(1) != null) {
                                    String landName = response.getDropdownResponse(1).getElementContent();
                                    if (landName == null) {
                                        break;
                                    }
                                    Land.toLand(Land.getLand(landName), player);
                                }
                                break;

                        }
                    }
                }
            }

        }
    }
}
