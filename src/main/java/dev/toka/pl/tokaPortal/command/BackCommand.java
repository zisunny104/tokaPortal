package dev.toka.pl.tokaPortal.command;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;

import static dev.toka.pl.tokaPortal.utils.PortalHistory.BACK;
import static dev.toka.pl.tokaPortal.utils.PortalHistory.toHistoryLocation;

public class BackCommand extends Command {
    public BackCommand() {
        super("back", Main.text("cmd.back.desc"));
        this.setUsage(Main.text("cmd.back.usage"));
        this.commandParameters.clear();
        this.commandParameters.put("help", new CommandParameter[]{CommandParameter.newEnum("動作", new String[]{"help"})});
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) { Main.help(sender, "back"); return true; }
        if (sender instanceof Player) {
            toHistoryLocation((Player) sender, BACK);
        } else {
            sender.sendMessage(Main.text("err.playerOnly"));
        }
        return false;
    }
}
