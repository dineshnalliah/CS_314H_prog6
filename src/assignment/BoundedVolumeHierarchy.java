package assignment;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;

public class BoundedVolumeHierarchy implements BVH{

    private static final int ALLOWED_IMBALANCE = 1;
    private static final Random random = new Random();

    private SplitMethod splitMethod = SPLIT_DEFAULT;

    private BVHNode root;
    private int size;
    private boolean removed;


    @Override
    public void setSplitMethod(SplitMethod splitMethod) {
        // SPLIT_DEFAULT is SPLIT_MEDIAN
        this.splitMethod = (splitMethod == null)? SPLIT_DEFAULT : splitMethod;
    }

    private static int height(BVHNode node) {
        return node == null? -1 : node.height;
    }

    @Override
    public void buildBVH(List<Shape> shapeList) {
        size = (shapeList == null)? 0 : shapeList.size();
        root = (size == 0)? null : build(new ArrayList<>(shapeList), 0);
    }

    private BVHNode build(List<Shape> shapes, int depth) {

        if (shapes.size() == 1) return new BVHNode(shapes.get(0));

        boolean useX = (depth % 2 == 0);

        List<Shape> left = new ArrayList<>();
        List<Shape> right = new ArrayList<>();

        switch (splitMethod) {
            case SPLIT_MIDRANGE:
                splitMidrange(shapes, useX, left, right);
                break;
            case SPLIT_MEDIAN:
                splitMedian(shapes, useX, left, right);
                break;
            case SPLIT_SURFACE_AREA:
                break;
        }

        return new BVHNode(build(left, depth+1), build(right, depth+1));
    }

    private static double center(Shape s, boolean useX) {
        return useX ? s.getCenter().x : s.getCenter().y;
    }


    private void splitMidrange(List<Shape> shapes, boolean useX, List<Shape> left, List<Shape> right) {
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (Shape s : shapes) {
            low = Math.min(low, useX ? s.getMinSurroundingPoint().x : s.getMinSurroundingPoint().y);
            high = Math.max(high, useX ? s.getMaxSurroundingPoint().x : s.getMaxSurroundingPoint().y);
        }
        double split = (low + high) / 2.0;

        List<Shape> onLine = new ArrayList<>();
        for (Shape s : shapes) {
            double c = center(s, useX);
            if (Shape.isClose(c, split)) onLine.add(s);
            else if (c < split) left.add(s);
            else right.add(s);
        }

        for (Shape s : onLine) (left.size() <= right.size() ? left : right).add(s); // even it out

        if (left.isEmpty() || right.isEmpty()) {
            List<Shape> full = left.isEmpty() ? right : left;
            List<Shape> empty = left.isEmpty() ? left : right;
            Shape nearest = Collections.min(full,
                    Comparator.comparingDouble(s -> Math.abs(center(s, useX) - split)));
            full.remove(nearest);
            empty.add(nearest);
        }
    }

    private void splitMedian(List<Shape> shapes, boolean useX, List<Shape> left, List<Shape> right) {
        shapes.sort(Comparator.comparingDouble(s -> center(s, useX))); // sort the shapes based on their centers, least to greatest
        int mid = shapes.size() / 2;

        // split the original shape list into two left and right shape lists
        left.addAll(shapes.subList(0, mid));
        right.addAll(shapes.subList(mid, shapes.size()));
    }



    @Override
    public void insert(Shape shape) {
        if (shape == null) return;
        root = insert(root, new BVHNode(shape));
        size++;
    }

    private BVHNode insert(BVHNode node, BVHNode leaf) {
        if (node == null) return leaf;
        if (node.isLeaf()) return new BVHNode(node, leaf); // leaf becomes a parent of two leaves
        if (chooseLeft(node.left, node.right, leaf)) node.left = insert(node.left, leaf);
        else node.right = insert(node.right, leaf);
        node.updateHeights();
        return balance(node);
    }

    private boolean chooseLeft(BVHNode l, BVHNode r, BVHNode leaf) {
        if (l.isLeaf() != r.isLeaf()) return l.isLeaf();           // prefer leaves
        long gl = growth(l, leaf), gr = growth(r, leaf);
        if (gl != gr) return gl < gr;                               // less area growth
        if (l.height != r.height) return l.height < r.height;       // shorter
        return random.nextBoolean();                                // random
    }

    private static long growth(BVHNode n, BVHNode leaf) {
        long w = Math.max(n.maxX, leaf.maxX) - Math.min(n.minX, leaf.minX);
        long h = Math.max(n.maxY, leaf.maxY) - Math.min(n.minY, leaf.minY);
        return w * h - n.area();
    }

    private BVHNode balance(BVHNode node) {

        if (node == null || node.isLeaf()) return node;
        int diff = height(node.left) - height(node.right);

        if (diff > ALLOWED_IMBALANCE) { // positive factor
            if (height(node.left.left) < height(node.left.right)) node.left = rotateLeft(node.left); // double rotation
            return rotateRight(node);
        }
        if (diff < -ALLOWED_IMBALANCE) { // negative balance factor
            if (height(node.right.right) < height(node.right.left)) node.right = rotateRight(node.right);
            return rotateLeft(node);
        }
        return node;
    }

    private BVHNode rotateRight(BVHNode k2) {
        BVHNode k1 = k2.left;
        k2.left = k1.right;
        k1.right = k2;
        k2.updateHeights();   // child first
        k1.updateHeights();   // then subroot
        return k1;
    }

    private BVHNode rotateLeft(BVHNode k2) {
        BVHNode k1 = k2.right;
        k2.right = k1.left;
        k1.left = k2;
        k2.updateHeights();
        k1.updateHeights();
        return k1;
    }

    @Override
    public void remove(Shape shape) {
        if (shape == null || root == null) return;
        removed = false;
        root = remove(root, shape, shape.getMinSurroundingPoint(), shape.getMaxSurroundingPoint());
        if (removed) size--;
    }

    private BVHNode remove(BVHNode node, Shape shape, Point low, Point high) {
        if (node.isLeaf()) {
            if (node.shape.equals(shape)) { removed = true; return null; }
            return node;
        }
        if (low.x < node.minX || low.y < node.minY || high.x > node.maxX || high.y > node.maxY) return node; // can't be here

        BVHNode newLeft = remove(node.left, shape, low, high);
        if (removed) {
            if (newLeft == null) return node.right; // sibling replaces parent
            node.left = newLeft;
        } else {
            BVHNode newRight = remove(node.right, shape, low, high);
            if (!removed) return node;  // not found in this subtree
            if (newRight == null) return node.left;
            node.right = newRight;
        }
        node.updateHeights();
        return balance(node);
    }

    @Override
    public Set<Shape> findCollision(Point2D.Double point) {
        Set<Shape> result = new HashSet<>();
        if (point != null) collide(root, point, result);

        return result;
    }

    private void collide(BVHNode node, Point2D.Double p, Set<Shape> out) {
        if (node == null || !node.contains(p.x, p.y)) return;
        if (node.isLeaf()) {
            if (node.shape.containsPoint(p)) out.add(node.shape);
            return;
        }
        collide(node.left, p, out);
        collide(node.right, p, out);
    }

    @Override
    public Shape intersectRay(Point2D.Double origin, Vector2D direction) {
        if (root == null || origin == null || direction == null) return null;

        // Shape.findIntersection needs an int Point
        Point o = new Point((int) Math.round(origin.x), (int) Math.round(origin.y));
        Hit best = new Hit();
        trace(root, o, direction, Math.hypot(direction.getX(), direction.getY()), best);
        return best.shape;
    }

    private void trace(BVHNode node, Point o, Vector2D d, double len, Hit best) {
        double entry = rayBoxEntry(node, o.x, o.y, d.getX(), d.getY());
        if (entry == Double.POSITIVE_INFINITY || entry * len > best.dist + Shape.EPSILON) return; 
        if (node.isLeaf()) {
            Point2D.Double p = node.shape.findIntersection(o, d);
            if (p != null) {
                double dist = o.distance(p);
                if (dist < best.dist) { best.dist = dist; best.shape = node.shape; }
            }
            return;
        }
        double eL = rayBoxEntry(node.left, o.x, o.y, d.getX(), d.getY());
        double eR = rayBoxEntry(node.right, o.x, o.y, d.getX(), d.getY());

        if (eL <= eR) { trace(node.left, o, d, len, best); trace(node.right, o, d, len, best); }
        else { trace(node.right, o, d, len, best); trace(node.left, o, d, len, best); }
    }

    private static double rayBoxEntry(BVHNode node, double ox, double oy, double dx, double dy) {
        double[] t = {0, Double.POSITIVE_INFINITY};
        if (!clip(ox, dx, node.minX, node.maxX, t) || !clip(oy, dy, node.minY, node.maxY, t)) return t[1];
        return t[0];
    } 

    private static boolean clip(double o, double d, double low, double high, double[] t) {
        if (d == 0) return o >= low && o <= high; // parallel to this slab
        double t1 = (low - o) / d, t2 = (high - o) / d;
        t[0] = Math.max(t[0], Math.min(t1, t2));
        t[1] = Math.min(t[1], Math.max(t1, t2));
        return t[0] <= t[1] + Shape.EPSILON;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendNode(root, 0, sb);
        return sb.toString();
    }

    private void appendNode(BVHNode n, int depth, StringBuilder sb) {
        if (n == null) return;
        sb.append("\t".repeat(depth)); // tabs based on how deep
        if (n.isLeaf()) {
            sb.append(n.shape.toString());
        } else {
            sb.append("[(").append(n.minX).append(", ").append(n.minY).append("), (")
            .append(n.maxX).append(", ").append(n.maxY).append(")]\n");
            appendNode(n.left, depth + 1, sb);
            appendNode(n.right, depth + 1, sb);
        }
    }



    // structure for closest hit found in intersectRay search
    private static class Hit { Shape shape; double dist = Double.POSITIVE_INFINITY; }


    // structure for nodes of the BVH

    private static class BVHNode {

        Shape shape;
        int minX, minY, maxX, maxY;
        BVHNode right;
        BVHNode left;
        int height;

        BVHNode(Shape shape) {
            this.shape = shape;
            this.height = 0;
        }

        BVHNode(BVHNode left, BVHNode right) {
            this.height = 0;
            this.left = left;
            this.right = right;

        }

        void updateHeights() {
            height = 1 + Math.max(left.height, right.height);

            // the minimum values of a subroot will always be the most extreme of its children
            minX = Math.min(left.minX, right.minX);
            minY = Math.min(left.minY, right.minY);
            maxX = Math.max(left.maxX, right.maxX);
            maxY = Math.max(left.maxY, right.maxY);
        }

        boolean isLeaf() { return shape != null; }

        long area() { return (long) (maxX - minX) * (maxY - minY); }

        boolean contains(double x, double y) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY;
        }

    }
}