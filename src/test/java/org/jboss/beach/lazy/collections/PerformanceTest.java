package org.jboss.beach.lazy.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.junit.Test;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class PerformanceTest {
    private static void backgroundTask(Callable<Void> callable) {
        final FutureTask<Void> task = new FutureTask<>(callable);
        final Thread thread = new Thread(task);
        thread.start();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore for now
        }
    }

    private static Iterable<String> step1Fast() {
        final LazyArrayList<String> result = new LazyArrayList<>();
        backgroundTask(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    sleep(100);
                    result.add("Item " + i);
                }
                result.done();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return null;
        });
        return result;
    }

    private static Iterable<String> step1Slow() {
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            sleep(100);
            result.add("Item " + i);
        }
        return result;
    }

    private static void step2(final Iterable<String> items) {
        for (String item : items) {
            System.out.println(item);
            sleep(100);
        }
    }

    @Test
    public void testFast() {
        final long start = System.currentTimeMillis();
        Iterable<String> result = step1Fast();
        step2(result);
        final long elapsed = System.currentTimeMillis() - start;
        System.out.println(elapsed);
    }

    @Test
    public void testSlow() {
        final long start = System.currentTimeMillis();
        Iterable<String> result = step1Slow();
        step2(result);
        final long elapsed = System.currentTimeMillis() - start;
        System.out.println(elapsed);
    }
}
