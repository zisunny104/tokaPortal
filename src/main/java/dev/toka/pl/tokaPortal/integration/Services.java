package dev.toka.pl.tokaPortal.integration;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.Event;
import cn.nukkit.level.Location;
import cn.nukkit.plugin.Plugin;
import dev.toka.pl.tokaPortal.Main;

import java.util.*;

/**
 * What teleporting needs to know about the player and the server's regions. Who counts as an administrator or a sponsor is a
 * permission node; regions come from tokaRegions and the city pass from tokaProfiles, reflectively, so none of them is linked into this JAR.
 */
public final class Services {
    public enum Feature { PLAYERS, REGIONS, CITY_PASS }

    public static final String ADMIN = "toka.identity.admin", SPONSOR = "toka.identity.sponsor";
    /** Silent teleport (no notice, invisible after) and forced teleport (the target may stop it) of /tpa. */
    public static final String TPA_SILENT = "tokaportal.tpa.silent", TPA_FORCE = "tokaportal.tpa.force";

    private Services() {}

    private static Plugin running(String name) {
        Plugin plugin = Server.getInstance().getPluginManager().getPlugin(name);
        return plugin != null && plugin.isEnabled() ? plugin : null;
    }

    public static boolean has(Feature feature) {
        return switch (feature) {
            case PLAYERS -> true;
            case REGIONS -> running("tokaRegions") != null;
            case CITY_PASS -> running("tokaProfiles") != null;
        };
    }

    public static boolean require(CommandSender sender, Feature... features) {
        for (Feature feature : features) if (!has(feature)) {
            sender.sendMessage(Main.text("err.featureOff", feature));
            return false;
        }
        return true;
    }

    /** When each player's teleport is over; a player is "in a portal" until then (the blindness a teleport gives lasts as long). */
    private static final Map<UUID, Long> teleportingUntil = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long TELEPORT_MILLIS = 2000;

    public static void markTeleporting(Player player) { teleportingUntil.put(player.getUniqueId(), System.currentTimeMillis() + TELEPORT_MILLIS); }

    public static PlayerInfo getPlayerInfo(Player player) { return new PlayerInfo(player); }

    public static final class PlayerInfo {
        private final Player player;
        private PlayerInfo(Player player) { this.player = player; }
        public Player getPlayer() { return player; }
        public String getName() { return player.getName(); }
        public String getNameTag() { return player.getNameTag(); }
        public void sendText(String text) { player.sendMessage(text); }
        public boolean isTopQuanXian() { return player.isOp() || player.hasPermission(ADMIN); }
        /** How /tpa treats this player: the administrator identity and operators teleport silently, the sponsor identity forced; both can also be granted by node. */
        public dev.toka.pl.tokaPortal.tpa.TpaRules.Tier tpaTier() {
            return dev.toka.pl.tokaPortal.tpa.TpaRules.tier(
                    dev.toka.pl.tokaPortal.tpa.TpaRules.silent(player.isOp(), player.hasPermission(TPA_SILENT), player.hasPermission(ADMIN)),
                    dev.toka.pl.tokaPortal.tpa.TpaRules.force(player.hasPermission(TPA_FORCE), player.hasPermission(SPONSOR)));
        }
        public boolean isPortalStatus() { return teleportingUntil.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis(); }
        public String getRegionInfo() { Region region = getRegion(); return region == null ? Main.text("status.here") : region.getInfo(); }
        public Region getRegion() { return Region.at(player.getLocation()); }
    }

    /** A region of tokaRegions, read through its accessors. */
    public static final class Region {
        private final Object delegate;
        private Region(Object delegate) { this.delegate = delegate; }

        private static Object index() {
            Plugin plugin = running("tokaRegions");
            if (plugin == null) return null;
            try { return plugin.getClass().getMethod("regions").invoke(plugin); }
            catch (ReflectiveOperationException | LinkageError unavailable) { return null; }
        }

        private Object get(String accessor) {
            try { return delegate.getClass().getMethod(accessor).invoke(delegate); }
            catch (ReflectiveOperationException | LinkageError unavailable) { return null; }
        }

        public String getName() { return Objects.toString(get("name"), ""); }
        /** The name players read: the one written for the server's language, else any written name, else the plain name. */
        public String getInfo() {
            if (get("localizedNames") instanceof Map<?, ?> names && !names.isEmpty()) {
                String language = Main.getInstance().getConfig().getString("language", "zh-TW");
                Object own = names.get(language);
                if (own != null) return color() + own;
                for (Object value : names.values()) if (value != null && !value.toString().isBlank()) return color() + value;
            }
            return getName();
        }

        /** The name without the colour. */
        public String getPlainInfo() { return getInfo().replaceAll("§.", ""); }

        private String color() { return Objects.toString(get("color"), ""); }

        /** Whether an administrator has set a teleport point for the region. */
        public boolean hasTeleportPoint() { return get("teleport") != null; }

        /** Where players sent to this region arrive; null when the region has no teleport point or its world is not loaded. */
        public Location getTpr() {
            Object point = get("teleport"), world = get("world");
            if (point == null || world == null) return null;
            var level = Server.getInstance().getLevelByName(world.toString());
            if (level == null) return null;
            try {
                Class<?> type = point.getClass();
                return new Location(number(type, point, "x"), number(type, point, "y"), number(type, point, "z"),
                        number(type, point, "yaw"), number(type, point, "pitch"), level);
            } catch (ReflectiveOperationException | LinkageError unavailable) { return null; }
        }

        private static double number(Class<?> type, Object point, String accessor) throws ReflectiveOperationException {
            return ((Number) type.getMethod(accessor).invoke(point)).doubleValue();
        }

        public static Region getRegion(String name) {
            Object index = index();
            if (index == null || name == null) return null;
            try {
                Object found = index.getClass().getMethod("byName", String.class).invoke(index, name);
                return found instanceof Optional<?> value && value.isPresent() ? new Region(value.get()) : null;
            } catch (ReflectiveOperationException | LinkageError unavailable) { return null; }
        }

        static Region at(Location at) {
            Object index = index();
            if (index == null || at.getLevel() == null) return null;
            try {
                Object found = index.getClass().getMethod("at", String.class, int.class, int.class, int.class)
                        .invoke(index, at.getLevel().getFolderName(), at.getFloorX(), at.getFloorY(), at.getFloorZ());
                return found instanceof Optional<?> value && value.isPresent() ? new Region(value.get()) : null;
            } catch (ReflectiveOperationException | LinkageError unavailable) { return null; }
        }

        /** Every region that players can be sent to, in the order tokaRegions keeps them. */
        public static Map<Object, Region> getRegions() {
            Map<Object, Region> result = new LinkedHashMap<>();
            Object index = index();
            if (index == null) return result;
            try {
                Object all = index.getClass().getMethod("all").invoke(index);
                if (all instanceof Collection<?> regions) for (Object region : regions) {
                    Region wrapped = new Region(region);
                    if (wrapped.get("teleport") != null) result.put(wrapped.get("id"), wrapped);
                }
            } catch (ReflectiveOperationException | LinkageError unavailable) { /* none */ }
            return result;
        }
    }

    public static void sendCityPassForm(Player player) {
        if (!require(player, Feature.CITY_PASS)) return;
        Plugin profiles = running("tokaProfiles");
        try { profiles.getClass().getMethod("openCityPass", Player.class).invoke(profiles, player); }
        catch (ReflectiveOperationException | LinkageError unavailable) { player.sendMessage(Main.text("err.featureOff", Feature.CITY_PASS)); }
    }

    public static boolean callEvent(Event event) {
        Server.getInstance().getPluginManager().callEvent(event);
        return !(event instanceof Cancellable cancellable) || !cancellable.isCancelled();
    }

    public static List<String> getPlayerNameList(Player player) {
        return Server.getInstance().getOnlinePlayers().values().stream()
                .filter(other -> !other.equals(player)).map(Player::getName).toList();
    }
}
