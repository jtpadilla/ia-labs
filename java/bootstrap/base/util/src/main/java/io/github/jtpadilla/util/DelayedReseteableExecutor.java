package io.github.jtpadilla.util;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class DelayedReseteableExecutor {

    final private Duration timeDelay;
    private Runnable runnable;
    private Instant limit;
    private ThreadFactory threadFactory;

    volatile private Thread thread;

    public DelayedReseteableExecutor(Duration timeDelay) {
        this.timeDelay = timeDelay;
        this.threadFactory = Thread.ofVirtual().name("DelayedReseteableExecutor", 0).factory();
    }

    public void shutdown() {
        Thread shutdownThread = thread;
        if (shutdownThread != null) {
            thread = null;
            try {
                shutdownThread.join();
            } catch (InterruptedException e) {
            }
        }
    }

    public void set(Runnable runnable) {
        synchronized (this) {
            this.runnable = runnable;
            limit = Instant.now().plus(timeDelay);
            if (thread == null) {
                thread = threadFactory.newThread(new PrivateRunnable(runnable));
                thread.start();
            }
        }
    }

    private class PrivateRunnable implements Runnable {

        final private Runnable runnable;

        private PrivateRunnable(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            try {
                while(thread != null) {
                    TimeUnit.SECONDS.sleep(2);
                    synchronized (DelayedReseteableExecutor.this) {
                        if (limit != null && Instant.now().isAfter(limit)) {
                            DelayedReseteableExecutor.this.runnable.run();
                            DelayedReseteableExecutor.this.runnable = null;
                            limit = null;
                            thread = null;
                            return;
                        }
                    }
                }
            } catch (InterruptedException e) {
            }
        }

    }

}
