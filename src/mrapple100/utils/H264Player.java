package mrapple100.utils;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;

import javax.swing.*;
import java.io.*;

public class H264Player {
    public static void main(String[] args) {

        try {
            new DemuxAndDecodeH264().start("video0.h264",new JLabel());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
            //FileOutputStream fos = new FileOutputStream("Hexh264");
            FileInputStream fin = new FileInputStream("video0.h264");

            byte[] b = new byte[1];
            while(fin.read(b)>0){
                System.out.print(DecodeUtil.byteArrayToHexString(b));

           // fos.flush();
            }
            // fos.close()

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
// Create a new frame to display the video
        CanvasFrame frame = new CanvasFrame("H264 Video Player");
        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

// Open the video file
        FFmpegLogCallback.set();
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("352x288Foreman.264");
       // grabber.setFormat("h264");
       // grabber.setPixelFormat(avcodec. AV_PIX_FMT_BGR24);
        try {
            grabber.start();


    // Read and display video frames
            System.out.println(frame.isVisible());
            System.out.println(grabber.getFrameNumber());
            System.out.println(grabber.getLengthInFrames());
            while (frame.isVisible() && (grabber.getFrameNumber() < grabber.getLengthInFrames())) {
                System.out.println(grabber.getFrameNumber());
                Frame fr = grabber.grab();
                System.out.println(fr);
                frame.showImage(fr);
            }

    // Release resources
            grabber.stop();
            grabber.release();
            frame.dispose();
        } catch (FFmpegFrameGrabber.Exception e) {
            e.printStackTrace();
        }*/
    }
}
