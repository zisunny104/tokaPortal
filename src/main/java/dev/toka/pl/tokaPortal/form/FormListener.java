package dev.toka.pl.tokaPortal.form;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.plugin.Plugin;

/** Hands the answers of the windows to what was bound to them, and forgets the choices of a player who leaves. */
public final class FormListener implements Listener {
    private FormListener() {
    }

    /** Register once in onEnable. */
    public static void register(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new FormListener(), plugin);
    }

    /** Drops every pending window and choice when the plugin is disabled. */
    public static void clear() {
        Registry.clear();
        Preferences.clear();
    }

    @EventHandler
    public void onRespond(PlayerFormRespondedEvent event) {
        Registry.answer(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Preferences.forget(event.getPlayer().getUniqueId());
        Registry.forget(event.getPlayer());
    }
}
