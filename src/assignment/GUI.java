package assignment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedList;

public class GUI {
    public static Visualization visualization = new Visualization();

    public static void main(String[] args) {

        // set up a new BVH

        BoundedVolumeHierarchy bvh = new BoundedVolumeHierarchy();
        bvh.buildBVH(new java.util.ArrayList<>(java.util.List.of(
                new Triangle(new Point(0, 100), new Point(80, 20), new Point(80, 100)), 
                new Triangle(new Point(-120, -60), new Point(-40, -90), new Point(-70, -20)),
                new Triangle(new Point(60, -150), new Point(150, -120), new Point(110, -60)))));
        visualization.setBoundingVolumeHierarchy(bvh);


        JFrame frame = new JFrame("BVH Visualization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        BVHVisualization bvhVisualization = new BVHVisualization();
        frame.add(bvhVisualization);
        frame.setVisible(true);
        frame.setResizable(false);
    }
}

class BVHVisualization extends JPanel {
    public BVHVisualization() {
        this.setFocusable(true);
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);

                if (GUI.visualization.moveSquare(e.getKeyChar())) {
                    repaint();
                }
            }
        });
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        LinkedList<Point> points = GUI.visualization.drawGUI();
        for (Point point : points) {
            g.fillRect(point.x, point.y, 1, 1);
        }

        g.setColor(Color.BLUE);
        g.fillRect(190, 190, 21, 21);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 400);
    }
}
