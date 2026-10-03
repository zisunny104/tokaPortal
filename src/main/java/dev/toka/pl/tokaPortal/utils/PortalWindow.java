package dev.toka.pl.tokaPortal.utils;

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
import dev.toka.pl.tokaPortal.integration.ZeroIntegration;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.Feature.*;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.PlayerInfo;

import dev.toka.pl.tokaPortal.integration.ZeroIntegration.Land;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.teleportPoint;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.Region;

import static dev.toka.pl.tokaPortal.utils.Portal.*;
import static dev.toka.pl.tokaPortal.utils.Utils.*;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.getPlayerInfo;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.sendCityPassForm;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.getPlayerNameList;

public class PortalWindow implements Listener {
    private static final java.util.Map<FormWindow, Player> windows = new java.util.WeakHashMap<>();
    private static final java.util.Map<FormWindow, Player> requests = new java.util.WeakHashMap<>();

    private static void show(Player player, FormWindow window) {
        windows.put(window, player);
        player.showFormWindow(window);
    }

    public static void sendPortalMainWindow(Player player) {
        FormWindowSimple window = new FormWindowSimple(TITLE_PORTAL_MAIN, "請選擇傳送目的");
        if (ZeroIntegration.has(REGIONS)) window.addButton(new ElementButton(BUTTON_PORTAL_REGION_LIST));
        if (ZeroIntegration.has(LAND)) window.addButton(new ElementButton(BUTTON_PORTAL_LAND_LIST));
        window.addButton(new ElementButton(BUTTON_PORTAL_PLAYER_LIST));
        if (ZeroIntegration.has(POINTS)) {
            window.addButton(new ElementButton(BUTTON_PORTAL_SHOPBOX_LIST));
            window.addButton(new ElementButton(BUTTON_PORTAL_TELEPORT_LIST));
        }
        if (ZeroIntegration.has(CITY_PASS)) window.addButton((new ElementButton(BUTTON_PASS)));
        window.addButton(new ElementButton(BUTTON_CLOSE));
        show(player, window);
    }

    private static void sendPortalTeleportPointListWindow(Player player) {
        if (!ZeroIntegration.require(player, POINTS)) return;
        FormWindowSimple window = new FormWindowSimple(TITLE_PORTAL_TELEPORT_LIST, "傳送點列表 \n點擊進行傳送");
        window.addButton(new ElementButton(BUTTON_BACK));
        for (teleportPoint point : teleportPoint.getPoints().values()) {
            if (point.getType().equals("point")) {
                window.addButton(new ElementButton(point.getInfo()));
            }
        }
        show(player, window);
    }

    private static void sendPortalShopBoxListWindow(Player player) {
        if (!ZeroIntegration.require(player, POINTS)) return;
        FormWindowSimple window = new FormWindowSimple(TITLE_PORTAL_SHOPBOX_LIST, "點擊進行傳送");
        window.addButton(new ElementButton(BUTTON_BACK));
        for (teleportPoint point : teleportPoint.getPoints().values()) {
            if (point.getType().equals("shopbox")) {
                window.addButton(new ElementButton(point.getInfo()));
            }
        }
        show(player, window);
    }

    private static void sendPortalRegionListWindow(Player player) {
        if (!ZeroIntegration.require(player, REGIONS)) return;
        FormWindowSimple window = new FormWindowSimple(TITLE_PORTAL_REGION_LIST, "點擊按鈕 進行傳送");
        window.addButton(new ElementButton(BUTTON_BACK));
        for (Region region : Region.getRegions().values()) {
            window.addButton(new ElementButton(region.getInfo()));
        }

        show(player, window);
    }

    public static void sendPortalLandListWindow(Player player) {
        if (!ZeroIntegration.require(player, LAND)) return;
        FormWindowCustom window = new FormWindowCustom(TITLE_PORTAL_LAND_LIST);
        if (Land.getLandNameListByOwner(player).size() > 0) {
            window.addElement(new ElementLabel("選取需要傳送的領地後\n點擊下方'送出'按鈕 進行傳送"));
            window.addElement(new ElementDropdown("領地傳送列表", Land.getLandNameListByOwner(player)));
        } else {
            window.addElement(new ElementLabel("您沒有可傳送的領地"));
        }

        show(player, window);
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

        show(player, window);
    }

    public static void sendPortalTpaAcceptWindow(Player player) {
        Player requester = Portal.getTpaFrom(player);
        if (requester == null) return;
        String text = "玩家 %pl 想要傳送到你身邊".replace("%pl", requester.getName());
        FormWindowSimple window = new FormWindowSimple(TITLE_PORTAL_TPA_ACCEPT, text);
        requests.put(window, requester);
        window.addButton(new ElementButton(BUTTON_YES));
        window.addButton(new ElementButton(BUTTON_NO));

        show(player, window);
    }

    @EventHandler
    public void PortalRespond(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        FormWindow window = event.getWindow();
        if (windows.remove(window) != player) return;
        Player requester = requests.remove(window);
        if (event.wasClosed() || event.getResponse() == null) {
            if (requester != null && Portal.getTpaFrom(player) == requester) delTpaMap(player);
            return;
        }
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
                        break;

                        case TITLE_PORTAL_TELEPORT_LIST: {
                            if (!ZeroIntegration.require(player, POINTS)) break;
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
                            if (!ZeroIntegration.require(player, POINTS)) break;
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
                            if (!ZeroIntegration.require(player, REGIONS)) break;
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
                            Player fromPlayer = Portal.getTpaFrom(player);
                            if (fromPlayer != requester) break;
                            if (fromPlayer == null || !fromPlayer.isOnline()) {
                                delTpaMap(player);
                                break;
                            }
                            PlayerInfo fromPli = getPlayerInfo(fromPlayer);
                            if (button.equals(BUTTON_YES)) {
                                if (fromPlayer.isOnline()) {
                                    if (portal(fromPlayer, player.getLocation(), player.getName())) fromPli.sendText("§a[傳送]§b您已傳送到玩家§e %1 §b身旁§r".replace("%1", player.getName()));
                                    delTpaMap(player);
                                }
                            } else if (button.equals(BUTTON_NO)) {
                                fromPli.sendText("§a[傳送]§b玩家§e %1 §b取消你的傳送要求§r".replace("%1", player.getName()));
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
                                if (!ZeroIntegration.require(player, LAND)) break;
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
