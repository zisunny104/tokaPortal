package dev.toka.pl.tokaPortal.plan;
import com.djrapitops.plan.extension.NotReadyException;
import com.djrapitops.plan.extension.extractor.ExtensionExtractor;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
class PlanTest {
 @Test void settingsArePrivateAndRefreshBounded(){
  assertFalse(PlanSettings.parse(Map.of()).enabled());
  assertFalse(PlanSettings.parse(Map.of()).homes());
  assertEquals(100,PlanSettings.parse(Map.of("plan",Map.of("refresh-seconds",0))).refreshTicks());
 }
 @Test void snapshotDetachedMissingAndClosedUnavailable(){
  UUID id=UUID.randomUUID();Map<UUID,Long> source=new HashMap<>();source.put(id,3L);
  var snapshot=new PlanSnapshot(source,5,7);source.clear();assertEquals(3L,snapshot.homes().get(id));
  assertThrows(UnsupportedOperationException.class,()->snapshot.homes().clear());
  AtomicReference<PlanSnapshot> published=new AtomicReference<>();
  PlanExtension extension=new PlanExtension(new PlanSettings(true,true,true,600),published::get,s->s);
  new ExtensionExtractor(extension).validateAnnotations();
  assertThrows(NotReadyException.class,extension::snapshot);
  published.set(snapshot);assertSame(snapshot,extension.snapshot());extension.close();
  assertThrows(NotReadyException.class,extension::snapshot);
 }
}
