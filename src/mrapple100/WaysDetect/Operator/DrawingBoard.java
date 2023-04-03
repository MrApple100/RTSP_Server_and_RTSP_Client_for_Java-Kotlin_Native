package mrapple100.WaysDetect.Operator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class DrawingBoard extends JPanel implements MouseListener, MouseMotionListener {
    private int prevX, prevY, currX, currY;


    private  BufferedImage image;  // BufferedImage, на который будет происходить рисование
    private Graphics2D g2d;       // графический контекст для рисования на BufferedImage

    public DrawingBoard(int width, int height) {
        // создаем новый BufferedImage и получаем его графический контекст
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d =(Graphics2D) image.createGraphics();

        // Настройте прозрачность фона
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, width, height);

// Настройте кисть
        g2d.setComposite(AlphaComposite.SrcOver.derive(1.0f)); // установите альфа-значение равным 1.0
        g2d.setColor(Color.GREEN);
        g2d.setStroke(new BasicStroke(10));

        // добавляем слушателей мыши
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
       // g2d.drawLine(prevX, prevY, currX, currY); // рисование линии

        g.drawImage(image, 0, 0, null); // отображаем BufferedImage на JPanel
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {
        currX = e.getX();
        currY = e.getY();

    }

    public void mouseDragged(MouseEvent e) {

        prevX = currX;
        prevY = currY;
        currX = e.getX();
        currY = e.getY();


        g2d.drawLine(prevX, prevY, currX, currY);
        this.getGraphics().drawLine(prevX, prevY, currX, currY);

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    public void mouseReleased(MouseEvent e) {
        // нет необходимости здесь что-то делать
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    // другие методы интерфейса MouseListener и MouseMotionListener

    public static void main(String[] args) {
        JFrame frame = new JFrame("Drawing Panel");
        DrawingBoard panel = new DrawingBoard(500, 800);
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
    }

    public static BufferedImage overlayImages(BufferedImage bottomImage, BufferedImage topImage, int x, int y) {
        // Создаем новое изображение с размерами нижнего изображения
        BufferedImage combinedImage = new BufferedImage(bottomImage.getWidth(), bottomImage.getHeight(), BufferedImage.TYPE_INT_RGB);

        // Получаем графический контекст для нового изображения
        Graphics2D g2d = combinedImage.createGraphics();

        // Накладываем нижнее изображение на новое изображение
        g2d.drawImage(bottomImage, 0, 0, null);

        // Накладываем верхнее изображение на новое изображение в заданных координатах
        g2d.drawImage(topImage, x, y, null);

        // Освобождаем ресурсы графического контекста
        g2d.dispose();

        return combinedImage;
    }

    public synchronized BufferedImage getImage() {
        return image;
    }
}
