package dev.toka.pl.tokaPortal.tpa;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.potion.Effect;
import cn.nukkit.scheduler.TaskHandler;
import dev.toka.pl.tokaPortal.Main;
import dev.toka.pl.tokaPortal.integration.Services.PlayerInfo;
import dev.toka.pl.tokaPortal.tpa.TpaRequests.Kind;
import dev.toka.pl.tokaPortal.tpa.TpaRequests.Request;
import dev.toka.pl.tokaPortal.utils.Portal;
import dev.toka.pl.tokaPortal.utils.PortalWindow;

import java.util.UUID;

import static dev.toka.pl.tokaPortal.integration.Services.getPlayerInfo;

/**
 * Player-to-player teleport on the server: the requests with their bar at the top of the screen, do-not-disturb, the forced
 * teleport and the silent one. The bar is the one of the restart countdown: tokaDisplay when it is there, else the action bar.
 * Main thread only.
 */
public final class TpaFlow {
    private static final TpaRequests requests = new TpaRequests();
    private static final DoNotDisturb dnd = new DoNotDisturb();
    private static TaskHandler dndTask;

    private TpaFlow() {
    }

    public static void start(Main plugin) {
        if (dndTask != null) dndTask.cancel();
        dndTask = plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, TpaFlow::tickDnd, 20);
    }

    /** onDisable: forget every request and every do-not-disturb without a word. */
    public static void shutdown() {
        if (dndTask != null) dndTask.cancel();
        dndTask = null;
        requests.clear();
        dnd.clear();
    }

    /** Reload: the requests end, the do-not-disturb stays. */
    public static void cancelAll() {
        requests.clear();
    }

    private static int config(String key, int fallback) {
        return Main.getInstance().getConfig().getInt(key, fallback);
    }

    private static Player online(UUID id) {
        Player player = Server.getInstance().getOnlinePlayers().get(id);
        return player != null && player.isOnline() ? player : null;
    }

    /** The request waiting for this target, or null. */
    public static Request pending(Player target) {
        return requests.forTarget(target.getUniqueId());
    }

    /** /tpa (here=false) and /tpahere (here=true): always a question for the other player. */
    public static void request(Player from, Player to, boolean here) {
        request(from, to, here, false);
    }

    /**
     * /tpahere with the word force: a player of the force or silent tier brings the other player over. The other one is not asked and
     * do-not-disturb does not stop it, but there is always the countdown with its bar, and /tpdeny stops it.
     */
    public static void request(Player from, Player to, boolean here, boolean force) {
        if (from == null || to == null || !from.isOnline() || !to.isOnline()) return;
        PlayerInfo fromInfo = getPlayerInfo(from);
        if (from.getUniqueId().equals(to.getUniqueId())) {
            fromInfo.sendText(Main.text("err.tpa.self"));
            return;
        }
        if (here && force && fromInfo.tpaTier() == TpaRules.Tier.NORMAL) {
            fromInfo.sendText(Main.text("err.tpa.pullDenied"));
            return;
        }
        var tier = here ? TpaRules.Tier.NORMAL : fromInfo.tpaTier();
        if (tier == TpaRules.Tier.SILENT) {
            silent(from, to);
            return;
        }
        if (tier == TpaRules.Tier.NORMAL && !(here && force) && dnd.isOn(to.getUniqueId(), System.currentTimeMillis())) {
            fromInfo.sendText(Main.text("err.tpa.dnd"));
            return;
        }
        Kind kind = here ? (force ? Kind.PULL : Kind.HERE) : tier == TpaRules.Tier.FORCE ? Kind.FORCE : Kind.ASK;
        int seconds = kind == Kind.FORCE || kind == Kind.PULL ? TpaRules.forceDelay(config("tpa.force.delay-seconds", TpaRules.FORCE_DEFAULT))
                : TpaRules.requestSeconds(config("tpa.request.seconds", TpaRules.REQUEST_DEFAULT));
        Request request = new Request(from.getUniqueId(), to.getUniqueId(), kind, seconds);
        switch (requests.add(request)) {
            case TARGET_BUSY -> fromInfo.sendText(Main.text("err.tpa.pending"));
            case SENDER_BUSY -> fromInfo.sendText(Main.text("err.tpa.senderBusy"));
            case SELF -> fromInfo.sendText(Main.text("err.tpa.self"));
            case OK -> begin(request, from, to);
        }
    }

    private static void begin(Request request, Player from, Player to) {
        Main plugin = Main.getInstance();
        TaskHandler timer = plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, () -> tick(request), 20);
        request.onEnd(timer::cancel);
        PlayerInfo fromInfo = getPlayerInfo(from);
        switch (request.kind()) {
            case ASK -> fromInfo.sendText(Main.text("msg.tpa.sent", to.getName()));
            case HERE -> fromInfo.sendText(Main.text("msg.tpa.sentHere", to.getName()));
            case FORCE -> fromInfo.sendText(Main.text("msg.tpa.forced", to.getName(), request.total()));
            case PULL -> fromInfo.sendText(Main.text("msg.tpa.pulled", to.getName(), request.total()));
        }
        bar(request, from, to);
        if (request.kind() != Kind.FORCE && request.kind() != Kind.PULL && plugin.getConfig().getBoolean("tpa.request.window", false)) PortalWindow.sendPortalTpaAcceptWindow(to);
    }

    private static void tick(Request request) {
        if (requests.forTarget(request.target()) != request) return;
        Player from = online(request.sender()), to = online(request.target());
        if (from == null || to == null) {
            requests.remove(request);
            return;
        }
        if (request.tick() > 0) {
            bar(request, from, to);
            return;
        }
        requests.remove(request);
        if (request.kind() == Kind.FORCE) go(from, to);
        else if (request.kind() == Kind.PULL) go(to, from);
        else getPlayerInfo(from).sendText(Main.text("msg.tpa.expired", to.getName()));
    }

    private static void bar(Request request, Player from, Player to) {
        String key = switch (request.kind()) {
            case ASK -> "ui.bar.tpa.ask";
            case HERE -> "ui.bar.tpa.here";
            case FORCE -> "ui.bar.tpa.force";
            case PULL -> "ui.bar.tpa.pull";
        };
        TopBar.show(to, Main.text(key, from.getName(), request.left()), (float) request.left() / request.total(),
                TopBar.color(request.kind() == Kind.FORCE || request.kind() == Kind.PULL, request.left(), request.total()));
    }

    /** The traveller goes to the destination through the portal path, with its event and checks. */
    private static void go(Player traveller, Player destination) {
        if (Portal.portal(traveller, destination.getLocation(), destination.getName()))
            getPlayerInfo(traveller).sendText(Main.text("msg.tpa.arrived", destination.getName()));
    }

    private static void silent(Player from, Player to) {
        if (!Portal.portal(from, to.getLocation(), to.getName())) return;
        getPlayerInfo(from).sendText(Main.text("msg.tpa.arrived", to.getName()));
        int seconds = TpaRules.invisibleSeconds(config("tpa.silent.invisible-seconds", TpaRules.INVISIBLE_DEFAULT));
        from.addEffect(Effect.getEffect(Effect.INVISIBILITY).setVisible(false).setAmplifier(0).setDuration(seconds * 20));
    }

    /** /tpaccept, and the agree button of the window (expected is the request the window was made for, else null). */
    public static void accept(Player target, Request expected) {
        Request request = pending(target);
        if (request == null || (expected != null && request != expected)) {
            if (expected == null) getPlayerInfo(target).sendText(Main.text("err.tpa.noRequest"));
            return;
        }
        if (request.kind() == Kind.FORCE || request.kind() == Kind.PULL) {
            getPlayerInfo(target).sendText(Main.text("err.tpa.forcedNoAccept"));
            return;
        }
        requests.remove(request);
        Player from = online(request.sender());
        if (from == null) {
            getPlayerInfo(target).sendText(Main.text("err.tpa.gone"));
            return;
        }
        if (request.kind() == Kind.HERE) go(target, from);
        else go(from, target);
    }

    /** /tpdeny, and the refuse button of the window; it also stops a forced teleport. */
    public static void deny(Player target, Request expected) {
        Request request = pending(target);
        if (request == null || (expected != null && request != expected)) {
            if (expected == null) getPlayerInfo(target).sendText(Main.text("err.tpa.noRequest"));
            return;
        }
        requests.remove(request);
        Player from = online(request.sender());
        if (from != null) getPlayerInfo(from).sendText(Main.text("msg.tpa.declined", target.getName()));
        getPlayerInfo(target).sendText(Main.text("msg.tpa.denied"));
    }

    /** /tpcancel: the sender takes the request back. */
    public static void cancel(Player sender) {
        Request request = requests.forSender(sender.getUniqueId());
        if (request == null) {
            getPlayerInfo(sender).sendText(Main.text("err.tpa.noSent"));
            return;
        }
        requests.remove(request);
        Player target = online(request.target());
        if (target != null) getPlayerInfo(target).sendText(Main.text("msg.tpa.withdrawn", sender.getName()));
        getPlayerInfo(sender).sendText(Main.text("msg.tpa.stopped"));
    }

    /** /dnd [on|off]; no argument switches. */
    public static void dnd(Player player, String argument) {
        boolean on = dnd.isOn(player.getUniqueId(), System.currentTimeMillis());
        boolean want = argument == null ? !on : argument.equalsIgnoreCase("on");
        PlayerInfo info = getPlayerInfo(player);
        if (!want) {
            dnd.disable(player.getUniqueId());
            info.sendText(Main.text("msg.dnd.off"));
            return;
        }
        boolean unlimited = player.hasPermission("tokaportal.dnd.unlimited");
        int minutes = TpaRules.dndMinutes(config("dnd.max-minutes", TpaRules.DND_DEFAULT_MINUTES));
        dnd.enable(player.getUniqueId(), System.currentTimeMillis(), minutes, unlimited);
        info.sendText(unlimited ? Main.text("msg.dnd.onUnlimited") : Main.text("msg.dnd.on", minutes));
    }

    private static void tickDnd() {
        for (UUID id : dnd.expire(System.currentTimeMillis())) {
            Player player = online(id);
            if (player != null) getPlayerInfo(player).sendText(Main.text("msg.dnd.expired"));
        }
    }

    /** The player left: their requests end, the sender is told when it was their target who left, the do-not-disturb goes. */
    public static void playerQuit(Player player) {
        UUID id = player.getUniqueId();
        for (Request request : requests.removeInvolving(id)) {
            if (!request.target().equals(id)) continue;
            Player from = online(request.sender());
            if (from != null) getPlayerInfo(from).sendText(Main.text("err.tpa.gone"));
        }
        dnd.disable(id);
    }
}
