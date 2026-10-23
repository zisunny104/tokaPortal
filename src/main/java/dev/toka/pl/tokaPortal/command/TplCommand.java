package dev.toka.pl.tokaPortal.command;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.command.data.CommandDataVersions;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.Player;
import dev.toka.pl.tokaPortal.integration.LandIntegration;
import dev.toka.pl.tokaPortal.integration.Services.PlayerInfo;

import static dev.toka.pl.tokaPortal.utils.PortalWindow.sendPortalLandListWindow;
import static dev.toka.pl.tokaPortal.integration.Services.getPlayerInfo;

public class TplCommand extends Command {

    public TplCommand() {
        super("tpl", Main.text("cmd.tpl.desc"));
        this.setUsage(Main.text("cmd.tpl.usage"));
        this.commandParameters.clear();
        this.commandParameters.put("def", new CommandParameter[]{
                new CommandParameter(Main.text("param.landNameOrId"), false)
        });
    }

    @Override
    public CommandDataVersions generateCustomCommandData(Player player) {
        var data=super.generateCustomCommandData(player);
        var names=new java.util.TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        LandIntegration.targets(player).ifPresent(targets->targets.forEach(target->{names.add(target.name());names.add(target.id());}));
        var manual=new CommandParameter[]{CommandParameter.newType(Main.text("param.landNameOrId"),false,CommandParamType.STRING)};
        if(names.isEmpty())return CommandSuggestions.parameters(data,manual);
        return CommandSuggestions.parameters(data,
                new CommandParameter[]{CommandSuggestions.names(Main.text("param.landName"),false,"TokaPortalLands",new java.util.ArrayList<>(names))},manual);
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) { Main.help(sender, "tpl"); return true; }
        if (!sender.isPlayer()) {
            sender.sendMessage(Main.text("err.playerOnly"));
            return false;
        }
        Player player = (Player) sender;
        PlayerInfo pli = getPlayerInfo(player);
        if (args.length > 0) {
            String name = String.join(" ",args).strip();
            LandIntegration.Lookup lookup=LandIntegration.lookup(player,name);
            if("AMBIGUOUS".equals(lookup.status())){player.sendMessage(Main.text("err.land.ambiguous"));return true;}
            if("UNAVAILABLE".equals(lookup.status())){player.sendMessage(Main.text("err.land.unavailable"));return true;}
            if("OK".equals(lookup.status())&&lookup.target()!=null){
                var target=lookup.target();
                if(dev.toka.pl.tokaPortal.utils.Portal.toLandClaim(player,target.id()))
                    pli.sendText(Main.text("msg.land.arrived",target.name()));
                return true;
            }
            pli.sendText(Main.text("err.land.notFound", name));
        } else {
            sendPortalLandListWindow(player);
        }
        return false;
    }
}
