package dev.toka.pl.tokaPortal.command;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.Player;
import dev.toka.pl.tokaPortal.utils.PortalWindow;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.PlayerInfo;
import dev.toka.pl.tokaPortal.point.teleportPoint;

import static dev.toka.pl.tokaPortal.utils.Portal.toPoint;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.getPlayerInfo;
import static dev.toka.pl.tokaPortal.point.teleportPoint.getPoint;

public class TppCommand extends Command {
    public TppCommand() {
        super("tpp", Main.text("cmd.tpp.desc"));
        this.setUsage(Main.text("cmd.tpp.usage"));
        this.commandParameters.clear();
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help") && getPoint(args[0]) == null) { Main.help(sender, "tpp"); return true; }

        if (!(sender instanceof Player)) { sender.sendMessage(Main.text("err.playerOnly")); return false; }
        Player player = (Player) sender;
        PlayerInfo pli = getPlayerInfo(player);
        if (args.length > 0) {
            String name = args[0];
            teleportPoint point = getPoint(name);
            if (point != null) {
                toPoint(name, player);
                pli.sendText(Main.text("msg.point.arrived", name, point.getInfo()));
            } else {
                pli.sendText(Main.text("err.point.cannotTeleport", name));
            }
        } else {
            PortalWindow.sendPortalMainWindow(player);
        }
        return false;
    }
}
