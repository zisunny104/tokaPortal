package dev.toka.pl.tokaPortal.utils;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.element.ElementSlider;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.window.FormWindowCustom;
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

    private record Item(String text, java.util.function.Consumer<Player> action) {
    }

    /** What one player chose for the lists: the words to look for, the order and the number of items on a page (0 for the default of the config). */
    private record View(String query, int sort, int perPage) {
    }

    private static final java.util.Map<java.util.UUID, View> views = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<FormWindow, java.util.function.Consumer<FormResponseCustom>> options = new java.util.WeakHashMap<>();

    private static View view(Player player) {
        return views.getOrDefault(player.getUniqueId(), new View("", 0, 0));
    }

    public static void forget(Player player) {
        views.remove(player.getUniqueId());
    }

    private static int configInt(String key, int fallback) {
        return Main.getInstance().getConfig().getInt(key, fallback);
    }

    /**
     * A list in pages: the back button first, the items of the page, then the buttons for the previous and the next page.
     * A long list also has a button for the words to look for, the order and the number of items on a page, which the player
     * keeps until leaving. A list with nothing in it shows the empty text. The buttons are the items themselves, so two items
     * with the same text cannot be mixed up.
     */
    private static void sendList(Player player, String key, java.util.List<Item> all, int page) {
        View view = view(player);
        java.util.List<Item> items = new java.util.ArrayList<>(all);
        if (!view.query().isEmpty()) {
            String needle = view.query().toLowerCase(java.util.Locale.ROOT);
            items.removeIf(item -> !item.text().toLowerCase(java.util.Locale.ROOT).contains(needle));
        }
        if (view.sort() == 1) items.sort(java.util.Comparator.comparing(item -> item.text().toLowerCase(java.util.Locale.ROOT)));
        int min = Math.max(3, configInt("window-items-min", 3));
        int max = Math.max(min, configInt("window-items-max", 30));
        int perPage = Math.min(max, Math.max(min, view.perPage() > 0 ? view.perPage() : configInt("window-items-per-page", 10)));
        dev.toka.i18n.Pager.Page<Item> shown = dev.toka.i18n.Pager.of(items, perPage, page);
        String title = Main.text(key + ".title");
        if (shown.pages() > 1) title = Main.text("ui.page.title", title, shown.page(), shown.pages());
        String content = Main.text(!all.isEmpty() && items.isEmpty() ? "ui.search.none" : all.isEmpty() ? key + ".empty" : key + ".hint", view.query());
        Menu menu = new Menu(title, content).back();
        for (Item item : shown.lines()) menu.button(item.text(), item.action());
        if (shown.page() > 1) menu.button(Main.text("ui.button.prevPage"), p -> sendList(p, key, all, shown.page() - 1));
        if (shown.page() < shown.pages()) menu.button(Main.text("ui.button.nextPage"), p -> sendList(p, key, all, shown.page() + 1));
        if (all.size() > configInt("window-search-threshold", 8) || !view.query().isEmpty()) {
            menu.button(Main.text("ui.button.options"), p -> sendOptions(p, key, all, min, max, perPage));
        }
        menu.show(player);
    }

    /** The form for the words to look for, the order and the number of items on a page. */
    private static void sendOptions(Player player, String key, java.util.List<Item> all, int min, int max, int perPage) {
        View view = view(player);
        FormWindowCustom window = new FormWindowCustom(Main.text("ui.options.title"));
        window.addElement(new ElementLabel(Main.text("ui.options.hint", all.size())));
        window.addElement(new ElementInput(Main.text("ui.options.query"), Main.text("ui.options.queryHint"), view.query()));
        window.addElement(new ElementDropdown(Main.text("ui.options.sort"),
                java.util.List.of(Main.text("ui.options.sortDefault"), Main.text("ui.options.sortName")), view.sort()));
        window.addElement(new ElementSlider(Main.text("ui.options.perPage"), min, max, 1, perPage));
        options.put(window, response -> {
            String query = response.getInputResponse(1) == null ? "" : response.getInputResponse(1).strip();
            int sort = response.getDropdownResponse(2) == null ? 0 : response.getDropdownResponse(2).getElementID();
            int per = Math.round(response.getSliderResponse(3));
            views.put(player.getUniqueId(), new View(query, sort, per));
            sendList(player, key, all, 1);
        });
        forms.put(window, new Form(player, java.util.List.of(), null));
        player.showFormWindow(window);
    }

    private static void sendPointList(Player player, String type, String key) {
        java.util.List<Item> items = new java.util.ArrayList<>();
        for (teleportPoint point : teleportPoint.getPoints().values()) {
            if (point.getType().equals(type)) items.add(new Item(point.getInfo(), p -> Portal.toPoint(point, p)));
        }
        sendList(player, key, items, 1);
    }

    private static void sendPortalRegionListWindow(Player player) {
        if (!ZeroIntegration.require(player, REGIONS)) return;
        java.util.List<Item> items = new java.util.ArrayList<>();
        for (Region region : Region.getRegions().values()) {
            items.add(new Item(region.getInfo(), p -> {
                if (ZeroIntegration.require(p, REGIONS)) toRegion(region, p);
            }));
        }
        sendList(player, "ui.portal.regionList", items, 1);
    }

    public static void sendPortalLandListWindow(Player player) {
        if (!ZeroIntegration.require(player, LAND)) return;
        java.util.List<Item> items = new java.util.ArrayList<>();
        for (String name : Land.getLandNameListByOwner(player)) {
            items.add(new Item(name, p -> {
                Land land = Land.getLand(name);
                if (land != null && ZeroIntegration.require(p, LAND)) Land.toLand(land, p);
            }));
        }
        sendList(player, "ui.portal.landList", items, 1);
    }

    private static void sendPortalPlayerListWindow(Player player) {
        java.util.List<Item> items = new java.util.ArrayList<>();
        for (String name : getPlayerNameList(player)) {
            items.add(new Item(name, p -> {
                Player target = Server.getInstance().getPlayer(name);
                if (target != null && target.isOnline()) Portal.Tpa(p, target);
            }));
        }
        sendList(player, "ui.portal.playerList", items, 1);
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
    public void onQuit(PlayerQuitEvent event) {
        forget(event.getPlayer());
    }

    @EventHandler
    public void PortalRespond(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        Form form = forms.remove(event.getWindow());
        if (form == null || form.player() != player) return;
        var optionAction = options.remove(event.getWindow());
        if (optionAction != null) {
            if (!event.wasClosed() && event.getResponse() instanceof FormResponseCustom custom) optionAction.accept(custom);
            return;
        }
        if (event.wasClosed() || !(event.getResponse() instanceof FormResponseSimple response)) {
            if (form.requester() != null && Portal.getTpaFrom(player) == form.requester()) delTpaMap(player);
            return;
        }
        int id = response.getClickedButtonId();
        if (id >= 0 && id < form.actions().size()) form.actions().get(id).accept(player);
    }
}
