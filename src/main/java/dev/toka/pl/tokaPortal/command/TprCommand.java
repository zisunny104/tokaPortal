package dev.toka.pl.tokaPortal.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandDataVersions;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.integration.Services;
import dev.toka.pl.tokaPortal.integration.Services.Region;
import dev.toka.pl.tokaPortal.utils.Portal;

import java.util.List;
import java.util.stream.Collectors;

/** The command form of the tpr sign: go to the teleport point of a region, named by its code or by the name players read. */
public class TprCommand extends Command {

    public TprCommand() {
        super("tpr", Main.text("cmd.tpr.desc"));
        this.setUsage(Main.text("cmd.tpr.usage"));
        commandParameters.clear();
        commandParameters.put("default", new CommandParameter[]{CommandParameter.newType(Main.text("param.regionName"), false, CommandParamType.STRING)});
    }

    private static List<String> codes() {
        return Region.getRegions().values().stream().map(Region::getName).sorted().toList();
    }

    @Override
    public CommandDataVersions generateCustomCommandData(Player player) {
        var data = super.generateCustomCommandData(player);
        var manual = new CommandParameter[]{CommandParameter.newType(Main.text("param.regionName"), CommandParamType.STRING)};
        var names = codes();
        if (names.isEmpty()) return CommandSuggestions.parameters(data, manual, CommandSuggestions.help());
        return CommandSuggestions.parameters(data,
                new CommandParameter[]{CommandSuggestions.names(Main.text("param.regionName"), false, "TokaPortalRegions", names)}, manual, CommandSuggestions.help());
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) { Main.help(sender, "tpr"); return true; }
        if (!(sender instanceof Player player)) { sender.sendMessage(Main.text("err.playerOnly")); return false; }
        if (args.length == 0) { sender.sendMessage(Main.text("cmd.tpr.usage")); return false; }
        if (!Services.require(player, Services.Feature.REGIONS)) return false;
        String wanted = String.join(" ", args).strip();
        Region found = null;
        for (Region region : Region.getRegions().values()) {
            if (region.getName().equalsIgnoreCase(wanted) || region.getPlainInfo().equalsIgnoreCase(wanted)) { found = region; break; }
        }
        if (found == null) found = Region.getRegion(wanted);
        if (found == null) {
            String available = Region.getRegions().values().stream().map(Region::getPlainInfo).sorted().collect(Collectors.joining("、"));
            player.sendMessage(Main.text("err.regionUnknown", wanted, available.isEmpty() ? "-" : available));
            return false;
        }
        Portal.toRegion(found, player);
        return true;
    }
}
