package dev.toka.pl.tokaPortal.utils;

import cn.nukkit.Player;
import cn.nukkit.Server;
import dev.toka.pl.tokaPortal.form.Confirm;
import dev.toka.pl.tokaPortal.form.FormTexts;
import dev.toka.pl.tokaPortal.form.ListMenu;
import dev.toka.pl.tokaPortal.form.Menu;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.integration.Services;
import dev.toka.pl.tokaPortal.integration.LandIntegration;
import dev.toka.pl.tokaPortal.integration.Services.Region;
import dev.toka.pl.tokaPortal.point.teleportPoint;

import java.util.ArrayList;
import java.util.List;

import static dev.toka.pl.tokaPortal.integration.Services.Feature.*;
import static dev.toka.pl.tokaPortal.integration.Services.getPlayerInfo;
import static dev.toka.pl.tokaPortal.integration.Services.getPlayerNameList;
import static dev.toka.pl.tokaPortal.integration.Services.sendCityPassForm;
import static dev.toka.pl.tokaPortal.utils.Portal.*;

/** The windows of the portal menu, built from the windows of the form package. */
public final class PortalWindow {
    private static final String ICON_POINT = "textures/items/compass_item";
    private static final String ICON_SHOPBOX = "textures/blocks/chest_front";
    private static final String ICON_REGION = "textures/items/map_filled";
    private static final String ICON_LAND = "textures/items/name_tag";
    private static final String ICON_PLAYER = "textures/items/ender_pearl";
    private static final String ICON_PASS = "textures/items/emerald";
    private static final String ICON_CLOSE = "textures/ui/cancel";
    public static final String ICON_HOME = "textures/items/bed_red";

    private PortalWindow() {
    }

    /** The texts of the windows, from the language file. */
    public static FormTexts texts() {
        return new FormTexts(Main.text("ui.button.back"), Main.text("ui.button.prevPage"), Main.text("ui.button.nextPage"),
                Main.text("ui.button.options"), Main.text("ui.options.title"), Main.text("ui.options.hint", "${0}"),
                Main.text("ui.options.query"), Main.text("ui.options.queryHint"), Main.text("ui.options.sort"),
                Main.text("ui.options.sortDefault"), Main.text("ui.options.sortName"), Main.text("ui.options.perPage"),
                Main.text("ui.search.none", "${0}"), Main.text("ui.page.title", "${0}", "${1}", "${2}"));
    }

    private static int configInt(String key, int fallback) {
        return Main.getInstance().getConfig().getInt(key, fallback);
    }

    /** The limits of the lists, from the config. */
    public static ListMenu.Settings settings() {
        int min = Math.max(3, configInt("window-items-min", 3));
        int max = Math.max(min, configInt("window-items-max", 30));
        return new ListMenu.Settings(configInt("window-items-per-page", 10), min, max, configInt("window-search-threshold", 8));
    }

    /** The icon path for a button, or null when the icons are turned off in the config. */
    public static String icon(String path) {
        return Main.getInstance().getConfig().getBoolean("window-icons", true) ? path : null;
    }

    /** A list with the title, hint and empty text found under the key. */
    public static ListMenu list(String key, List<ListMenu.Item> items, java.util.function.Consumer<Player> back) {
        return new ListMenu(texts(), settings()).title(Main.text(key + ".title")).hint(Main.text(key + ".hint"))
                .emptyText(Main.text(key + ".empty")).back(back).items(items);
    }

    public static void sendPortalMainWindow(Player player) {
        List<ListMenu.Item> points = pointItems("point");
        List<ListMenu.Item> shopboxes = pointItems("shopbox");
        Menu menu = new Menu(Main.text("ui.portal.title"), Main.text("ui.portal.prompt"));
        if (Services.has(REGIONS) && !Region.getRegions().isEmpty()) {
            menu.button(Main.text("ui.button.regionList"), icon(ICON_REGION), PortalWindow::sendPortalRegionListWindow);
        }
        if (LandIntegration.targets(player).map(list->!list.isEmpty()).orElse(false)) {
            menu.button(Main.text("ui.button.landList"), icon(ICON_LAND), PortalWindow::sendPortalLandListWindow);
        }
        if (!getPlayerNameList(player).isEmpty()) {
            menu.button(Main.text("ui.button.playerList"), icon(ICON_PLAYER), PortalWindow::sendPortalPlayerListWindow);
        }
        if (!shopboxes.isEmpty()) {
            menu.button(Main.text("ui.button.shopboxList"), icon(ICON_SHOPBOX), p -> sendPointList(p, "shopbox", "ui.portal.shopboxList"));
        }
        if (!points.isEmpty()) {
            menu.button(Main.text("ui.button.teleportList"), icon(ICON_POINT), p -> sendPointList(p, "point", "ui.portal.pointList"));
        }
        if (Services.has(CITY_PASS)) menu.button(Main.text("ui.button.pass"), icon(ICON_PASS), p -> sendCityPassForm(p));
        menu.button(Main.text("ui.button.close"), icon(ICON_CLOSE), p -> {
        });
        menu.show(player);
    }

    private static List<ListMenu.Item> pointItems(String type) {
        List<ListMenu.Item> items = new ArrayList<>();
        for (teleportPoint point : teleportPoint.getPoints().values()) {
            if (point.getType().equals(type)) items.add(new ListMenu.Item(point.getInfo(), null, p -> Portal.toPoint(point, p)));
        }
        return items;
    }

    private static void sendPointList(Player player, String type, String key) {
        list(key, pointItems(type), PortalWindow::sendPortalMainWindow).show(player);
    }

    private static void sendPortalRegionListWindow(Player player) {
        if (!Services.require(player, REGIONS)) return;
        List<ListMenu.Item> items = new ArrayList<>();
        for (Region region : Region.getRegions().values()) {
            items.add(ListMenu.Item.of(region.getInfo(), p -> {
                if (Services.require(p, REGIONS)) toRegion(region, p);
            }));
        }
        list("ui.portal.regionList", items, PortalWindow::sendPortalMainWindow).show(player);
    }

    public static void sendPortalLandListWindow(Player player) {
        List<ListMenu.Item> items = new ArrayList<>();
        java.util.Set<String> names=new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        LandIntegration.targets(player).ifPresent(targets->targets.forEach(target->{
            names.add(target.name());
            items.add(ListMenu.Item.of(target.name(),p->{
                if(Portal.toLandClaim(p,target.id()))
                    getPlayerInfo(p).sendText(Main.text("msg.land.arrived",target.name()));
            }));
        }));
        if(items.isEmpty()){player.sendMessage(Main.text(LandIntegration.available()?"ui.form.landList.empty":"err.land.unavailable"));return;}
        list("ui.portal.landList", items, PortalWindow::sendPortalMainWindow).show(player);
    }

    private static void sendPortalPlayerListWindow(Player player) {
        List<ListMenu.Item> items = new ArrayList<>();
        for (String name : getPlayerNameList(player)) {
            items.add(ListMenu.Item.of(name, p -> {
                Player target = Server.getInstance().getPlayer(name);
                if (target != null && target.isOnline()) Portal.Tpa(p, target);
            }));
        }
        list("ui.portal.playerList", items, PortalWindow::sendPortalMainWindow).show(player);
    }

    /** Asks the player to agree to the request; closing the window or the cancel button declines it. */
    public static void sendPortalTpaAcceptWindow(Player player) {
        Player requester = Portal.getTpaFrom(player);
        if (requester == null) return;
        new Confirm(Main.text("ui.portal.tpaTitle"), Main.text("ui.portal.tpaPrompt", requester.getName()),
                Main.text("ui.button.agree"), Main.text("ui.button.refuse"))
                .onConfirm(p -> {
                    Player from = Portal.getTpaFrom(p);
                    if (from != requester) return;
                    if (!from.isOnline()) {
                        delTpaMap(p);
                        return;
                    }
                    if (portal(from, p.getLocation(), p.getName())) getPlayerInfo(from).sendText(Main.text("msg.tpa.arrived", p.getName()));
                    delTpaMap(p);
                })
                .onCancel(p -> {
                    Player from = Portal.getTpaFrom(p);
                    if (from != requester) return;
                    if (from.isOnline()) getPlayerInfo(from).sendText(Main.text("msg.tpa.declined", p.getName()));
                    delTpaMap(p);
                })
                .show(player);
    }
}
