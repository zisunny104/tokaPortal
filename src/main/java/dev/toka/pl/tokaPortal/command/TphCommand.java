package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import dev.toka.pl.tokaPortal.form.HomeListForm;
import dev.toka.pl.tokaPortal.point.HomePoint;
import dev.toka.pl.tokaPortal.provider.IDataProvider;
import prj.toka.zero.player.PlayerInfo;

import static dev.toka.pl.tokaPortal.Main.getProvider;
import static dev.toka.pl.tokaPortal.utils.Portal.toPoint;
import static prj.toka.zero.player.Players.getPlayerInfo;

public class TphCommand extends Command {

    public TphCommand() {
        super("tph", "傳送至領地");
        this.commandParameters.clear();
        this.commandParameters.put("def", new CommandParameter[]{
                new CommandParameter("住家名稱", false)
        });
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("[傳送]請在遊戲內進行");
            return false;
        }
        Player player = (Player) sender;
        PlayerInfo pli = getPlayerInfo(player);
        IDataProvider provider = getProvider();
        if(!player.getName().equals("Ethan940114") || pli.isTopQuanXian()){
            player.sendMessage("[傳送]實驗功能 尚未開發完成!");
            return false;
        }
        if (args.length > 0) {
            String name = args[0];
            HomePoint home = provider.getHomePoint(name);
            if (home != null) {
                toPoint(home, player);
                pli.sendText("[傳送]已傳送至住家 " + name);
                return true;
            }
            pli.sendText("[傳送]住家 " + name + " 無法傳送(不存在)");
        } else {
            player.showFormWindow(new HomeListForm(player));
        }
        return false;
    }
}
