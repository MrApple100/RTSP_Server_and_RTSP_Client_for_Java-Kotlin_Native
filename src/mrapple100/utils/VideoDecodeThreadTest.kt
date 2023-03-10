package mrapple100.utils


import mrapple100.Client.rtsp.codec.FrameQueue
import org.bytedeco.ffmpeg.avcodec.AVCodec
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avcodec.av_packet_unref
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.ffmpeg.global.avutil.av_frame_free
import org.bytedeco.ffmpeg.global.swscale.sws_getContext
import org.bytedeco.ffmpeg.global.swscale.sws_scale
import org.bytedeco.ffmpeg.swscale.SwsContext
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.DoublePointer
import java.awt.Image
import java.awt.Toolkit
import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JLabel

class VideoDecodeThreadTest (
        private val framePlace: JLabel,
        private val videoFrameQueue: FrameQueue
) : Thread() {
    var spspps: ByteArray? = null;

    private val codec: AVCodec = avcodec.avcodec_find_decoder(avcodec.AV_CODEC_ID_H264)
    private val context: AVCodecContext = avcodec.avcodec_alloc_context3(codec);
    private val opts = AVDictionary()

    private var swsContext: SwsContext? = null





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
               // println("TEST HERE")

                // Preventing BufferOverflowException
                // if (length > byteBuffer.limit()) throw DecoderFatalException("Error")

                val h264bytes = videoFrameQueue.pop()
                if (h264bytes == null) {
                    //    Log.d(TAG, "Empty video frame")
                    // Release input buffer
                } else {
                    if(h264bytes!!.data[4]== "103".toByte()){
                        spspps = h264bytes.data!!
                       // println("103TEST "+DecodeUtil.byteArrayToHexString(h264bytes.data).subSequence(0,50))
                    }else{
                        val datasrc = h264bytes!!.data
                       // println("OSTTEST "+DecodeUtil.byteArrayToHexString(h264bytes!!.data).subSequence(0,50))

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

                            avcodec.avcodec_open2(context, codec, opts)

                            // }
                            av_packet_unref(packet)
                            val framepointer = BytePointer(bb)

                            //  println("ADDRESSCONTEXT " + "${context.address()}")
                            //  println("ADDRESS " + "${framepointer.address()}")
                            packet.data(framepointer)



                            packet.size(spsppsAndFrame.size)
                            var ret = avcodec.avcodec_send_packet(context, packet)
                            if (ret < 0) {
                                println("ERROR SEND")
                            } else {
                                //   println("${context.width()} ${context.height()}")

                                swsContext = sws_getContext(
                                        context.width(), context.height(), context.pix_fmt(),
                                        context.width(), context.height(), avutil.AV_PIX_FMT_RGB24,
                                        0, null, null, DoublePointer())

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

                                //  println("${++whileenter}")
                                //   println("NICE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                                //println("${swsContext}")
                                //  println("NICE2")
                                val size = avutil.av_image_get_buffer_size(avutil.AV_PIX_FMT_RGB24, context.width(), context.height(), 1)
                                val buffer: BytePointer = BytePointer(avutil.av_malloc(size.toLong()))
                                avutil.av_image_fill_arrays(rgbFrame.data(), rgbFrame.linesize(), buffer, avutil.AV_PIX_FMT_RGB24, context.width(), context.height(), 1)
                                //  println("Nice3")
                                sws_scale(swsContext, frame.data(), frame.linesize(), 0, context.height(), rgbFrame.data(), rgbFrame.linesize())
                                val output = ByteArray(size)
                                buffer.get(output)



                                //  println("OUTPUT" + DecodeUtil.byteArrayToHexString(output)[0])

                                val toolkit = Toolkit.getDefaultToolkit()
                                //display the image as an ImageIcon object
                                //println("Success!")
                                // println("output : ${output[0]} ${output[1]} ${output[2]} ${output.size}")
                                var img = createRGBImage(output, context.width(), context.height())

                                //println("IMG "+DecodeUtil.byteArrayToHexString(output).subSequence(0,100))
                                //  println("img : ${(img!!.raster.dataBuffer as DataBufferByte).data[0]} ${(img!!.raster.dataBuffer as DataBufferByte).data[1]} ${(img!!.raster.dataBuffer as DataBufferByte).data[2]} ${output.size}")

                                var baos: ByteArrayOutputStream? = null
                                try {
                                    baos = ByteArrayOutputStream()
                                    ImageIO.write(img, "jpg", baos)
                                } finally {
                                    try {
                                        baos!!.close()
                                    } catch (e: java.lang.Exception) {
                                    }
                                }
                                //   println("baos : ${baos!!.toByteArray()[0]} ${baos.toByteArray()[1]} ${baos.toByteArray()[2]} ${baos.toByteArray().size}" )

                                //   println("Success!")

                                //   println(""+System.currentTimeMillis()+" "+baos!!.toByteArray().size)
                                framePlace.icon = ImageIcon(toolkit.createImage(baos!!.toByteArray(), 0, baos.size()).getScaledInstance(600,800, Image.SCALE_DEFAULT))


                                baos.reset()
                                av_packet_unref(packet)
                                av_frame_free(frame)
                                av_frame_free(rgbFrame)
//                    avcodec_close(context)
//                    avcodec_free_context(context)
//
//                    sws_freeContext(swsContext)
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
       // private val TAG: String = VideoDecodeThread::class.java.simpleName
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

