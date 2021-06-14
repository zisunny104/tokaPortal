package dev.toka.pl.tokaPortal.command;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.Player;
import dev.toka.pl.tokaPortal.utils.Portal;
import prj.toka.zero.player.PlayerInfo;

import static prj.toka.zero.player.Players.getPlayerInfo;

public class SpawnCommand extends Command {

    public SpawnCommand() {
        super("spawn", "回到重生點");
        this.commandParameters.clear();
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (sender.isPlayer()) {
            Player player = (Player) sender;
            PlayerInfo pli = getPlayerInfo(player);
            Portal.toPoint("spawn", player);
            pli.sendText("§a[傳送]§b已傳送至重生點 §f");
        } else {
            sender.sendMessage("[傳送]請在遊戲內進行");
        }
        return false;
    }
}