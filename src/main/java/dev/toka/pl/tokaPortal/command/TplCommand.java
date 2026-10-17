package dev.toka.pl.tokaPortal.command;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.Player;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.PlayerInfo;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.Land;

import static dev.toka.pl.tokaPortal.utils.PortalWindow.sendPortalLandListWindow;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.getPlayerInfo;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.Land.getLand;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.Land.toLand;

public class TplCommand extends Command {

    public TplCommand() {
        super("tpl", Main.text("cmd.tpl.desc"));
        this.setUsage(Main.text("cmd.tpl.usage"));
        this.commandParameters.clear();
        this.commandParameters.put("def", new CommandParameter[]{
                new CommandParameter(Main.text("param.landName"), false)
        });
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help") && getLand(args[0]) == null) { Main.help(sender, "tpl"); return true; }
        if (!dev.toka.pl.tokaPortal.integration.ZeroIntegration.require(sender, dev.toka.pl.tokaPortal.integration.ZeroIntegration.Feature.LAND)) return false;
        if (!sender.isPlayer()) {
            sender.sendMessage(Main.text("err.playerOnly"));
            return false;
        }
        Player player = (Player) sender;
        PlayerInfo pli = getPlayerInfo(player);
        if (args.length > 0) {
            String name = args[0];
            Land land = getLand(name);
            if (land != null) {
                toLand(land, player);
                pli.sendText(Main.text("msg.land.arrived", name));
                return true;
            }
            pli.sendText(Main.text("err.land.notFound", name));
        } else {
            sendPortalLandListWindow(player);
        }
        return false;
    }
}
