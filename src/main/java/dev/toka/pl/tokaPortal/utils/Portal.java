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
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.teleportPoint;
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
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.teleportPoint.getPoint;
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
        if (!ZeroIntegration.require(player, POINTS)) return;
        toPoint(getPoint(pointName), player);
    }

    public static void toPoint(teleportPoint point, Player player) {
        if (player == null) {
            return;
        }
        if (point == null) {
            player.sendTitle("§c未知傳送點", "§e無法傳送");
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
            player.sendTitle("§c未知住家", "§e無法傳送");
            return;
        }
        if (!home.isCreator(player)) {
            player.sendTitle("§c無效住家", "§e無法傳送");
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
            player.sendTitle("§c未知區域", "§e無法傳送");
            return;
        }
        portal(player, region.getTpr(), region.getInfo());
    }

    public static void Tpa(Player fromPlayer, Player toPlayer) {
        if (fromPlayer == null || toPlayer == null || !fromPlayer.isOnline() || !toPlayer.isOnline()) return;
        PlayerInfo fromPli = getPlayerInfo(fromPlayer);
        PlayerInfo toPli = getPlayerInfo(toPlayer);
        if (fromPli.isTopQuanXian()) {
            toPli.sendText("§a[傳送]§b玩家§e %1 §b即將傳送到你身邊!§r"
                    .replace("%1", fromPlayer.getName()));
            if (portal(fromPlayer, toPlayer.getLocation(), toPlayer.getName()))
                fromPli.sendText("§a[傳送]§b您已傳送到玩家§e %1 §b身旁§r"
                    .replace("%1", toPlayer.getName()));
            return;
        }
        if (fromPli.isHighQuanXian()) {
            fromPli.sendText("§a[傳送]§b即將傳送至玩家§e %1 §b身邊§r"
                    .replace("%1", toPlayer.getName()));
            toPli.sendText("§a[傳送]§b玩家§e %1 §b即將傳送到你身邊!§r"
                    .replace("%1", fromPlayer.getName()));
            (new NukkitRunnable() {
                public void run() {
                    if (!fromPlayer.isOnline() || !toPlayer.isOnline()) return;
                    if (portal(fromPlayer, toPlayer.getLocation(), toPli.getName()))
                        fromPli.sendText("§a[傳送]§b您已傳送到玩家§e %1 §b身旁§r"
                            .replace("%1", toPlayer.getName()));
                    this.cancel();
                }
            }).runTaskLater(Main.getInstance(), 30);
        } else {
            if (tpaMap.containsKey(toPlayer)) {
                fromPli.sendText("[傳送]對方仍有待處理的傳送請求，請稍後再試。");
                return;
            }
            tpaMap.put(toPlayer, fromPlayer);
            fromPli.sendText("§a[傳送]§b您已發送傳送要求給§e %1 §b請等待對方回應§r"
                    .replace("%1", toPlayer.getName()));
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
        return portal(player, location, "傳送點");
    }

    public static boolean portal(Player player, Location location, String title) {
        if (player == null || !player.isOnline() || location == null || location.getLevel() == null) {
            if (player != null) player.sendMessage("[傳送]目的地世界尚未載入，無法傳送。");
            return false;
        }
        PlayerInfo pli = getPlayerInfo(player);
        PlayerPortalEvent ev = new PlayerPortalEvent(
                pli, pli.getRegionInfo(), title, player.getLocation(), location);
        if (callEvent(ev)) {
            if (!player.teleport(location)) return false;
            player.sendTitle(title, "§b正在傳送...", 1, 20, 5);
            Effect effectBLINDNESS = getEffect(BLINDNESS).setVisible(false)
                    .setAmplifier(0).setDuration(40);
            player.addEffect(effectBLINDNESS);
            return true;
        }
        return false;
    }

    public static void setHome(Player player, String name) {
        if (!canUseHome(player)) return;
        PlayerInfo pli = getPlayerInfo(player);
        if (name == null || name.isBlank()) {
            pli.sendText("[傳送]住家名稱不得為空!");
            return;
        }
        if (getProvider().getHomePoint(name) != null) {
            pli.sendText("[傳送]已存在此名稱的住家，請更換一個名稱後再試。");
            return;
        }
        if (pli.getRegion() != null) {
            if (canAddHomeRegionList.contains(pli.getRegion().getName())) {
                try { getProvider().addHomePoint(name, player, player.getLocation()); }
                catch (RuntimeException e) {
                    Main.getInstance().getLogger().error("住家儲存失敗", e);
                    pli.sendText("[傳送]資料儲存失敗，住家未建立。請聯絡管理員。");
                    return;
                }
                pli.sendText("[傳送]已成功設定住家'%name'!".replace("%name", name));
                player.sendCommandData();
                return;
            }
        }
        pli.sendText("[傳送]此區域無法設置住家。");
    }

    public static void delHome(Player player, String name) {
        PlayerInfo pli = getPlayerInfo(player);
        HomePoint point = getProvider().getHomePoint(name);
        if (point == null) {
            pli.sendText("[傳送]找不到要刪除的住家!");
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
                Main.getInstance().getLogger().error("住家刪除失敗", e);
                pli.sendText("[傳送]資料儲存失敗，住家未刪除。請聯絡管理員。");
                return;
            }
            pli.sendText("[傳送]成功刪除住家!");
            player.sendCommandData();
            return;
        }
        pli.sendText("[傳送]發生未知的錯誤!本次並未造成任何修改。");
    }

    private static boolean canUseHome(Player player) {
        if (!ZeroIntegration.require(player, PLAYERS, REGIONS)) return false;
        if (player.isOp() || getPlayerInfo(player).isTopQuanXian()) return true;
        player.sendMessage("[傳送]住家功能僅限管理員使用。");
        return false;
    }
}
