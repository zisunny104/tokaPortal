package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandDataVersions;
import cn.nukkit.command.data.CommandParameter;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.tpa.TpaFlow;

public class DndCommand extends Command {

    public DndCommand() {
        super("dnd", Main.text("cmd.dnd.desc"));
        this.setUsage(Main.text("cmd.dnd.usage"));
        commandParameters.clear();
        commandParameters.put("default", new CommandParameter[]{CommandSuggestions.fixed(Main.text("param.action"), true, "TokaPortalDnd", "on", "off")});
    }

    @Override
    public CommandDataVersions generateCustomCommandData(Player player) {
        var data = super.generateCustomCommandData(player);
        return CommandSuggestions.parameters(data, new CommandParameter[]{CommandSuggestions.fixed(Main.text("param.action"), true, "TokaPortalDnd", "on", "off")}, CommandSuggestions.help());
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) { Main.help(sender, "dnd"); return true; }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Main.text("err.playerOnly"));
            return false;
        }
        if (args.length > 0 && !args[0].equalsIgnoreCase("on") && !args[0].equalsIgnoreCase("off")) {
            sender.sendMessage(Main.text("cmd.dnd.usage"));
            return false;
        }
        TpaFlow.dnd(player, args.length > 0 ? args[0] : null);
        return true;
    }
}
