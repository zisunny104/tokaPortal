package dev.toka.pl.tokaPortal.command;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.Player;
import cn.nukkit.command.data.CommandDataVersions;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.command.data.CommandParamType;

import static dev.toka.pl.tokaPortal.utils.Portal.Tpa;

public class TpaCommand extends Command {

    public TpaCommand() {
        super("tpa", Main.text("cmd.tpa.desc"));
        this.setUsage(Main.text("cmd.tpa.usage"));
        commandParameters.clear();
        commandParameters.put("default", new CommandParameter[]{CommandParameter.newType(Main.text("param.playerName"), CommandParamType.STRING)});
    }

    @Override
    public CommandDataVersions generateCustomCommandData(Player player) {
        var data = super.generateCustomCommandData(player);
        var names = CommandSuggestions.playerNames(player);
        var manual = new CommandParameter[]{CommandParameter.newType(Main.text("param.playerName"), CommandParamType.STRING)};
        if (names.isEmpty()) return CommandSuggestions.parameters(data, manual);
        return CommandSuggestions.parameters(data,
                new CommandParameter[]{CommandSuggestions.names(Main.text("param.playerName"), false, "TokaPortalPlayers", names)}, manual);
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help") && Server.getInstance().getPlayer(args[0]) == null) { Main.help(sender, "tpa"); return true; }
        if (sender.isPlayer()) {
            Player player = (Player) sender;
            if (args.length > 0) {
                Player toPlayer = Server.getInstance().getPlayer(args[0]);
                if (toPlayer != null) {
                    Tpa(player, toPlayer);
                }
            } else {
                sender.sendMessage(Main.text("msg.tpa.usage", Main.text("cmd.tpa.usage")));
            }
        } else {
            sender.sendMessage(Main.text("err.playerOnly"));
        }
        return false;
    }

}
