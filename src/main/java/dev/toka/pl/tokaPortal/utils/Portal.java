package dev.toka.pl.tokaPortal.utils;

import cn.nukkit.Player;
import cn.nukkit.event.Listener;
import cn.nukkit.level.Location;
import cn.nukkit.potion.Effect;
import cn.nukkit.scheduler.NukkitRunnable;
import dev.toka.pl.tokaPortal.event.PlayerPortalEvent;
import dev.toka.pl.tokaPortal.point.HomePoint;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.PlayerInfo;
import dev.toka.pl.tokaPortal.point.teleportPoint;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.Region;

import java.util.ArrayList;
import java.util.HashMap;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.Feature.*;

import static cn.nukkit.potion.Effect.BLINDNESS;
import static cn.nukkit.potion.Effect.getEffect;
import static dev.toka.pl.tokaPortal.Main.getProvider;
import static dev.toka.pl.tokaPortal.utils.PortalWindow.sendPortalTpaAcceptWindow;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.getPlayerInfo;
import static dev.toka.pl.tokaPortal.point.teleportPoint.getPoint;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.Region.getRegion;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.callEvent;

public class Portal implements Listener {
    public static final ArrayList<String> canAddHomeRegionList = new ArrayList<>();
    private static final HashMap<Player, Player> tpaMap = new HashMap<>();

    static {//可以新增住家的區域(Region)列表
        canAddHomeRegionList.add("v1");
        canAddHomeRegionList.add("v2");
        canAddHomeRegionList.add("v3");
        canAddHomeRegionList.add("v4");
        canAddHomeRegionList.add("HePingZhiDi");
    }

    public static void toPoint(String pointName, Player player) {

        toPoint(getPoint(pointName), player);
    }

    public static void toPoint(teleportPoint point, Player player) {
        if (player == null) {
            return;
        }
        if (point == null) {
            player.sendTitle(Main.text("ui.title.unknownPoint"), Main.text("ui.title.cannotTeleport"));
            return;
        }
        portal(player, point.getLocation(), point.getInfo());
    }

    public static void toPoint(HomePoint home, Player player) {
        if (player == null) {
            return;
        }
        if (!canUseHome(player)) return;
        if (home == null) {
            player.sendTitle(Main.text("ui.title.unknownHome"), Main.text("ui.title.cannotTeleport"));
            return;
        }
        if (!home.isCreator(player)) {
            player.sendTitle(Main.text("ui.title.invalidHome"), Main.text("ui.title.cannotTeleport"));
            return;
        }
        portal(player, home.getLocation(), home.getName());
    }

    public static void toRegion(String regionName, Player player) {
        if (!ZeroIntegration.require(player, REGIONS)) return;
        toRegion(getRegion(regionName), player);
    }

    public static void toRegion(Region region, Player player) {
        if (player == null) {
            return;
        }
        if (region == null) {
            player.sendTitle(Main.text("ui.title.unknownRegion"), Main.text("ui.title.cannotTeleport"));
            return;
        }
        portal(player, region.getTpr(), region.getInfo());
    }

    public static void Tpa(Player fromPlayer, Player toPlayer) {
        if (fromPlayer == null || toPlayer == null || !fromPlayer.isOnline() || !toPlayer.isOnline()) return;
        PlayerInfo fromPli = getPlayerInfo(fromPlayer);
        PlayerInfo toPli = getPlayerInfo(toPlayer);
        if (fromPli.isTopQuanXian()) {
            toPli.sendText(Main.text("msg.tpa.incoming", fromPlayer.getName()));
            if (portal(fromPlayer, toPlayer.getLocation(), toPlayer.getName()))
                fromPli.sendText(Main.text("msg.tpa.arrived", toPlayer.getName()));
            return;
        }
        if (fromPli.isHighQuanXian()) {
            fromPli.sendText(Main.text("msg.tpa.soon", toPlayer.getName()));
            toPli.sendText(Main.text("msg.tpa.incoming", fromPlayer.getName()));
            (new NukkitRunnable() {
                public void run() {
                    if (!fromPlayer.isOnline() || !toPlayer.isOnline()) return;
                    if (portal(fromPlayer, toPlayer.getLocation(), toPli.getName()))
                        fromPli.sendText(Main.text("msg.tpa.arrived", toPlayer.getName()));
                    this.cancel();
                }
            }).runTaskLater(Main.getInstance(), 30);
        } else {
            if (tpaMap.containsKey(toPlayer)) {
                fromPli.sendText(Main.text("err.tpa.pending"));
                return;
            }
            tpaMap.put(toPlayer, fromPlayer);
            fromPli.sendText(Main.text("msg.tpa.sent", toPlayer.getName()));
            sendPortalTpaAcceptWindow(toPlayer);
            Main.getInstance().getServer().getScheduler().scheduleDelayedTask(Main.getInstance(), () -> {
                if (tpaMap.get(toPlayer) == fromPlayer) tpaMap.remove(toPlayer);
            }, 1200);
        }

    }

    public static Player getTpaFrom(Player toPlayer) {
        return tpaMap.get(toPlayer);
    }

    public static void delTpaMap(Player toPlayer) {
        tpaMap.remove(toPlayer);
    }

    public static void clearPlayer(Player player) {
        tpaMap.remove(player);
        tpaMap.values().removeIf(player::equals);
    }

    public static boolean portal(Player player, Location location) {
        return portal(player, location, Main.text("status.pointDefault"));
    }

    public static boolean portal(Player player, Location location, String title) {
        if (player == null || !player.isOnline() || location == null || location.getLevel() == null) {
            if (player != null) player.sendMessage(Main.text("err.worldNotLoaded"));
            return false;
        }
        PlayerInfo pli = getPlayerInfo(player);
        PlayerPortalEvent ev = new PlayerPortalEvent(
                pli, pli.getRegionInfo(), title, player.getLocation(), location);
        if (callEvent(ev)) {
            if (!player.teleport(location)) return false;
            player.sendTitle(title, Main.text("ui.title.teleporting"), 1, 20, 5);
            Effect effectBLINDNESS = getEffect(BLINDNESS).setVisible(false)
                    .setAmplifier(0).setDuration(40);
            player.addEffect(effectBLINDNESS);
            return true;
        }
        return false;
    }

    /** The words in reserved-names; when the config has none the default words are used, so that the check is never off. */
    public static boolean isReservedName(String name) {
        java.util.List<String> words = Main.getInstance().getConfig().getStringList("reserved-names");
        if (words == null || words.isEmpty()) {
            words = java.util.List.of("help", "list", "set", "del", "edit");
        }
        String clean = name.strip();
        return words.stream().anyMatch(word -> word.equalsIgnoreCase(clean));
    }

    public static void setHome(Player player, String name) {
        if (!canUseHome(player)) return;
        PlayerInfo pli = getPlayerInfo(player);
        if (name == null || name.isBlank()) {
            pli.sendText(Main.text("err.home.nameEmpty"));
            return;
        }
        if (isReservedName(name)) {
            pli.sendText(Main.text("err.home.reservedName", name.strip()));
            return;
        }
        if (getProvider().getHomePoint(name) != null) {
            pli.sendText(Main.text("err.home.exists"));
            return;
        }
        if (pli.getRegion() != null) {
            if (canAddHomeRegionList.contains(pli.getRegion().getName())) {
                try { getProvider().addHomePoint(name, player, player.getLocation()); }
                catch (RuntimeException e) {
                    Main.getInstance().getLogger().error(Main.text("log.homes.saveFailed"), e);
                    pli.sendText(Main.text("err.home.saveFailed"));
                    return;
                }
                pli.sendText(Main.text("msg.home.set", name));
                player.sendCommandData();
                return;
            }
        }
        pli.sendText(Main.text("err.home.regionDenied"));
    }

    public static void delHome(Player player, String name) {
        PlayerInfo pli = getPlayerInfo(player);
        HomePoint point = getProvider().getHomePoint(name);
        if (point == null) {
            pli.sendText(Main.text("err.home.delNotFound"));
            return;
        }
        delHome(player, point);
    }

    public static void delHome(Player player, HomePoint home) {
        if (!canUseHome(player)) return;
        PlayerInfo pli = getPlayerInfo(player);
        if (home != null && home.isCreator(player)) {
            try { getProvider().delHomePoint(home); }
            catch (RuntimeException e) {
                Main.getInstance().getLogger().error(Main.text("log.homes.deleteFailed"), e);
                pli.sendText(Main.text("err.home.deleteFailed"));
                return;
            }
            pli.sendText(Main.text("msg.home.deleted"));
            player.sendCommandData();
            return;
        }
        pli.sendText(Main.text("err.home.unknown"));
    }

    private static boolean canUseHome(Player player) {
        if (!ZeroIntegration.require(player, PLAYERS, REGIONS)) return false;
        if (player.isOp() || getPlayerInfo(player).isTopQuanXian()) return true;
        player.sendMessage(Main.text("err.homeAdminOnly"));
        return false;
    }
}
