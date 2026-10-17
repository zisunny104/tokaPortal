package dev.toka.pl.tokaPortal.command;

import dev.toka.pl.tokaPortal.Main;
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
        super("tph", Main.text("cmd.tph.desc"));
        this.setUsage(Main.text("cmd.tph.usage"));
        this.commandParameters.clear();
        this.commandParameters.put("def", new CommandParameter[]{
                new CommandParameter(Main.text("param.homeName"), true)
        });
    }

    @Override
    public CommandDataVersions generateCustomCommandData(Player player) {
        if (!ZeroIntegration.has(ZeroIntegration.Feature.PLAYERS)
                || !ZeroIntegration.has(ZeroIntegration.Feature.REGIONS)) return null;
        if (!player.isOp() && !getPlayerInfo(player).isTopQuanXian()) return null;
        var data = super.generateCustomCommandData(player);
        var names = CommandSuggestions.homeNames(player);
        var manual = new CommandParameter[]{CommandParameter.newType(Main.text("param.homeName"), true, CommandParamType.STRING)};
        if (names.isEmpty()) return CommandSuggestions.parameters(data, manual);
        return CommandSuggestions.parameters(data,
                new CommandParameter[]{CommandSuggestions.names(Main.text("param.homeName"), true, "TokaPortalHomes", names)}, manual);
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help") && getProvider().getHomePoint(args[0]) == null) { Main.help(sender, "tph"); return true; }
        if (!dev.toka.pl.tokaPortal.integration.ZeroIntegration.require(sender, dev.toka.pl.tokaPortal.integration.ZeroIntegration.Feature.PLAYERS, dev.toka.pl.tokaPortal.integration.ZeroIntegration.Feature.REGIONS)) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage(Main.text("err.playerOnly"));
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
                pli.sendText(Main.text("err.home.notFound", name));
            } else {
                player.showFormWindow(new HomeListForm(player));
            }
        } else {
            player.sendMessage(Main.text("err.homeAdminOnly"));
            return false;
        }
        return false;
    }
}
