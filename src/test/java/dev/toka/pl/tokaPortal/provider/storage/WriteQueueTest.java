package dev.toka.pl.tokaPortal.provider.storage;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class WriteQueueTest {
    @Test
    void runsInOrderOnBackgroundThreadAndReportsFailures() {
        List<String> errors = new CopyOnWriteArrayList<>();
        List<Integer> order = new CopyOnWriteArrayList<>();
        Set<String> threads = ConcurrentHashMap.newKeySet();
        WriteQueue queue = new WriteQueue((key, error) -> errors.add(key));
        for (int i = 0; i < 50; i++) {
            int n = i;
            queue.submit("fail", () -> {
                threads.add(Thread.currentThread().getName());
                if (n == 10) throw new IOException("boom");
                order.add(n);
            });
        }
        assertTrue(queue.drain(5000));
        assertEquals(49, order.size());
        assertTrue(order.equals(order.stream().sorted().toList()));
        assertEquals(List.of("fail"), errors);
        assertEquals(Set.of("tokaPortal-writer"), threads);
        queue.shutdown();
    }
}
