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


import mrapple100.Server.MediaBufferInfo;
import mrapple100.Server.encoder.utils.CodecUtil;
import mrapple100.utils.ByteUtils;
import mrapple100.utils.DecodeUtil;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
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

        //need convert byteBuffer(yuv420) to h264
        byte[] h264 = encodeYuvToH264(byteBuffer.array());
        if(h264!=null) {
            ByteBuffer bb = ByteBuffer.wrap(h264);
            byteBuffer.rewind();
            processOutput(bb, bufferInfo);
        }
    }

    public byte[] encodeYuvToH264(byte[] yuvData) {

        // Открываем кодек
        System.out.println(yuvData.length);
        avcodec_open2(c, codec, new AVDictionary());
        // Создаем структуры данных для YUV420 кадра и H.264 пакета
        AVFrame yuvFrame = av_frame_alloc();
        System.out.println(c.width()+" "+c.height());
        yuvFrame.width(c.width());
        yuvFrame.height(c.height());

        yuvFrame.format(AV_PIX_FMT_YUV420P);
        yuvFrame.pts(0);
        yuvFrame.data(0, new BytePointer(yuvData));
        av_frame_get_buffer(yuvFrame,32); //было 32

        System.out.println(yuvFrame.width()+" "+ yuvFrame.height());
        AVPacket h264Packet = new AVPacket();
       // h264Packet.size(av_image_get_buffer_size(AV_PIX_FMT_YUV420P, c.width(), c.height(), 1) );
       // System.out.println(av_image_get_buffer_size(AV_PIX_FMT_YUV420P, c.width(), c.height(), 1));
        h264Packet.data(null);
        h264Packet.size(0);
        av_packet_unref(h264Packet);

        // Кодируем YUV420 кадр в H.264 пакет
        int result = avcodec_send_frame(c, yuvFrame);
        System.out.println(result);
        if(result<0) {
            BytePointer ep = new BytePointer(AV_ERROR_MAX_STRING_SIZE);
            av_make_error_string(ep, AV_ERROR_MAX_STRING_SIZE, result);
            System.out.println("Can not send frame:" + ep.getString());
        }
        while (result == 0) {
            result = avcodec_receive_packet(c, h264Packet);
            System.out.println("hello "+result);
            if(result<0) {
                BytePointer ep = new BytePointer(AV_ERROR_MAX_STRING_SIZE);
                av_make_error_string(ep, AV_ERROR_MAX_STRING_SIZE, result);
                System.out.println("Can not receive frame:" + ep.getString());
            }

            if (result == 0 && (h264Packet.flags() & AV_PKT_FLAG_KEY) == 1) {
                // Нашли IDR кадр, записываем его в буфер
                byte[] h264Data = new byte[h264Packet.size()];
                h264Packet.data().get(h264Data);
                av_packet_unref(h264Packet);
                av_frame_unref(yuvFrame);
                avcodec_close(c);
                return h264Data;
            }
        }
        // Если не удалось найти IDR кадр, возвращаем null
        av_packet_unref(h264Packet);
        av_frame_unref(yuvFrame);
        avcodec_close(c);
        return null;
    }
}



