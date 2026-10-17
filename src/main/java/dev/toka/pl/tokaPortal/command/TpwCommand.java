package dev.toka.pl.tokaPortal.command;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.command.data.CommandDataVersions;
import cn.nukkit.level.Level;

public class TpwCommand extends Command {

    public TpwCommand() {
        super("tpw", Main.text("cmd.tpw.desc"));
        this.setUsage(Main.text("cmd.tpw.usage"));
        this.commandParameters.clear();
        this.commandParameters.put("def", new CommandParameter[]{
                new CommandParameter(Main.text("param.worldName"), false)
        });
    }

    @Override
    public CommandDataVersions generateCustomCommandData(Player player) {
        if (!player.isOp()) return null;
        var data = super.generateCustomCommandData(player);
        var names = CommandSuggestions.worldNames();
        var manual = new CommandParameter[]{CommandParameter.newType(Main.text("param.worldName"), cn.nukkit.command.data.CommandParamType.STRING)};
        if (names.isEmpty()) return CommandSuggestions.parameters(data, manual);
        return CommandSuggestions.parameters(data,
                new CommandParameter[]{CommandSuggestions.names(Main.text("param.worldName"), false, "TokaPortalWorlds", names)}, manual);
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help") && Server.getInstance().getLevelByName(args[0]) == null) { Main.help(sender, "tpw"); return true; }
        if (!(sender instanceof Player)) { sender.sendMessage(Main.text("err.playerOnly")); return false; }
        Player player = (Player) sender;
        if (args.length > 0) {
            Level world = Server.getInstance().getLevelByName(args[0]);
            if (player.isOp()) {
                if (world != null) {
                    player.teleport(world.getSafeSpawn());
                    player.sendMessage(Main.text("msg.world.arrived", args[0]));
                } else {
                    Server.getInstance().loadLevel(args[0]);
                    world = Server.getInstance().getLevelByName(args[0]);
                    if (world != null) {
                        player.teleport(world.getSafeSpawn());
                        player.sendMessage(Main.text("msg.world.loadedArrived", args[0]));
                    } else {
                        player.sendMessage(Main.text("err.world.cannotTeleport", args[0]));
                    }
                }
            } else {
                player.sendMessage(Main.text("err.noPermission"));
            }
        }
        return false;
    }
}
