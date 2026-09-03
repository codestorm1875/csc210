package fleet.ui;

import fleet.data.FleetData;
import fleet.model.MutablePoint;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Live isometric city map. Tracker snapshots remain immutable while this view
 * interpolates between them for fluid motion on the Swing event thread.
 */
public final class FleetMapPanel extends JPanel {
    private static final int PAD_X = 48;
    private static final int PAD_Y = 40;
    private static final int[] VERTICAL_ROADS = {12, 37, 62, 87};
    private static final int[] HORIZONTAL_ROADS = {15, 40, 65, 90};
    private static final Color GROUND = new Color(222, 230, 224);
    private static final Color ROAD = new Color(79, 84, 89);
    private static final Color ROAD_EDGE = new Color(196, 202, 199);
    private static final Color LANE = new Color(241, 204, 93);
    private static final Color PARK = new Color(153, 190, 151);

    private final Map<String, AnimatedVehicle> vehicles = new LinkedHashMap<>();
    private final Timer animationTimer = new Timer(16, event -> repaint());
    private String selectedVehicle;
    private String hoveredVehicle;
    private BiConsumer<Integer, Integer> dispatchHandler = (x, y) -> { };
    private Consumer<String> selectionHandler = ignored -> { };

    public FleetMapPanel() {
        setBackground(new Color(228, 233, 236));
        ToolTipManager.sharedInstance().registerComponent(this);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                String next = vehicleAt(event.getPoint());
                if (!java.util.Objects.equals(next, hoveredVehicle)) {
                    hoveredVehicle = next;
                    setCursor(Cursor.getPredefinedCursor(
                            next == null ? Cursor.CROSSHAIR_CURSOR : Cursor.HAND_CURSOR));
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hoveredVehicle = null;
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                String hit = vehicleAt(event.getPoint());
                if (hit != null) {
                    selectionHandler.accept(hit);
                    return;
                }
                if (selectedVehicle == null) return;
                MutablePoint point = mapPoint(event.getPoint());
                dispatchHandler.accept(point.x, point.y);
            }
        };
        addMouseMotionListener(mouse);
        addMouseListener(mouse);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        animationTimer.start();
    }

    @Override
    public void removeNotify() {
        animationTimer.stop();
        super.removeNotify();
    }

    public void setSnapshot(Map<String, MutablePoint> snapshot) {
        long now = System.nanoTime();
        vehicles.keySet().retainAll(snapshot.keySet());
        snapshot.forEach((id, location) -> vehicles.compute(id, (key, vehicle) -> {
            if (vehicle == null) return new AnimatedVehicle(location.x, location.y);
            vehicle.moveTo(location.x, location.y, now);
            return vehicle;
        }));
        repaint();
    }

    public void setSelectedVehicle(String selectedVehicle) {
        this.selectedVehicle = selectedVehicle;
        repaint();
    }

    public void setDispatchHandler(BiConsumer<Integer, Integer> handler) {
        dispatchHandler = handler;
    }

    public void setSelectionHandler(Consumer<String> handler) {
        selectionHandler = handler;
    }

    public Map<String, Telemetry> telemetrySnapshot() {
        long now = System.nanoTime();
        Map<String, Telemetry> result = new LinkedHashMap<>();
        vehicles.forEach((id, vehicle) -> result.put(id, vehicle.telemetry(now)));
        return Map.copyOf(result);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        drawBackdrop(g);
        drawGround(g);
        drawRoadNetwork(g);
        List<Building> buildings = prepareCity(g);
        drawStreetDetails(g);
        drawTrails(g);
        drawDepthSortedScene(g, buildings);
        drawActiveVehicleLabels(g);
        drawMapChrome(g);
        g.dispose();
    }

    private void drawBackdrop(Graphics2D g) {
        g.setPaint(new java.awt.GradientPaint(0, 0, new Color(236, 241, 244),
                0, getHeight(), new Color(214, 224, 227)));
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawGround(Graphics2D g) {
        Polygon ground = polygon(project(0, 0), project(100, 0),
                project(100, 100), project(0, 100));
        Polygon shadow = translate(ground, 0, 9);
        g.setColor(new Color(32, 42, 45, 38));
        g.fillPolygon(shadow);
        g.setColor(GROUND);
        g.fillPolygon(ground);
        g.setColor(new Color(166, 178, 173));
        g.drawPolygon(ground);
    }

    private void drawRoadNetwork(Graphics2D g) {
        g.setStroke(new BasicStroke(22f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        g.setColor(ROAD_EDGE);
        for (int x : VERTICAL_ROADS) drawMapLine(g, x, 0, x, 100);
        for (int y : HORIZONTAL_ROADS) drawMapLine(g, 0, y, 100, y);

        g.setStroke(new BasicStroke(17f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        g.setColor(ROAD);
        for (int x : VERTICAL_ROADS) drawMapLine(g, x, 0, x, 100);
        for (int y : HORIZONTAL_ROADS) drawMapLine(g, 0, y, 100, y);

        float[] dash = {5f, 6f};
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10f, dash, 0f));
        g.setColor(LANE);
        for (int x : VERTICAL_ROADS) drawMapLine(g, x, 0, x, 100);
        for (int y : HORIZONTAL_ROADS) drawMapLine(g, 0, y, 100, y);
    }

    private List<Building> prepareCity(Graphics2D g) {
        List<Building> buildings = new ArrayList<>();
        int[][] blocks = {
                {1, 19, 24, 12}, {1, 44, 24, 13}, {1, 69, 24, 14},
                {18, 19, 32, 12}, {18, 44, 31, 13}, {18, 69, 31, 14},
                {43, 19, 43, 12}, {43, 44, 43, 13}, {43, 69, 43, 14},
                {68, 19, 56, 12}, {68, 44, 56, 13}, {68, 69, 56, 14},
                {92, 19, 68, 12}, {92, 44, 68, 13}, {92, 69, 68, 14}
        };
        for (int index = 0; index < blocks.length; index++) {
            int[] block = blocks[index];
            if (index == 7 || index == 11) {
                drawPark(g, block[0], block[1], 15, 15);
            } else {
                buildings.add(new Building(block[0], block[1], block[0] > 90 ? 6 : 14, 14,
                        block[2] + (index % 3) * 3, buildingColor(index)));
            }
        }
        return buildings;
    }

    private void drawPark(Graphics2D g, int x, int y, int width, int depth) {
        Polygon lot = mapRectangle(x, y, width, depth);
        g.setColor(PARK);
        g.fillPolygon(lot);
        g.setColor(new Color(111, 156, 112));
        g.drawPolygon(lot);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                Point base = project(x + 3 + column * 4, y + 3 + row * 4);
                g.setColor(new Color(57, 102, 67, 80));
                g.fillOval(base.x - 5, base.y - 1, 12, 5);
                g.setColor(new Color(58, 123, 73));
                g.fillOval(base.x - 4, base.y - 9, 9, 9);
                g.setColor(new Color(104, 160, 90));
                g.fillOval(base.x - 1, base.y - 12, 7, 8);
            }
        }
    }

    private void drawBuilding(Graphics2D g, Building building) {
        Polygon footprint = mapRectangle(building.x, building.y, building.width, building.depth);
        Point[] base = points(footprint);
        Point[] top = new Point[base.length];
        for (int index = 0; index < base.length; index++) {
            top[index] = new Point(base[index].x, base[index].y - building.height);
        }

        g.setColor(new Color(38, 45, 49, 45));
        g.fillPolygon(translate(footprint, 7, 8));
        g.setColor(shade(building.color, 0.70));
        g.fillPolygon(polygon(base[1], base[2], top[2], top[1]));
        g.setColor(shade(building.color, 0.84));
        g.fillPolygon(polygon(base[2], base[3], top[3], top[2]));
        g.setColor(building.color);
        g.fillPolygon(polygon(top));
        g.setColor(new Color(255, 255, 255, 75));
        g.drawPolygon(polygon(top));

        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(193, 225, 231, 150));
        int floors = Math.max(2, building.height / 8);
        for (int floor = 1; floor < floors; floor++) {
            double ratio = floor / (double) floors;
            int yOffset = (int) (building.height * ratio);
            g.drawLine(base[1].x, base[1].y - yOffset, base[2].x, base[2].y - yOffset);
            g.drawLine(base[2].x, base[2].y - yOffset, base[3].x, base[3].y - yOffset);
        }
    }

    private void drawStreetDetails(Graphics2D g) {
        g.setColor(new Color(248, 250, 250, 180));
        g.setStroke(new BasicStroke(1.2f));
        for (int x : VERTICAL_ROADS) {
            for (int y : HORIZONTAL_ROADS) {
                Point center = project(x, y);
                g.drawLine(center.x - 11, center.y + 5, center.x - 6, center.y + 8);
                g.drawLine(center.x + 6, center.y - 8, center.x + 11, center.y - 5);
            }
        }
    }

    private void drawTrails(Graphics2D g) {
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Map.Entry<String, AnimatedVehicle> entry : vehicles.entrySet()) {
            Deque<WorldPoint> trail = entry.getValue().trail;
            if (trail.size() < 2) continue;
            Path2D path = new Path2D.Double();
            int index = 0;
            for (WorldPoint world : trail) {
                Point point = project(world.x, world.y);
                if (index++ == 0) path.moveTo(point.x, point.y);
                else path.lineTo(point.x, point.y);
            }
            Color color = vehicleColor(entry.getKey());
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 92));
            g.draw(path);
        }
    }

    private void drawDepthSortedScene(Graphics2D g, List<Building> buildings) {
        long now = System.nanoTime();
        List<VehicleRender> renderedVehicles = new ArrayList<>();
        vehicles.forEach((id, vehicle) -> {
            WorldPoint position = vehicle.position(now);
            renderedVehicles.add(new VehicleRender(id, vehicle, position,
                    position.x + (100 - position.y)));
        });
        renderedVehicles.sort(Comparator.comparingDouble(VehicleRender::depth));
        buildings.sort(Comparator.comparingDouble(Building::renderDepth));

        int vehicleIndex = 0;
        int buildingIndex = 0;
        while (vehicleIndex < renderedVehicles.size() || buildingIndex < buildings.size()) {
            double vehicleDepth = vehicleIndex < renderedVehicles.size()
                    ? renderedVehicles.get(vehicleIndex).depth : Double.POSITIVE_INFINITY;
            double buildingDepth = buildingIndex < buildings.size()
                    ? buildings.get(buildingIndex).renderDepth() : Double.POSITIVE_INFINITY;
            if (vehicleDepth <= buildingDepth) {
                drawVehicle(g, renderedVehicles.get(vehicleIndex++));
            } else {
                drawBuilding(g, buildings.get(buildingIndex++));
            }
        }
    }

    private void drawVehicle(Graphics2D g, VehicleRender rendered) {
        Point screen = project(rendered.position.x, rendered.position.y);
        boolean selected = rendered.id.equals(selectedVehicle);
        boolean hovered = rendered.id.equals(hoveredVehicle);
        if (selected || hovered) drawSelection(g, screen, selected);
        drawCar(g, rendered.id, rendered.vehicle, screen);
    }

    private void drawActiveVehicleLabels(Graphics2D g) {
        long now = System.nanoTime();
        String[] active = {selectedVehicle, hoveredVehicle};
        for (int index = 0; index < active.length; index++) {
            String id = active[index];
            if (id == null || !vehicles.containsKey(id)) continue;
            if (index == 1 && id.equals(selectedVehicle)) continue;
            AnimatedVehicle vehicle = vehicles.get(id);
            WorldPoint position = vehicle.position(now);
            drawVehicleLabel(g, id, project(position.x, position.y), id.equals(selectedVehicle));
            if (id.equals(selectedVehicle)) drawTelemetryCard(g, id, vehicle, position, now);
        }
    }

    private void drawSelection(Graphics2D g, Point point, boolean selected) {
        int pulse = selected ? (int) (3 * (1 + Math.sin(System.nanoTime() / 180_000_000.0))) : 0;
        g.setColor(new Color(255, 255, 255, selected ? 205 : 130));
        g.setStroke(new BasicStroke(selected ? 2.4f : 1.5f));
        g.drawOval(point.x - 17 - pulse, point.y - 10 - pulse / 2,
                34 + pulse * 2, 20 + pulse);
    }

    private void drawCar(Graphics2D graphics, String id, AnimatedVehicle vehicle, Point point) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(point.x, point.y - 4);
        g.rotate(vehicle.screenAngle());
        String type = FleetData.typeOf(id);
        int length = type.equals("Truck") ? 27 : type.equals("Delivery van") ? 24 : 21;
        int width = type.equals("Truck") ? 11 : 10;
        Color color = vehicleColor(id);

        g.setColor(new Color(21, 27, 31, 80));
        g.fillRoundRect(-length / 2 + 3, -width / 2 + 4, length, width, 5, 5);
        g.setColor(new Color(32, 35, 38));
        g.fillRect(-length / 3, -width / 2 - 2, 6, 3);
        g.fillRect(length / 5, -width / 2 - 2, 6, 3);
        g.fillRect(-length / 3, width / 2 - 1, 6, 3);
        g.fillRect(length / 5, width / 2 - 1, 6, 3);

        g.setColor(color);
        g.fillRoundRect(-length / 2, -width / 2, length, width, 5, 5);
        g.setColor(shade(color, 0.73));
        g.drawRoundRect(-length / 2, -width / 2, length, width, 5, 5);
        g.setColor(new Color(192, 225, 234));
        g.fillRoundRect(-3, -width / 2 + 2, 9, width - 4, 3, 3);
        g.setColor(new Color(250, 244, 200));
        g.fillRect(length / 2 - 3, -width / 2 + 2, 2, 2);
        g.fillRect(length / 2 - 3, width / 2 - 4, 2, 2);

        if (type.equals("Police")) {
            g.setColor(new Color(220, 38, 38));
            g.fillRect(0, -2, 2, 2);
            g.setColor(new Color(37, 99, 235));
            g.fillRect(2, -2, 2, 2);
        } else if (type.equals("Taxi")) {
            g.setColor(new Color(35, 35, 35));
            g.fillRect(-1, -2, 4, 2);
        }
        g.dispose();
    }

    private void drawVehicleLabel(Graphics2D g, String id, Point point, boolean selected) {
        g.setFont(Theme.SMALL.deriveFont(Font.BOLD));
        FontMetrics metrics = g.getFontMetrics();
        int width = metrics.stringWidth(id) + 14;
        int x = Math.min(point.x + 13, getWidth() - width - 7);
        int y = point.y - 28;
        g.setColor(new Color(24, 24, 27, selected ? 235 : 200));
        g.fillRoundRect(x, y, width, 21, 7, 7);
        g.setColor(Color.WHITE);
        g.drawString(id, x + 7, y + 15);
    }

    private void drawTelemetryCard(Graphics2D g, String id, AnimatedVehicle vehicle,
            WorldPoint position, long now) {
        Telemetry telemetry = vehicle.telemetry(now);
        int width = 190;
        int height = 72;
        int x = 14;
        int y = getHeight() - height - 14;
        g.setColor(new Color(24, 24, 27, 224));
        g.fillRoundRect(x, y, width, height, 8, 8);

        g.setFont(Theme.BODY.deriveFont(Font.BOLD, 13f));
        g.setColor(Color.WHITE);
        g.drawString(id, x + 12, y + 20);
        g.setFont(Theme.SMALL);
        g.setColor(new Color(212, 212, 216));
        g.drawString(FleetData.typeOf(id) + "  |  "
                + Math.round(position.x) + ", " + Math.round(position.y), x + 12, y + 38);
        g.setColor(new Color(74, 222, 128));
        g.fillOval(x + 12, y + 52, 7, 7);
        g.setColor(Color.WHITE);
        g.setFont(Theme.SMALL.deriveFont(Font.BOLD));
        g.drawString(Math.round(telemetry.speedKmh()) + " km/h  |  "
                + telemetry.heading() + "  |  " + telemetry.status(), x + 26, y + 60);
    }

    private void drawMapChrome(Graphics2D g) {
        g.setFont(Theme.SMALL.deriveFont(Font.BOLD));
        g.setColor(new Color(24, 24, 27, 205));
        g.fillRoundRect(14, 14, 88, 28, 8, 8);
        g.setColor(new Color(74, 222, 128));
        g.fillOval(24, 24, 7, 7);
        g.setColor(Color.WHITE);
        g.drawString("GPS LIVE", 38, 32);

        int compassX = getWidth() - 38;
        int compassY = 34;
        g.setColor(new Color(255, 255, 255, 215));
        g.fillOval(compassX - 18, compassY - 18, 36, 36);
        g.setColor(Theme.TEXT);
        g.drawOval(compassX - 18, compassY - 18, 36, 36);
        Polygon north = polygon(new Point(compassX, compassY - 13),
                new Point(compassX - 4, compassY + 2), new Point(compassX + 4, compassY + 2));
        g.setColor(Theme.CORAL);
        g.fillPolygon(north);
        g.setFont(Theme.SMALL.deriveFont(Font.BOLD, 9f));
        g.setColor(Theme.TEXT);
        g.drawString("N", compassX - 3, compassY + 13);
    }

    private void drawMapLine(Graphics2D g, double x1, double y1, double x2, double y2) {
        Point first = project(x1, y1);
        Point second = project(x2, y2);
        g.drawLine(first.x, first.y, second.x, second.y);
    }

    private Point project(double x, double y) {
        double mapY = 100 - y;
        double scaleX = Math.max(1.6, (getWidth() - PAD_X * 2) / 200.0);
        double scaleY = Math.max(0.78, (getHeight() - PAD_Y * 2) / 200.0);
        int screenX = (int) Math.round(getWidth() / 2.0 + (x - mapY) * scaleX);
        int screenY = (int) Math.round(PAD_Y + (x + mapY) * scaleY);
        return new Point(screenX, screenY);
    }

    private MutablePoint mapPoint(Point screen) {
        double scaleX = Math.max(1.6, (getWidth() - PAD_X * 2) / 200.0);
        double scaleY = Math.max(0.78, (getHeight() - PAD_Y * 2) / 200.0);
        double difference = (screen.x - getWidth() / 2.0) / scaleX;
        double sum = (screen.y - PAD_Y) / scaleY;
        int x = clamp((int) Math.round((sum + difference) / 2.0));
        int mapY = clamp((int) Math.round((sum - difference) / 2.0));
        int y = 100 - mapY;
        int roadX = nearestRoad(VERTICAL_ROADS, x);
        int roadY = nearestRoad(HORIZONTAL_ROADS, y);
        if (Math.abs(x - roadX) <= Math.abs(y - roadY)) x = roadX;
        else y = roadY;
        return new MutablePoint(x, y);
    }

    private Polygon mapRectangle(int x, int y, int width, int depth) {
        return polygon(project(x, y), project(x + width, y),
                project(x + width, y - depth), project(x, y - depth));
    }

    private String vehicleAt(Point mouse) {
        long now = System.nanoTime();
        String nearest = null;
        double distance = 18;
        for (Map.Entry<String, AnimatedVehicle> entry : vehicles.entrySet()) {
            WorldPoint world = entry.getValue().position(now);
            double candidate = project(world.x, world.y).distance(mouse);
            if (candidate < distance) {
                distance = candidate;
                nearest = entry.getKey();
            }
        }
        return nearest;
    }

    private Color vehicleColor(String id) {
        return switch (FleetData.typeOf(id)) {
            case "Taxi" -> new Color(241, 185, 37);
            case "Police" -> new Color(51, 102, 183);
            case "Delivery van" -> Theme.TEAL;
            default -> Theme.CORAL;
        };
    }

    private Color buildingColor(int index) {
        Color[] colors = {
                new Color(174, 185, 190), new Color(209, 197, 174),
                new Color(159, 180, 183), new Color(192, 177, 168),
                new Color(171, 178, 197)
        };
        return colors[index % colors.length];
    }

    private Color shade(Color color, double amount) {
        return new Color((int) (color.getRed() * amount),
                (int) (color.getGreen() * amount),
                (int) (color.getBlue() * amount));
    }

    private Polygon polygon(Point... points) {
        int[] x = new int[points.length];
        int[] y = new int[points.length];
        for (int index = 0; index < points.length; index++) {
            x[index] = points[index].x;
            y[index] = points[index].y;
        }
        return new Polygon(x, y, points.length);
    }

    private Point[] points(Polygon polygon) {
        Point[] result = new Point[polygon.npoints];
        for (int index = 0; index < polygon.npoints; index++) {
            result[index] = new Point(polygon.xpoints[index], polygon.ypoints[index]);
        }
        return result;
    }

    private Polygon translate(Polygon source, int x, int y) {
        Polygon result = new Polygon(source.xpoints, source.ypoints, source.npoints);
        result.translate(x, y);
        return result;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private int nearestRoad(int[] roads, int value) {
        int nearest = roads[0];
        for (int road : roads) {
            if (Math.abs(road - value) < Math.abs(nearest - value)) nearest = road;
        }
        return nearest;
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        String hit = vehicleAt(event.getPoint());
        if (hit != null) {
            AnimatedVehicle vehicle = vehicles.get(hit);
            long now = System.nanoTime();
            WorldPoint point = vehicle.position(now);
            Telemetry telemetry = vehicle.telemetry(now);
            return hit + "  |  " + FleetData.typeOf(hit) + "  |  "
                    + Math.round(telemetry.speedKmh()) + " km/h  |  "
                    + telemetry.heading() + "  |  " + Math.round(point.x) + ", " + Math.round(point.y);
        }
        return selectedVehicle == null ? "Select a vehicle to dispatch"
                : "Dispatch " + selectedVehicle + " to this point";
    }

    private static final class AnimatedVehicle {
        private double startX;
        private double startY;
        private double targetX;
        private double targetY;
        private long startedAt;
        private double headingX = 1;
        private double headingY;
        private double speedKmh;
        private long lastGpsUpdate;
        private final Deque<WorldPoint> trail = new ArrayDeque<>();

        private AnimatedVehicle(double x, double y) {
            startX = targetX = x;
            startY = targetY = y;
            startedAt = System.nanoTime();
            lastGpsUpdate = startedAt;
            trail.add(new WorldPoint(x, y));
        }

        private void moveTo(double x, double y, long now) {
            if (x == targetX && y == targetY) return;
            WorldPoint current = position(now);
            startX = current.x;
            startY = current.y;
            headingX = x - current.x;
            headingY = y - current.y;
            double measuredSpeed = Math.hypot(headingX, headingY) / 0.55 * 3.6;
            speedKmh = speedKmh == 0 ? measuredSpeed : speedKmh * 0.35 + measuredSpeed * 0.65;
            targetX = x;
            targetY = y;
            startedAt = now;
            lastGpsUpdate = now;
            trail.addLast(new WorldPoint(x, y));
            while (trail.size() > 10) trail.removeFirst();
        }

        private WorldPoint position(long now) {
            double elapsed = (now - startedAt) / 1_000_000.0;
            double progress = Math.max(0, Math.min(1, elapsed / 390.0));
            double eased = progress * progress * (3 - 2 * progress);
            return new WorldPoint(startX + (targetX - startX) * eased,
                    startY + (targetY - startY) * eased);
        }

        private double screenAngle() {
            double mapHeadingY = -headingY;
            double screenX = headingX - mapHeadingY;
            double screenY = headingX + mapHeadingY;
            return Math.atan2(screenY, screenX);
        }

        private Telemetry telemetry(long now) {
            long ageMillis = Math.max(0, (now - lastGpsUpdate) / 1_000_000);
            double currentSpeed = ageMillis > 1_300 ? 0 : speedKmh;
            String status = ageMillis > 1_800 ? "Signal stale"
                    : currentSpeed < 1 ? "Idle" : "Moving";
            return new Telemetry(currentSpeed, heading(), ageMillis, status);
        }

        private String heading() {
            if (Math.abs(headingX) > Math.abs(headingY)) return headingX >= 0 ? "East" : "West";
            return headingY >= 0 ? "North" : "South";
        }
    }

    public record Telemetry(double speedKmh, String heading, long ageMillis, String status) {
    }

    private record WorldPoint(double x, double y) {
    }

    private record VehicleRender(String id, AnimatedVehicle vehicle, WorldPoint position, double depth) {
    }

    private record Building(int x, int y, int width, int depth, int height, Color color) {
        private double renderDepth() {
            return x + width + (100 - (y - depth));
        }
    }
}
