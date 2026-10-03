package dev.toka.pl.tokaPortal.command;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.Player;
import dev.toka.pl.tokaPortal.utils.PortalWindow;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.PlayerInfo;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.teleportPoint;

import static dev.toka.pl.tokaPortal.utils.Portal.toPoint;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.getPlayerInfo;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.teleportPoint.getPoint;

public class TppCommand extends Command {
    public TppCommand() {
        super("tpp", "顯示傳送列表");
        this.commandParameters.clear();
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!dev.toka.pl.tokaPortal.integration.ZeroIntegration.require(sender, dev.toka.pl.tokaPortal.integration.ZeroIntegration.Feature.POINTS)) return false;
        if (!(sender instanceof Player)) { sender.sendMessage("[傳送]請在遊戲內進行"); return false; }
        Player player = (Player) sender;
        PlayerInfo pli = getPlayerInfo(player);
        if (args.length > 0) {
            String name = args[0];
            teleportPoint point = getPoint(name);
            if (point != null) {
                toPoint(name, player);
                pli.sendText("§a[傳送]§b已傳送至傳送點 §f" + name + " ( " + point.getInfo() + " §f)");
            } else {
                pli.sendText("[傳送]傳送點 " + name + " 無法傳送");
            }
        } else {
            PortalWindow.sendPortalMainWindow(player);
        }
        return false;
    }
}
