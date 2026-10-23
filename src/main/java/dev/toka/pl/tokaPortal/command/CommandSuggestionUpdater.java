package dev.toka.pl.tokaPortal.command;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.level.LevelLoadEvent;
import cn.nukkit.event.level.LevelUnloadEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import dev.toka.pl.tokaPortal.Main;

public final class CommandSuggestionUpdater implements Listener {
    /** Called when the plugin is disabled; the cached world folders must not survive a reload. */
    public static void reset() { CommandSuggestions.reset(); }

    private boolean queued;

    private void refresh() {
        if (queued) return;
        queued = true;
        Main plugin = Main.getInstance();
        plugin.getServer().getScheduler().scheduleDelayedTask(plugin, () -> {
            queued = false;
            plugin.refreshCommandData();
        }, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) { refresh(); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) { refresh(); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoad(LevelLoadEvent event) { refresh(); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUnload(LevelUnloadEvent event) { refresh(); }
}
