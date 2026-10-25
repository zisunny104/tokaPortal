package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.tpa.TpaFlow;

public class TpDenyCommand extends Command {

    public TpDenyCommand() {
        super("tpdeny", Main.text("cmd.tpdeny.desc"));
        this.setUsage(Main.text("cmd.tpdeny.usage"));
        commandParameters.clear();
        commandParameters.put("default", new CommandParameter[0]);
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Main.text("err.playerOnly"));
            return false;
        }
        TpaFlow.deny(player, null);
        return true;
    }
}
