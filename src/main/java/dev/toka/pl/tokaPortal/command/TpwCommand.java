package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.level.Level;

public class TpwCommand extends Command {

    public TpwCommand() {
        super("tpw", "傳送到世界");
        this.commandParameters.clear();
        this.commandParameters.put("def", new CommandParameter[]{
                new CommandParameter("世界名稱", false)
        });
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        Player player = Server.getInstance().getPlayer(sender.getName());
        if (args.length > 0) {
            Level world = Server.getInstance().getLevelByName(args[0]);
            if (player.isOp()) {
                if (world != null) {
                    player.teleport(world.getSafeSpawn());
                    player.sendMessage("[傳送]已傳送至世界 " + args[0]);
                } else {
                    Server.getInstance().loadLevel(args[0]);
                    world = Server.getInstance().getLevelByName(args[0]);
                    if (world != null) {
                        player.teleport(world.getSafeSpawn());
                        player.sendMessage("[傳送]已載入並傳送至世界 " + args[0]);
                    } else {
                        player.sendMessage("[傳送]世界 " + args[0] + " 無法傳送(錯誤的名稱或是未載入)");
                    }
                }
            } else {
                player.sendMessage("[傳送]權限不足");
            }
        }
        return false;
    }
}
