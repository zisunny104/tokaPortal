package dev.toka.pl.tokaPortal.command;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.Player;
import dev.toka.pl.tokaPortal.Main;
public class WildCommand extends Command {
 public WildCommand(){super("wild","資源世界隨機傳送");this.commandParameters.clear();}
 public boolean execute(CommandSender sender,String label,String[] args){
  if(!(sender instanceof Player player)){sender.sendMessage("[傳送]僅可在遊戲內使用");return true;}
  Main.getInstance().getWildService().execute(player);return true;
 }
}
