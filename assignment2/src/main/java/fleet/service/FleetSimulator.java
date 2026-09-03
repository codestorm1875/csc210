package fleet.service;

import fleet.model.MonitorVehicleTracker;
import fleet.model.MutablePoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Simulates independent GPS feeds that concurrently update the shared tracker. */
public final class FleetSimulator implements AutoCloseable {
    private static final int[] VERTICAL_ROADS = {12, 37, 62, 87};
    private static final int[] HORIZONTAL_ROADS = {15, 40, 65, 90};
    private final MonitorVehicleTracker tracker;
    private final ScheduledExecutorService executor;
    private final List<ScheduledFuture<?>> tasks = new ArrayList<>();
    private final Set<String> workerNames = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, MotionState> motion = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicLong updateCount = new AtomicLong();

    public FleetSimulator(MonitorVehicleTracker tracker, int workers) {
        this.tracker = tracker;
        AtomicInteger number = new AtomicInteger();
        executor = Executors.newScheduledThreadPool(workers, runnable -> {
            Thread thread = new Thread(runnable, "gps-updater-" + number.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void start() {
        running.set(true);
        if (!tasks.isEmpty()) return;

        for (String vehicleId : tracker.getLocations().keySet()) {
            long initialDelay = ThreadLocalRandom.current().nextLong(100, 450);
            tasks.add(executor.scheduleWithFixedDelay(
                    () -> updateVehicle(vehicleId), initialDelay, 550, TimeUnit.MILLISECONDS));
        }
    }

    public void pause() {
        running.set(false);
    }

    public void resume() {
        running.set(true);
    }

    public boolean isRunning() {
        return running.get();
    }

    public long updateCount() {
        return updateCount.get();
    }

    public Set<String> workerNames() {
        return Set.copyOf(workerNames);
    }

    private void updateVehicle(String vehicleId) {
        if (!running.get()) return;
        workerNames.add(Thread.currentThread().getName());
        MutablePoint current = tracker.getLocation(vehicleId);
        if (current == null) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        MotionState state = motion.computeIfAbsent(vehicleId, id -> initialMotion(id, current));
        int speed = random.nextInt(2, 5);
        int nextX = current.x;
        int nextY = current.y;

        if (state.horizontal) {
            nextY = nearest(HORIZONTAL_ROADS, current.y);
            nextX = current.x + state.direction * speed;
            if (nextX <= 2 || nextX >= 98) {
                state.direction *= -1;
                nextX = clamp(nextX);
            }
            int crossing = crossingRoad(current.x, nextX, VERTICAL_ROADS);
            if (crossing >= 0 && random.nextDouble() < 0.28) {
                nextX = crossing;
                state.horizontal = false;
                state.direction = random.nextBoolean() ? 1 : -1;
            }
        } else {
            nextX = nearest(VERTICAL_ROADS, current.x);
            nextY = current.y + state.direction * speed;
            if (nextY <= 2 || nextY >= 98) {
                state.direction *= -1;
                nextY = clamp(nextY);
            }
            int crossing = crossingRoad(current.y, nextY, HORIZONTAL_ROADS);
            if (crossing >= 0 && random.nextDouble() < 0.28) {
                nextY = crossing;
                state.horizontal = true;
                state.direction = random.nextBoolean() ? 1 : -1;
            }
        }
        tracker.setLocation(vehicleId, nextX, nextY);
        updateCount.incrementAndGet();
    }

    private MotionState initialMotion(String vehicleId, MutablePoint point) {
        boolean onVerticalRoad = distanceToNearest(VERTICAL_ROADS, point.x)
                < distanceToNearest(HORIZONTAL_ROADS, point.y);
        int direction = (vehicleId.hashCode() & 1) == 0 ? 1 : -1;
        return new MotionState(!onVerticalRoad, direction);
    }

    private int crossingRoad(int from, int to, int[] roads) {
        int low = Math.min(from, to);
        int high = Math.max(from, to);
        for (int road : roads) {
            if (road > low && road <= high) return road;
        }
        return -1;
    }

    private int nearest(int[] roads, int value) {
        int nearest = roads[0];
        for (int road : roads) {
            if (Math.abs(road - value) < Math.abs(nearest - value)) nearest = road;
        }
        return nearest;
    }

    private int distanceToNearest(int[] roads, int value) {
        return Math.abs(nearest(roads, value) - value);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    @Override
    public synchronized void close() {
        running.set(false);
        tasks.forEach(task -> task.cancel(true));
        tasks.clear();
        executor.shutdownNow();
    }

    private static final class MotionState {
        private boolean horizontal;
        private int direction;

        private MotionState(boolean horizontal, int direction) {
            this.horizontal = horizontal;
            this.direction = direction;
        }
    }
}
