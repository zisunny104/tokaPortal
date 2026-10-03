package dev.toka.pl.tokaPortal.utils;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerTeleportEvent;
import cn.nukkit.level.Location;
import dev.toka.pl.tokaPortal.event.PlayerPortalEvent;
import dev.toka.pl.tokaPortal.integration.ZeroIntegration.PlayerInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.getPlayerInfo;
import static dev.toka.pl.tokaPortal.integration.ZeroIntegration.callEvent;

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
        }

        if (portalHistoryMap.get(player) == null) {
            pli.sendText("[傳送]尚無傳送紀錄。");
            return false;
        }

        int historyStatus = portalHistoryStatusMap.get(player);
        ArrayList<Location> historyList = portalHistoryMap.get(player);

        if (!Objects.equals(status, NEXT) && !Objects.equals(status, BACK)) return false;
        int target = Objects.equals(status, NEXT) ? historyStatus + 1 : historyStatus - 1;
        if (target < 0 || target >= historyList.size()) {
            pli.sendText("[傳送]沒有可前往的傳送紀錄。");
            return false;
        }
        Location loc = historyList.get(target);
        if (loc.getLevel() == null) { pli.sendText("[傳送]紀錄的世界尚未載入。"); return false; }
        String toInfo = Objects.equals(status, NEXT) ? "下個傳送位置" : "前次傳送位置";

        PlayerPortalEvent ev = new PlayerPortalEvent(pli, "目前位置", toInfo, player.getLocation(), loc, false);
        callEvent(ev);
        if (!ev.isCancelled()) {
            Location playerBeforeTeleport = player.getLocation().clone();
            navigating.add(player);
            boolean success;
            try { success = player.teleport(loc); } finally { navigating.remove(player); }
            if (!success) return false;
            if (historyStatus == historyList.size()) historyList.add(playerBeforeTeleport);
            portalHistoryStatusMap.put(player, target);
            pli.sendText("§a[傳送]§b已返回" + toInfo);
            return true;
        }
        return false;
    }

    private void addHistoryLocation(Player player, Location location) {
        portalHistoryMap.computeIfAbsent(player, k -> new ArrayList<>());
        ArrayList<Location> historyList = portalHistoryMap.get(player);
        int cursor = portalHistoryStatusMap.getOrDefault(player, historyList.size());
        if (cursor < historyList.size()) historyList.subList(cursor, historyList.size()).clear();
        historyList.add(location.clone());
        if (historyList.size() > historyLimit) {
            historyList.remove(0);
        }
        portalHistoryMap.put(player, historyList);
        portalHistoryStatusMap.put(player, historyList.size());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        addHistoryLocation(player, player.getLocation());
    }

    private static final java.util.Set<Player> navigating = new java.util.HashSet<>();

    @EventHandler(priority = cn.nukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!navigating.contains(event.getPlayer())) addHistoryLocation(event.getPlayer(), event.getFrom());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        portalHistoryMap.remove(player);
        portalHistoryStatusMap.remove(player);
        navigating.remove(player);
        Portal.clearPlayer(player);
    }
}
