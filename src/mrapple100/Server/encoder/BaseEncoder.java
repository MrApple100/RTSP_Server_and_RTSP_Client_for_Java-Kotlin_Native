/*
 * Copyright (C) 2021 pedroSG94.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mrapple100.Server.encoder;


import mrapple100.Client.rtsp.codec.FrameQueue;
import mrapple100.Server.MediaBufferInfo;
import mrapple100.Server.encoder.utils.CodecUtil;
import mrapple100.Server.rtspserver.RtspServerCamera1;
import mrapple100.utils.ByteUtils;
import mrapple100.utils.DecodeUtil;
import mrapple100.utils.VideoDecodeThreadTest;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.nio.ByteBuffer;
import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

/**
 * Created by pedro on 18/09/19.
 */
public abstract class BaseEncoder implements EncoderCallback {

    protected String TAG = "BaseEncoder";
    private final MediaBufferInfo bufferInfo = new MediaBufferInfo();
    protected BlockingQueue<Frame> queue = new ArrayBlockingQueue<>(1);
    protected BlockingQueue<ByteBuffer> queuebb = new ArrayBlockingQueue<>(1);

    protected AVCodec codec;
    protected AVCodecContext c;


    protected RtspServerCamera1 rtspServer;
    protected VideoDecodeThreadTest videoDecodeThreadTest;
    private FrameQueue videoFrameQueue = new FrameQueue(1);
    private Boolean spsppssetted = false;

    private byte[] lastbytes;




    protected static long presentTimeUs;
    protected volatile boolean running = false;
    protected boolean isBufferMode = true;
    protected CodecUtil.Force force = CodecUtil.Force.FIRST_COMPATIBLE_FOUND;
    private long oldTimeStamp = 0L;
    protected boolean shouldReset = true;

    public void restart() {
        start(false);
        initCodec();
    }

    public void start() {
        if (presentTimeUs == 0) {
            presentTimeUs = System.nanoTime() / 1000;
        }
        start(true);
        initCodec();
    }


    private void initCodec() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    try {
                        getDataFromEncoder();
                    } catch (IllegalStateException | InterruptedException e) {
                        //Log.i(TAG, "Encoding error", e);
                        reloadCodec();
                    }
                }
            }
        }).start();

        running = true;
    }

    public abstract void reset();

    public abstract void start(boolean resetTs);

    protected abstract void stopImp();

    protected void fixTimeStamp(MediaBufferInfo info) {
        if (oldTimeStamp > info.presentationTimeUs) {
            info.presentationTimeUs = oldTimeStamp;
        } else {
            oldTimeStamp = info.presentationTimeUs;
        }
    }

    private void reloadCodec() {
        //Sometimes encoder crash, we will try recover it. Reset encoder a time if crash
        if (shouldReset) {
            //  //Log.e(TAG, "Encoder crashed, trying to recover it");
            reset();
        }
    }

    public void stop() {
        stop(true);
    }

    public void stop(boolean resetTs) {
        if (resetTs) {
            presentTimeUs = 0;
        }
        running = false;
        stopImp();

        queue.clear();
        queue = new ArrayBlockingQueue<>(1);

        oldTimeStamp = 0L;
    }


    //here
    protected void getDataFromEncoder() throws IllegalStateException, InterruptedException {

        inputAvailable();


        outputAvailable(bufferInfo);

    }

    protected abstract Frame getInputFrame() throws InterruptedException;

    protected abstract long calculatePts(Frame frame, long presentTimeUs);

    private void processInput() throws IllegalStateException {
        try {
            Frame frame = getInputFrame();
            while (frame == null) frame = getInputFrame();
//      byteBuffer.clear();
//      int size = Math.max(0, Math.min(frame.getSize(), byteBuffer.remaining()) - frame.getOffset());
//      byteBuffer.put(frame.getBuffer(), frame.getOffset(), size);
//      long pts = calculatePts(frame, presentTimeUs);
            //System.out.println("Frame: "+DecodeUtil.byteArrayToHexString(frame.getBuffer()));
            queuebb.offer(ByteBuffer.wrap(frame.getBuffer()));
//      mediaCodec.queueInputBuffer(inBufferIndex, 0, size, pts, 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            //Log.i(TAG, "Encoding error", e);
        }
    }

    protected abstract void checkBuffer(@NotNull ByteBuffer byteBuffer,
                                        @NotNull MediaBufferInfo bufferInfo);

    protected abstract void sendBuffer(@NotNull ByteBuffer byteBuffer,
                                       @NotNull MediaBufferInfo bufferInfo);

    private void processOutput(@NotNull ByteBuffer byteBuffer, @NotNull MediaBufferInfo bufferInfo) throws IllegalStateException {
        checkBuffer(byteBuffer, bufferInfo);
        try {
          //  System.out.println("PUSH");
            videoFrameQueue.push(new FrameQueue.Frame(byteBuffer.array(),0,byteBuffer.array().length,System.currentTimeMillis()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(videoDecodeThreadTest==null){
            videoDecodeThreadTest = new VideoDecodeThreadTest(rtspServer.getFrameAfterPlace(),videoFrameQueue);
            videoDecodeThreadTest.start();
        }
        sendBuffer(byteBuffer, bufferInfo);

    }

    public void setForce(CodecUtil.Force force) {
        this.force = force;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void inputAvailable()
            throws IllegalStateException {


        processInput();
    }

    @Override
    public void outputAvailable(@NotNull MediaBufferInfo bufferInfo) throws IllegalStateException, InterruptedException {
        ByteBuffer byteBuffer;
        byteBuffer = queuebb.take();
       // System.out.println("OUTPUT1 "+DecodeUtil.byteArrayToHexString(byteBuffer.array()).substring(0,200));

        //need convert byteBuffer(yuv420) to h264
        byte[] h264 = encodeYuvToH264(byteBuffer.array());
//        if(!spsppssetted){
//            spsppssetted=true;
//            h264 = byteBuffer.array();
//        }else{
//            // System.out.println(DecodeUtil.byteArrayToHexString(bytes));
//            // System.out.println(DecodeUtil.byteArrayToHexString(bytes).length());
//            // System.out.println(DecodeUtil.byteArrayToHexString(bytes).indexOf("000000165"));
//            // System.out.println((bytes.length*2-DecodeUtil.byteArrayToHexString(bytes).indexOf("000000165"))/2);
//            h264 = new byte[(byteBuffer.array().length*2-DecodeUtil.byteArrayToHexString(byteBuffer.array()).indexOf("0000000165"))/2];
//            System.arraycopy(byteBuffer.array(),DecodeUtil.byteArrayToHexString(byteBuffer.array()).indexOf("000000165")/2,h264,0,(byteBuffer.array().length*2-DecodeUtil.byteArrayToHexString(byteBuffer.array()).indexOf("0000000165"))/2);
//        }
        if(h264!=null) {
           // System.out.println("OUTPUT2 "+DecodeUtil.byteArrayToHexString(h264).substring(0,100));

            ByteBuffer bb = ByteBuffer.wrap(h264);
            byteBuffer.rewind();
            processOutput(bb, bufferInfo);
            if(lastbytes!=null)
                if (Arrays.equals(h264,lastbytes))
                    System.out.println("YEEEEEEEEEEEEEEEEEEEEEEEEEEEEEES");
            lastbytes = h264;

        }
    }

    public byte[] encodeYuvToH264(byte[] yuvData) {

        ByteBuffer bb = ByteBuffer.wrap(yuvData);
        bb.rewind();

        try {
            //////////////////////////////////////////////////////////////////////////

          //  System.out.println(yuvData.length);
             AVPacket packet = new AVPacket();
             AVFrame frame = new AVFrame();

            frame.width(c.width());
            frame.height(c.height());
            frame.format(AV_PIX_FMT_YUV420P);
            PointerPointer pp = new PointerPointer<Pointer>(frame);
            BytePointer bp = new BytePointer(yuvData);
            av_frame_get_buffer(frame,32); //было 32
            av_image_fill_arrays(
                    frame.data(),
                    frame.linesize(),
                    bp
                    ,
                    AV_PIX_FMT_YUV420P,
                    c.width(),
                    c.height(),
                    1);
           // av_packet_unref(packet);
           // packet.data(new BytePointer());
           // packet.size(yuvData.length*2);

            if(packet==null){
                System.out.println("Не удалось выделить память для пакета");
            }

            byte[] h264Bytes = new byte[c.width()* c.height()];
            int h264Size = 0;
            int frameCount =0;
            int ret;
                ret = av_frame_make_writable(frame);
                    System.out.println("Make " + ret);

//                byte[] y = new byte[c.width()*c.height()];
//                byte[] u = new byte[y.length / 4];
//                byte[] v = new byte[u.length];
//
//
//                System.arraycopy(yuvData, 0, y, 0, y.length);
//                System.arraycopy(yuvData, y.length, u, 0, u.length);
//                System.arraycopy(yuvData, y.length+u.length, v, 0, v.length);
//
//                frame.data(0, new BytePointer(y));
//                frame.data(1, new BytePointer(u));
//                frame.data(2, new BytePointer(v));
                //BytePointer framepointer = new BytePointer(bb);

                //  println("ADDRESSCONTEXT " + "${context.address()}")
                //  println("ADDRESS " + "${framepointer.address()}")
                // packet.data(framepointer);


                //packet.size(yuvData.length);
                ret = avcodec_send_frame(c, frame);
                if (ret < 0) {
                    System.out.println("ERROR SEND " + ret);
                } else {
                    //   println("${context.width()} ${context.height()}")

                    //var whileenter = 0;
                    // while (ret >= 0) {
                    //  println("OKOKOKOK0")
                    //  println("codecContext: " + context.slice_count() + " " + context.gop_size() + " " + context.extradata_size() + " " + context.slices() + " " + context.frame_number() + " " + context.extradata() + " " + context.flags())

                    //  println(context.frame_number())
                    //  println(context)
                        ret = avcodec_receive_packet(c, packet);

                        if (ret == AVERROR_EAGAIN() || ret == AVERROR_EOF()) {
                            System.out.println(AVERROR_EAGAIN());
                            System.out.println(AVERROR_EOF());
                        }
                        if (ret < 0) {
                            System.out.println("ERROR RECEIVE " + ret);
                            return null;



                    }
                    // byte[] out = new byte[frame.]
                    BytePointer d = packet.data();
                    byte[] bytes = new byte[packet.size()];
                    if(!spsppssetted){
                        spsppssetted=true;
                        d.get(bytes);
                        return bytes;
                    }else{

                        d.get(bytes);
                     //   System.out.println(DecodeUtil.byteArrayToHexString(bytes));
                        // System.out.println(DecodeUtil.byteArrayToHexString(bytes).length());
                        // System.out.println(DecodeUtil.byteArrayToHexString(bytes).indexOf("000000165"));
                        // System.out.println((bytes.length*2-DecodeUtil.byteArrayToHexString(bytes).indexOf("000000165"))/2);
                        byte[] withoutspspps = new byte[(bytes.length*2-DecodeUtil.byteArrayToHexString(bytes).indexOf("000000165"))/2+1];
                        System.arraycopy(bytes,DecodeUtil.byteArrayToHexString(bytes).indexOf("000000165")/2+1,withoutspspps,1,(bytes.length*2-DecodeUtil.byteArrayToHexString(bytes).indexOf("000000165"))/2);
                        return withoutspspps;
                    }
                }
                av_frame_free(frame);
                av_packet_unref(packet);




        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
//        // Открываем кодек
//        System.out.println(yuvData.length);
//         ret = avcodec_open2(c, codec, new AVDictionary());
//        System.out.println("codec "+ret);
//        // Создаем структуры данных для YUV420 кадра и H.264 пакета
//        AVFrame yuvFrame = av_frame_alloc();
//        System.out.println(c.width()+" "+c.height());
//        yuvFrame.width(c.width());
//        yuvFrame.height(c.height());
//
//        yuvFrame.format(AV_PIX_FMT_YUV420P);
//        yuvFrame.pts(0);
//        yuvFrame.data(0, new BytePointer(yuvData));
//        av_frame_get_buffer(yuvFrame,32); //было 32
//
//        System.out.println(yuvFrame.width()+" "+ yuvFrame.height());
//        AVPacket h264Packet = av_packet_alloc();
//        if(h264Packet == null) {
//            throw new IllegalStateException("Unable to initialize video context");
//        }
//
//       // h264Packet.size(av_image_get_buffer_size(AV_PIX_FMT_YUV420P, c.width(), c.height(), 1) );
//       // System.out.println(av_image_get_buffer_size(AV_PIX_FMT_YUV420P, c.width(), c.height(), 1));
//        h264Packet.data(null);
//        h264Packet.size(0);
//        av_packet_unref(h264Packet);
//
//        // Кодируем YUV420 кадр в H.264 пакет
//        int result = avcodec_send_frame(c, yuvFrame);
//        System.out.println(result);
//        if(result<0) {
//            BytePointer ep = new BytePointer(AV_ERROR_MAX_STRING_SIZE);
//            av_make_error_string(ep, AV_ERROR_MAX_STRING_SIZE, result);
//            System.out.println("Can not send frame:" + ep.getString());
//        }
//        while (result == 0) {
//            result = avcodec_receive_packet(c, h264Packet);
//            System.out.println("hello "+result);
//            if(result<0) {
//                BytePointer ep = new BytePointer(AV_ERROR_MAX_STRING_SIZE);
//                av_make_error_string(ep, AV_ERROR_MAX_STRING_SIZE, result);
//                System.out.println("Can not receive frame:" + ep.getString());
//            }
//
//            if (result == 0 && (h264Packet.flags() & AV_PKT_FLAG_KEY) == 1) {
//                // Нашли IDR кадр, записываем его в буфер
//                byte[] h264Data = new byte[h264Packet.size()];
//                h264Packet.data().get(h264Data);
//                av_packet_unref(h264Packet);
//                av_frame_unref(yuvFrame);
//                avcodec_close(c);
//                return h264Data;
//            }
//        }
//        // Если не удалось найти IDR кадр, возвращаем null
//        av_packet_unref(h264Packet);
//        av_frame_unref(yuvFrame);
//        avcodec_close(c);
//        return null;
    }
}



