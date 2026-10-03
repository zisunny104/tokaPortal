package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandDataVersions;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.command.data.CommandParamType;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration;
import dev.toka.pl.tokaPortal.form.home.HomeDelForm;
import dev.toka.pl.tokaPortal.form.home.HomeEditListForm;
import dev.toka.pl.tokaPortal.form.home.HomeListForm;
import dev.toka.pl.tokaPortal.form.home.HomeSetForm;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.PlayerInfo;

import static dev.toka.pl.tokaPortal.utils.Portal.setHome;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.getPlayerInfo;

public class HomeCommand extends Command {

    public HomeCommand() {
        super("home", "住家系統指令");
        commandParameters.clear();
    }

    @Override
    public CommandDataVersions generateCustomCommandData(Player player) {
        if (!ZeroIntegration.has(ZeroIntegration.Feature.PLAYERS)
                || !ZeroIntegration.has(ZeroIntegration.Feature.REGIONS)) return null;
        if (!player.isOp() && !getPlayerInfo(player).isTopQuanXian()) return null;
        var data = super.generateCustomCommandData(player);
        var names = CommandSuggestions.homeNames(player);
        var basic = new CommandParameter[]{CommandSuggestions.names("操作", true, "TokaPortalHomeView", java.util.List.of("list", "edit"))};
        var set = new CommandParameter[]{CommandSuggestions.names("操作", false, "TokaPortalHomeSet", java.util.List.of("set")),
                CommandParameter.newType("新住家名稱", true, CommandParamType.STRING)};
        var del = new CommandParameter[]{CommandSuggestions.names("操作", false, "TokaPortalHomeDelete", java.util.List.of("del")),
                CommandParameter.newType("住家名稱", true, CommandParamType.STRING)};
        if (names.isEmpty()) return CommandSuggestions.parameters(data, basic, set, del);
        return CommandSuggestions.parameters(data, basic, set, del,
                new CommandParameter[]{CommandSuggestions.names("操作", false, "TokaPortalHomeDelete", java.util.List.of("del")),
                        CommandSuggestions.names("住家名稱", true, "TokaPortalHomes", names)});
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
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
                                player.showFormWindow(new HomeDelForm(args[1]));
                            } else {
                                player.showFormWindow(new HomeEditListForm(player));
                            }
                            break;
                        case "edit":
                            player.showFormWindow(new HomeEditListForm(player));
                            break;
                        case "list":
                        default:
                            player.showFormWindow(new HomeListForm(player));
                    }
                } else {
                    player.showFormWindow(new HomeListForm(player));
                }
                return true;
            } else {
                player.sendMessage("[傳送]住家功能僅限管理員使用。");
                return false;
            }
        } else {
            sender.sendMessage("僅供遊戲內使用");
            return false;
        }
    }
}
