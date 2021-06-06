package dev.toka.pl.tokaPortal.command;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.Position;
import cn.nukkit.Player;
import prj.toka.zero.player.PlayerInfo;
import prj.toka.zero.utils.Utils;

import static prj.toka.zero.player.Players.getPlayerInfo;

public class WildCommand extends Command {
    public WildCommand() {
        super("wild", "隨機傳送指令");
        this.commandParameters.clear();
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            PlayerInfo pli = getPlayerInfo(player);
            if (player.getLevel().getName().equals("ZiYuan")) {
                if (pli.getBossBarLength() >= 90) {
                    int plX = (int) player.getPosition().x;
                    int plZ = (int) player.getPosition().z;
                    int x = Utils.rand(plX - 375, plX + 375);
                    int z = Utils.rand(plZ - 375, plZ + 375);
                    int y = player.getLevel().getHighestBlockAt(x, z) + 1;
                    player.teleport(new Position(x, y, z, player.getLevel()));
                    pli.sendText("[傳送]隨機傳送成功");
                    pli.reBossBarLength();
                } else {
                    pli.sendText("[傳送]請等待冷卻時間結束後, 再進行傳送.");
                }
            } else {
                pli.sendText("[傳送]傳送失敗 僅可在資源區使用");
            }
        } else {
            sender.sendMessage("[傳送]僅可在遊戲內使用!");
        }

        return false;
    }
}
