package dev.toka.pl.tokaPortal.plan;
import java.util.*;
/** Produced on the main thread; published atomically and read by Plan's background providers. */
public record PlanSnapshot(Map<UUID, Long> homes, long totalHomes, long publicPoints) {
    public PlanSnapshot { homes = Map.copyOf(homes); }
}
