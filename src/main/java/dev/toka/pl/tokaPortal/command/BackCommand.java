package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import dev.toka.pl.tokaPortal.Portal;

public class BackCommand extends Command {
    public BackCommand() {
        super("back", "返回前次傳送地點");
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        if (sender.isPlayer()) {
            Player player = (Player) sender;
            Portal.backToLastPortalLocation(player);

            /*//TODO:將要刪除
            PlayerInfo pli = getPlayerInfo(player);
            Location loc = getPlayerInfo(player).getDeathLocation();
            if (loc != null) {
                if (pli.getBossBarLength() >= 50) {
                    callEvent(new PlayerPortalEvent(pli, "目前位置", "前次傳送位置", player.getLocation(), loc));
                    player.teleport(loc);
                    pli.sendText("§a[傳送]§b已返回前次傳送地點");
                    pli.reBossBarLength();
                } else {
                    pli.sendText("§a[傳送]§b請等待冷卻時間結束後, \n再次進行傳送");
                }
            } else {
                pli.sendText("§a[傳送]§b請先進行§e傳送§b(?");
            }*/
        } else {
            sender.sendMessage("§a[傳送]§e請在遊戲內進行§r");
        }

        return false;
    }
}