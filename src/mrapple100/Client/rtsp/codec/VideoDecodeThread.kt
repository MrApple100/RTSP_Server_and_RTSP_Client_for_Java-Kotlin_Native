package mrapple100.Client.rtsp.codec

import mrapple100.Server.encoder.Frame
import mrapple100.Server.rtspserver.RtspServerCamera1
import mrapple100.WaysDetect.Operator.DrawingBoard
import org.bytedeco.ffmpeg.avcodec.AVCodec
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avcodec.av_packet_unref
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.ffmpeg.global.avutil.*
import org.bytedeco.ffmpeg.global.swscale.sws_getContext
import org.bytedeco.ffmpeg.global.swscale.sws_scale
import org.bytedeco.ffmpeg.swscale.SwsContext
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.DoublePointer
import java.awt.*
import java.awt.color.ColorSpace
import java.awt.image.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JLabel


class VideoDecodeThread (
        private val codec:AVCodec,
        private val context: AVCodecContext,
        private val opts:AVDictionary,
    private val framePlace: JLabel,
    private val rtspServer:RtspServerCamera1,
    private val videoFrameQueue: FrameQueue
    ) : Thread() {
    var spspps: ByteArray? = null;

//    private val codec: AVCodec = avcodec.avcodec_find_decoder(avcodec.AV_CODEC_ID_H264)
//    private val context: AVCodecContext = avcodec.avcodec_alloc_context3(codec);
//    private val opts = AVDictionary()


    var swsContext: SwsContext? = null
    var size:Int? =null
    var buffer: BytePointer?=null
    var output:ByteArray? =null
    var swsContext2: SwsContext?=null
    var sizeyuv:Int?=null
    var bufferyuv: BytePointer?=null
    var outputyuv: ByteArray?=null

    var g2d:Graphics2D?=null




    private var frameq: FrameQueue.Frame? =null


    private var exitFlag: AtomicBoolean = AtomicBoolean(false)

    fun stopAsync() {
      //  if (DEBUG) Log.v(TAG, "stopAsync()")
        exitFlag.set(true)
        // Wake up sleep() code
        interrupt()
    }



    override fun run() {
     //   if (DEBUG) Log.d(TAG, "$name started")

        try {




            // Main loop
            while (!exitFlag.get()) {

                    // Preventing BufferOverflowException
                    // if (length > byteBuffer.limit()) throw DecoderFatalException("Error")

                    frameq = videoFrameQueue.pop()
                    if (frameq == null) {
                    //    Log.d(TAG, "Empty video frame")
                        // Release input buffer
                    } else {
                        if(frameq!!.data[4]== "103".toByte()){
                            spspps = frameq!!.data
                            //println("103 "+DecodeUtil.byteArrayToHexString(spspps).subSequence(0,50))
                        }else{
                            val datasrc = frameq!!.data
                            //println("OST "+DecodeUtil.byteArrayToHexString(frameq!!.data).subSequence(0,50))

                         //   println(DecodeUtil.byteArrayToHexString(datasrc))

                            var spsppsAndFrame = ByteArray((spspps?.size ?: 0) + datasrc.size)
                            spspps?.copyInto(spsppsAndFrame, 0, 0, spspps!!.size)
                            datasrc.copyInto(spsppsAndFrame, spspps!!.size, 0, datasrc.size)

                            val bb = ByteBuffer.wrap(spsppsAndFrame)
                            bb.rewind()

                            try {
                                //////////////////////////////////////////////////////////////////////////

                                val frame: AVFrame = avutil.av_frame_alloc();
                                val packet: AVPacket = AVPacket()
                                val rgbFrame: AVFrame = avutil.av_frame_alloc()



                                // }
                                av_packet_unref(packet)
                                val framepointer = BytePointer(bb)

                              //  println("ADDRESSCONTEXT " + "${context.address()}")
                              //  println("ADDRESS " + "${framepointer.address()}")
                                packet.data(framepointer)

                               // println("THREAD "+packet.pts())


                                packet.size(spsppsAndFrame.size)
                                var ret = avcodec.avcodec_send_packet(context, packet)
                                if (ret < 0) {
                                    println("ERROR SEND")
                                } else {
                                 //   println("${context.width()} ${context.height()}")

                                    if(swsContext==null) {
                                        swsContext = sws_getContext(
                                                context.width(), context.height(), context.pix_fmt(),
                                                context.width(), context.height(), avutil.AV_PIX_FMT_RGB24,
                                                0, null, null, DoublePointer())
                                    }
                                    var whileenter = 0;
                                    // while (ret >= 0) {
                                  //  println("OKOKOKOK0")
                                  //  println("codecContext: " + context.slice_count() + " " + context.gop_size() + " " + context.extradata_size() + " " + context.slices() + " " + context.frame_number() + " " + context.extradata() + " " + context.flags())

                                  //  println(context.frame_number())
                                  //  println(context)
                                    ret = avcodec.avcodec_receive_frame(context, frame);
                                    if (ret < 0) {
                                        println("ERROR RECEIVE")
                                    }
                                  //  println("OKOKOKOK1")
                                    // println(frame)
                                  //  println("OKOKOKOK2")

//                            if (ret == AVERROR_EAGAIN() || ret == AVERROR_EOF()) {
//                                println("ret: " + ret)
//                                continue
//                            } else if (ret < 0) {
//                                break
//                            }
                                  //  println("THREADFrame "+frame.pts())

                                  //  println("${++whileenter}")
                                 //   println("NICE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                                    //println("${swsContext}")
                                  //  println("NICE2")
                                    if(buffer==null) {
                                        size = avutil.av_image_get_buffer_size(avutil.AV_PIX_FMT_RGB24, context.width(), context.height(), 1)
                                        buffer = BytePointer(avutil.av_malloc(size!!.toLong()))
                                        output = ByteArray(size!!)
                                    }
                                    avutil.av_image_fill_arrays(rgbFrame.data(), rgbFrame.linesize(), buffer, avutil.AV_PIX_FMT_RGB24, context.width(), context.height(), 1)
                                  //  println("Nice3")
                                    sws_scale(swsContext, frame.data(), frame.linesize(), 0, context.height(), rgbFrame.data(), rgbFrame.linesize())

                                    buffer!!.get(output)



                                  //  println("OUTPUT" + DecodeUtil.byteArrayToHexString(output)[0])

                                    val toolkit = Toolkit.getDefaultToolkit()
                                    //display the image as an ImageIcon object
                                    //println("Success!")
                                   // println("output : ${output[0]} ${output[1]} ${output[2]} ${output.size}")
                                    var img = createRGBImage(output!!, context.width(), context.height())

                                    //  println("img : ${(img!!.raster.dataBuffer as DataBufferByte).data[0]} ${(img!!.raster.dataBuffer as DataBufferByte).data[1]} ${(img!!.raster.dataBuffer as DataBufferByte).data[2]} ${output.size}")


                                 //   println("baos : ${baos!!.toByteArray()[0]} ${baos.toByteArray()[1]} ${baos.toByteArray()[2]} ${baos.toByteArray().size}" )

                                 //   println("Success!")

                                 //   println(""+System.currentTimeMillis()+" "+baos!!.toByteArray().size)

///////////////////  OPERATOR WORK
                                    val gr2 = img!!.createGraphics()
                                  //  rtspServer.getDrawingBoard().paint(rtspServer.getDrawingBoard().graphics);
                                    gr2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                                   // val gr3 = rtspServer.getDrawingBoard().graphics as Graphics2D
                                   // gr3.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f))
                                   // rtspServer.getDrawingBoard().paint(gr2);


                                    gr2.drawImage(rtspServer.getDrawingBoard().image.getScaledInstance(context.width(),context.height(), Image.SCALE_DEFAULT),0,0,null)

                                    gr2.dispose()

                                   // img = draw

                                   // output = img.getData().getb


/////////////////////
                                    var baosjpg: ByteArrayOutputStream? = null
                                    try {
                                        baosjpg = ByteArrayOutputStream()
                                        ImageIO.write(img, "jpg", baosjpg)
                                    } finally {
                                        try {
                                            baosjpg!!.close()
                                        } catch (e: java.lang.Exception) {
                                        }
                                    }


//                                    var baos: ByteArrayOutputStream? = null
//                                    try {
//                                        baos = ByteArrayOutputStream()
//                                        ImageIO.write(img, "bmp", baos)
//                                    } finally {
//                                        try {
//                                            baos!!.close()
//                                        } catch (e: java.lang.Exception) {
//                                        }
//                                    }
                                    //println("IMGIMG "+img!!.data.dataBuffer.size)

                                    val width: Int = img.getWidth()
                                    val height: Int = img.getHeight()
                                    val pixels: IntArray = img.getRGB(0, 0, width, height, null, 0, width)
                                    val buffer = ByteBuffer.allocate(width * height * 3)
                                    for (pixel in pixels) {
                                        buffer.put((pixel shr 16 and 0xFF).toByte()) // red
                                        buffer.put((pixel shr 8 and 0xFF).toByte()) // green
                                        buffer.put((pixel and 0xFF).toByte()) // blue
                                    }
                                    val baos = buffer.array()

                                    output = baos

                                  //  println("OUTPUTOUTPUT "+output!!.size)
                                    //convert rgb to yuv
                                    //println("${context.width()} ${ context.height()}")



                                        // Пересчитать RGB24 в YUV420


// Конвертировать RGB в YUV420

                                    val frameAfterRGB = av_frame_alloc();
                                    frameAfterRGB.width(context.width())
                                    frameAfterRGB.height(context.height())
                                    frameAfterRGB.format(AV_PIX_FMT_RGB24)
                                  //  val pp2: PointerPointer<*> = PointerPointer<Pointer>(frameAfterRGB)
                                    val bp2 = BytePointer(ByteBuffer.wrap(output))
                                   // av_frame_get_buffer(frameAfterRGB, 32) //было 32

                                    av_image_fill_arrays(
                                            frameAfterRGB.data(),
                                            frameAfterRGB.linesize(),
                                            bp2
                                            ,
                                            AV_PIX_FMT_RGB24,
                                            context.width(),
                                            context.height(),
                                            1)

                                      //  println("start")
                                    //    println("IMG "+img!!.width+" "+img!!.height) 640x480

                                    if(swsContext2==null) {
                                        swsContext2 = sws_getContext(context.width(), context.height(), AV_PIX_FMT_RGB24, context.width(), context.height(), AV_PIX_FMT_YUV420P, 0, null, null, DoublePointer())
                                    }
                                        //  println("swsContext "+swsContext.address())
                                    //  println("out "+output.size)
                                    val yuvFrame: AVFrame = avutil.av_frame_alloc()
                                    if(bufferyuv==null) {
                                        sizeyuv = avutil.av_image_get_buffer_size(avutil.AV_PIX_FMT_YUV420P, context.width(), context.height(), 1)
                                        bufferyuv = BytePointer(avutil.av_malloc(sizeyuv!!.toLong()))
                                         outputyuv = ByteArray(sizeyuv!!)
                                    }
                                    avutil.av_image_fill_arrays(yuvFrame.data(), yuvFrame.linesize(), bufferyuv, avutil.AV_PIX_FMT_YUV420P, context.width(), context.height(), 1)


                                    // val yuv420 = ByteArray(output.size/2)
                                    //val ppOutput = PointerPointer<Pointer>(output)
                                    //val ppYuv420 = PointerPointer<Pointer>(yuv420)
                                    sws_scale(swsContext2,frameAfterRGB.data(),frameAfterRGB.linesize(),0,context.height(),yuvFrame.data(),yuvFrame.linesize())
                                    bufferyuv!!.get(outputyuv!!)
                                  //  sws_scale(swsContext2, ppOutput, IntPointer( context.width()*3), 0, context.height(), ppYuv420, IntPointer( context.width()))
                                      //  println("swsscale is ok "+ yuv420.size)

                                        //sws_freeContext(swsContext);
                                      //  println("yuv "+yuv420.size)

//                                        val iplImage: IplImage = IplImage.create(context.width(), context.height(), opencv_core.IPL_DEPTH_8U, 3)
//                                        var data = BytePointer(iplImage.imageData())
//                                        var bytes = output.clone()
//                                        data.get(bytes)
//                                        opencv_imgproc.cvCvtColor(iplImage, iplImage, opencv_imgproc.CV_RGB2YUV)
                                       // val yuv420 = YUVUtil.convertRgbToYuv420(output,context.width(),context.height())

                                        rtspServer.inputYUVData(rtspServer,Frame(outputyuv, 0, 0))
                                        //iplImage.deallocate()
                                       // sws_freeContext(swsContext2)
                                   // av_frame_free(frame)
                                   // av_frame_free(rgbFrame)
                                    // }
                                    //TESTTESTTEST
//                                    val frame2 = av_frame_alloc();
//                                    frame2.width(context.width())
//                                    frame2.height(context.height())
//                                    frame2.format(AV_PIX_FMT_YUV420P)
//                                    val pp: PointerPointer<*> = PointerPointer<Pointer>(frame2)
//                                    val bp = BytePointer(ByteBuffer.wrap(outputyuv))
//                                    av_frame_get_buffer(frame2, 32) //было 32
//
//                                    av_image_fill_arrays(
//                                            frame2.data(),
//                                            frame2.linesize(),
//                                            bp
//                                            ,
//                                            AV_PIX_FMT_YUV420P,
//                                            context.width(),
//                                            context.height(),
//                                            1)
////                                    val y = ByteArray(context.width() * context.height())
////                                    val u = ByteArray(y.size / 4)
////                                    val v = ByteArray(u.size)
////
////
////                                    System.arraycopy(outputyuv, 0, y, 0, y.size)
////                                    System.arraycopy(outputyuv, y.size, u, 0, u.size)
////                                    System.arraycopy(outputyuv, y.size + u.size, v, 0, v.size)
////
////                                    frame.data(0, BytePointer(*y))
////                                    frame.data(1, BytePointer(*u))
////                                    frame.data(2, BytePointer(*v))
//
//                                    var swsContext3 = sws_getContext(
//                                            context.width(), context.height(), context.pix_fmt(),
//                                            context.width(), context.height(), avutil.AV_PIX_FMT_RGB24,
//                                            0, null, null, DoublePointer())
//
//                                    val size2 = avutil.av_image_get_buffer_size(avutil.AV_PIX_FMT_RGB24, context.width(), context.height(), 1)
//                                    val buffer2: BytePointer = BytePointer(avutil.av_malloc(size2.toLong()))
//                                    avutil.av_image_fill_arrays(rgbFrame.data(), rgbFrame.linesize(), buffer2, avutil.AV_PIX_FMT_RGB24, context.width(), context.height(), 1)
//                                    //  println("Nice3")
//                                    sws_scale(swsContext3, frame2.data(), frame2.linesize(), 0, context.height(), rgbFrame.data(), rgbFrame.linesize())
//                                    val output3 = ByteArray(size)
//                                    buffer2.get(output3)
//                                  //  println(output3.size)//h*w*3
//
//                                    var img3 = createRGBImage(output3, context.width(), context.height())
//                                    //  println("img : ${(img!!.raster.dataBuffer as DataBufferByte).data[0]} ${(img!!.raster.dataBuffer as DataBufferByte).data[1]} ${(img!!.raster.dataBuffer as DataBufferByte).data[2]} ${output.size}")
//
//                                    var baos3: ByteArrayOutputStream? = null
//                                    try {
//                                        baos3 = ByteArrayOutputStream()
//                                        ImageIO.write(img3, "jpg", baos3)
//                                    } finally {
//                                        try {
//                                            baos3!!.close()
//                                        } catch (e: java.lang.Exception) {
//                                        }
//                                    }
                                    //TESTTESTTEST

                                    framePlace.icon = ImageIcon(toolkit.createImage(baosjpg!!.toByteArray(), 0, baosjpg.size()).getScaledInstance(800,500, Image.SCALE_DEFAULT))

                                    bp2.close()
                                    img!!.flush()
                                    //baos. reset()
                                    baosjpg!!.reset()


                                    //buffer.close() no need
                                    //bufferyuv.close() no need
                                    framepointer.close()
                                    av_packet_unref(packet)
                                    av_frame_free(frame)
                                    av_frame_free(frameAfterRGB)
                                    av_frame_free(yuvFrame)
                                    av_frame_free(rgbFrame)
                                   // draw.flush()

//                    avcodec_close(context)
//                    avcodec_free_context(context)
//
//                    sws_freeContext(swsContext)
//                    av_frame_free(frame)
                                    // av_frame_free(rgb)
                                    //////////////////////////////////////////////////////////////////
                               }
                            } catch (e:Exception){
                                println(e.localizedMessage)
                            }

                        }
                       // println("${frameq.data[4]}")
                    }


                if (exitFlag.get()) break


//                // All decoded frames have been rendered, we can stop playing now
//                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
//                 //   if (DEBUG) Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM")
//                    break
//                }
            }
            g2d!!.dispose()


            videoFrameQueue.clear()

        } catch (e: Exception) {
         //   Log.e(TAG, "$name stopped due to '${e.message}'")
            // While configuring stopAsync can be called and surface released. Just exit.
            if (!exitFlag.get()) e.printStackTrace()
            return
        }

     //   if (DEBUG) Log.d(TAG, "$name stopped")
    }


    companion object {
        private val TAG: String = VideoDecodeThread::class.java.simpleName
        private const val DEBUG = false
    }

    private fun createRGBImage(bytes: ByteArray, width: Int, height: Int): BufferedImage? {
        val buffer = DataBufferByte(bytes, bytes.size)
        val cm: ColorModel = ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), intArrayOf(8, 8, 8), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE)
        return BufferedImage(cm, Raster.createInterleavedRaster(buffer, width, height, width * 3, 3, intArrayOf(0, 1, 2), null), false, null)
    }
    fun hexToRGB(argbHex: Int): IntArray? {
        val rgb = IntArray(3)
        rgb[0] = argbHex and 0xFF0000 shr 16 //get red
        rgb[1] = argbHex and 0xFF00 shr 8 //get green
        rgb[2] = argbHex and 0xFF //get blue
        return rgb //return array
    }

}

