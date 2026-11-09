package assignment;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class Visualization {

    private static final int SCREEN = 400, HALF_SCREEN = 200;
    private static final int HALF_SQUARE = 10;
    private static final int STEP = 5;

    BoundedVolumeHierarchy boundingVolumeHierarchy;
    private int squareX, squareY;

    /**
     * Initialize state of square to be at (0, 0)
     */
    public Visualization () {
        squareX = 0;
        squareY = 0;
    }

    /**
     * @param boundingVolumeHierarchy        the BVH to set the boundedVolumeHierarchy to
     */
    public void setBoundingVolumeHierarchy(BoundedVolumeHierarchy boundingVolumeHierarchy) {
        this.boundingVolumeHierarchy = boundingVolumeHierarchy;
    }

    /**
     * Moves the square within the environment if it's able to move in the given direction
     *
     * @param keyPressed        the character of the keyPressed
     * @return              if the environment should be redrawn or not
     */
    public boolean moveSquare (char keyPressed) {
        int dx = 0, dy = 0;
        switch (Character.toLowerCase(keyPressed)) {
            case 'w': dy = -STEP; break;
            case 's': dy = STEP;  break;
            case 'a': dx = -STEP; break;
            case 'd': dx = STEP;  break;
            default: return false;
        }
        if (boundingVolumeHierarchy != null && sweepHitsTriangle(dx, dy)) return false;
        squareX += dx;
        squareY += dy;
        return true;
    }

    private boolean sweepHitsTriangle(int dx, int dy) {
        int minX = Math.min(squareX, squareX + dx) - HALF_SQUARE;
        int maxX = Math.max(squareX, squareX + dx) + HALF_SQUARE;
        int minY = Math.min(squareY, squareY + dy) - HALF_SQUARE;
        int maxY = Math.max(squareY, squareY + dy) + HALF_SQUARE;
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                if (!boundingVolumeHierarchy.findCollision(new Point2D.Double(x, y)).isEmpty())
                    return true;
        return false;
    }

    /**
     * Calculates all the pixels visible from the square in the center
     *
     * @return              All points that should be drawn other than the square
     */
    public LinkedList<Point> drawGUI() {
        LinkedList<Point> pixels = new LinkedList<>();
        if (boundingVolumeHierarchy == null) return pixels;
        Set<Point> seen = new HashSet<>();
        for (int i = 0; i < SCREEN; i++) {
            castRay(i, 0, seen, pixels);
            castRay(i, SCREEN - 1, seen, pixels);
            castRay(0, i, seen, pixels);
            castRay(SCREEN - 1, i, seen, pixels);
        }
        return pixels;
    }

    private void castRay(int guiX, int guiY, Set<Point> seen, LinkedList<Point> pixels) {
        Vector2D dir = new Vector2D(guiX - HALF_SCREEN, guiY - HALF_SCREEN); // never zero on the border
        Shape hit = boundingVolumeHierarchy.intersectRay(new Point2D.Double(squareX, squareY), dir);
        if (hit == null) return;
        Point2D.Double p = hit.findIntersection(new Point(squareX, squareY), dir);
        if (p == null) return;
        int gx = (int) Math.round(p.x - squareX + HALF_SCREEN);
        int gy = (int) Math.round(p.y - squareY + HALF_SCREEN);
        Point px = new Point(gx, gy);
        if (gx >= 0 && gx < SCREEN && gy >= 0 && gy < SCREEN && seen.add(px)) pixels.add(px);
    }
}
