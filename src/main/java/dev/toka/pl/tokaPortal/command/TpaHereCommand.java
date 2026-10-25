package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandDataVersions;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.tpa.TpaFlow;

public class TpaHereCommand extends Command {

    public TpaHereCommand() {
        super("tpahere", Main.text("cmd.tpahere.desc"));
        this.setUsage(Main.text("cmd.tpahere.usage"));
        commandParameters.clear();
        commandParameters.put("default", new CommandParameter[]{CommandParameter.newType(Main.text("param.playerName"), false, CommandParamType.STRING)});
    }

    @Override
    public CommandDataVersions generateCustomCommandData(Player player) {
        var data = super.generateCustomCommandData(player);
        var names = CommandSuggestions.playerNames(player);
        var manual = new CommandParameter[]{CommandParameter.newType(Main.text("param.playerName"), false, CommandParamType.STRING)};
        if (names.isEmpty()) return CommandSuggestions.parameters(data, manual, CommandSuggestions.help());
        return CommandSuggestions.parameters(data,
                new CommandParameter[]{CommandSuggestions.names(Main.text("param.playerName"), false, "TokaPortalPlayers", names)},
                new CommandParameter[]{CommandSuggestions.names(Main.text("param.playerName"), false, "TokaPortalPlayers", names), CommandParameter.newEnum("force", true, new String[]{"force"})}, manual, CommandSuggestions.help());
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help") && Server.getInstance().getPlayer(args[0]) == null) { Main.help(sender, "tpahere"); return true; }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Main.text("err.playerOnly"));
            return false;
        }
        if (args.length == 0) {
            sender.sendMessage(Main.text("cmd.tpahere.usage"));
            return false;
        }
        Player target = Server.getInstance().getPlayer(args[0]);
        boolean force = args.length > 1 && args[1].equalsIgnoreCase("force");
        if (target == null) player.sendMessage(Main.text("err.player.notFound", args[0]));
        else TpaFlow.request(player, target, true, force);
        return true;
    }
}
