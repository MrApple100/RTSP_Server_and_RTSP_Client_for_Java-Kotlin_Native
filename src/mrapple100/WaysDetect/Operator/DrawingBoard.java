package mrapple100.WaysDetect.Operator;

import mrapple100.utils.YuvConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;

public class DrawingBoard extends JPanel implements MouseListener, MouseMotionListener {
    private int prevXL, prevYL, currXL, currYL;
    private int prevXP, prevYP, currXP, currYP;
    private int Mouse_Last=0;

    private double koefW=1;
    private double koefH=1;

    private byte[] imagebyte;
    private  BufferedImage image;  // BufferedImage, на который будет происходить рисование
    private int width=1920,height=1080;

    private ArrayBlockingQueue<PrevCurLine> prevCurLineDeque = new ArrayBlockingQueue<PrevCurLine>(1000);
    private JPanel instance;
    public DrawingBoard(int width, int height, JLabel message) {
        this.width=width;
        this.height=height;
        // создаем новый BufferedImage и получаем его графический контекст
     //   image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.koefW=1920/width;
        this.koefH=1080/height;
        imagebyte = new byte[(int) (width*height*4)];
        image = YuvConverter.Companion.createARGBImage2(imagebyte,(int) (width),(int) (height));
        Graphics2D g2d =(Graphics2D) image.createGraphics();

        // Настройте прозрачность фона
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, width,height);//(int) (width*koefW), (int) (height*koefH));

// Настройте кисть
        g2d.setComposite(AlphaComposite.SrcOver.derive(1.0f)); // установите альфа-значение равным 1.0
        g2d.setColor(Color.GREEN);
        g2d.setStroke(new BasicStroke(10));
        g2d.dispose();
        instance = this;
        instance.add(message);
        // добавляем слушателей мыши
        addMouseListener(this);
        addMouseMotionListener(this);
        CleanerPCLsThread cleanerPCLsThread = new CleanerPCLsThread(prevCurLineDeque);
        cleanerPCLsThread.start();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
       // g2d.drawLine(prevX, prevY, currX, currY); // рисование линии

        g.drawImage(image, 0, 0, null); // отображаем BufferedImage на JPanel
        g.dispose();
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {
        Mouse_Last=e.getButton();
        switch (e.getButton()) {
            case MouseEvent.BUTTON1: {
                currXL = (int) (e.getX());
                currYL = (int) (e.getY());
                break;
            }
            case MouseEvent.BUTTON3:{
                currXP = (int) (e.getX());
                currYP = (int) (e.getY());
                break;
            }

            default: {
                break;
            }
        }

    }

    public void mouseDragged(MouseEvent e) {

        switch (Mouse_Last){
            case MouseEvent.BUTTON1:{
                prevXL = currXL;
                prevYL = currYL;
                currXL = e.getX();
                currYL = e.getY();
                Graphics2D g2d =(Graphics2D) image.createGraphics();
                g2d.setComposite(AlphaComposite.SrcOver.derive(1.0f)); // установите альфа-значение равным 1.0
                g2d.setColor(Color.GREEN);
                g2d.setStroke(new BasicStroke(10));
                g2d.drawLine(prevXL, prevYL, currXL, currYL);
                g2d.dispose();
                this.getGraphics().drawLine(prevXL, prevYL, currXL, currYL);
                break;
            }
            case MouseEvent.BUTTON3:{
                prevXP = currXP;
                prevYP = currYP;
                currXP = e.getX();
                currYP = e.getY();
                prevCurLineDeque.add(new PrevCurLine(prevXP,prevYP,currXP,currYP));
                Graphics2D g2d =(Graphics2D) image.createGraphics();


                    g2d.setComposite(AlphaComposite.SrcOver.derive(1.0f)); // установите альфа-значение равным 1.0
                    g2d.setColor(Color.GREEN);
                    g2d.setStroke(new BasicStroke(10));
                    g2d.drawLine(prevXP, prevYP, currXP, currYP);
                    this.getGraphics().drawLine(prevXP, prevYP, currXP, currYP);

                g2d.dispose();

                break;
            }

            default: {
                break;
            }
        }


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
        DrawingBoard panel = new DrawingBoard(1920, 1080,new JLabel());
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

    public BufferedImage getImage() {

        return image;
    }
    class PrevCurLine{
        private int prevX, prevY, currX, currY;

        public PrevCurLine(int prevX, int prevY, int currX, int currY) {
            this.prevX = prevX;
            this.prevY = prevY;
            this.currX = currX;
            this.currY = currY;
        }
    }
    class CleanerPCLsThread extends Thread{
        private ArrayBlockingQueue<PrevCurLine> prevCurLineDeque;

        public CleanerPCLsThread(ArrayBlockingQueue<PrevCurLine> prevCurLineDeque) {
            this.prevCurLineDeque = prevCurLineDeque;
        }

        @Override
        public void run() {
            super.run();
            while(true) {
                PrevCurLine pcl = null;
                if (prevCurLineDeque.size() > 0) {
                    pcl = prevCurLineDeque.poll();
                }
                if (pcl != null) {
                    try {
                    Thread.sleep(20l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                        Iterator<PrevCurLine> it = prevCurLineDeque.iterator();
                        Graphics2D g2d = (Graphics2D) image.createGraphics();

//                    //чистка
//                    g2d.clearRect(0, 0, image.getWidth(), image.getHeight()); // очистка рисунка
//
//                    // Настройте прозрачность фона
//                    g2d.setComposite(AlphaComposite.Clear);
//                    g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
//// Настройте кисть
//                    g2d.setComposite(AlphaComposite.SrcOver.derive(1.0f)); // установите альфа-значение равным 1.0
//                    //чистка
                        g2d.setComposite(AlphaComposite.SrcOver.derive(1.0f)); // установите альфа-значение равным 1.0
                        g2d.setColor(Color.BLACK);
                        g2d.setStroke(new BasicStroke(10));
                        g2d.drawLine(pcl.prevX, pcl.prevY, pcl.currX, pcl.currY);
                        instance.getGraphics().drawLine(pcl.prevX, pcl.prevY, pcl.currX, pcl.currY);

                        g2d.dispose();
                    }
                }



        }
    }
}
