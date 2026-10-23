package dev.toka.pl.tokaPortal.command;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.Player;
import dev.toka.pl.tokaPortal.utils.Portal;
import dev.toka.pl.tokaPortal.integration.Services.PlayerInfo;

import static dev.toka.pl.tokaPortal.integration.Services.getPlayerInfo;

public class SpawnCommand extends Command {

    public SpawnCommand() {
        super("spawn", Main.text("cmd.spawn.desc"));
        this.setUsage(Main.text("cmd.spawn.usage"));
        this.commandParameters.clear();
        this.commandParameters.put("help", new CommandParameter[]{CommandParameter.newEnum(Main.text("param.action"), new String[]{"help"})});
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) { Main.help(sender, "spawn"); return true; }

        if (sender.isPlayer()) {
            Player player = (Player) sender;
            PlayerInfo pli = getPlayerInfo(player);
            Portal.toPoint("spawn", player);
            pli.sendText(Main.text("msg.spawn.arrived"));
        } else {
            sender.sendMessage(Main.text("err.playerOnly"));
        }
        return false;
    }
}
