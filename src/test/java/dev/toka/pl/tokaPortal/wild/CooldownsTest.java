package dev.toka.pl.tokaPortal.wild;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
public class CooldownsTest {
 @Test public void clockIsIndependentOfDisplay(){var time=new AtomicLong(1000);var c=new Cooldowns(time::get);UUID id=UUID.randomUUID();assertEquals(0,c.remainingMillis(id));c.start(id,40);time.addAndGet(10_000_000_000L);assertEquals(30000,c.remainingMillis(id));time.addAndGet(30_000_000_000L);assertEquals(0,c.remainingMillis(id));assertTrue(c.players().isEmpty());}
 @Test public void playersAreIsolated(){var c=new Cooldowns(()->0);var a=UUID.randomUUID();c.start(a,40);assertEquals(0,c.remainingMillis(UUID.randomUUID()));assertEquals(40000,c.remainingMillis(a));assertThrows(IllegalArgumentException.class,()->c.start(a,0));}
}

