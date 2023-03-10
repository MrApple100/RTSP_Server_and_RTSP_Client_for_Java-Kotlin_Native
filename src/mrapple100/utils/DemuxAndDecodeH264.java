//package mrapple100.utils;
//
//import org.bytedeco.javacpp.*;
//import org.bytedeco.javacv.CanvasFrame;
//
//import javax.imageio.ImageIO;
//import javax.swing.*;
//import java.awt.*;
//import java.awt.image.BufferedImage;
//import java.awt.image.DataBufferByte;
//import java.io.*;
//import java.nio.ByteBuffer;
//import java.time.Duration;
//import java.time.temporal.ChronoUnit;
//
//import static org.bytedeco.javacpp.avcodec.*;
//import static org.bytedeco.javacpp.avformat.*;
//import static org.bytedeco.javacpp.avutil.*;
//import static org.bytedeco.javacpp.presets.avutil.AVERROR_EAGAIN;
//
//*
// * Read and decode h264 video from matroska (MKV) container
//
//
//public final class DemuxAndDecodeH264 {
//* Matroska format context
//
//    private AVFormatContext avfmtCtx;
//
//* Matroska video stream information
//
//    private AVStream videoStream;
//
//* matroska packet
//
//    private AVPacket avpacket;
//
//* H264 Decoder ID
//
//    private AVCodec codec;
//
//* H264 Decoder context
//
//    private AVCodecContext codecContext;
//
//* yuv420 frame
//
//    private AVFrame yuv420Frame;
//
//* RGB frame
//
//    private AVFrame rgbFrame;
//
//* java RGB frame
//
//    private BufferedImage img;
//
//* yuv420 to rgb converter
//
//    private swscale.SwsContext sws_ctx;
//
//* number of frame
//
//    private int nframe;
//
// 1/1000 of second
//
//    private AVRational tb1000;
//
//    public DemuxAndDecodeH264() {
//        tb1000 = new AVRational();
//        tb1000.num(1);
//        tb1000.den(1000);
//    }
//
//    public static void main(String file) throws IOException {
//       // new DemuxAndDecodeH264().start(new byte[7],null);
//    }
//    public void start(String filename,JLabel framePlace) throws IOException {
//        av_log_set_level(AV_LOG_VERBOSE);
//
//        openInput(filename);
//        findVideoStream();
//        initDecoder();
//        initRgbFrame();
//        initYuv420Frame();
//        getSwsContext();
//
//        avpacket = new avcodec.AVPacket();
//        while ((av_read_frame(avfmtCtx, avpacket)) >= 0) {
//            if (avpacket.stream_index() == videoStream.index()) {
//                processAVPacket(avpacket,framePlace);
//            }
//            av_packet_unref(avpacket);
//        }
//        // now process delayed frames
//        processAVPacket(null,framePlace);
//        free();
//    }
//    public void start(byte[] data, JLabel framePlace) throws IOException {
//        av_log_set_level(AV_LOG_VERBOSE);
//
//        //openInput(data);
//        //findVideoStream();
//        initDecoder();
//        System.out.println("DECODER");
//        initRgbFrame();
//        initYuv420Frame();
//        System.out.println("BOTH FRAMES");
//        getSwsContext();
//
//        System.out.println("HERE");
//
//        avpacket = new avcodec.AVPacket();
//        av_init_packet(avpacket);
//       // avpacket.pts(AV_NOPTS_VALUE);
//       // avpacket.dts(AV_NOPTS_VALUE);
//       // BytePointer pointer = new BytePointer(data).position(0);
//       // pointer.deallocate();
//        //avpacket.data(pointer);
//        //avpacket.size(data.length);
//        av_packet_from_data(avpacket,data,data.length);
//       // avpacket.pos(-1);
//
////        while ((av_read_frame(avfmtCtx, avpacket)) >= 0) {
////            if (avpacket.stream_index() == videoStream.index()) {
//                processAVPacket(avpacket,framePlace);
//           // }
//            av_packet_unref(avpacket);
//       // }
//        // now process delayed frames
//        processAVPacket(null,framePlace);
//        free();
//    }
//    private AVFormatContext openInput(String filename) throws IOException {
//        avfmtCtx = new AVFormatContext(null);
//        BytePointer filePointer = new BytePointer(filename);
//        int r = avformat.avformat_open_input(avfmtCtx, filePointer, null, null);
//        filePointer.deallocate();
//        if (r < 0) {
//            avfmtCtx.close();
//            throw new IOException("avformat_open_input error: " + r);
//        }
//        return avfmtCtx;
//    }
//
//    private AVFormatContext openInput(byte[] data) throws IOException {
//        avfmtCtx = new AVFormatContext(null);
//        BytePointer filePointer = new BytePointer(data).position(0);
//        int r = avformat.avformat_open_input(avfmtCtx, filePointer, null, null);
//        filePointer.deallocate();
//        if (r < 0) {
//            avfmtCtx.close();
//            throw new IOException("avformat_open_input error: " + r);
//        }
//        return avfmtCtx;
//    }
//
//    private void findVideoStream() throws IOException {
//        int r = avformat_find_stream_info(avfmtCtx, (PointerPointer) null);
//        if (r < 0) {
//            avformat_close_input(avfmtCtx);
//            avfmtCtx.close();
//            throw new IOException("error: " + r);
//        }
//
//        PointerPointer<AVCodec> decoderRet = new PointerPointer<>(1);
//        int videoStreamNumber = av_find_best_stream(avfmtCtx, AVMEDIA_TYPE_VIDEO, -1, -1, decoderRet, 0);
//        if (videoStreamNumber < 0) {
//            throw new IOException("failed to find video stream");
//        }
//
//        if (decoderRet.get(AVCodec.class).id() != AV_CODEC_ID_H264) {
//            throw new IOException("failed to find h264 stream");
//        }
//        decoderRet.deallocate();
//        videoStream =  avfmtCtx.streams(videoStreamNumber);
//    }
//
//    private void initDecoder() {
//        codec = avcodec_find_decoder(AV_CODEC_ID_H264);
//        codecContext = avcodec_alloc_context3(codec);
//        if((codec.capabilities() & avcodec.AV_CODEC_CAP_TRUNCATED) != 0) {
//            codecContext.flags(codecContext.flags() | avcodec.AV_CODEC_CAP_TRUNCATED);
//        }
//        AVDictionary opts = new AVDictionary();
//
//          avcodec_parameters_to_context(codecContext, videoStream.codecpar());
////        codecContext.width(480);
////        codecContext.height(640);
////        codecContext.pix_fmt(0);
//        if(avcodec_open2(codecContext, codec, (PointerPointer) null) < 0) {
//            throw new RuntimeException("Error: could not open codec.\n");
//        }
//    }
//
//    private void initYuv420Frame() {
//        yuv420Frame = av_frame_alloc();
//        if (yuv420Frame == null) {
//            throw new RuntimeException("Could not allocate video frame\n");
//        }
//    }
//
//    private void initRgbFrame() {
//        rgbFrame = av_frame_alloc();
//        rgbFrame.format(AV_PIX_FMT_BGR24);
//        rgbFrame.width(codecContext.width());
//        rgbFrame.height(codecContext.height());
//        System.out.println(rgbFrame.linesize()+ " "+rgbFrame.data().get() + " " +rgbFrame.width() + " "+ rgbFrame.height());
//        int ret = av_image_alloc(rgbFrame.data(),
//                rgbFrame.linesize(),
//                rgbFrame.width(),
//                rgbFrame.height(),
//                rgbFrame.format(),
//                1);
//        if (ret < 0) {
//            throw new RuntimeException("could not allocate buffer!");
//        }
//
//        img = new BufferedImage(rgbFrame.width(), rgbFrame.height(), BufferedImage.TYPE_3BYTE_BGR);
//    }
//
//    private void getSwsContext() {
//        System.out.println(codecContext.pix_fmt());
//        sws_ctx = swscale.sws_getContext(
//                codecContext.width(), codecContext.height(), codecContext.pix_fmt(),
//                rgbFrame.width(), rgbFrame.height(), rgbFrame.format(),
//                0, null, null, (DoublePointer) null);
//    }
//
//    private void processAVPacket(AVPacket avpacket,JLabel framePlace) throws IOException {
//        int ret = avcodec.avcodec_send_packet(codecContext, avpacket);
//        if (ret < 0) {
//            throw new RuntimeException("Error sending a packet for decoding\n");
//        }
//        System.out.println("GOOD");
//        receiveFrames(framePlace);
//    }
//
//    private void receiveFrames(JLabel framePlace) throws IOException {
//        int ret = 0;
//        while (ret >= 0) {
//            byte[] bbb = new byte[codecContext.extradata_size()];
////            System.out.println("codecContext: "+codecContext.slice_count() + " "+ codecContext.gop_size() + " "+ codecContext.extradata_size()+ " "+ codecContext.slices()+ " "+ codecContext.frame_number()+" "+ codecContext.extradata().get(bbb)+ " "+ codecContext.flags());
////            System.out.println(bbb);
////            System.out.println(DecodeUtil.byteArrayToHexString(bbb));
//            ret = avcodec.avcodec_receive_frame(codecContext, yuv420Frame);
////            System.out.println("HI");
////            System.out.println("YUV420: "+yuv420Frame);
//            if (ret == AVERROR_EAGAIN() || ret == AVERROR_EOF()) {
//                continue;
//            } else
//            if (ret < 0) {
//                throw new RuntimeException("error during decoding");
//            }
//            swscale.sws_scale(sws_ctx, yuv420Frame.data(), yuv420Frame.linesize(), 0,
//                    yuv420Frame.height(), rgbFrame.data(), rgbFrame.linesize());
//
//            rgbFrame.best_effort_timestamp(yuv420Frame.best_effort_timestamp());
//            processFrame(rgbFrame,framePlace);
//        }
//    }
//
//
//
//    private void processFrame(AVFrame rgbFrame,JLabel framePlace) throws IOException {
//        DataBufferByte buffer = (DataBufferByte) img.getRaster().getDataBuffer();
//        rgbFrame.data(0).get(buffer.getData());
//
//        ByteArrayOutputStream baos = null;
//        try {
//            baos = new ByteArrayOutputStream();
//            ImageIO.write(img, "png", baos);
//        } finally {
//            try {
//                baos.close();
//            } catch (Exception e) {
//            }
//        }
//       // ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//
//        Toolkit toolkit = Toolkit.getDefaultToolkit();
//       // icon = new ImageIcon(toolkit.createImage(buffer.getData(), 0, buffer.getData().length));
//        System.out.println("Success!");
//
//      //  CanvasFrame frame = new CanvasFrame("H264 Video Player");
//       // framePlace.setIcon(new ImageIcon(toolkit.createImage(baos.toByteArray(), 0, baos.size())));
//
//      //  framePlace.setIcon(icon);
//        //JOptionPane.showMessageDialog(null, framePlace);
//
//        long ptsMillis = av_rescale_q(rgbFrame.best_effort_timestamp(), videoStream.time_base(), tb1000);
//        Duration d = Duration.of(ptsMillis, ChronoUnit.MILLIS);
//
//        String name = String.format("img_%05d_%02d-%02d-%02d-%03d.png", ++nframe,
//                d.toHours(),
//                d.toMinutes(),
//                d.getSeconds(),
//                d.toMillis());
//        ImageIO.write(img, "png", new File(name));
//    }
//
//
//    private void free() {
//        av_packet_unref(avpacket);
//        avcodec.avcodec_close(codecContext);
//        avcodec.avcodec_free_context(codecContext);
//
//        swscale.sws_freeContext(sws_ctx);
//        av_frame_free(rgbFrame);
//        av_frame_free(yuv420Frame);
//        avformat.avformat_close_input(avfmtCtx);
//        avformat.avformat_free_context(avfmtCtx);
//    }
//}
