package dev.toka.pl.tokaPortal.command;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandDataVersions;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import dev.toka.pl.tokaPortal.form.PointWindows;
import dev.toka.pl.tokaPortal.utils.PortalWindow;
import dev.toka.pl.tokaPortal.integration.Services.PlayerInfo;
import dev.toka.pl.tokaPortal.point.teleportPoint;

import java.util.ArrayList;
import java.util.List;

import static dev.toka.pl.tokaPortal.integration.Services.getPlayerInfo;
import static dev.toka.pl.tokaPortal.point.teleportPoint.*;

public class PortalCommand extends Command {
    private static final String MANAGE = "tokaPortal.points.manage", ADMIN = "tokaPortal.admin";

    public PortalCommand() {
        super("portal", Main.text("cmd.portal.desc"));
        this.setUsage(Main.text("cmd.portal.usage"));
        this.commandParameters.clear();
        this.commandParameters.put("actions", new CommandParameter[]{CommandSuggestions.fixed(Main.text("param.action"), true, "TokaPortalAction", "help", "server")});
    }

    /** Offers only the words the player may use, so the list of an ordinary player stays short. */
    @Override
    public CommandDataVersions generateCustomCommandData(Player player) {
        var data = super.generateCustomCommandData(player);
        String action = Main.text("param.action"), manageLabel = Main.text("param.manage");
        boolean manage = player.hasPermission(MANAGE), admin = player.hasPermission(ADMIN), op = player.isOp();
        var words = new ArrayList<String>(List.of("help", "server"));
        if (manage) words.add("point");
        if (op) words.add("legacy");
        if (admin) {
            words.add("plan");
            words.add("reload");
        }
        var overloads = new ArrayList<CommandParameter[]>();
        overloads.add(new CommandParameter[]{CommandSuggestions.fixed(action, true, "TokaPortalAction", words.toArray(String[]::new))});
        overloads.add(new CommandParameter[]{CommandSuggestions.fixed(action, false, "TokaPortalHelp", "help"),
                CommandParameter.newType(Main.text("param.page"), true, CommandParamType.STRING)});
        if (manage) {
            var point = CommandSuggestions.fixed(action, false, "TokaPortalPointAction", "point", "tpp");
            overloads.add(new CommandParameter[]{point, CommandSuggestions.fixed(manageLabel, false, "TokaPortalPointManage", "add", "set"),
                    CommandParameter.newType(Main.text("param.pointName"), false, CommandParamType.STRING),
                    CommandParameter.newType(Main.text("param.pointType"), true, CommandParamType.STRING)});
            var del = CommandSuggestions.fixed(manageLabel, false, "TokaPortalPointDelete", "del");
            overloads.add(new CommandParameter[]{point, del, CommandParameter.newType(Main.text("param.pointName"), false, CommandParamType.STRING)});
            var points = CommandSuggestions.pointNames();
            if (!points.isEmpty()) overloads.add(new CommandParameter[]{point, del,
                    CommandSuggestions.names(Main.text("param.pointName"), false, "TokaPortalPoints", points)});
        }
        if (op) {
            var legacy = CommandSuggestions.fixed(action, false, "TokaPortalLegacy", "legacy");
            overloads.add(new CommandParameter[]{legacy, CommandSuggestions.fixed(manageLabel, false, "TokaPortalLegacyList", "list"),
                    CommandParameter.newType(Main.text("param.page"), true, CommandParamType.INT)});
            overloads.add(new CommandParameter[]{legacy, CommandSuggestions.fixed(manageLabel, false, "TokaPortalLegacyClaim", "claim"),
                    CommandParameter.newType(Main.text("param.legacyName"), false, CommandParamType.STRING),
                    CommandParameter.newType(Main.text("param.playerName"), false, CommandParamType.STRING)});
        }
        if (admin) {
            var plan = CommandSuggestions.fixed(action, false, "TokaPortalPlan", "plan");
            overloads.add(new CommandParameter[]{plan, CommandSuggestions.fixed(manageLabel, false, "TokaPortalPlanStatus", "status")});
            overloads.add(new CommandParameter[]{plan, CommandSuggestions.fixed(manageLabel, false, "TokaPortalPlanMode", "mode"),
                    CommandSuggestions.fixed(Main.text("param.mode"), false, "TokaPortalPlanModes", "auto", "enable", "disable")});
            overloads.add(new CommandParameter[]{plan, CommandSuggestions.fixed(manageLabel, false, "TokaPortalPlanFields", "fields"),
                    CommandSuggestions.fixed(Main.text("param.field"), false, "TokaPortalPlanFieldNames", "all", "none", "homes", "public-points")});
        }
        return CommandSuggestions.parameters(data, overloads.toArray(CommandParameter[][]::new));
    }

    /** Creates a public point at the player's position; the command and the window both come here. */
    public static boolean addPoint(Player player, String name, String type) {
        PlayerInfo pli = getPlayerInfo(player);
        if (!dev.toka.pl.tokaPortal.utils.Portal.isValidName(name) || !dev.toka.pl.tokaPortal.utils.Portal.isValidName(type)) {
            pli.sendText(Main.text("err.point.nameInvalid"));
            return false;
        }
        if (dev.toka.pl.tokaPortal.utils.Portal.isReservedName(name)) {
            pli.sendText(Main.text("err.point.reservedName", name));
            return false;
        }
        if (getPoint(name) != null) {
            pli.sendText(Main.text("err.point.duplicate"));
            return false;
        }
        boolean created;
        try {
            created = setTeleportPoint(name, player.getName(), player.getUniqueId(), type, name, player.getLocation());
        } catch (RuntimeException e) {
            Main.getInstance().getLogger().error(Main.text("log.points.saveFailed"), e);
            created = false;
        }
        if (!created) {
            player.sendMessage(Main.text("err.point.createDisabled"));
            return false;
        }
        pli.sendText(Main.text("msg.point.created", name));
        Main.getInstance().refreshCommandData();
        return true;
    }

    /** Deletes a public point when the player made it or is an operator. */
    public static boolean delPoint(Player player, String name) {
        PlayerInfo pli = getPlayerInfo(player);
        teleportPoint point = getPoint(name);
        if (point == null) {
            pli.sendText(Main.text("err.point.deleteNotFound"));
            return false;
        }
        if (!point.isCreator(player) && !player.isOp()) {
            pli.sendText(Main.text("err.point.deleteDenied"));
            return false;
        }
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
        Main.getInstance().refreshCommandData();
        return true;
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission(ADMIN)) { sender.sendMessage(Main.text("err.noPermission")); return; }
        try {
            Main.getInstance().reloadAll();
            sender.sendMessage(Main.text("msg.portal.reloaded"));
        } catch (RuntimeException e) {
            Main.getInstance().getLogger().error(Main.text("log.reloadFailed"), e);
            sender.sendMessage(Main.text("err.portal.reloadFailed"));
        }
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if(Main.getInstance().planCommand(sender,args))return true;
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) { dev.toka.pl.tokaPortal.utils.HelpOverview.send(sender, args); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("legacy")) { legacy(sender, args); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) { reload(sender); return true; }
        if (sender.isPlayer()) {
            Player player = (Player) sender;
            PlayerInfo pli = getPlayerInfo(player);
            if (args.length > 0) {
                switch (args[0].toLowerCase()) {
                    case "tpp":
                    case "point": {
                        if (!player.hasPermission(MANAGE)) { player.sendMessage(Main.text("err.noPermission")); return false; }
                        if (args.length < 2) { PointWindows.open(player); return true; }
                        switch (args[1].toLowerCase()) {
                            case "add":
                            case "set":
                                if (args.length > 2) addPoint(player, args[2], args.length > 3 ? args[3] : "point");
                                else pli.sendText(Main.text("err.point.nameInvalid"));
                                break;
                            case "del":
                                if (args.length > 2) delPoint(player, args[2]);
                                else pli.sendText(Main.text("err.point.deleteNameInvalid"));
                                break;
                            default:
                                player.sendMessage(Main.text("msg.point.usage"));
                        }
                        break;
                    }

                    case "server": {
                        player.sendMessage(Main.text("msg.portal.serverUnavailable"));
                        break;
                    }

                    default:
                        break;
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
