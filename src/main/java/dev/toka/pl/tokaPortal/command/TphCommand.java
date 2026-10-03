package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.command.data.CommandDataVersions;
import cn.nukkit.command.data.CommandParamType;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration;
import dev.toka.pl.tokaPortal.form.home.HomeListForm;
import dev.toka.pl.tokaPortal.point.HomePoint;
import dev.toka.pl.tokaPortal.provider.IDataProvider;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.PlayerInfo;

import static dev.toka.pl.tokaPortal.Main.getProvider;
import static dev.toka.pl.tokaPortal.utils.Portal.toPoint;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.getPlayerInfo;

public class TphCommand extends Command {

    public TphCommand() {
        super("tph", "傳送至住家");
        this.commandParameters.clear();
        this.commandParameters.put("def", new CommandParameter[]{
                new CommandParameter("住家名稱", true)
        });
    }

    @Override
    public CommandDataVersions generateCustomCommandData(Player player) {
        if (!ZeroIntegration.has(ZeroIntegration.Feature.PLAYERS)
                || !ZeroIntegration.has(ZeroIntegration.Feature.REGIONS)) return null;
        if (!player.isOp() && !getPlayerInfo(player).isTopQuanXian()) return null;
        var data = super.generateCustomCommandData(player);
        var names = CommandSuggestions.homeNames(player);
        var manual = new CommandParameter[]{CommandParameter.newType("住家名稱", true, CommandParamType.STRING)};
        if (names.isEmpty()) return CommandSuggestions.parameters(data, manual);
        return CommandSuggestions.parameters(data,
                new CommandParameter[]{CommandSuggestions.names("住家名稱", true, "TokaPortalHomes", names)}, manual);
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!dev.toka.pl.tokaPortal.integration.ZeroIntegration.require(sender, dev.toka.pl.tokaPortal.integration.ZeroIntegration.Feature.PLAYERS, dev.toka.pl.tokaPortal.integration.ZeroIntegration.Feature.REGIONS)) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("[傳送]請在遊戲內進行");
            return false;
        }
        Player player = (Player) sender;
        PlayerInfo pli = getPlayerInfo(player);
        IDataProvider provider = getProvider();
        if (player.isOp() || pli.isTopQuanXian()) {
            if (args.length > 0) {
                String name = args[0];
                HomePoint home = provider.getHomePoint(name);
                if (home != null) {
                    toPoint(home, player);

                    return true;
                }
                pli.sendText("[傳送]住家 " + name + " 無法傳送(不存在)");
            } else {
                player.showFormWindow(new HomeListForm(player));
            }
        } else {
            player.sendMessage("[傳送]住家功能僅限管理員使用。");
            return false;
        }
        return false;
    }
}
