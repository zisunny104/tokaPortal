package dev.toka.pl.tokaPortal.tpa;

import dev.toka.pl.tokaPortal.tpa.TpaRequests.Kind;
import dev.toka.pl.tokaPortal.tpa.TpaRequests.Request;
import dev.toka.pl.tokaPortal.tpa.TpaRequests.Result;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TpaLogicTest {
    private final UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();

    @Test
    void secondsAreClamped() {
        assertEquals(15, TpaRules.requestSeconds(0));
        assertEquals(15, TpaRules.requestSeconds(-5));
        assertEquals(20, TpaRules.requestSeconds(20));
        assertEquals(30, TpaRules.requestSeconds(500));
        assertEquals(3, TpaRules.forceDelay(0));
        assertEquals(60, TpaRules.forceDelay(1000));
        assertEquals(1, TpaRules.invisibleSeconds(0));
        assertEquals(600, TpaRules.invisibleSeconds(99999));
        assertEquals(1, TpaRules.dndMinutes(-3));
    }

    @Test
    void tiersMapOntoPermissions() {
        assertEquals(TpaRules.Tier.NORMAL, TpaRules.tier(TpaRules.silent(false, false, false), TpaRules.force(false, false)));
        assertEquals(TpaRules.Tier.SILENT, TpaRules.tier(TpaRules.silent(true, false, false), false));
        assertEquals(TpaRules.Tier.SILENT, TpaRules.tier(TpaRules.silent(false, true, false), false));
        assertEquals(TpaRules.Tier.SILENT, TpaRules.tier(TpaRules.silent(false, false, true), TpaRules.force(true, true)));
        assertEquals(TpaRules.Tier.FORCE, TpaRules.tier(false, TpaRules.force(true, false)));
        assertEquals(TpaRules.Tier.FORCE, TpaRules.tier(false, TpaRules.force(false, true)));
    }

    @Test
    void dndStopsAtTheCapAndUnlimitedStays() {
        DoNotDisturb dnd = new DoNotDisturb();
        long end = dnd.enable(a, 1000, 30, false);
        assertEquals(1000 + 30 * 60_000L, end);
        assertTrue(dnd.isOn(a, end - 1));
        assertFalse(dnd.isOn(a, end));
        assertEquals(java.util.List.of(a), dnd.expire(end));
        assertTrue(dnd.expire(end).isEmpty());
        dnd.enable(b, 0, 30, true);
        assertTrue(dnd.isOn(b, Long.MAX_VALUE - 1));
        assertTrue(dnd.expire(Long.MAX_VALUE - 1).isEmpty());
        assertTrue(dnd.disable(b));
        assertFalse(dnd.isOn(b, 0));
        dnd.enable(c, 0, 0, false);
        assertTrue(dnd.isOn(c, 59_999));
    }

    @Test
    void oneRequestPerTargetAndPerSender() {
        TpaRequests requests = new TpaRequests();
        assertEquals(Result.SELF, requests.add(new Request(a, a, Kind.ASK, 20)));
        assertEquals(Result.OK, requests.add(new Request(a, b, Kind.ASK, 20)));
        assertEquals(Result.TARGET_BUSY, requests.add(new Request(c, b, Kind.ASK, 20)));
        assertEquals(Result.SENDER_BUSY, requests.add(new Request(a, c, Kind.HERE, 20)));
        assertEquals(1, requests.size());
    }

    @Test
    void endingARequestRunsItsHookOnceAndFreesBothSides() {
        TpaRequests requests = new TpaRequests();
        AtomicInteger ended = new AtomicInteger();
        Request request = new Request(a, b, Kind.ASK, 20);
        request.onEnd(ended::incrementAndGet);
        requests.add(request);
        assertSame(request, requests.forTarget(b));
        assertSame(request, requests.forSender(a));
        assertTrue(requests.remove(request));
        assertFalse(requests.remove(request));
        assertEquals(1, ended.get());
        assertNull(requests.forTarget(b));
        assertEquals(Result.OK, requests.add(new Request(a, c, Kind.ASK, 20)));
    }

    @Test
    void quittingEndsRequestsOfBothRoles() {
        TpaRequests requests = new TpaRequests();
        AtomicInteger ended = new AtomicInteger();
        Request out = new Request(b, c, Kind.ASK, 20), in = new Request(a, b, Kind.FORCE, 10);
        out.onEnd(ended::incrementAndGet);
        in.onEnd(ended::incrementAndGet);
        requests.add(out);
        requests.add(in);
        assertEquals(2, requests.removeInvolving(b).size());
        assertEquals(2, ended.get());
        assertEquals(0, requests.size());
    }

    @Test
    void countdownReachesZeroAndStaysThere() {
        Request request = new Request(a, b, Kind.ASK, 2);
        assertEquals(1, request.tick());
        assertEquals(0, request.tick());
        assertEquals(0, request.tick());
        assertEquals(2, request.total());
    }

    @Test
    void clearEndsEverything() {
        TpaRequests requests = new TpaRequests();
        AtomicInteger ended = new AtomicInteger();
        Request one = new Request(a, b, Kind.ASK, 20), two = new Request(c, a, Kind.HERE, 20);
        one.onEnd(ended::incrementAndGet);
        two.onEnd(ended::incrementAndGet);
        requests.add(one);
        requests.add(two);
        requests.clear();
        assertEquals(2, ended.get());
        assertEquals(0, requests.size());
    }

    @Test
    void barColourFollowsTheTimeLeftAndBlinksAtTheEnd() {
        assertEquals("GREEN", TopBar.color(false, 20, 20));
        assertEquals("YELLOW", TopBar.color(false, 9, 20));
        assertEquals("RED", TopBar.color(false, 5, 20));
        assertEquals("WHITE", TopBar.color(false, 4, 20));
        assertEquals("RED", TopBar.color(false, 3, 20));
        assertEquals("PURPLE", TopBar.color(true, 9, 10));
        assertEquals("RED", TopBar.color(true, 4, 10));
        assertEquals("PURPLE", TopBar.color(true, 3, 10));
    }
}
