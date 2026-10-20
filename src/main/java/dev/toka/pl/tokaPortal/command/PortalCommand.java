package dev.toka.pl.tokaPortal.command;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.Location;
import dev.toka.pl.tokaPortal.utils.PortalWindow;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.PlayerInfo;
import dev.toka.pl.tokaPortal.point.teleportPoint;

import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.getPlayerInfo;
import static dev.toka.pl.tokaPortal.point.teleportPoint.*;

public class PortalCommand extends Command {

    public PortalCommand() {
        super("portal", Main.text("cmd.portal.desc"));
        this.setUsage(Main.text("cmd.portal.usage"));
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if(Main.getInstance().planCommand(sender,args))return true;
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) { dev.toka.pl.tokaPortal.utils.HelpOverview.send(sender, args); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("legacy")) { legacy(sender, args); return true; }
        if (sender.isPlayer()) {
            Player player = (Player) sender;
            PlayerInfo pli = getPlayerInfo(player);
            String pl = player.getName();
            if (args.length > 0) {
                switch (args[0].toLowerCase()) {
                    case "tpp":
                    case "point": {
                        if (!player.hasPermission("tokaPortal.points.manage")) { player.sendMessage(Main.text("err.noPermission")); return false; }
                        if (args.length < 2) { player.sendMessage(Main.text("msg.point.usage")); return false; }
                        if (args[1] != null) {
                            switch (args[1].toLowerCase()) {
                                case "add":
                                case "set": {
                                    if (args.length > 2) {
                                        String name = args[2];
                                        String type = "point";
                                        String info = name;
                                        Location location = player.getLocation();
                                        if (args.length > 3) {
                                            type = args[3];
                                        }

                                        if (!dev.toka.pl.tokaPortal.utils.Portal.isValidName(name) || !dev.toka.pl.tokaPortal.utils.Portal.isValidName(type)) {
                                            pli.sendText(Main.text("err.point.nameInvalid"));
                                            return false;
                                        }
                                        if (dev.toka.pl.tokaPortal.utils.Portal.isReservedName(name)) {
                                            pli.sendText(Main.text("err.point.reservedName", name));
                                            return false;
                                        }
                                        if (getPoint(name) == null) {
                                            boolean created;
                                            try {
                                                created = setTeleportPoint(name, pl, player.getUniqueId(), type, info, location);
                                            } catch (RuntimeException e) {
                                                Main.getInstance().getLogger().error(Main.text("log.points.saveFailed"), e);
                                                created = false;
                                            }
                                            if (!created) {
                                                player.sendMessage(Main.text("err.point.createDisabled"));
                                                return false;
                                            }
                                            pli.sendText(Main.text("msg.point.created", name));
                                        } else {
                                            pli.sendText(Main.text("err.point.duplicate"));
                                        }
                                    } else {
                                        pli.sendText(Main.text("err.point.nameInvalid"));
                                    }
                                    break;
                                }

                                case "del": {
                                    if (args.length > 2) {
                                        String name = args[2];
                                        teleportPoint point = getPoint(name);
                                        if (point != null) {
                                            if (point.isCreator(player) || player.isOp()) {
                                                boolean deleted;
                                                try {
                                                    deleted = delTeleportPoint(point);
                                                } catch (RuntimeException e) {
                                                    Main.getInstance().getLogger().error(Main.text("log.points.saveFailed"), e);
                                                    deleted = false;
                                                }
                                                if (!deleted) {
                                                    player.sendMessage(Main.text("err.point.deleteDisabled"));
                                                    return false;
                                                }
                                                pli.sendText(Main.text("msg.point.deleted", name));
                                            } else {
                                                pli.sendText(Main.text("err.point.deleteDenied"));
                                            }
                                        } else {
                                            pli.sendText(Main.text("err.point.deleteNotFound"));
                                        }
                                    } else {
                                        pli.sendText(Main.text("err.point.deleteNameInvalid"));
                                    }
                                    break;
                                }
                            }
                        }
                        break;
                    }

                    case "server": {
                        player.sendMessage(Main.text("msg.portal.serverUnavailable"));
                        break;
                    }

                    case "help": {
                        dev.toka.pl.tokaPortal.utils.HelpOverview.send(sender, args);
                        break;
                    }
                }
            } else {
                PortalWindow.sendPortalMainWindow(player);
            }
            return true;
        } else {
            sender.sendMessage(Main.text("err.playerOnly"));
            return false;
        }
    }

    /** 住家或傳送點的 legacy 名稱群組，供清單合併顯示。 */
    private record LegacyLine(String name, int count, boolean disputed, boolean point) {}

    /** /portal legacy list [頁] | claim <名稱> <玩家>：管理尚未對應 UUID 的舊名稱住家與傳送點（OP，console 可用）。 */
    private void legacy(CommandSender sender, String[] args) {
        if (!sender.isOp()) { sender.sendMessage(Main.text("err.noPermission")); return; }
        var provider = Main.getProvider();
        String sub = args.length > 1 ? args[1].toLowerCase() : "";
        if (provider == null || (!sub.equals("list") && !sub.equals("claim"))) { sender.sendMessage(Main.text("msg.legacy.usage")); return; }
        var points = Main.getInstance().getPublicPoints();
        if (sub.equals("list")) {
            var groups = new java.util.ArrayList<LegacyLine>();
            for (var group : provider.legacyGroups()) groups.add(new LegacyLine(group.name(), group.homes(), group.disputed(), false));
            if (points != null) for (var group : points.legacyGroups()) groups.add(new LegacyLine(group.name(), group.homes(), group.disputed(), true));
            groups.sort(java.util.Comparator.comparing((LegacyLine line) -> line.name().toLowerCase(java.util.Locale.ROOT)).thenComparing(LegacyLine::point));
            if (groups.isEmpty()) { sender.sendMessage(Main.text("msg.legacy.empty")); return; }
            var page = dev.toka.i18n.Pager.of(groups, 10, dev.toka.i18n.Pager.parse(args.length > 2 ? args[2] : null, 1));
            sender.sendMessage(Main.text("msg.legacy.header", groups.size()));
            for (var group : page.lines()) {
                sender.sendMessage(Main.text(group.disputed() ? "msg.legacy.lineDisputed" : "msg.legacy.line", group.name(), group.count(),
                        Main.text(group.point() ? "msg.legacy.typePoint" : "msg.legacy.typeHome")));
            }
            if (page.pages() > 1) sender.sendMessage(Main.text("ui.page.footer", page.page(), page.pages(), "/portal legacy list"));
            return;
        }
        if (args.length != 4) { sender.sendMessage(Main.text("msg.legacy.usage")); return; }
        String legacyName = args[2], target = args[3];
        Main.getInstance().getOwnerMigration().resolve(target, found -> {
            var current = Main.getProvider();
            if (current == null) return;
            if (found.isEmpty()) { sender.sendMessage(Main.text("err.player.notFound", target)); return; }
            String shown = Main.getInstance().getServer().getOnlinePlayers().values().stream()
                    .filter(online -> online.getUniqueId().equals(found.get())).map(Player::getName).findFirst().orElse(target);
            int homes = current.claimLegacy(legacyName, found.get(), shown);
            var currentPoints = Main.getInstance().getPublicPoints();
            int claimedPoints = currentPoints == null ? 0 : currentPoints.claimLegacy(legacyName, found.get(), shown);
            sender.sendMessage(homes + claimedPoints > 0 ? Main.text("msg.legacy.claimed", legacyName, shown, homes, claimedPoints) : Main.text("err.legacy.notFound", legacyName));
        });
    }
}
