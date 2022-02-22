package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;

import static dev.toka.pl.tokaPortal.utils.PortalHistory.NEXT;
import static dev.toka.pl.tokaPortal.utils.PortalHistory.toHistoryLocation;

public class NextCommand extends Command {
    public NextCommand() {
        super("next", "前往下個傳送地點");
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        if (sender instanceof Player) {
            toHistoryLocation((Player) sender, NEXT);
        } else {
            sender.sendMessage("§a[傳送]§e請在遊戲內進行§r");
        }
        return false;
    }
}