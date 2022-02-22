package dev.toka.pl.tokaPortal.utils;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.level.Location;
import dev.toka.pl.tokaPortal.event.PlayerPortalEvent;
import prj.toka.zero.player.PlayerInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import static prj.toka.zero.player.Players.getPlayerInfo;
import static prj.toka.zero.utils.Utils.callEvent;

public class PortalHistory implements Listener {

    public static final String NEXT = "next", BACK = "back";
    private static final HashMap<Player, ArrayList<Location>> portalHistoryMap = new HashMap<>();
    private static final HashMap<Player, Integer> portalHistoryStatusMap = new HashMap<>();
    public static int historyLimit = 15;//傳送紀錄保留次數

    public static boolean toHistoryLocation(Player player, String status) {
        PlayerInfo pli = getPlayerInfo(player);

        if (pli.isPortalStatus()) {
            pli.sendText("§a[傳送]§b請等待冷卻時間結束後, \n再次進行傳送");
            return false;
        }//如果玩家仍在傳送模式則無法傳送

        if (portalHistoryMap.get(player) == null) {
            pli.sendText("§a[傳送]§b請先進行§e傳送§b(?)");
            return false;
        }//如果玩家傳送紀錄不存在則無法傳送

        int historyStatus = portalHistoryStatusMap.get(player);
        ArrayList<Location> historyList = portalHistoryMap.get(player);

        if (historyStatus == historyList.size()) {
            if (Objects.equals(status, NEXT)) {
                return false;
            }
        }//如果玩家沒有下個可傳送點則無法前往

        Location loc = new Location();
        String toInfo = "未知傳送位置";

        switch (status) {
            case NEXT:
                loc = historyList.get(historyStatus + 1);
                toInfo = "下個傳送位置";
                break;
            case BACK:
                loc = historyList.get(historyStatus);
                toInfo = "前次傳送位置";
                break;
        }

        PlayerPortalEvent ev = new PlayerPortalEvent(pli, "目前位置", toInfo, player.getLocation(), loc, false);
        callEvent(ev);
        if (!ev.isCancelled()) {
            player.teleport(loc);
            pli.sendText("§a[傳送]§b已返回" + toInfo);
            return true;
        }
        return false;
    }

    private void addHistoryLocation(Player player, Location location) {
        portalHistoryMap.computeIfAbsent(player, k -> new ArrayList<>());
        ArrayList<Location> historyList = portalHistoryMap.get(player);
        historyList.add(location);
        if (historyList.size() > historyLimit) {
            historyList.remove(0);
        }
        portalHistoryMap.put(player, historyList);
        portalHistoryStatusMap.put(player, historyList.size());
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!event.canBack()) return;
        addHistoryLocation(event.getPlayer(), event.getFrom());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        addHistoryLocation(player, player.getLocation());
    }
}
