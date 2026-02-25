package io.github.jtpadilla.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TemporalContainer<T> {

    @FunctionalInterface
    public interface ContainerListener<T> {
        void processElements(List<T> elements);
    }

    private final List<T> buffer;
    private final int maxElements;
    private final long maxTime; // No longer maxTimeMs
    private final TimeUnit timeUnit; //  Store the TimeUnit
    private final ContainerListener<T> listener;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> currentTask;
    private final Lock lock = new ReentrantLock();


    public TemporalContainer(int maxElements, long maxTime, TimeUnit timeUnit, ContainerListener<T> listener) {
        if (maxElements <= 0) {
            throw new IllegalArgumentException("maxElements must be greater than 0");
        }
        if (maxTime <= 0) {
            throw new IllegalArgumentException("maxTime must be greater than 0");
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("timeUnit cannot be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }

        this.buffer = new ArrayList<>();
        this.maxElements = maxElements;
        this.maxTime = maxTime;
        this.timeUnit = timeUnit; // Store timeUnit
        this.listener = listener;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
        this.currentTask = null;
        restartTimer();
    }

    public void add(T element) {
        lock.lock();
        try {
            buffer.add(element);
            if (buffer.size() >= maxElements) {
                processAndClear();
            } else {
                restartTimer();
            }
        } finally {
            lock.unlock();
        }
    }

    private void processAndClear() {
        if (buffer.isEmpty()) {
            return;
        }
        cancelCurrentTask();
        List<T> bufferCopy = new ArrayList<>(buffer);
        listener.processElements(bufferCopy);
        buffer.clear();
        restartTimer();
}

    private synchronized void restartTimer() {
        cancelCurrentTask();
        currentTask = scheduler.schedule(this::processAndClear, maxTime, timeUnit); // Use stored timeUnit
    }

    private void cancelCurrentTask() {
        if (currentTask != null) {
            currentTask.cancel(false);
            currentTask = null;
        }
    }

    public void close() {
        cancelCurrentTask();
        scheduler.shutdownNow();
        try {
            processAndClear();
        } finally {
            lock.unlock();
        }

        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                System.err.println("Executor did not terminate in a timely manner.");
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for executor termination.");
            Thread.currentThread().interrupt();
        }
    }


}