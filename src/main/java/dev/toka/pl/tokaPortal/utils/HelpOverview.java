package dev.toka.pl.tokaPortal.utils;

import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.Plugin;
import dev.toka.i18n.Pager;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.integration.Services;
import dev.toka.pl.tokaPortal.integration.Services.Feature;
import dev.toka.pl.tokaPortal.integration.LandIntegration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The list behind /portal help: every teleport command, whether the features it needs are on, and the commands of the
 * other toka plugins that are installed, in pages. The text of each command is the first usage line of its own help.
 */
public final class HelpOverview {
    private record Entry(String command, int line, Feature... needs) {
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry("portal", 1), new Entry("spawn", 1), new Entry("back", 1), new Entry("next", 1), new Entry("wild", 1), new Entry("tpa", 1),
            new Entry("tpp", 1), new Entry("tpl", 1), new Entry("home", 1, Feature.PLAYERS, Feature.REGIONS), new Entry("tph", 1, Feature.PLAYERS, Feature.REGIONS),
            new Entry("tpw", 1), new Entry("portal", 2), new Entry("portal", 3), new Entry("portal", 4), new Entry("portal", 5),
            new Entry("portal", 6), new Entry("portal", 7));

    /** The key after help.overview.other. and the name of the plugin that has the commands. */
    private static final String[][] OTHERS = {{"worlds", "tokaWorlds"}, {"sign", "tokaSigns"}};

    private HelpOverview() {
    }

    public static void send(CommandSender sender, String[] args) {
        List<String> lines = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            List<String> help = Main.lines("help." + entry.command(), Main.helpArguments(entry.command()));
            if (entry.line() >= help.size()) {
                continue;
            }
            String text = help.get(entry.line());
            String missing = java.util.Arrays.stream(entry.needs()).filter(feature -> !Services.has(feature))
                    .map(feature -> Main.text("status.feature." + feature.name())).collect(Collectors.joining("、"));
            if(entry.command().equals("tpl")&&!LandIntegration.available())missing=Main.text("help.overview.needs","tokaLand");
            lines.add(missing.isEmpty() ? text : text + " " + Main.text("help.overview.needs", missing));
        }
        for (String[] other : OTHERS) {
            Plugin plugin = Main.getInstance().getServer().getPluginManager().getPlugin(other[1]);
            if (plugin != null && plugin.isEnabled()) {
                lines.add(Main.text("help.overview.other." + other[0]));
            }
        }
        int perPage = Math.max(3, Main.getInstance().getConfig().getInt("help-lines-per-page", 8));
        Pager.Page<String> page = Pager.of(lines, perPage, Pager.parse(args.length > 1 ? args[1] : null, 1));
        sender.sendMessage(Main.text("help.overview.title"));
        for (String line : page.lines()) {
            sender.sendMessage(line);
        }
        if (page.pages() > 1) {
            sender.sendMessage(Main.text("ui.page.footer", page.page(), page.pages(), "/portal help"));
        }
    }
}
