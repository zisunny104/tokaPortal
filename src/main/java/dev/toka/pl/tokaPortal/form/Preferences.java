package dev.toka.pl.tokaPortal.form;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** The choices of the players for the lists, kept in memory until they leave. */
public final class Preferences {
    private static final Map<UUID, View> VIEWS = new ConcurrentHashMap<>();

    private Preferences() {
    }

    public static View get(UUID player) {
        return VIEWS.getOrDefault(player, View.DEFAULT);
    }

    public static void set(UUID player, View view) {
        VIEWS.put(player, view);
    }

    static void clear() {
        VIEWS.clear();
    }

    public static void forget(UUID player) {
        VIEWS.remove(player);
    }
}
