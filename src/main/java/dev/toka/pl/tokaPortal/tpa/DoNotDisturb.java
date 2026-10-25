package dev.toka.pl.tokaPortal.tpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Who does not want requests, and until when. Memory only; the time is passed in. */
public final class DoNotDisturb {
    private final Map<UUID, Long> until = new HashMap<>();

    /** Switches on; the end is the cap from now, or never for an unlimited player. Returns the end in milliseconds. */
    public long enable(UUID id, long now, int maxMinutes, boolean unlimited) {
        long end = unlimited ? Long.MAX_VALUE : now + TpaRules.dndMinutes(maxMinutes) * 60_000L;
        until.put(id, end);
        return end;
    }

    public boolean disable(UUID id) {
        return until.remove(id) != null;
    }

    public boolean isOn(UUID id, long now) {
        Long end = until.get(id);
        return end != null && end > now;
    }

    /** Removes the ones that ran out and returns them, so that each can be told. */
    public List<UUID> expire(long now) {
        List<UUID> ended = new ArrayList<>();
        until.entrySet().removeIf(entry -> {
            if (entry.getValue() > now) return false;
            ended.add(entry.getKey());
            return true;
        });
        return ended;
    }

    public void clear() {
        until.clear();
    }
}
