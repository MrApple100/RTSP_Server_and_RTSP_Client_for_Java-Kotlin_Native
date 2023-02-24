package mrapple100.Client;

import org.bytedeco.javacpp.BytePointer;

import java.nio.IntBuffer;

import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;

public class DMDecoder {

    private static final String LOG_TAG = "DMDecoder";

    private AVCodec avCodec;
    private AVCodecContext avCodecContext;
    private AVFrame avFrame;
    private AVPacket avPacket;
    private boolean wasIFrame;
    private long IFrameTimeStampMs;
    private int maxFps;
    private int codecId;

   // private DMDecoderCallback callback;

    public DMDecoder() {
      //  this.callback = cb;
        this.codecId = AV_CODEC_ID_H264;
        avcodec_register_all();
        restart();
    }

    public void restart() {
        stop();
        start();
    }

    public void stop() {
       // frames = 0;
        if (avCodecContext != null) {
            avcodec_close(avCodecContext);
            avcodec_free_context(avCodecContext);
            avCodecContext = null;
        }

        if (avCodec != null) {
            av_free(avCodec);
            avCodec = null;
        }

        if (avFrame != null) {
            av_frame_free(avFrame);
            avFrame = null;
        }

        if (avPacket != null) {
            av_free_packet(avPacket);
            avPacket = null;
        }
    }

    public void start() {
        avCodec = avcodec_find_decoder(codecId);

        avCodecContext = avcodec_alloc_context3(avCodec);
        AVDictionary opts = new AVDictionary();
        avcodec_open2(avCodecContext, avCodec, opts);

        avFrame = av_frame_alloc();
        avPacket = new AVPacket();
        av_init_packet(avPacket);
    }

    public VideoFrame decode(byte[] data, int dataOffset, int dataSize) {
        avPacket.pts(AV_NOPTS_VALUE);
        avPacket.dts(AV_NOPTS_VALUE);
        avPacket.data(new BytePointer(data).position(dataOffset));
        avPacket.size(dataSize);
        avPacket.pos(-1);

        IntBuffer gotPicture = IntBuffer.allocate(1);


        int processedBytes = avcodec_decode_video2(
                avCodecContext, avFrame, gotPicture, avPacket);

        if (avFrame.width() == 0 || avFrame.height() == 0) return null;

        VideoFrame frame = new VideoFrame();

        frame.colorPlane0 = new byte[avFrame.width() * avFrame.height()];
        frame.colorPlane1 = new byte[avFrame.width() / 2 * avFrame.height() / 2];
        frame.colorPlane2 = new byte[avFrame.width() / 2 * avFrame.height() / 2];

        if (avFrame.data(0) != null) avFrame.data(0).get(frame.colorPlane0);
        if (avFrame.data(1) != null) avFrame.data(1).get(frame.colorPlane1);
        if (avFrame.data(2) != null) avFrame.data(2).get(frame.colorPlane2);

        frame.lineSize0 = avFrame.width();
        frame.lineSize1 = avFrame.width() / 2;
        frame.lineSize2 = avFrame.width() / 2;

        frame.width = avFrame.width();
        frame.height = avFrame.height();

        return frame;
    }
    public class VideoFrame {
        public byte[] colorPlane0;
        public byte[] colorPlane1;
        public byte[] colorPlane2;
        public int lineSize0;
        public int lineSize1;
        public int lineSize2;
        public int width;
        public int height;
        public long presentationTime;
    }
}