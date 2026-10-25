package dev.toka.pl.tokaPortal.tpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The pending requests, one per target and one per sender, by player id. Whatever ends a request runs its end hook once. */
public final class TpaRequests {
    /** ASK: the sender goes to the target. HERE: the target goes to the sender. FORCE: the sender goes to the target unless stopped. */
    public enum Kind { ASK, HERE, FORCE }

    public enum Result { OK, SELF, TARGET_BUSY, SENDER_BUSY }

    public static final class Request {
        private final UUID sender, target;
        private final Kind kind;
        private final int total;
        private int left;
        private Runnable end = () -> {
        };

        public Request(UUID sender, UUID target, Kind kind, int seconds) {
            this.sender = sender;
            this.target = target;
            this.kind = kind;
            this.total = Math.max(1, seconds);
            this.left = this.total;
        }

        public UUID sender() { return sender; }

        public UUID target() { return target; }

        public Kind kind() { return kind; }

        public int total() { return total; }

        public int left() { return left; }

        /** One second passes; returns the seconds left. */
        public int tick() { return left = Math.max(0, left - 1); }

        /** What to do when the request is over, such as stopping its timer. */
        public void onEnd(Runnable hook) { this.end = hook == null ? () -> {
        } : hook; }
    }

    private final Map<UUID, Request> byTarget = new HashMap<>();
    private final Map<UUID, Request> bySender = new HashMap<>();

    public Result add(Request request) {
        if (request.sender.equals(request.target)) return Result.SELF;
        if (byTarget.containsKey(request.target)) return Result.TARGET_BUSY;
        if (bySender.containsKey(request.sender)) return Result.SENDER_BUSY;
        byTarget.put(request.target, request);
        bySender.put(request.sender, request);
        return Result.OK;
    }

    public Request forTarget(UUID target) { return byTarget.get(target); }

    public Request forSender(UUID sender) { return bySender.get(sender); }

    /** Ends the request; false when it was not pending any more. */
    public boolean remove(Request request) {
        if (request == null || byTarget.get(request.target) != request) return false;
        byTarget.remove(request.target);
        bySender.remove(request.sender);
        request.end.run();
        return true;
    }

    /** Ends every request the player is in, as sender or as target, and returns them. */
    public List<Request> removeInvolving(UUID player) {
        List<Request> found = new ArrayList<>();
        Request asTarget = byTarget.get(player), asSender = bySender.get(player);
        if (asTarget != null) found.add(asTarget);
        if (asSender != null && asSender != asTarget) found.add(asSender);
        found.forEach(this::remove);
        return found;
    }

    /** Ends every request. */
    public void clear() {
        new ArrayList<>(byTarget.values()).forEach(this::remove);
    }

    public int size() { return byTarget.size(); }
}
