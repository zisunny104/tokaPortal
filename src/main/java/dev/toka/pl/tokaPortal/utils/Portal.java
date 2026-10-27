package dev.toka.pl.tokaPortal.utils;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.Listener;
import cn.nukkit.level.Location;
import cn.nukkit.potion.Effect;
import dev.toka.pl.tokaPortal.event.PlayerPortalEvent;
import dev.toka.pl.tokaPortal.point.HomePoint;
import dev.toka.pl.tokaPortal.tpa.TpaFlow;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.integration.Services.PlayerInfo;
import dev.toka.pl.tokaPortal.point.teleportPoint;
import dev.toka.pl.tokaPortal.integration.Services.Region;

import java.util.ArrayList;
import dev.toka.pl.tokaPortal.integration.Services;
import dev.toka.pl.tokaPortal.integration.LandIntegration;
import static dev.toka.pl.tokaPortal.integration.Services.Feature.*;

import static cn.nukkit.potion.Effect.BLINDNESS;
import static cn.nukkit.potion.Effect.getEffect;
import static dev.toka.pl.tokaPortal.Main.getProvider;
import static dev.toka.pl.tokaPortal.integration.Services.getPlayerInfo;
import static dev.toka.pl.tokaPortal.point.teleportPoint.getPoint;
import static dev.toka.pl.tokaPortal.integration.Services.Region.getRegion;
import static dev.toka.pl.tokaPortal.integration.Services.callEvent;

public class Portal implements Listener {
    public static final ArrayList<String> canAddHomeRegionList = new ArrayList<>();

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
        if (!Services.require(player, REGIONS)) return;
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
        if (!region.hasTeleportPoint()) {
            player.sendMessage(Main.text("err.regionNoTeleport", region.getInfo()));
            return;
        }
        portal(player, region.getTpr(), region.getInfo());
    }

    /** Asks the target for the teleport of the sender; the tier of the sender decides how (see TpaFlow). */
    public static void Tpa(Player fromPlayer, Player toPlayer) {
        TpaFlow.request(fromPlayer, toPlayer, false);
    }

    public static void clearPlayer(Player player) {
        TpaFlow.playerQuit(player);
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
            Services.markTeleporting(player);
            player.sendTitle(title, Main.text("ui.title.teleporting"), 1, 20, 5);
            Effect effectBLINDNESS = getEffect(BLINDNESS).setVisible(false)
                    .setAmplifier(0).setDuration(40);
            player.addEffect(effectBLINDNESS);
            return true;
        }
        return false;
    }

    /** Public stable-ID entry point for Land signs and other optional integrations. */
    public static boolean toLandClaim(Player player,String claimId) {
        if(player==null||!player.isOnline()||claimId==null||claimId.isBlank())return false;
        LandIntegration.Lookup lookup=LandIntegration.lookup(player,claimId);
        if(!"OK".equals(lookup.status())||lookup.target()==null){
            player.sendMessage(Main.text("err.land.notFound",claimId));return false;
        }
        var target=lookup.target();var level=Server.getInstance().getLevelByName(target.world());
        if(level==null){player.sendMessage(Main.text("err.worldNotLoaded"));return false;}
        return portal(player,new Location(target.x(),target.y(),target.z(),target.yaw(),target.pitch(),level),target.name());
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

    /** A name of 1 to 32 characters without control characters or the colour sign. */
    public static boolean isValidName(String name) {
        return name != null && !name.isBlank() && name.length() <= 32 && name.chars().noneMatch(c -> c < 32 || c == 127 || c == '§');
    }

    public static void setHome(Player player, String name) {
        if (!canUseHome(player)) return;
        PlayerInfo pli = getPlayerInfo(player);
        if (name == null || name.isBlank()) {
            pli.sendText(Main.text("err.home.nameEmpty"));
            return;
        }
        if (!isValidName(name)) {
            pli.sendText(Main.text("err.home.nameInvalid"));
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
        if (!Services.require(player, PLAYERS, REGIONS)) return false;
        if (player.isOp() || getPlayerInfo(player).isTopQuanXian()) return true;
        player.sendMessage(Main.text("err.homeAdminOnly"));
        return false;
    }
}
