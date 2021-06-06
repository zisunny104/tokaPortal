package dev.toka.pl.tokaPortal;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.level.Location;
import cn.nukkit.potion.Effect;
import cn.nukkit.scheduler.NukkitRunnable;
import dev.toka.pl.tokaPortal.event.PlayerPortalEvent;
import prj.toka.zero.Main;
import prj.toka.zero.player.PlayerInfo;
import prj.toka.zero.player.quanxian.QuanXian;
import prj.toka.zero.ser.portal.teleportPoint.teleportPoint;
import prj.toka.zero.ser.region.Region;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static dev.toka.pl.tokaPortal.PortalWindow.sendPortalTpaAcceptWindow;
import static prj.toka.zero.player.Players.getPlayerInfo;
import static prj.toka.zero.ser.portal.teleportPoint.teleportPoint.getPoint;
import static prj.toka.zero.ser.region.Region.getRegion;
import static prj.toka.zero.utils.Utils.TIP_TYPE_PORTMD_INT;
import static prj.toka.zero.utils.Utils.callEvent;

public class Portal implements Listener {
    private static final HashMap<Player, Player> tpaMap = new HashMap<>();

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

    public static void Tpa(Player Fplayer, Player Tplayer) {
        PlayerInfo Fpli = getPlayerInfo(Fplayer);
        PlayerInfo Tpli = getPlayerInfo(Tplayer);
        if (QuanXian.isHighQuanXian(Fplayer) && !QuanXian.isTopQuanXian(Tplayer)) {
            Fpli.sendText("§a[傳送]§b即將傳送至玩家§e %1 §b身邊§r".replace("%1", Tplayer.getName()));
            Tpli.sendText("§a[傳送]§b玩家§e %1 §b即將傳送到你身邊!§r".replace("%1", Fplayer.getName()));
            (new NukkitRunnable() {
                public void run() {
                    portal(Fplayer, Tplayer.getLocation(), Tpli.getName());
                    Fpli.sendText("§a[傳送]§b您已傳送到玩家§e %1 §b身旁§r".replace("%1", Tplayer.getName()));
                    this.cancel();
                }
            }).runTaskLater(Main.getInstance(), 30);
        } else {
            tpaMap.put(Tplayer, Fplayer);
            Fpli.sendText("§a[傳送]§b您已發送傳送要求給§e %1 §b請等待對方回應§r".replace("%1", Tplayer.getName()));
            sendPortalTpaAcceptWindow(Tplayer);
        }

    }

    public static void toServer(Player player, String ip, int port) {
        InetSocketAddress address = new InetSocketAddress(ip, port);
        player.transfer(address);
    }

    public static Player getTpaFrom(Player Tplayer) {
        return tpaMap.get(Tplayer);
    }

    public static void delTpaMap(Player Tplayer) {
        tpaMap.remove(Tplayer);
    }

    public static void portal(Player player, Location location) {
        portal(player, location, "傳送點");
    }

    public static void portal(Player player, Location location, String title) {
        PlayerInfo pli = getPlayerInfo(player);
        CompletableFuture.runAsync(() -> player.teleport(location));
        pli.setInPortalStatus();
        pli.setTipType(TIP_TYPE_PORTMD_INT);
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
                pli.setOutPortalStatus();
                this.cancel();
            }
        }).runTaskLater(Main.getInstance(), 20);
    }

    @EventHandler
    public void setLastPortalLocation(PlayerPortalEvent event){
        event.getPli().setDeathLocation(event.getFrom());
    }
}
