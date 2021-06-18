package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import dev.toka.pl.tokaPortal.form.HomeDelForm;
import dev.toka.pl.tokaPortal.form.HomeEditListForm;
import dev.toka.pl.tokaPortal.form.HomeListForm;
import dev.toka.pl.tokaPortal.form.HomeSetForm;
import prj.toka.zero.player.PlayerInfo;

import static dev.toka.pl.tokaPortal.Main.getProvider;
import static dev.toka.pl.tokaPortal.utils.Portal.setHome;
import static prj.toka.zero.player.Players.getPlayerInfo;

public class HomeCommand extends Command {

    public HomeCommand() {
        super("home", "住家系統指令");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            PlayerInfo pli = getPlayerInfo(player);
            if (!player.getName().equals("Ethan940114") || pli.isTopQuanXian()) {
                player.sendMessage("[傳送]實驗功能 尚未開發完成!");
                return false;
            }
            if (args.length > 0) {
                switch (args[0].toLowerCase()) {
                    case "set":
                        if (args[1] != null) {
                            setHome(player, args[1]);
                        } else {
                            player.showFormWindow(new HomeSetForm());
                        }
                        break;
                    case "del":
                        if (args[1] != null) {
                            player.showFormWindow(new HomeDelForm(getProvider().getHomePoint(args[1])));
                        } else {
                            player.showFormWindow(new HomeEditListForm(player));
                        }
                        break;
                    case "edit":
                        player.showFormWindow(new HomeEditListForm(player));
                        break;
                    case "list":
                    default:
                        player.showFormWindow(new HomeListForm(player));
                }
            } else {
                player.showFormWindow(new HomeListForm(player));
            }
            return true;
        } else {
            sender.sendMessage("僅供遊戲內使用");
            return false;
        }
    }
}
