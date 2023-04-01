package mrapple100.WaysDetect.Operator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class DrawingBoard extends JPanel implements MouseListener, MouseMotionListener {
    private int prevX, prevY, currX, currY;

    public DrawingBoard() {
        //setPreferredSize(new Dimension(500, 500));
        setBackground(Color.green);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.drawLine(prevX, prevY, currX, currY);
    }

    private void draw(int x, int y) {
        prevX = currX;
        prevY = currY;
        currX = x;
        currY = y;

        Graphics g = getGraphics();
        g.setColor(Color.BLACK);
        g.drawLine(prevX, prevY, currX, currY);
        g.dispose();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Не используется
    }

    @Override
    public void mousePressed(MouseEvent e) {
        currX = e.getX();
        currY = e.getY();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Не используется
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Не используется
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Не используется
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        draw(x, y);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Не используется
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Drawing Board");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new DrawingBoard());
        frame.pack();
        frame.setVisible(true);
    }
}
