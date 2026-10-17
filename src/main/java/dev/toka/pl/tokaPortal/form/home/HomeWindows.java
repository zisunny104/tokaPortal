package dev.toka.pl.tokaPortal.form.home;

import cn.nukkit.Player;
import dev.toka.pl.tokaPortal.form.Confirm;
import dev.toka.pl.tokaPortal.form.ListMenu;
import dev.toka.pl.tokaPortal.form.Menu;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.point.HomePoint;
import dev.toka.pl.tokaPortal.utils.PortalWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static dev.toka.pl.tokaPortal.Main.getProvider;
import static dev.toka.pl.tokaPortal.utils.Portal.delHome;
import static dev.toka.pl.tokaPortal.utils.Portal.toPoint;

/** The windows of the homes: the list to teleport, the list to edit, the actions of one home and the confirmation of a delete. */
public final class HomeWindows {
    private HomeWindows() {
    }

    private static List<ListMenu.Item> items(Player player, String icon, Function<HomePoint, java.util.function.Consumer<Player>> action) {
        List<ListMenu.Item> items = new ArrayList<>();
        for (HomePoint home : getProvider().getHomePointsByCreator(player)) {
            items.add(new ListMenu.Item(home.getName(), icon, action.apply(home)));
        }
        return items;
    }

    /** The homes of the player to teleport to; back goes to the portal menu. */
    public static void sendList(Player player) {
        PortalWindow.list("ui.home.list", items(player, PortalWindow.icon(PortalWindow.ICON_HOME),
                home -> p -> toPoint(getProvider().getHomePoint(home.getName()), p)), PortalWindow::sendPortalMainWindow).show(player);
    }

    /** The homes of the player to edit; back goes to the list to teleport. */
    public static void sendEditList(Player player) {
        PortalWindow.list("ui.home.editList", items(player, null, home -> p -> sendEdit(p, home.getName())), HomeWindows::sendList).show(player);
    }

    /** The actions for one home: back to the list, or delete. */
    public static void sendEdit(Player player, String name) {
        new Menu(Main.text("ui.home.edit.title", name), Main.text("ui.home.edit.prompt"))
                .button(Main.text("ui.home.edit.back"), HomeWindows::sendEditList)
                .button(Main.text("ui.home.edit.delete"), p -> sendDelete(p, name))
                .show(player);
    }

    /** Asks before deleting; cancel or closing the window goes back to the actions of the home. */
    public static void sendDelete(Player player, String name) {
        new Confirm(Main.text("ui.home.del.title"), Main.text("ui.home.del.confirm", name),
                Main.text("ui.home.del.button"), Main.text("ui.common.cancel"))
                .onConfirm(p -> {
                    delHome(p, name);
                    sendEditList(p);
                })
                .onCancel(p -> sendEdit(p, name))
                .show(player);
    }
}
