package dev.toka.pl.tokaPortal.utils;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerTeleportEvent;
import cn.nukkit.level.Location;
import dev.toka.pl.tokaPortal.Main;
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
            pli.sendText(Main.text("err.portalCooldown"));
            return false;
        }

        if (portalHistoryMap.get(player) == null) {
            pli.sendText(Main.text("err.history.none"));
            return false;
        }

        int historyStatus = portalHistoryStatusMap.get(player);
        ArrayList<Location> historyList = portalHistoryMap.get(player);

        if (!Objects.equals(status, NEXT) && !Objects.equals(status, BACK)) return false;
        int target = Objects.equals(status, NEXT) ? historyStatus + 1 : historyStatus - 1;
        if (target < 0 || target >= historyList.size()) {
            pli.sendText(Main.text("err.history.noTarget"));
            return false;
        }
        Location loc = historyList.get(target);
        if (loc.getLevel() == null) { pli.sendText(Main.text("err.history.worldNotLoaded")); return false; }
        String toInfo = Objects.equals(status, NEXT) ? Main.text("status.history.next") : Main.text("status.history.back");

        PlayerPortalEvent ev = new PlayerPortalEvent(pli, Main.text("status.here"), toInfo, player.getLocation(), loc, false);
        callEvent(ev);
        if (!ev.isCancelled()) {
            Location playerBeforeTeleport = player.getLocation().clone();
            navigating.add(player);
            boolean success;
            try { success = player.teleport(loc); } finally { navigating.remove(player); }
            if (!success) return false;
            if (historyStatus == historyList.size()) historyList.add(playerBeforeTeleport);
            portalHistoryStatusMap.put(player, target);
            pli.sendText(Main.text("msg.history.arrived", toInfo));
            return true;
        }
        return false;
    }

    private void addHistoryLocation(Player player, Location location) {
        ArrayList<Location> historyList = portalHistoryMap.computeIfAbsent(player, k -> new ArrayList<>());
        int cursor = portalHistoryStatusMap.getOrDefault(player, historyList.size());
        if (cursor < historyList.size()) historyList.subList(cursor, historyList.size()).clear();
        historyList.add(location.clone());
        if (historyList.size() > historyLimit) {
            historyList.remove(0);
        }
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
