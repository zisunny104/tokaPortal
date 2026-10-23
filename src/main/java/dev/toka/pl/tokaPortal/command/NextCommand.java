package dev.toka.pl.tokaPortal.command;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;

import static dev.toka.pl.tokaPortal.utils.PortalHistory.NEXT;
import static dev.toka.pl.tokaPortal.utils.PortalHistory.toHistoryLocation;

public class NextCommand extends Command {
    public NextCommand() {
        super("next", Main.text("cmd.next.desc"));
        this.setUsage(Main.text("cmd.next.usage"));
        this.commandParameters.clear();
        this.commandParameters.put("help", new CommandParameter[]{CommandParameter.newEnum("動作", new String[]{"help"})});
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) { Main.help(sender, "next"); return true; }
        if (sender instanceof Player) {
            toHistoryLocation((Player) sender, NEXT);
        } else {
            sender.sendMessage(Main.text("err.playerOnly"));
        }
        return false;
    }
}
