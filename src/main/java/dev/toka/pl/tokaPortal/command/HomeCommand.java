package dev.toka.pl.tokaPortal.command;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandDataVersions;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.command.data.CommandParamType;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration;
import dev.toka.pl.tokaPortal.form.home.HomeWindows;
import dev.toka.pl.tokaPortal.form.home.HomeSetForm;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.PlayerInfo;

import static dev.toka.pl.tokaPortal.utils.Portal.setHome;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.getPlayerInfo;

public class HomeCommand extends Command {

    public HomeCommand() {
        super("home", Main.text("cmd.home.desc"));
        this.setUsage(Main.text("cmd.home.usage"));
        commandParameters.clear();
    }

    @Override
    public CommandDataVersions generateCustomCommandData(Player player) {
        if (!ZeroIntegration.has(ZeroIntegration.Feature.PLAYERS)
                || !ZeroIntegration.has(ZeroIntegration.Feature.REGIONS)) return null;
        if (!player.isOp() && !getPlayerInfo(player).isTopQuanXian()) return null;
        var data = super.generateCustomCommandData(player);
        var names = CommandSuggestions.homeNames(player);
        var basic = new CommandParameter[]{CommandSuggestions.names(Main.text("param.action"), true, "TokaPortalHomeView", java.util.List.of("list", "edit"))};
        var set = new CommandParameter[]{CommandSuggestions.names(Main.text("param.action"), false, "TokaPortalHomeSet", java.util.List.of("set")),
                CommandParameter.newType(Main.text("param.newHomeName"), true, CommandParamType.STRING)};
        var del = new CommandParameter[]{CommandSuggestions.names(Main.text("param.action"), false, "TokaPortalHomeDelete", java.util.List.of("del")),
                CommandParameter.newType(Main.text("param.homeName"), true, CommandParamType.STRING)};
        if (names.isEmpty()) return CommandSuggestions.parameters(data, basic, set, del);
        return CommandSuggestions.parameters(data, basic, set, del,
                new CommandParameter[]{CommandSuggestions.names(Main.text("param.action"), false, "TokaPortalHomeDelete", java.util.List.of("del")),
                        CommandSuggestions.names(Main.text("param.homeName"), true, "TokaPortalHomes", names)});
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) { Main.help(sender, "home"); return true; }
        if (!dev.toka.pl.tokaPortal.integration.ZeroIntegration.require(sender, dev.toka.pl.tokaPortal.integration.ZeroIntegration.Feature.PLAYERS, dev.toka.pl.tokaPortal.integration.ZeroIntegration.Feature.REGIONS)) return false;
        if (sender instanceof Player) {
            Player player = (Player) sender;
            PlayerInfo pli = getPlayerInfo(player);
            if (player.isOp() || pli.isTopQuanXian()) {
                if (args.length > 0) {
                    switch (args[0].toLowerCase()) {
                        case "set":
                            if (args.length == 2) {
                                setHome(player, args[1]);
                            } else {
                                player.showFormWindow(new HomeSetForm());
                            }
                            break;
                        case "del":
                            if (args.length == 2) {
                                HomeWindows.sendDelete(player, args[1]);
                            } else {
                                HomeWindows.sendEditList(player);
                            }
                            break;
                        case "edit":
                            HomeWindows.sendEditList(player);
                            break;
                        case "list":
                        default:
                            HomeWindows.sendList(player);
                    }
                } else {
                    HomeWindows.sendList(player);
                }
                return true;
            } else {
                player.sendMessage(Main.text("err.homeAdminOnly"));
                return false;
            }
        } else {
            sender.sendMessage(Main.text("err.playerOnly"));
            return false;
        }
    }
}
