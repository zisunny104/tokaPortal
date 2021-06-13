package dev.toka.pl.tokaPortal.event;

import cn.nukkit.Player;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.Event;
import cn.nukkit.event.HandlerList;
import cn.nukkit.level.Location;
import prj.toka.zero.player.PlayerInfo;

public class PlayerPortalEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    boolean cancelled = false;

    protected Player player;
    protected PlayerInfo pli;
    protected String fromInfo;
    protected String toInfo;
    protected Location from;
    protected Location to;
    protected boolean canBack;

    public PlayerPortalEvent(PlayerInfo pli, String fromInfo, String toInfo, Location from, Location to) {
        this.player = pli.getPlayer();
        this.pli = pli;
        this.fromInfo = fromInfo;
        this.toInfo = toInfo;
        this.from = from;
        this.to = to;
        this.canBack = true;
    }
    public PlayerPortalEvent(PlayerInfo pli, String fromInfo, String toInfo, Location from, Location to, boolean canBack) {
        this.player = pli.getPlayer();
        this.pli = pli;
        this.fromInfo = fromInfo;
        this.toInfo = toInfo;
        this.from = from;
        this.to = to;
        this.canBack = canBack;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerInfo getPli() {
        return pli;
    }

    public String getFromInfo() {
        return fromInfo;
    }

    public String getToInfo() {
        return toInfo;
    }

    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }

    public boolean canBack(){
        return canBack;
    }

    public String toString() {
        return "[傳送]" + pli.getNameTag() + "從'"
                + fromInfo + "'(" + from.getFloorX() + "," + from.getFloorY() + "," + from.getFloorZ() + "," + from.level.getName() + ") 傳送至 ' "
                + toInfo + "'(" + to.getFloorX() + "," + to.getFloorY() + "," + to.getFloorZ() + "," + to.level.getName() + ")";
    }


    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    public static HandlerList getHandlers() {
        return handlers;
    }
}

