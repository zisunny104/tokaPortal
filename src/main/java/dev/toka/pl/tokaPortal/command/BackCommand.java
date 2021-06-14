package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;

import static dev.toka.pl.tokaPortal.utils.Portal.backToLastPortalLocation;

public class BackCommand extends Command {
    public BackCommand() {
        super("back", "返回前次傳送地點");
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        if (sender instanceof Player) {
            backToLastPortalLocation((Player) sender);
        } else {
            sender.sendMessage("§a[傳送]§e請在遊戲內進行§r");
        }
        return false;
    }
}