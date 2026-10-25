package dev.toka.pl.tokaPortal.tpa;

import cn.nukkit.Player;
import cn.nukkit.Server;

/** A line at the top of the screen: tokaDisplay when it is there (the way the restart countdown does it), else the action bar. */
final class TopBar {
    private TopBar() {
    }

    /**
     * The colour of the bar for the seconds left: green while there is time, yellow, then red; in the last five seconds it blinks once a
     * second (the bar is redrawn every second). A forced teleport is purple and blinks between red and purple, so it is never mistaken
     * for a question.
     */
    static String color(boolean forced, int left, int total) {
        boolean blink = left <= 5 && left % 2 == 0;
        if (forced) return blink ? "RED" : "PURPLE";
        if (blink) return "WHITE";
        float share = total <= 0 ? 0f : (float) left / total;
        return share > 0.5f ? "GREEN" : share > 0.25f ? "YELLOW" : "RED";
    }

    /** The line stays two seconds unless replaced; fill is how full the bar is, 0 to 1; colour is a bar colour name or null for the default. */
    static void show(Player player, String line, float fill, String color) {
        var display = Server.getInstance().getPluginManager().getPlugin("tokaDisplay");
        if (display != null && display.isEnabled()) {
            try {
                Class.forName("dev.toka.display.Main", true, display.getClass().getClassLoader())
                        .getMethod("flash", Player.class, String.class, int.class, float.class, String.class).invoke(null, player, line, 2, fill, color);
                return;
            } catch (ReflectiveOperationException | LinkageError older) { /* action bar below */ }
        }
        player.sendActionBar(line);
    }
}
