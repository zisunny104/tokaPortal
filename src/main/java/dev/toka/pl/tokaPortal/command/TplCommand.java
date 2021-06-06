package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.Player;
import prj.toka.zero.player.PlayerInfo;
import prj.toka.zero.ser.land.Land;

import static dev.toka.pl.tokaPortal.PortalWindow.sendPortalLandListWindow;
import static prj.toka.zero.player.Players.getPlayerInfo;
import static prj.toka.zero.ser.land.Land.getLand;
import static prj.toka.zero.ser.land.Land.toLand;

public class TplCommand extends Command {

    public TplCommand() {
        super("tpl", "傳送至領地");
        this.commandParameters.clear();
        this.commandParameters.put("def", new CommandParameter[]{
                new CommandParameter("領地名稱", false)
        });
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!sender.isPlayer()) {
            sender.sendMessage("[傳送]請在遊戲內進行");
        }
        Player player = Server.getInstance().getPlayer(sender.getName());
        PlayerInfo pli = getPlayerInfo(player);
        if (args.length > 0) {
            String name = args[0];
            Land land = getLand(name);
            if (land != null) {
                toLand(land, player);
                pli.sendText("[傳送]已傳送至領地 " + name);
                return true;
            }
            pli.sendText("[傳送]領地 " + name + " 無法傳送(不存在)");
        } else {
            sendPortalLandListWindow(player);
        }
        return false;
    }
}
