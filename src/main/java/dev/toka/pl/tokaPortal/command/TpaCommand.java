package dev.toka.pl.tokaPortal.command;

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
        super("tpa", "普通權限傳送");
        commandParameters.clear();
        commandParameters.put("default", new CommandParameter[]{CommandParameter.newType("玩家名稱", CommandParamType.STRING)});
    }

    @Override
    public CommandDataVersions generateCustomCommandData(Player player) {
        var data = super.generateCustomCommandData(player);
        var names = CommandSuggestions.playerNames(player);
        var manual = new CommandParameter[]{CommandParameter.newType("玩家名稱", CommandParamType.STRING)};
        if (names.isEmpty()) return CommandSuggestions.parameters(data, manual);
        return CommandSuggestions.parameters(data,
                new CommandParameter[]{CommandSuggestions.names("玩家名稱", false, "TokaPortalPlayers", names)}, manual);
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (sender.isPlayer()) {
            Player player = (Player) sender;
            if (args.length > 0) {
                Player toPlayer = Server.getInstance().getPlayer(args[0]);
                if (toPlayer != null) {
                    Tpa(player, toPlayer);
                }
            } else {
                sender.sendMessage("§a[傳送]§b用法: §e'/tpa <玩家代號>'§r");
            }
        } else {
            sender.sendMessage("§a[傳送]§e請在遊戲內進行§r");
        }
        return false;
    }

}
