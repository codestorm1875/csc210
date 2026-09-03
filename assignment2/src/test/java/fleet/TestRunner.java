package fleet;

import fleet.data.FleetData;
import fleet.model.MonitorVehicleTracker;
import fleet.model.MutablePoint;
import fleet.service.FleetSimulator;
import fleet.ui.FleetMapPanel;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TestRunner {
    private int passed;

    public static void main(String[] args) throws Exception {
        new TestRunner().run();
    }

    private void run() throws Exception {
        testInitialDefensiveCopy();
        testLocationDefensiveCopy();
        testStableSnapshot();
        testUnmodifiableSnapshotMap();
        testUnknownVehicle();
        testAtomicCoordinateUpdates();
        testSimulatorUsesWorkers();
        testMapRenders();
        System.out.println("All " + passed + " tests passed.");
    }

    private void testInitialDefensiveCopy() {
        MutablePoint originalPoint = new MutablePoint(10, 20);
        Map<String, MutablePoint> original = new LinkedHashMap<>();
        original.put("CAR-1", originalPoint);
        MonitorVehicleTracker tracker = new MonitorVehicleTracker(original);
        originalPoint.x = 99;
        original.put("CAR-2", new MutablePoint(3, 4));
        check(tracker.getLocation("CAR-1").x == 10, "constructor deep-copies points");
        check(tracker.size() == 1, "constructor does not retain caller map");
    }

    private void testLocationDefensiveCopy() {
        MonitorVehicleTracker tracker = trackerWithOneVehicle();
        MutablePoint exposed = tracker.getLocation("CAR-1");
        exposed.x = 77;
        exposed.y = 88;
        MutablePoint internal = tracker.getLocation("CAR-1");
        check(internal.x == 10 && internal.y == 20, "getLocation returns a defensive copy");
    }

    private void testStableSnapshot() {
        MonitorVehicleTracker tracker = trackerWithOneVehicle();
        Map<String, MutablePoint> snapshot = tracker.getLocations();
        tracker.setLocation("CAR-1", 30, 40);
        check(snapshot.get("CAR-1").x == 10 && snapshot.get("CAR-1").y == 20,
                "old snapshot stays unchanged after an update");
        check(tracker.getLocation("CAR-1").x == 30, "tracker receives the new location");
    }

    private void testUnmodifiableSnapshotMap() {
        Map<String, MutablePoint> snapshot = trackerWithOneVehicle().getLocations();
        boolean rejected = false;
        try {
            snapshot.put("CAR-2", new MutablePoint());
        } catch (UnsupportedOperationException expected) {
            rejected = true;
        }
        check(rejected, "snapshot map cannot be structurally modified");
    }

    private void testUnknownVehicle() {
        boolean rejected = false;
        try {
            trackerWithOneVehicle().setLocation("UNKNOWN", 1, 2);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "unknown vehicle update is rejected");
    }

    private void testAtomicCoordinateUpdates() throws Exception {
        MonitorVehicleTracker tracker = trackerWithOneVehicle();
        AtomicBoolean mixedPairSeen = new AtomicBoolean();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        Future<?> firstWriter = executor.submit(() -> writeRepeatedly(tracker, start, 11, 11));
        Future<?> secondWriter = executor.submit(() -> writeRepeatedly(tracker, start, 22, 22));
        Future<?> reader = executor.submit(() -> {
            await(start);
            for (int index = 0; index < 60_000; index++) {
                MutablePoint point = tracker.getLocation("CAR-1");
                if (point.x != point.y && !(point.x == 10 && point.y == 20)) {
                    mixedPairSeen.set(true);
                    return;
                }
            }
        });
        start.countDown();
        firstWriter.get();
        secondWriter.get();
        reader.get();
        executor.shutdownNow();
        check(!mixedPairSeen.get(), "readers never observe a partially updated coordinate pair");
    }

    private void testSimulatorUsesWorkers() throws Exception {
        MonitorVehicleTracker tracker = new MonitorVehicleTracker(FleetData.initialLocations());
        try (FleetSimulator simulator = new FleetSimulator(tracker, 4)) {
            simulator.start();
            Thread.sleep(1_400);
            check(simulator.updateCount() > 0, "GPS simulator produces updates");
            check(simulator.workerNames().size() >= 2, "GPS simulator uses multiple updater threads");
            simulator.pause();
            long count = simulator.updateCount();
            Thread.sleep(650);
            check(simulator.updateCount() == count, "paused simulator stops changing locations");
        }
    }

    private void testMapRenders() {
        FleetMapPanel map = new FleetMapPanel();
        map.setSnapshot(FleetData.initialLocations());
        map.setSelectedVehicle("CAB-101");
        Map<String, MutablePoint> moved = new LinkedHashMap<>(FleetData.initialLocations());
        moved.put("CAB-101", new MutablePoint(23, 15));
        map.setSnapshot(moved);
        FleetMapPanel.Telemetry telemetry = map.telemetrySnapshot().get("CAB-101");
        check(telemetry.speedKmh() > 0, "map derives vehicle speed from GPS movement");
        check(telemetry.heading().equals("East"), "map derives vehicle heading from GPS movement");
        map.setSize(760, 480);
        BufferedImage image = new BufferedImage(760, 480, BufferedImage.TYPE_INT_RGB);
        map.paint(image.getGraphics());
        int background = image.getRGB(2, 2);
        boolean hasContent = false;
        outer:
        for (int y = 10; y < 470; y += 5) {
            for (int x = 10; x < 750; x += 5) {
                if (image.getRGB(x, y) != background) {
                    hasContent = true;
                    break outer;
                }
            }
        }
        check(hasContent, "fleet map renders nonblank pixels");
    }

    private MonitorVehicleTracker trackerWithOneVehicle() {
        return new MonitorVehicleTracker(Map.of("CAR-1", new MutablePoint(10, 20)));
    }

    private void writeRepeatedly(MonitorVehicleTracker tracker, CountDownLatch start, int x, int y) {
        await(start);
        for (int index = 0; index < 40_000; index++) {
            tracker.setLocation("CAR-1", x, y);
        }
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private void check(boolean condition, String name) {
        if (!condition) throw new AssertionError("FAILED: " + name);
        passed++;
        System.out.println("PASS  " + name);
    }
}
