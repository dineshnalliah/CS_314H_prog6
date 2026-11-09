package assignment;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/* 
 * Any comments and methods here are purely descriptions or suggestions.
 * This is your test file. Feel free to change this as much as you want.
 */

public class BVHTest {

    private static List<Shape> shapes;        // shared random triangles
    private static Triangle diagonal;         // the Figure 3 triangle
    private BoundedVolumeHierarchy bvh;

    private static Triangle tri(int ax, int ay, int bx, int by, int cx, int cy) {
        return new Triangle(new Point(ax, ay), new Point(bx, by), new Point(cx, cy));
    }

    // This will run ONCE before all other tests. It can be useful to setup up
    // global variables and anything needed for all of the tests.
    @BeforeAll
    static void setupAll() {
        Random rng = new Random(314);
        shapes = new ArrayList<>();
        while (shapes.size() < 60) {
            Point a = new Point(rng.nextInt(300), rng.nextInt(300));
            Point b = new Point(a.x + 1 + rng.nextInt(30), a.y + rng.nextInt(30));
            Point c = new Point(a.x + rng.nextInt(30), a.y + 1 + rng.nextInt(30));
            long cross = (long) (b.x - a.x) * (c.y - a.y) - (long) (b.y - a.y) * (c.x - a.x);
            if (cross != 0) shapes.add(new Triangle(a, b, c)); // skip degenerate triangles
        }
        diagonal = tri(0, 100, 80, 20, 80, 100);
    }

    // This will run before EACH test.
    @BeforeEach
    void setupEach() {
        bvh = new BoundedVolumeHierarchy();
    }

    // You can test your BVH here
    @Test
    void testBVH() {
        // empty tree
        assertTrue(bvh.findCollision(new Point2D.Double(0, 0)).isEmpty(), "empty tree: findCollision");
        assertNull(bvh.intersectRay(new Point2D.Double(0, 0), new Vector2D(1, 0)), "empty tree: intersectRay");
        bvh.remove(tri(0, 0, 1, 0, 0, 1)); // should not throw

        // example from the spec (median split)
        bvh.setSplitMethod(BVH.SplitMethod.SPLIT_MEDIAN);
        bvh.buildBVH(new ArrayList<>(List.of(tri(4, 5, 5, 3, 5, 5), tri(1, 5, 2, 3, 3, 5),
                tri(2, 2, 3, 1, 4, 2), tri(0, 0, 1, 0, 1, 2))));
        String[] lines = bvh.toString().split("\n");
        assertEquals(7, lines.length, "Figure 2 tree should print 7 lines:\n" + bvh);
        assertEquals("[(0, 0), (5, 5)]", lines[0], "root box\n" + bvh);
        assertEquals("\t[(0, 0), (3, 5)]", lines[1], "left box\n" + bvh);
        assertEquals("\t[(2, 1), (5, 5)]", lines[4], "right box\n" + bvh);
    }

    // You can test your Visualization here
    @Test
    void testVisualization() {
        // no triangles
        Visualization empty = new Visualization();
        bvh.buildBVH(new ArrayList<>());
        empty.setBoundingVolumeHierarchy(bvh);
        assertTrue(empty.drawGUI().isEmpty(), "nothing should be drawn with no triangles");
        assertTrue(empty.moveSquare('w'), "move should succeed with no triangles");
        assertFalse(empty.moveSquare('x'), "non-WASD keys should not move the square");
        
    }

}
