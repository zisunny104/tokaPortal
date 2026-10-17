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
import dev.toka.pl.tokaPortal.point.teleportPoint;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.Region;

import static dev.toka.pl.tokaPortal.utils.Portal.*;
import dev.toka.pl.tokaPortal.Main;
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
        FormWindowSimple window = new FormWindowSimple(Main.text("ui.portal.title"), Main.text("ui.portal.prompt"));
        if (ZeroIntegration.has(REGIONS)) window.addButton(new ElementButton(Main.text("ui.button.regionList")));
        if (ZeroIntegration.has(LAND)) window.addButton(new ElementButton(Main.text("ui.button.landList")));
        window.addButton(new ElementButton(Main.text("ui.button.playerList")));
        {
            window.addButton(new ElementButton(Main.text("ui.button.shopboxList")));
            window.addButton(new ElementButton(Main.text("ui.button.teleportList")));
        }
        if (ZeroIntegration.has(CITY_PASS)) window.addButton((new ElementButton(Main.text("ui.button.pass"))));
        window.addButton(new ElementButton(Main.text("ui.button.close")));
        show(player, window);
    }

    private static void sendPortalTeleportPointListWindow(Player player) {

        FormWindowSimple window = new FormWindowSimple(Main.text("ui.portal.pointList.title"), Main.text("ui.portal.pointList.hint"));
        window.addButton(new ElementButton(Main.text("ui.button.back")));
        for (teleportPoint point : teleportPoint.getPoints().values()) {
            if (point.getType().equals("point")) {
                window.addButton(new ElementButton(point.getInfo()));
            }
        }
        show(player, window);
    }

    private static void sendPortalShopBoxListWindow(Player player) {

        FormWindowSimple window = new FormWindowSimple(Main.text("ui.portal.shopboxList.title"), Main.text("ui.portal.shopboxList.hint"));
        window.addButton(new ElementButton(Main.text("ui.button.back")));
        for (teleportPoint point : teleportPoint.getPoints().values()) {
            if (point.getType().equals("shopbox")) {
                window.addButton(new ElementButton(point.getInfo()));
            }
        }
        show(player, window);
    }

    private static void sendPortalRegionListWindow(Player player) {
        if (!ZeroIntegration.require(player, REGIONS)) return;
        FormWindowSimple window = new FormWindowSimple(Main.text("ui.portal.regionList.title"), Main.text("ui.portal.regionList.hint"));
        window.addButton(new ElementButton(Main.text("ui.button.back")));
        for (Region region : Region.getRegions().values()) {
            window.addButton(new ElementButton(region.getInfo()));
        }

        show(player, window);
    }

    public static void sendPortalLandListWindow(Player player) {
        if (!ZeroIntegration.require(player, LAND)) return;
        FormWindowCustom window = new FormWindowCustom(Main.text("ui.portal.landList.title"));
        if (Land.getLandNameListByOwner(player).size() > 0) {
            window.addElement(new ElementLabel(Main.text("ui.portal.landList.hint")));
            window.addElement(new ElementDropdown(Main.text("ui.portal.landList.dropdown"), Land.getLandNameListByOwner(player)));
        } else {
            window.addElement(new ElementLabel(Main.text("ui.portal.landList.empty")));
        }

        show(player, window);
    }

    private static void sendPortalPlayerListWindow(Player player) {
        FormWindowCustom window;
        if (Server.getInstance().getOnlinePlayers().size() > 1) {
            window = new FormWindowCustom(Main.text("ui.portal.playerList.title"));
            window.addElement(new ElementLabel(Main.text("ui.portal.playerList.hint")));
            window.addElement(new ElementDropdown(Main.text("ui.portal.playerList.dropdown"), getPlayerNameList(player)));
        } else {
            window = new FormWindowCustom(Main.text("ui.portal.playerNone.title"));
            window.addElement(new ElementLabel(Main.text("ui.portal.playerNone.hint")));
        }

        show(player, window);
    }

    public static void sendPortalTpaAcceptWindow(Player player) {
        Player requester = Portal.getTpaFrom(player);
        if (requester == null) return;
        String text = Main.text("ui.portal.tpaPrompt", requester.getName());
        FormWindowSimple window = new FormWindowSimple(Main.text("ui.portal.tpaTitle"), text);
        requests.put(window, requester);
        window.addButton(new ElementButton(Main.text("ui.button.agree")));
        window.addButton(new ElementButton(Main.text("ui.button.refuse")));

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
        if (window instanceof FormWindowSimple) {
            String title = ((FormWindowSimple) event.getWindow()).getTitle();
            String button = ((FormResponseSimple) event.getResponse()).getClickedButton().getText();
            String back = Main.text("ui.button.back");

            if (title.equals(Main.text("ui.portal.title"))) {
                if (button.equals(Main.text("ui.button.teleportList"))) {
                    sendPortalTeleportPointListWindow(player);
                } else if (button.equals(Main.text("ui.button.shopboxList"))) {
                    sendPortalShopBoxListWindow(player);
                } else if (button.equals(Main.text("ui.button.regionList"))) {
                    sendPortalRegionListWindow(player);
                } else if (button.equals(Main.text("ui.button.playerList"))) {
                    sendPortalPlayerListWindow(player);
                } else if (button.equals(Main.text("ui.button.landList"))) {
                    sendPortalLandListWindow(player);
                } else if (button.equals(Main.text("ui.button.pass"))) {
                    sendCityPassForm(player);
                }
            } else if (title.equals(Main.text("ui.portal.pointList.title"))
                    || title.equals(Main.text("ui.portal.shopboxList.title"))) {
                if (button.equals(back)) {
                    sendPortalMainWindow(player);
                }
                for (teleportPoint point : teleportPoint.getPoints().values()) {
                    if (button.equals(point.getInfo())) {
                        Portal.toPoint(point, event.getPlayer());
                    }
                }
            } else if (title.equals(Main.text("ui.portal.regionList.title"))) {
                if (!ZeroIntegration.require(player, REGIONS)) return;
                if (button.equals(back)) {
                    sendPortalMainWindow(player);
                }
                for (Region region : Region.getRegions().values()) {
                    if (button.equals(region.getInfo())) {
                        toRegion(region, event.getPlayer());
                    }
                }
            } else if (title.equals(Main.text("ui.portal.tpaTitle"))) {
                Player fromPlayer = Portal.getTpaFrom(player);
                if (fromPlayer != requester) return;
                if (fromPlayer == null || !fromPlayer.isOnline()) {
                    delTpaMap(player);
                    return;
                }
                PlayerInfo fromPli = getPlayerInfo(fromPlayer);
                if (button.equals(Main.text("ui.button.agree"))) {
                    if (fromPlayer.isOnline()) {
                        if (portal(fromPlayer, player.getLocation(), player.getName())) fromPli.sendText(Main.text("msg.tpa.arrived", player.getName()));
                        delTpaMap(player);
                    }
                } else if (button.equals(Main.text("ui.button.refuse"))) {
                    fromPli.sendText(Main.text("msg.tpa.declined", player.getName()));
                    delTpaMap(player);
                }
            }
        } else if (window instanceof FormWindowCustom) {
            String title = ((FormWindowCustom) event.getWindow()).getTitle();
            FormResponseCustom response = ((FormWindowCustom) window).getResponse();
            if (title.equals(Main.text("ui.portal.playerList.title"))) {
                String toPl = response.getDropdownResponse(1).getElementContent();
                if (toPl == null) {
                    return;
                }
                Player TPlayer = Server.getInstance().getPlayer(toPl);
                Portal.Tpa(player, TPlayer);
            } else if (title.equals(Main.text("ui.portal.landList.title"))) {
                if (!ZeroIntegration.require(player, LAND)) return;
                if (response.getDropdownResponse(1) != null) {
                    String landName = response.getDropdownResponse(1).getElementContent();
                    if (landName == null) {
                        return;
                    }
                    Land.toLand(Land.getLand(landName), player);
                }
            }
        }
    }
}
