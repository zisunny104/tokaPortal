package dev.toka.pl.tokaPortal.utils;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerTeleportEvent;
import cn.nukkit.level.Location;
import cn.nukkit.potion.Effect;
import cn.nukkit.scheduler.NukkitRunnable;
import dev.toka.pl.tokaPortal.event.PlayerPortalEvent;
import dev.toka.pl.tokaPortal.point.HomePoint;
import prj.toka.zero.Main;
import prj.toka.zero.player.PlayerInfo;
import prj.toka.zero.player.quanxian.QuanXian;
import prj.toka.zero.ser.portal.teleportPoint.teleportPoint;
import prj.toka.zero.ser.region.Region;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static dev.toka.pl.tokaPortal.Main.getProvider;
import static dev.toka.pl.tokaPortal.utils.PortalWindow.sendPortalTpaAcceptWindow;
import static prj.toka.zero.player.Players.getPlayerInfo;
import static prj.toka.zero.ser.portal.teleportPoint.teleportPoint.getPoint;
import static prj.toka.zero.ser.region.Region.getRegion;
import static prj.toka.zero.utils.Utils.callEvent;

public class Portal implements Listener {
    private static final HashMap<Player, Player> tpaMap = new HashMap<>();
    private static final HashMap<Player, ArrayList<Location>> backMap = new HashMap<>();
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
            player.sendTitle("§c未知傳送點", "§e無法傳送");
            return;
        }
        portal(player, point.getLocation(), point.getInfo());
    }

    public static void toPoint(HomePoint home, Player player) {
        if (player == null) {
            return;
        }
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
        PlayerInfo fromPli = getPlayerInfo(fromPlayer);
        PlayerInfo toPli = getPlayerInfo(toPlayer);
        if (QuanXian.isHighQuanXian(fromPlayer) && !QuanXian.isTopQuanXian(toPlayer)) {
            fromPli.sendText("§a[傳送]§b即將傳送至玩家§e %1 §b身邊§r".replace("%1", toPlayer.getName()));
            toPli.sendText("§a[傳送]§b玩家§e %1 §b即將傳送到你身邊!§r".replace("%1", fromPlayer.getName()));
            (new NukkitRunnable() {
                public void run() {
                    portal(fromPlayer, toPlayer.getLocation(), toPli.getName());
                    fromPli.sendText("§a[傳送]§b您已傳送到玩家§e %1 §b身旁§r".replace("%1", toPlayer.getName()));
                    this.cancel();
                }
            }).runTaskLater(Main.getInstance(), 30);
        } else {
            tpaMap.put(toPlayer, fromPlayer);
            fromPli.sendText("§a[傳送]§b您已發送傳送要求給§e %1 §b請等待對方回應§r".replace("%1", toPlayer.getName()));
            sendPortalTpaAcceptWindow(toPlayer);
        }

    }

    public static void toServer(Player player, String ip, int port) {
        InetSocketAddress address = new InetSocketAddress(ip, port);
        player.transfer(address);
    }

    public static Player getTpaFrom(Player toPlayer) {
        return tpaMap.get(toPlayer);
    }

    public static void delTpaMap(Player toPlayer) {
        tpaMap.remove(toPlayer);
    }

    public static void portal(Player player, Location location) {
        portal(player, location, "傳送點");
    }

    public static void portal(Player player, Location location, String title) {
        PlayerInfo pli = getPlayerInfo(player);
        pli.sendText("§a[傳送]§e傳送進行中 請不要隨意移動!");
        CompletableFuture.runAsync(() -> player.teleport(location));
        pli.setInPortalStatus();
        //pli.setTipType(TIP_TYPE_PORTMD_INT);
        player.sendTitle(title, "§b正在傳送...", 1, 20, 5);
        callEvent(new PlayerPortalEvent(pli, pli.getRegion().getInfo(), title, player.getLocation(), location));
        Effect effectBLINDNESS = Effect.getEffect(Effect.BLINDNESS).setVisible(false).setAmplifier(0).setDuration(40);
        player.addEffect(effectBLINDNESS);
        Effect effectREGENERATION = Effect.getEffect(Effect.REGENERATION).setVisible(false).setAmplifier(0).setDuration(20);
        (new NukkitRunnable() {
            public void run() {
                player.teleport(location);
                player.addEffect(effectREGENERATION);
                pli.setDefTipType();
                pli.sendText("§a[傳送]§e傳送完成!");
                pli.setOutPortalStatus();
                this.cancel();
            }
        }).runTaskLater(Main.getInstance(), 20);
    }

    public static void addBackLocation(Player player, Location loc) {
        if (!canBack(player)) {
            backMap.put(player, new ArrayList<>());
        }
        ArrayList<Location> backList = backMap.get(player);
        backList.add(loc);
        if (backList.size() > 5) {
            backList.remove(0);
        }
        backMap.put(player, backList);
    }

    public static Location getBackLocation(Player player) {
        return getBackLocation(player, true);
    }

    public static Location getBackLocation(Player player, boolean removeLast) {
        ArrayList<Location> backList = backMap.get(player);
        Location loc = backList.get(backList.size() - 1);
        if (removeLast) {
            backList.remove(backList.size() - 1);
        }
        backMap.put(player, backList);
        return loc;
    }

    public static boolean canBack(Player player) {
        ArrayList<Location> backList = backMap.get(player);
        return backList != null && !backList.isEmpty();
    }

    public static void backToLastPortalLocation(Player player) {
        PlayerInfo pli = getPlayerInfo(player);

        if (pli.isPortalStatus()) {
            pli.sendText("§a[傳送]§b請等待冷卻時間結束後, \n再次進行傳送");
            return;
        }
        if (canBack(player)) {
            Location loc = getBackLocation(player);
            callEvent(new PlayerPortalEvent(pli, "目前位置", "前次傳送位置", player.getLocation(), loc, false));
            player.teleport(loc);
            pli.sendText("§a[傳送]§b已返回前次傳送地點");
        } else {
            pli.sendText("§a[傳送]§b請先進行§e傳送§b(?)");
        }
    }

    @EventHandler
    public void setLastPortalLocation(PlayerPortalEvent event) {
        if (!event.canBack()) {
            return;
        }
        addBackLocation(event.getPlayer(), event.getFrom());
    }

    @EventHandler
    public void setLastPortalLocation(PlayerTeleportEvent event) {
        if (event.getPlayer().isOp()) {
            event.getPlayer().sendMessage("[DEBUG]設定返回點 " + event.getFrom().toString());
            addBackLocation(event.getPlayer(), event.getFrom());
        }
    }

    public static void setHome(Player player, String name) {
        PlayerInfo pli = getPlayerInfo(player);
        if (name == null || name.equals("")) {
            pli.sendText("[傳送]住家名稱不得為空!");
            return;
        }
        if (getProvider().getHomePoint(name) != null) {
            pli.sendText("[傳送]已存在此名稱的住家，請更換一個名稱後再試。");
            return;
        }
        if (canAddHomeRegionList.contains(pli.getRegion().getName())) {
            getProvider().addHomePoint(name, player, player.getLocation());
            pli.sendText("[傳送]已成功設定住家'%name'!".replace("%name", name));
            return;
        }
        pli.sendText("[傳送]此區域無法設置住家。");
    }

    public static void delHome(Player player, Object home) {
        PlayerInfo pli = getPlayerInfo(player);
        if (home instanceof String) {
            HomePoint point = getProvider().getHomePoint((String) home);
            if (point == null) {
                pli.sendText("[傳送]找不到要刪除的住家!");
                return;
            }
            home = point;
        }
        if (home instanceof HomePoint) {
            getProvider().delHomePoint(home);
            pli.sendText("[傳送]成功刪除住家!");
            return;
        }
        pli.sendText("[傳送]發生未知的錯誤!本次並未造成任何修改。");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        addBackLocation(player, player.getLocation());
    }
}
