package dev.toka.pl.tokaPortal.tpa;

import cn.nukkit.Player;
import cn.nukkit.Server;

/** A line at the top of the screen: tokaDisplay when it is there (the way the restart countdown does it), else the action bar. */
final class TopBar {
    private TopBar() {
    }

    /** The line stays two seconds unless replaced; fill is how full the bar is, 0 to 1. */
    static void show(Player player, String line, float fill) {
        var display = Server.getInstance().getPluginManager().getPlugin("tokaDisplay");
        if (display != null && display.isEnabled()) {
            try {
                Class.forName("dev.toka.display.Main", true, display.getClass().getClassLoader())
                        .getMethod("flash", Player.class, String.class, int.class, float.class, String.class).invoke(null, player, line, 2, fill, null);
                return;
            } catch (ReflectiveOperationException | LinkageError older) { /* action bar below */ }
        }
        player.sendActionBar(line);
    }
}
