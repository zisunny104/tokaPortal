package dev.toka.pl.tokaPortal.wild;
import java.util.*;
import java.util.function.LongSupplier;
/** Cooldown deadlines per player on a monotonic clock. */
public final class Cooldowns {
 private final Map<UUID,Long> deadlines=new HashMap<>();private final LongSupplier clock;
 public Cooldowns(LongSupplier clock){this.clock=clock;}
 public long remainingMillis(UUID uuid){Long deadline=deadlines.get(uuid);return deadline==null?0:Math.max(0,(deadline-clock.getAsLong())/1_000_000L);}
 public void start(UUID uuid,long seconds){if(seconds<1||seconds>86400)throw new IllegalArgumentException("seconds");deadlines.put(uuid,clock.getAsLong()+seconds*1_000_000_000L);}
 public Set<UUID> players(){deadlines.entrySet().removeIf(e->e.getValue()<=clock.getAsLong());return Set.copyOf(deadlines.keySet());}
 public void clear(){deadlines.clear();}
}
