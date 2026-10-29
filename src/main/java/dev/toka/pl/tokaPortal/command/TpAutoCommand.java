package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandDataVersions;
import cn.nukkit.command.data.CommandParameter;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.tpa.TpaFlow;

public class TpAutoCommand extends Command {

    public TpAutoCommand() {
        super("tpauto", Main.text("cmd.tpauto.desc"));
        this.setUsage(Main.text("cmd.tpauto.usage"));
        commandParameters.clear();
        commandParameters.put("default", new CommandParameter[]{choices()});
    }

    private static CommandParameter choices() {
        return CommandSuggestions.fixed(Main.text("param.action"), true, "TokaPortalAuto", "on", "off", "default");
    }

    @Override
    public CommandDataVersions generateCustomCommandData(Player player) {
        return CommandSuggestions.parameters(super.generateCustomCommandData(player), new CommandParameter[]{choices()}, CommandSuggestions.help());
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) { Main.help(sender, "tpauto"); return true; }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Main.text("err.playerOnly"));
            return false;
        }
        if (args.length > 0 && !args[0].equalsIgnoreCase("on") && !args[0].equalsIgnoreCase("off") && !args[0].equalsIgnoreCase("default")) {
            sender.sendMessage(Main.text("cmd.tpauto.usage"));
            return false;
        }
        TpaFlow.auto(player, args.length > 0 ? args[0] : null);
        return true;
    }
}
