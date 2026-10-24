package dev.toka.pl.tokaPortal.form;

import cn.nukkit.Player;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.window.FormWindowCustom;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.command.PortalCommand;
import dev.toka.pl.tokaPortal.point.teleportPoint;
import dev.toka.pl.tokaPortal.utils.Portal;
import dev.toka.pl.tokaPortal.utils.PortalWindow;

import java.util.ArrayList;
import java.util.List;

/** The windows that manage public points; they call the same code as /portal point and open only for people who may manage points. */
public final class PointWindows {
    private static final String PERMISSION = "tokaPortal.points.manage";
    private static final List<String> TYPES = List.of("point", "shopbox");
    private static final String ICON_ADD = "textures/ui/color_plus", ICON_LIST = "textures/items/compass_item";

    private PointWindows() {
    }

    public static void open(Player player) {
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Main.text("err.noPermission"));
            return;
        }
        new Menu(Main.text("ui.points.title"), Main.text("ui.points.prompt", teleportPoint.getPoints().size()))
                .back(PortalWindow.texts(), PortalWindow::sendPortalMainWindow)
                .button(Main.text("ui.points.add"), PortalWindow.icon(ICON_ADD), PointWindows::sendAdd)
                .button(Main.text("ui.points.list"), PortalWindow.icon(ICON_LIST), PointWindows::sendList)
                .show(player);
    }

    private static void sendAdd(Player player) {
        FormWindowCustom window = new FormWindowCustom(Main.text("ui.points.add"));
        var at = player.getLocation();
        window.addElement(new ElementLabel(Main.text("ui.points.here", at.getLevel().getName(),
                String.format("%,d", at.getFloorX()), String.format("%,d", at.getFloorY()), String.format("%,d", at.getFloorZ()))));
        window.addElement(new ElementInput(Main.text("ui.points.name"), Main.text("ui.points.namePlaceholder")));
        window.addElement(new ElementDropdown(Main.text("ui.points.type"), TYPES));
        Registry.show(player, window, (p, event) -> {
            if (event.wasClosed() || !(event.getResponse() instanceof FormResponseCustom response)) {
                open(p);
                return;
            }
            String name = response.getInputResponse(1) == null ? "" : response.getInputResponse(1).strip();
            var type = response.getDropdownResponse(2);
            PortalCommand.addPoint(p, name, TYPES.get(type == null ? 0 : Math.min(type.getElementID(), TYPES.size() - 1)));
            open(p);
        });
    }

    private static void sendList(Player player) {
        List<ListMenu.Item> items = new ArrayList<>();
        for (teleportPoint point : teleportPoint.getPoints().values()) {
            items.add(new ListMenu.Item(Main.text("ui.points.item", point.getName(), point.getType()), PortalWindow.icon(ICON_LIST), p -> sendDetail(p, point.getName())));
        }
        PortalWindow.list("ui.points.listing", items, PointWindows::open).show(player);
    }

    private static void sendDetail(Player player, String name) {
        teleportPoint point = teleportPoint.getPoint(name);
        if (point == null) {
            sendList(player);
            return;
        }
        new Menu(Main.text("ui.points.detail", name), Main.text("ui.points.detailInfo", point.getType(), point.getCreator(), point.getName(), point.getInfo(),
                point.getOwner() == null ? Main.text("ui.points.legacyOwner") : point.getOwner().toString(), where(point)))
                .back(PortalWindow.texts(), PointWindows::sendList)
                .button(Main.text("ui.points.go"), PortalWindow.icon(ICON_LIST), p -> Portal.toPoint(point, p))
                .button(Main.text("ui.points.delete"), PortalWindow.icon("textures/ui/cancel"), p -> sendDelete(p, name))
                .show(player);
    }

    /** The world and the block the point is at, as plain text */
    private static String where(teleportPoint point) {
        var at = point.getLocation();
        return at == null || at.getLevel() == null ? "-" : at.getLevel().getFolderName() + " " + at.getFloorX() + ", " + at.getFloorY() + ", " + at.getFloorZ();
    }

    private static void sendDelete(Player player, String name) {
        new Confirm(Main.text("ui.points.delTitle"), Main.text("ui.points.delConfirm", name),
                Main.text("ui.points.delete"), Main.text("ui.common.cancel"))
                .onConfirm(p -> {
                    PortalCommand.delPoint(p, name);
                    sendList(p);
                })
                .onCancel(p -> sendDetail(p, name))
                .show(player);
    }
}
