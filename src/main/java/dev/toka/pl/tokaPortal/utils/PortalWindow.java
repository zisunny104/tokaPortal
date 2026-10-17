package dev.toka.pl.tokaPortal.utils;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindow;
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
    /** What each button of a window does, by the number of the button; the text of a button is never used to find out what was clicked. */
    private record Form(Player player, java.util.List<java.util.function.Consumer<Player>> actions, Player requester) {
    }

    private static final java.util.Map<FormWindow, Form> forms = new java.util.WeakHashMap<>();

    /** A window with buttons: each button is added together with the action it does. */
    private static final class Menu {
        private final FormWindowSimple window;
        private final java.util.List<java.util.function.Consumer<Player>> actions = new java.util.ArrayList<>();
        private Player requester;

        Menu(String title, String content) {
            this.window = new FormWindowSimple(title, content);
        }

        Menu button(String text, java.util.function.Consumer<Player> action) {
            this.window.addButton(new ElementButton(text));
            this.actions.add(action);
            return this;
        }

        Menu back() {
            return this.button(Main.text("ui.button.back"), PortalWindow::sendPortalMainWindow);
        }

        void show(Player player) {
            forms.put(this.window, new Form(player, this.actions, this.requester));
            player.showFormWindow(this.window);
        }
    }

    public static void sendPortalMainWindow(Player player) {
        Menu menu = new Menu(Main.text("ui.portal.title"), Main.text("ui.portal.prompt"));
        if (ZeroIntegration.has(REGIONS)) menu.button(Main.text("ui.button.regionList"), PortalWindow::sendPortalRegionListWindow);
        if (ZeroIntegration.has(LAND)) menu.button(Main.text("ui.button.landList"), PortalWindow::sendPortalLandListWindow);
        menu.button(Main.text("ui.button.playerList"), PortalWindow::sendPortalPlayerListWindow);
        menu.button(Main.text("ui.button.shopboxList"), p -> sendPointList(p, "shopbox", "ui.portal.shopboxList"));
        menu.button(Main.text("ui.button.teleportList"), p -> sendPointList(p, "point", "ui.portal.pointList"));
        if (ZeroIntegration.has(CITY_PASS)) menu.button(Main.text("ui.button.pass"), p -> sendCityPassForm(p));
        menu.button(Main.text("ui.button.close"), p -> {
        });
        menu.show(player);
    }

    /** The list of the points of one type; the buttons are the points themselves, so two points with the same text cannot be mixed up. */
    private static void sendPointList(Player player, String type, String key) {
        Menu menu = new Menu(Main.text(key + ".title"), Main.text(key + ".hint")).back();
        for (teleportPoint point : teleportPoint.getPoints().values()) {
            if (point.getType().equals(type)) {
                menu.button(point.getInfo(), p -> Portal.toPoint(point, p));
            }
        }
        menu.show(player);
    }

    private static void sendPortalRegionListWindow(Player player) {
        if (!ZeroIntegration.require(player, REGIONS)) return;
        Menu menu = new Menu(Main.text("ui.portal.regionList.title"), Main.text("ui.portal.regionList.hint")).back();
        for (Region region : Region.getRegions().values()) {
            menu.button(region.getInfo(), p -> {
                if (ZeroIntegration.require(p, REGIONS)) toRegion(region, p);
            });
        }
        menu.show(player);
    }

    public static void sendPortalLandListWindow(Player player) {
        if (!ZeroIntegration.require(player, LAND)) return;
        java.util.List<String> names = Land.getLandNameListByOwner(player);
        Menu menu = new Menu(Main.text("ui.portal.landList.title"), Main.text(names.isEmpty() ? "ui.portal.landList.empty" : "ui.portal.landList.hint")).back();
        for (String name : names) {
            menu.button(name, p -> {
                Land land = Land.getLand(name);
                if (land != null && ZeroIntegration.require(p, LAND)) Land.toLand(land, p);
            });
        }
        menu.show(player);
    }

    private static void sendPortalPlayerListWindow(Player player) {
        boolean others = Server.getInstance().getOnlinePlayers().size() > 1;
        Menu menu = new Menu(Main.text(others ? "ui.portal.playerList.title" : "ui.portal.playerNone.title"),
                Main.text(others ? "ui.portal.playerList.hint" : "ui.portal.playerNone.hint")).back();
        if (others) {
            for (String name : getPlayerNameList(player)) {
                menu.button(name, p -> {
                    Player target = Server.getInstance().getPlayer(name);
                    if (target != null && target.isOnline()) Portal.Tpa(p, target);
                });
            }
        }
        menu.show(player);
    }

    public static void sendPortalTpaAcceptWindow(Player player) {
        Player requester = Portal.getTpaFrom(player);
        if (requester == null) return;
        Menu menu = new Menu(Main.text("ui.portal.tpaTitle"), Main.text("ui.portal.tpaPrompt", requester.getName()));
        menu.requester = requester;
        menu.button(Main.text("ui.button.agree"), p -> {
            Player from = Portal.getTpaFrom(p);
            if (from != requester) return;
            if (!from.isOnline()) {
                delTpaMap(p);
                return;
            }
            if (portal(from, p.getLocation(), p.getName())) getPlayerInfo(from).sendText(Main.text("msg.tpa.arrived", p.getName()));
            delTpaMap(p);
        });
        menu.button(Main.text("ui.button.refuse"), p -> {
            Player from = Portal.getTpaFrom(p);
            if (from != requester) return;
            if (from.isOnline()) getPlayerInfo(from).sendText(Main.text("msg.tpa.declined", p.getName()));
            delTpaMap(p);
        });
        menu.show(player);
    }

    @EventHandler
    public void PortalRespond(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        Form form = forms.remove(event.getWindow());
        if (form == null || form.player() != player) return;
        if (event.wasClosed() || !(event.getResponse() instanceof FormResponseSimple response)) {
            if (form.requester() != null && Portal.getTpaFrom(player) == form.requester()) delTpaMap(player);
            return;
        }
        int id = response.getClickedButtonId();
        if (id >= 0 && id < form.actions().size()) form.actions().get(id).accept(player);
    }
}
