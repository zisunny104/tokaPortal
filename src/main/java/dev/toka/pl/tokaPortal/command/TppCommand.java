package dev.toka.pl.tokaPortal.command;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.command.data.CommandDataVersions;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.Player;
import dev.toka.pl.tokaPortal.utils.PortalWindow;
import dev.toka.pl.tokaPortal.integration.Services.PlayerInfo;
import dev.toka.pl.tokaPortal.point.teleportPoint;

import static dev.toka.pl.tokaPortal.utils.Portal.toPoint;
import static dev.toka.pl.tokaPortal.integration.Services.getPlayerInfo;
import static dev.toka.pl.tokaPortal.point.teleportPoint.getPoint;

public class TppCommand extends Command {
    public TppCommand() {
        super("tpp", Main.text("cmd.tpp.desc"));
        this.setUsage(Main.text("cmd.tpp.usage"));
        this.commandParameters.clear();
        this.commandParameters.put("def", new CommandParameter[]{CommandParameter.newType(Main.text("param.pointName"), true, CommandParamType.STRING)});
    }

    @Override
    public CommandDataVersions generateCustomCommandData(Player player) {
        var data=super.generateCustomCommandData(player);
        var names=teleportPoint.getPoints().keySet().stream().map(String::valueOf).sorted().toList();
        var manual=new CommandParameter[]{CommandParameter.newType(Main.text("param.pointName"),true,CommandParamType.STRING)};
        if(names.isEmpty())return CommandSuggestions.parameters(data,manual,CommandSuggestions.help());
        return CommandSuggestions.parameters(data,new CommandParameter[]{CommandSuggestions.names(Main.text("param.pointName"),true,"TokaPortalPoints",names)},manual,CommandSuggestions.help());
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help") && getPoint(args[0]) == null) { Main.help(sender, "tpp"); return true; }

        if (!(sender instanceof Player)) { sender.sendMessage(Main.text("err.playerOnly")); return false; }
        Player player = (Player) sender;
        PlayerInfo pli = getPlayerInfo(player);
        if (args.length > 0) {
            String name = args[0];
            teleportPoint point = getPoint(name);
            if (point != null) {
                toPoint(name, player);
                pli.sendText(Main.text("msg.point.arrived", name, point.getInfo()));
            } else {
                pli.sendText(Main.text("err.point.cannotTeleport", name));
            }
        } else {
            PortalWindow.sendPortalMainWindow(player);
        }
        return false;
    }
}
