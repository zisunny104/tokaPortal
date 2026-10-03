package dev.toka.pl.tokaPortal.integration;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.Event;
import cn.nukkit.level.Location;
import cn.nukkit.plugin.Plugin;
import dev.toka.pl.tokaPortal.Main;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Optional, reflective access to the original zero API. No zero classes are linked into this JAR. */
public final class ZeroIntegration {
    public enum Feature { PLAYERS, POINTS, POINT_MANAGEMENT, REGIONS, LAND, CITY_PASS, WILD }
    private static final String PREFIX = "prj.toka.zero.";
    private static final EnumSet<Feature> available = EnumSet.noneOf(Feature.class);
    private static Plugin zero;

    private ZeroIntegration() {}

    public static void initialize() {
        available.clear();
        zero = null;
        for (Plugin plugin : Server.getInstance().getPluginManager().getPlugins().values()) {
            if (!plugin.isEnabled()) continue;
            try {
                Class<?> main = Class.forName(PREFIX + "Main", false, plugin.getClass().getClassLoader());
                if (main.isInstance(plugin)) { zero = plugin; break; }
            } catch (ClassNotFoundException | LinkageError ignored) { }
        }
        if (zero != null) {
            check(Feature.PLAYERS, new String[][] {
                    {"player.Players", "getPlayerInfo", "cn.nukkit.Player"},
                    {"player.PlayerInfo", "isTopQuanXian"}, {"player.PlayerInfo", "isHighQuanXian"},
                    {"player.PlayerInfo", "isPortalStatus"}, {"player.PlayerInfo", "getRegionInfo"},
                    {"player.PlayerInfo", "getRegion"}});
            check(Feature.POINTS, new String[][] {
                    {"ser.portal.teleportPoint.teleportPoint", "getPoint", "java.lang.String"},
                    {"ser.portal.teleportPoint.teleportPoint", "getPoints"},
                    {"ser.portal.teleportPoint.teleportPoint", "getLocation"},
                    {"ser.portal.teleportPoint.teleportPoint", "getInfo"},
                    {"ser.portal.teleportPoint.teleportPoint", "getType"},
                    {"ser.portal.teleportPoint.teleportPoint", "getCreator"}});
            check(Feature.POINT_MANAGEMENT, new String[][] {
                    {"ser.portal.teleportPoint.teleportPoint", "setTeleportPoint", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "cn.nukkit.level.Location"},
                    {"ser.portal.teleportPoint.teleportPoint", "delTeleportPoint", PREFIX + "ser.portal.teleportPoint.teleportPoint"}});
            check(Feature.REGIONS, new String[][] {
                    {"ser.region.Region", "getRegion", "java.lang.String"}, {"ser.region.Region", "getRegions"},
                    {"ser.region.Region", "getName"}, {"ser.region.Region", "getTpr"}, {"ser.region.Region", "getInfo"}});
            check(Feature.LAND, new String[][] {
                    {"ser.land.Land", "getLand", "java.lang.String"},
                    {"ser.land.Land", "toLand", PREFIX + "ser.land.Land", "cn.nukkit.Player"},
                    {"ser.land.Land", "getLandNameListByOwner", "cn.nukkit.Player"}});
            check(Feature.CITY_PASS, new String[][] {{"player.citypass.CityPass", "sendCityPassForm", "cn.nukkit.Player"}});
            check(Feature.WILD, new String[][] {
                    {"player.Players", "getPlayerInfo", "cn.nukkit.Player"},
                    {"player.PlayerInfo", "getBossBarLength"}, {"player.PlayerInfo", "reBossBarLength"}});
        }
        Main.getInstance().getLogger().info("zero 可用整合: " + available);
        if (!has(Feature.PLAYERS) || !has(Feature.REGIONS))
            Main.getInstance().getLogger().warning("zero 玩家/區域 API 不可用，已關閉住家管理及住家傳送。");
        for (Feature feature : Feature.values()) if (!has(feature))
            Main.getInstance().getLogger().warning("已關閉 zero 功能: " + feature);
    }

    private static void check(Feature feature, String[][] signatures) {
        try {
            for (String[] signature : signatures) {
                Class<?> type = load(signature[0]);
                Class<?>[] params = new Class<?>[signature.length - 2];
                for (int i = 2; i < signature.length; i++)
                    params[i - 2] = Class.forName(signature[i], false, zero.getClass().getClassLoader());
                Method method = type.getMethod(signature[1], params);
                Class<?> expected = switch (signature[1]) {
                    case "getPlayerInfo" -> load("player.PlayerInfo");
                    case "getPoint" -> load("ser.portal.teleportPoint.teleportPoint");
                    case "getRegion" -> load("ser.region.Region");
                    case "getLand" -> load("ser.land.Land");
                    case "getLocation", "getTpr" -> Location.class;
                    case "getPoints", "getRegions" -> Map.class;
                    case "getLandNameListByOwner" -> List.class;
                    case "getInfo", "getType", "getName", "getCreator", "getRegionInfo" -> String.class;
                    case "isTopQuanXian", "isHighQuanXian", "isPortalStatus" -> boolean.class;
                    default -> null;
                };
                if (expected != null && !expected.isAssignableFrom(method.getReturnType()))
                    throw new NoSuchMethodException(signature[1] + " has an incompatible return type");
            }
            available.add(feature);
        } catch (ReflectiveOperationException | LinkageError | SecurityException e) {
            Main.getInstance().getLogger().warning("zero " + feature + " API 不相容: " + e.getMessage());
        }
    }

    public static boolean has(Feature feature) {
        return zero != null && zero.isEnabled() && available.contains(feature);
    }

    public static boolean require(CommandSender sender, Feature... features) {
        for (Feature feature : features) if (!has(feature)) {
            sender.sendMessage("[傳送]此功能已關閉：缺少或不相容的 zero " + feature + " API。");
            return false;
        }
        return true;
    }

    private static Class<?> load(String name) throws ClassNotFoundException {
        return Class.forName(PREFIX + name, false, zero.getClass().getClassLoader());
    }

    private static Object invoke(Feature feature, Object target, String type, String method, Object... args) {
        if (!has(feature)) return null;
        try {
            Class<?> clazz = target == null ? load(type) : target.getClass();
            for (Method candidate : clazz.getMethods()) {
                if (!candidate.getName().equals(method) || candidate.getParameterCount() != args.length) continue;
                if (target == null && !Modifier.isStatic(candidate.getModifiers())) continue;
                Class<?>[] params = candidate.getParameterTypes();
                boolean matches = true;
                for (int i = 0; i < params.length; i++) {
                    if (args[i] != null && !params[i].isInstance(args[i])) { matches = false; break; }
                }
                if (matches) return candidate.invoke(target, args);
            }
            throw new NoSuchMethodException(type + "." + method);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            available.remove(feature);
            Main.getInstance().getLogger().warning("zero 功能 " + feature + " 已關閉：" + e);
            return null;
        }
    }

    public static PlayerInfo getPlayerInfo(Player player) {
        return new PlayerInfo(player, invoke(Feature.PLAYERS, null, "player.Players", "getPlayerInfo", player));
    }

    public static final class PlayerInfo {
        private final Player player;
        private final Object delegate;
        private PlayerInfo(Player player, Object delegate) { this.player = player; this.delegate = delegate; }
        private Object get(String method) { return delegate == null ? null : invoke(Feature.PLAYERS, delegate, "player.PlayerInfo", method); }
        public Player getPlayer() { return player; }
        public String getName() { return player.getName(); }
        public String getNameTag() { return player.getNameTag(); }
        public void sendText(String text) { player.sendMessage(text); }
        public boolean isTopQuanXian() { return delegate == null ? player.isOp() : Boolean.TRUE.equals(get("isTopQuanXian")); }
        public boolean isHighQuanXian() { return delegate != null && Boolean.TRUE.equals(get("isHighQuanXian")); }
        public boolean isPortalStatus() { return delegate != null && Boolean.TRUE.equals(get("isPortalStatus")); }
        public String getRegionInfo() { Object value = get("getRegionInfo"); return value == null ? "目前位置" : value.toString(); }
        public Region getRegion() { Object value = get("getRegion"); return value == null ? null : new Region(value); }
        public double getBossBarLength() {
            Object info = invoke(Feature.WILD, null, "player.Players", "getPlayerInfo", player);
            Object value = info == null ? null : invoke(Feature.WILD, info, "player.PlayerInfo", "getBossBarLength");
            return value instanceof Number ? ((Number) value).doubleValue() : 0;
        }
        public void reBossBarLength() {
            Object info = invoke(Feature.WILD, null, "player.Players", "getPlayerInfo", player);
            if (info != null) invoke(Feature.WILD, info, "player.PlayerInfo", "reBossBarLength");
        }
    }

    public static final class teleportPoint {
        private final Object delegate;
        private teleportPoint(Object delegate) { this.delegate = delegate; }
        private Object get(String method) { return invoke(Feature.POINTS, delegate, "ser.portal.teleportPoint.teleportPoint", method); }
        public Location getLocation() { return (Location) get("getLocation"); }
        public String getInfo() { return Objects.toString(get("getInfo"), ""); }
        public String getType() { return Objects.toString(get("getType"), ""); }
        public String getCreator() { return Objects.toString(get("getCreator"), ""); }
        public static teleportPoint getPoint(String name) {
            Object value = invoke(Feature.POINTS, null, "ser.portal.teleportPoint.teleportPoint", "getPoint", name);
            return value == null ? null : new teleportPoint(value);
        }
        public static Map<Object, teleportPoint> getPoints() {
            Map<Object, teleportPoint> result = new LinkedHashMap<>();
            Object value = invoke(Feature.POINTS, null, "ser.portal.teleportPoint.teleportPoint", "getPoints");
            if (value instanceof Map<?, ?> map) map.forEach((key, point) -> result.put(key, new teleportPoint(point)));
            return result;
        }
        public static boolean setTeleportPoint(String name, String creator, String type, String info, Location location) {
            invoke(Feature.POINT_MANAGEMENT, null, "ser.portal.teleportPoint.teleportPoint", "setTeleportPoint", name, creator, type, info, location);
            return has(Feature.POINT_MANAGEMENT);
        }
        public static boolean delTeleportPoint(teleportPoint point) {
            invoke(Feature.POINT_MANAGEMENT, null, "ser.portal.teleportPoint.teleportPoint", "delTeleportPoint", point.delegate);
            return has(Feature.POINT_MANAGEMENT);
        }
    }

    public static final class Region {
        private final Object delegate;
        private Region(Object delegate) { this.delegate = delegate; }
        private Object get(String method) { return invoke(Feature.REGIONS, delegate, "ser.region.Region", method); }
        public String getName() { return Objects.toString(get("getName"), ""); }
        public String getInfo() { return Objects.toString(get("getInfo"), ""); }
        public Location getTpr() { return (Location) get("getTpr"); }
        public static Region getRegion(String name) {
            Object value = invoke(Feature.REGIONS, null, "ser.region.Region", "getRegion", name);
            return value == null ? null : new Region(value);
        }
        public static Map<Object, Region> getRegions() {
            Map<Object, Region> result = new LinkedHashMap<>();
            Object value = invoke(Feature.REGIONS, null, "ser.region.Region", "getRegions");
            if (value instanceof Map<?, ?> map) map.forEach((key, region) -> result.put(key, new Region(region)));
            return result;
        }
    }

    public static final class Land {
        private final Object delegate;
        private Land(Object delegate) { this.delegate = delegate; }
        public static Land getLand(String name) {
            Object value = invoke(Feature.LAND, null, "ser.land.Land", "getLand", name);
            return value == null ? null : new Land(value);
        }
        public static void toLand(Land land, Player player) {
            if (land != null) invoke(Feature.LAND, null, "ser.land.Land", "toLand", land.delegate, player);
        }
        public static List<String> getLandNameListByOwner(Player player) {
            Object value = invoke(Feature.LAND, null, "ser.land.Land", "getLandNameListByOwner", player);
            if (!(value instanceof List<?> list)) return Collections.emptyList();
            return list.stream().map(Object::toString).toList();
        }
    }

    public static void sendCityPassForm(Player player) {
        if (require(player, Feature.CITY_PASS)) invoke(Feature.CITY_PASS, null, "player.citypass.CityPass", "sendCityPassForm", player);
    }
    public static boolean callEvent(Event event) {
        Server.getInstance().getPluginManager().callEvent(event);
        return !(event instanceof Cancellable cancellable) || !cancellable.isCancelled();
    }
    public static List<String> getPlayerNameList(Player player) {
        return Server.getInstance().getOnlinePlayers().values().stream()
                .filter(other -> !other.equals(player)).map(Player::getName).toList();
    }
    public static int rand(int min, int max) { return ThreadLocalRandom.current().nextInt(min, max + 1); }
}
