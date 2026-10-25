package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.tpa.TpaFlow;

public class TpAcceptCommand extends Command {

    public TpAcceptCommand() {
        super("tpaccept", Main.text("cmd.tpaccept.desc"));
        this.setUsage(Main.text("cmd.tpaccept.usage"));
        commandParameters.clear();
        commandParameters.put("default", new CommandParameter[0]);
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Main.text("err.playerOnly"));
            return false;
        }
        TpaFlow.accept(player, null);
        return true;
    }
}
