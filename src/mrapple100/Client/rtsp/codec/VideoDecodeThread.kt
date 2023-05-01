package mrapple100.Client.rtsp.codec

import mrapple100.Server.encoder.Frame
import mrapple100.Server.rtspserver.RtspServerCamera1
import mrapple100.utils.DecodeUtil
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
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.ImageIcon
import javax.swing.JLabel


class VideoDecodeThread(
        private val codec: AVCodec,
        private val context: AVCodecContext,
        private val opts: AVDictionary,
        private val framePlace: JLabel,
        private val rtspServer: RtspServerCamera1,
        private val videoFrameQueue: FrameQueue
) : Thread() {
    var spspps: ByteArray? = null;

//    private val codec: AVCodec = avcodec.avcodec_find_decoder(avcodec.AV_CODEC_ID_H264)
//    private val context: AVCodecContext = avcodec.avcodec_alloc_context3(codec);
//    private val opts = AVDictionary()


    var swsContext: SwsContext? = null
    var size: Int? = null
    var buffer: BytePointer? = null
    var output: ByteArray? = null
    var swsContext2: SwsContext? = null
    var sizeyuv: Int? = null
    var bufferyuv: BytePointer? = null
    var outputyuv: ByteArray? = null

    var g2d: Graphics2D? = null

    var i=0;
    var totalAverage=0.0;


    private var frameq: FrameQueue.Frame? = null


    private var exitFlag: AtomicBoolean = AtomicBoolean(false)

    fun stopAsync() {
        //  if (DEBUG) Log.v(TAG, "stopAsync()")
        exitFlag.set(true)
        // Wake up sleep() code
        interrupt()
    }


    override fun run() {

        try {


            // Main loop
            while (!exitFlag.get()) {

                // Preventing BufferOverflowException
                // if (length > byteBuffer.limit()) throw DecoderFatalException("Error")

                frameq = videoFrameQueue.pop()
                val time0 = System.currentTimeMillis();

                if (frameq == null) {
                    //    Log.d(TAG, "Empty video frame")
                    // Release input buffer
                } else {
                    if (frameq!!.data[4] == "103".toByte()) {
                        spspps = frameq!!.data
                        //println("103 "+DecodeUtil.byteArrayToHexString(spspps).subSequence(0,50))
                    } else {
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

                            packet.data(framepointer)

                            packet.size(spsppsAndFrame.size)
                            var ret = avcodec.avcodec_send_packet(context, packet)
                            if (ret < 0) {
                                println("ERROR SEND")
                            } else {
                                //   println("${context.width()} ${context.height()}")

                                if (swsContext == null) {
                                    swsContext = sws_getContext(
                                            context.width(), context.height(), context.pix_fmt(),
                                            context.width(), context.height(), avutil.AV_PIX_FMT_RGB24,
                                            0, null, null, DoublePointer())
                                }
                                var whileenter = 0;



                                ret = avcodec.avcodec_receive_frame(context, frame);
                                if (ret < 0) {
                                    println("ERROR RECEIVE")
                                }

                                if (buffer == null) {
                                    size = avutil.av_image_get_buffer_size(avutil.AV_PIX_FMT_RGB24, context.width(), context.height(), 1)
                                    buffer = BytePointer(avutil.av_malloc(size!!.toLong()))
                                    output = ByteArray(size!!)
                                }
                                avutil.av_image_fill_arrays(rgbFrame.data(), rgbFrame.linesize(), buffer, avutil.AV_PIX_FMT_RGB24, context.width(), context.height(), 1)
                                sws_scale(swsContext, frame.data(), frame.linesize(), 0, context.height(), rgbFrame.data(), rgbFrame.linesize())

                                buffer!!.get(output)


                                //  println("OUTPUT" + DecodeUtil.byteArrayToHexString(output)[0])

                                val toolkit = Toolkit.getDefaultToolkit()
                                //display the image as an ImageIcon object
                                //println("Success!")
                                // println("output : ${output[0]} ${output[1]} ${output[2]} ${output.size}")

                                var img = createRGBImage(output!!, context.width(), context.height())
                                // println(output!!.size)
                                //  println(DecodeUtil.byteArrayToHexString(output).length)

                                //println(DecodeUtil.byteArrayToHexString(output).subSequence(0,12))

                                //  println("img : ${(img!!.raster.dataBuffer as DataBufferByte).data[0]} ${(img!!.raster.dataBuffer as DataBufferByte).data[1]} ${(img!!.raster.dataBuffer as DataBufferByte).data[2]} ${output.size}")


                                //   println("baos : ${baos!!.toByteArray()[0]} ${baos.toByteArray()[1]} ${baos.toByteArray()[2]} ${baos.toByteArray().size}" )

                                //   println("Success!")

                                //   println(""+System.currentTimeMillis()+" "+baos!!.toByteArray().size)

///////////////////  OPERATOR WORK
                                val time1 = System.currentTimeMillis();
                            if(false) {
                                val gr2 = img!!.createGraphics()

                                gr2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
                                val eskiz = rtspServer.getDrawingBoard().image.getScaledInstance(context.width(), context.height(), Image.SCALE_DEFAULT)
                                gr2.drawImage(eskiz, 0, 0, null)

                                gr2.dispose()
                            }
                                val time2 = System.currentTimeMillis();

/////////////////////
//                                var baosjpg: ByteArrayOutputStream? = null
//                                try {
//            //                                        val imageWithoutAlpha = BufferedImage(img.width,img.height,BufferedImage.TYPE_3BYTE_BGR)
//            //                                        imageWithoutAlpha.graphics.drawImage(img,0,0,null);
//                                    baosjpg = ByteArrayOutputStream()
//                                    ImageIO.write(img, "jpg", baosjpg)
//                                } finally {
//                                    try {
//                                        baosjpg!!.close()
//                                    } catch (e: java.lang.Exception) {
//                                    }
//                                }

                                val time3 = System.currentTimeMillis();

                                framePlace.icon = ImageIcon(img.getScaledInstance(800, 500, Image.SCALE_DEFAULT))

                                //только высвечивать эскиз
                                 // img = eskiz as BufferedImage


//                                    val imageWithoutAlpha = BufferedImage(img.width,img.height,BufferedImage.TYPE_3BYTE_BGR)
//                                    imageWithoutAlpha.graphics.drawImage(img,0,0,null)
//                                    img =imageWithoutAlpha


                                var conW=img.width
                                var conH=img.height
                        if(false) {

                            output = getBytesFromImage(img)
                            println("Size " + output!!.size)
                            println("Size " + DecodeUtil.byteArrayToHexString(output).subSequence(0,17))

                        }else{

                          //  output = getBytesFromImage(toBufferedImage(eskiz))

                            val eskiz = rtspServer.getDrawingBoard().image.getScaledInstance(context.width(), context.height(), Image.SCALE_DEFAULT)

                            val eskiz2 = toBufferedImage(eskiz)
                            conW=eskiz2.width
                            conH=eskiz2.height

                            val width: Int = eskiz2.width
                            val height: Int = eskiz2.height
                            val pixels: IntArray = eskiz2.getRGB(0, 0, width, height, null, 0, width)
                            val buffer = ByteBuffer.allocate(width * height * 3)
                            for (pixel in pixels) {
                                buffer.put((pixel shr 16 and 0xFF).toByte()) // red
                                buffer.put((pixel shr 8 and 0xFF).toByte()) // green
                                buffer.put((pixel and 0xFF).toByte()) // blue
                            }
                            val baos = buffer.array()
                            output = baos
                            println("Size " + output!!.size)
                            println("Size " + DecodeUtil.byteArrayToHexString(output).subSequence(0,17))
                        }
                                println("Size " + conW+" "+conH)



                                val time4 = System.currentTimeMillis();


                                // Пересчитать RGB24 в YUV420


// Конвертировать RGB в YUV420

                                val frameAfterRGB = av_frame_alloc();
                               // frameAfterRGB.width(context.width())
                               // frameAfterRGB.height(context.height())
                               // frameAfterRGB.format(AV_PIX_FMT_ABGR)
                                //  val pp2: PointerPointer<*> = PointerPointer<Pointer>(frameAfterRGB)
                                val bp2 = BytePointer(ByteBuffer.wrap(output))
                                // av_frame_get_buffer(frameAfterRGB, 32) //было 32

                                av_image_fill_arrays(
                                        frameAfterRGB.data(),
                                        frameAfterRGB.linesize(),
                                        bp2
                                        ,
                                        AV_PIX_FMT_RGB24,//AV_PIX_FMT_RGB24,
                                        conW,conH,
                                       // context.width(),
                                        //context.height(),
                                        1)


                                if (swsContext2 == null) {
                                    swsContext2 = sws_getContext(/*context.width(), context.height(),*/conW,conH, AV_PIX_FMT_RGB24,/*AV_PIX_FMT_RGB24,*/ /*context.width(), context.height(),*/conW,conH, AV_PIX_FMT_YUV420P, 0, null, null, DoublePointer())
                                }

                                val yuvFrame: AVFrame = avutil.av_frame_alloc()
                                if (bufferyuv == null) {
                                    sizeyuv = avutil.av_image_get_buffer_size(avutil.AV_PIX_FMT_YUV420P, /*context.width(), context.height(),*/conW,conH, 1)
                                    bufferyuv = BytePointer(avutil.av_malloc(sizeyuv!!.toLong()))
                                    outputyuv = ByteArray(sizeyuv!!)
                                }
                                avutil.av_image_fill_arrays(yuvFrame.data(), yuvFrame.linesize(), bufferyuv, avutil.AV_PIX_FMT_YUV420P, /*context.width(), context.height(),*/conW,conH, 1)

                                sws_scale(swsContext2, frameAfterRGB.data(), frameAfterRGB.linesize(), 0, /*context.height(),*/conH, yuvFrame.data(), yuvFrame.linesize())
                                bufferyuv!!.get(outputyuv!!)


                                val time5 = System.currentTimeMillis();
                                println("Time 5: "+time5)

                                rtspServer.inputYUVData(rtspServer, Frame(outputyuv, 0, 0))

                                if(i<15) {
                                    println("Convert from H264 to YUV to RGB: " + (time1 - time0));
                                    println("Operator Time: " + (time2 - time1));
                                    println("Create JPG Time: " + (time3 - time2));
                                    println("Otrisovka");
                                    println("Create Array of Eskiz Time: " + (time4 - time3));
                                    println("Convert RGB to YUV Time: " + (time5 - time4));
                                    println("Total: " + (time5 - time0))
                                    i++
                                    totalAverage += (time5-time0).toDouble();
                                }
                                if(i==15){
                                    totalAverage/=15;
                                    println("TotalAverage: "+totalAverage)
                                    i++;

                                }


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

                                bp2.close()
                                img!!.flush()

                                //  baos.reset()
                                //baosjpg!!.reset()


                                //buffer.close() no need
                                //bufferyuv.close() no need
                                framepointer.close()

                                av_packet_unref(packet)

                                av_frame_free(frame)

                                av_frame_free(frameAfterRGB)

                                av_frame_free(yuvFrame)

                                av_frame_free(rgbFrame)


                                //////////////////////////////////////////////////////////////////
                            }
                        } catch (e: Exception) {
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

    fun getBytesFromImage(image: BufferedImage): ByteArray? {
        // Получаем DataBufferByte из объекта BufferedImage
        val buffer = image.raster.dataBuffer as DataBufferByte

        // Получаем массив байтов из DataBufferByte
        return buffer.data
    }
    fun getIntsFromImage(image: BufferedImage): IntArray? {
        // Получаем DataBufferByte из объекта BufferedImage
        val buffer = image.raster.dataBuffer as DataBufferInt

        // Получаем массив байтов из DataBufferByte
        return buffer.data
    }

    fun toBufferedImage(img: Image): BufferedImage {
        if (img is BufferedImage) {
            return img
        }

        // Create a buffered image with transparency
        val bimage = BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB)//TYPE_INT_ARGB  TYPE_4BYTE_ABGR

        // Draw the image on to the buffered image
        val bGr = bimage.createGraphics()
        bGr.drawImage(img, 0, 0, null)
        bGr.dispose()

        // Return the buffered image
        return bimage
    }


    companion object {
        private val TAG: String = VideoDecodeThread::class.java.simpleName
        private const val DEBUG = false
    }

    private fun createRGBImage(bytes: ByteArray, width: Int, height: Int): BufferedImage {
        val buffer = DataBufferByte(bytes, bytes.size)
        val cm: ColorModel = ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), intArrayOf(8, 8, 8), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE)
        return BufferedImage(cm, Raster.createInterleavedRaster(buffer, width, height, width * 3, 3, intArrayOf(0, 1, 2), null), false, null)
    }
    private fun createYCbCrImage(bytes: ByteArray, width: Int, height: Int): BufferedImage? {
        val buffer = DataBufferByte(bytes, bytes.size)
        val cm: ColorModel = ComponentColorModel(ColorSpace.getInstance(ColorSpace.TYPE_YCbCr), intArrayOf(8, 8, 8), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE)
        return BufferedImage(cm, Raster.createInterleavedRaster(buffer, width, height, width * 3, 3, intArrayOf(0, 1, 2), null), false, null)
    }

    private fun createARGBImage2(bytes: ByteArray, width: Int, height: Int): BufferedImage? {
        val buffer = DataBufferByte(bytes, bytes.size)
        val colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB)
        val numBits = intArrayOf(8, 8, 8, 8)
        val colorModel = ComponentColorModel(colorSpace, numBits, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE)
        return BufferedImage(colorModel, Raster.createInterleavedRaster(buffer, width, height, width * 4, 4, intArrayOf(1, 2, 3, 0), null), false, null)
    }

    fun createARGBImage(data: ByteArray, width: Int, height: Int): BufferedImage? {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val imageData = (image.raster.dataBuffer as DataBufferByte).data
        System.arraycopy(data, 0, imageData, 0, data.size)
        return image
    }

    fun hexToRGB(argbHex: Int): IntArray? {
        val rgb = IntArray(3)
        rgb[0] = argbHex and 0xFF0000 shr 16 //get red
        rgb[1] = argbHex and 0xFF00 shr 8 //get green
        rgb[2] = argbHex and 0xFF //get blue
        return rgb //return array
    }

}

