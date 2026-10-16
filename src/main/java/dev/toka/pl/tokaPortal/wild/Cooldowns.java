package dev.toka.pl.tokaPortal.wild;
import java.util.*;
import java.util.function.LongSupplier;
/** Business clock independent of every display and player-session object. */
public final class Cooldowns {
 private final Map<UUID,Long> deadlines=new HashMap<>();private final LongSupplier clock;
 public Cooldowns(LongSupplier clock){this.clock=clock;}
 public long remainingMillis(UUID uuid){return Math.max(0,(deadlines.getOrDefault(uuid,Long.MIN_VALUE)==Long.MIN_VALUE?0:deadlines.get(uuid)-clock.getAsLong())/1_000_000L);}
 public void start(UUID uuid,long seconds){if(seconds<1||seconds>86400)throw new IllegalArgumentException("seconds");deadlines.put(uuid,clock.getAsLong()+seconds*1_000_000_000L);}
 public Set<UUID> players(){deadlines.entrySet().removeIf(e->e.getValue()<=clock.getAsLong());return Set.copyOf(deadlines.keySet());}
 public void clear(){deadlines.clear();}
}
