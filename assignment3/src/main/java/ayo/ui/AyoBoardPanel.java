package ayo.ui;

import ayo.model.GameSnapshot;
import ayo.model.Player;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/** Interactive two-row Ayo board drawn from the current immutable snapshot. */
public final class AyoBoardPanel extends JPanel {
    private static final Color WOOD = new Color(187, 126, 48);
    private static final Color WOOD_LIGHT = new Color(224, 168, 76);
    private static final Color PIT_DARK = new Color(101, 62, 30);
    private GameSnapshot snapshot;
    private Set<Integer> legalMoves = Set.of();
    private IntConsumer moveHandler = ignored -> { };
    private int hoveredPit = -1;
    private int lastSource = -1;
    private int lastLanding = -1;
    private Set<Integer> lastCaptured = Set.of();
    private boolean acceptingMoves = true;
    private String northRole = "COMPUTER";
    private String southRole = "YOU";

    public AyoBoardPanel() {
        setBackground(new Color(54, 87, 70));
        setPreferredSize(new Dimension(880, 470));
        ToolTipManager.sharedInstance().registerComponent(this);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                int next = pitAt(event.getPoint());
                if (next != hoveredPit) {
                    hoveredPit = next;
                    setCursor(Cursor.getPredefinedCursor(
                            acceptingMoves && legalMoves.contains(next)
                                    ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hoveredPit = -1;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                int pit = pitAt(event.getPoint());
                if (acceptingMoves && legalMoves.contains(pit)) moveHandler.accept(pit);
            }
        };
        addMouseMotionListener(mouse);
        addMouseListener(mouse);
    }

    public void setGame(GameSnapshot snapshot, List<Integer> legalMoves, boolean acceptingMoves) {
        this.snapshot = snapshot;
        this.legalMoves = new HashSet<>(legalMoves);
        this.acceptingMoves = acceptingMoves;
        repaint();
    }

    public void setMoveHandler(IntConsumer moveHandler) {
        this.moveHandler = moveHandler;
    }

    public void setPlayerRoles(String northRole, String southRole) {
        this.northRole = northRole;
        this.southRole = southRole;
        repaint();
    }

    public void highlightMove(int source, int landing, List<Integer> captured) {
        lastSource = source;
        lastLanding = landing;
        lastCaptured = new HashSet<>(captured);
        repaint();
    }

    public void clearHighlights() {
        lastSource = -1;
        lastLanding = -1;
        lastCaptured = Set.of();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawCloth(g);
        Rectangle board = boardBounds();
        drawBoard(g, board);
        if (snapshot != null) {
            for (int pit = 0; pit < 12; pit++) drawPit(g, pit, snapshot.seedsAt(pit));
            drawStore(g, true, snapshot.northCaptured(), board);
            drawStore(g, false, snapshot.southCaptured(), board);
            drawBoardGuides(g, board);
        }
        g.dispose();
    }

    private void drawCloth(Graphics2D g) {
        g.setColor(new Color(54, 87, 70));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(65, 101, 82));
        for (int y = 8; y < getHeight(); y += 18) {
            g.drawLine(0, y, getWidth(), y);
        }
    }

    private void drawBoard(Graphics2D g, Rectangle board) {
        g.setColor(new Color(26, 38, 34, 85));
        g.fillRoundRect(board.x + 8, board.y + 11, board.width, board.height, 46, 46);
        g.setPaint(new java.awt.GradientPaint(board.x, board.y, WOOD_LIGHT,
                board.x, board.y + board.height, WOOD));
        g.fillRoundRect(board.x, board.y, board.width, board.height, 46, 46);
        g.setColor(new Color(119, 73, 31));
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(board.x, board.y, board.width, board.height, 46, 46);
        g.setColor(new Color(246, 210, 132, 100));
        g.drawLine(board.x + 70, board.y + board.height / 2,
                board.x + board.width - 70, board.y + board.height / 2);
    }

    private void drawPit(Graphics2D g, int pit, int seedCount) {
        Ellipse2D shape = pitShape(pit);
        boolean legal = acceptingMoves && legalMoves.contains(pit);
        boolean hovered = pit == hoveredPit && legal;
        boolean captured = lastCaptured.contains(pit);

        if (legal || pit == lastSource || pit == lastLanding || captured) {
            Color ring = captured ? Theme.CORAL : (hovered ? Color.WHITE : Theme.GOLD);
            g.setColor(ring);
            g.setStroke(new BasicStroke(hovered ? 5f : 3f));
            g.draw(new Ellipse2D.Double(shape.getX() - 4, shape.getY() - 4,
                    shape.getWidth() + 8, shape.getHeight() + 8));
        }

        float radius = (float) (shape.getWidth() / 2);
        Point center = new Point((int) shape.getCenterX(), (int) shape.getCenterY());
        g.setPaint(new RadialGradientPaint(center, radius,
                new float[]{0f, 0.72f, 1f},
                new Color[]{new Color(151, 97, 43), PIT_DARK, new Color(77, 46, 25)}));
        g.fill(shape);
        g.setColor(new Color(243, 190, 91, 100));
        g.setStroke(new BasicStroke(2f));
        g.draw(shape);
        drawSeeds(g, shape, seedCount);
        drawCount(g, shape, seedCount);
    }

    private void drawSeeds(Graphics2D g, Ellipse2D pit, int count) {
        int visible = Math.min(count, 14);
        double radiusX = pit.getWidth() * 0.27;
        double radiusY = pit.getHeight() * 0.24;
        for (int index = 0; index < visible; index++) {
            double angle = -Math.PI / 2 + index * (Math.PI * 2 / Math.max(visible, 1));
            double ring = index < 4 ? 0.42 : 0.92;
            int x = (int) (pit.getCenterX() + Math.cos(angle) * radiusX * ring);
            int y = (int) (pit.getCenterY() + Math.sin(angle) * radiusY * ring);
            int size = Math.max(7, (int) (pit.getWidth() * 0.105));
            g.setColor(new Color(38, 39, 36, 85));
            g.fillOval(x - size / 2 + 2, y - size / 2 + 2, size, size);
            g.setPaint(new RadialGradientPaint(new Point(x - 2, y - 2), size,
                    new float[]{0f, 1f}, new Color[]{new Color(232, 225, 202), new Color(71, 73, 68)}));
            g.fillOval(x - size / 2, y - size / 2, size, size);
        }
    }

    private void drawCount(Graphics2D g, Ellipse2D pit, int count) {
        String value = Integer.toString(count);
        g.setFont(Theme.BODY.deriveFont(Font.BOLD, 15f));
        FontMetrics metrics = g.getFontMetrics();
        int x = (int) pit.getCenterX() - metrics.stringWidth(value) / 2;
        int y = (int) pit.getMaxY() - 10;
        g.setColor(new Color(255, 247, 222));
        g.drawString(value, x, y);
    }

    private void drawStore(Graphics2D g, boolean north, int count, Rectangle board) {
        int width = 58;
        int height = board.height - 70;
        int x = north ? board.x + 14 : board.x + board.width - width - 14;
        int y = board.y + 35;
        g.setColor(new Color(105, 64, 29));
        g.fillRoundRect(x, y, width, height, 30, 30);
        g.setColor(new Color(245, 204, 121));
        g.drawRoundRect(x, y, width, height, 30, 30);
        String player = north ? "N" : "S";
        String value = Integer.toString(count);
        g.setFont(Theme.BODY.deriveFont(Font.BOLD, 14f));
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(player, x + width / 2 - metrics.stringWidth(player) / 2, y + 29);
        g.setFont(Theme.HEADING);
        metrics = g.getFontMetrics();
        g.drawString(value, x + width / 2 - metrics.stringWidth(value) / 2, y + height / 2 + 8);
    }

    private void drawBoardGuides(Graphics2D g, Rectangle board) {
        g.setFont(Theme.SMALL.deriveFont(Font.BOLD));
        FontMetrics metrics = g.getFontMetrics();
        String north = "NORTH  |  " + northRole + "     SOWING DIRECTION  <";
        String south = "SOUTH  |  " + southRole + "     SOWING DIRECTION  >";
        g.setColor(new Color(245, 242, 226));
        g.drawString(north, board.x + board.width / 2 - metrics.stringWidth(north) / 2,
                Math.max(13, board.y - 9));
        g.drawString(south, board.x + board.width / 2 - metrics.stringWidth(south) / 2,
                Math.min(getHeight() - 6, board.y + board.height + 22));

        g.setFont(Theme.SMALL.deriveFont(Font.BOLD, 10f));
        metrics = g.getFontMetrics();
        for (int pit = 0; pit < 12; pit++) {
            Ellipse2D shape = pitShape(pit);
            String number = Integer.toString(pit < 6 ? pit + 1 : 12 - pit);
            int x = (int) shape.getCenterX() - metrics.stringWidth(number) / 2;
            int y = pit < 6 ? (int) shape.getMaxY() + 13 : (int) shape.getMinY() - 5;
            g.setColor(new Color(255, 244, 211));
            g.drawString(number, x, y);
        }
    }

    private Rectangle boardBounds() {
        int width = Math.min(getWidth() - 50, 900);
        int height = Math.min(getHeight() - 64, 360);
        width = Math.max(width, 600);
        height = Math.max(height, 270);
        return new Rectangle((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
    }

    private Ellipse2D pitShape(int pit) {
        Rectangle board = boardBounds();
        int sideSpace = 88;
        double usable = board.width - sideSpace * 2;
        double gap = 10;
        double pitWidth = (usable - gap * 5) / 6.0;
        double pitHeight = Math.min((board.height - 42) / 2.0, pitWidth * 0.88);
        int column = pit < 6 ? pit : 11 - pit;
        double x = board.x + sideSpace + column * (pitWidth + gap);
        double topY = board.y + 18;
        double y = pit >= 6 ? topY : board.y + board.height - pitHeight - 18;
        return new Ellipse2D.Double(x, y, pitWidth, pitHeight);
    }

    private int pitAt(Point point) {
        for (int pit = 0; pit < 12; pit++) {
            if (pitShape(pit).contains(point)) return pit;
        }
        return -1;
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int pit = pitAt(event.getPoint());
        if (pit < 0 || snapshot == null) return null;
        Player owner = pit < 6 ? Player.SOUTH : Player.NORTH;
        return owner.displayName() + " pit " + (pit < 6 ? pit + 1 : 12 - pit)
                + ": " + snapshot.seedsAt(pit) + " seeds";
    }
}
