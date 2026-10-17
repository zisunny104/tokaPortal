package dev.toka.pl.tokaPortal.command;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.Player;
import dev.toka.pl.tokaPortal.Main;
public class WildCommand extends Command {
 public WildCommand(){super("wild",Main.text("cmd.wild.desc"));this.setUsage(Main.text("cmd.wild.usage"));this.commandParameters.clear();}
 public boolean execute(CommandSender sender,String label,String[] args){
  if(args.length>0&&args[0].equalsIgnoreCase("help")){Main.help(sender,"wild");return true;}
  if(!(sender instanceof Player player)){sender.sendMessage(Main.text("err.playerOnly"));return true;}
  Main.getInstance().getWildService().execute(player);return true;
 }
}
