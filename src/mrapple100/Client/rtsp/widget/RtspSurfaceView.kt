package mrapple100.Client.rtsp.widget

//import mrapple100.Client.rtsp.codec.AudioDecodeThread
//import android.content.Context
//import android.media.MediaCodec
//import android.os.Handler
//import android.os.Looper
//import android.util.AttributeSet
//import android.view.SurfaceHolder
//import android.view.SurfaceView
//import mrapple100.Client.rtsp.codec.VideoDecodeThread


import mrapple100.Client.rtsp.RtspClient
import mrapple100.Client.rtsp.codec.FrameQueue
import mrapple100.Client.rtsp.codec.VideoDecodeThread
import mrapple100.Server.rtspserver.RtspServerCamera1
import mrapple100.utils.FrameSynchronizer
import mrapple100.utils.NetUtils
import org.bytedeco.ffmpeg.avcodec.AVCodec
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avformat.AVStream
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacpp.IntPointer
import java.io.*
import java.net.Socket
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.ImageIcon
import javax.swing.JLabel


open class RtspSurfaceView(
        val framePlace:JLabel,
        val RtspServer:RtspServerCamera1
) {

    var spspps: ByteArray? = null;
    var spsppsidr:ByteArray? = null;

    //GUI
    var icon: ImageIcon? = null
    //----
    var fsynch: FrameSynchronizer = FrameSynchronizer(100)



    var debug: Boolean = false

    private lateinit var uri: URI
    private var username: String? = null
    private var password: String? = null
    private var userAgent: String? = null
    private var requestVideo = true
    private var requestAudio = true
    private var rtspThread: RtspThread? = null
    private var videoFrameQueue = FrameQueue(1)
    private var audioFrameQueue = FrameQueue(10)
    private var videoDecodeThread: VideoDecodeThread? = null
  //  private var audioDecodeThread: AudioDecodeThread? = null
    private var surfaceWidth = 1920
    private var surfaceHeight = 1080
    private var statusListener: RtspStatusListener? = null
  //  private val uiHandler = Handler(Looper.getMainLooper())
    private var videoMimeType: String = "video/avc"
    private var audioMimeType: String = ""
    private var audioSampleRate: Int = 0
    private var audioChannelCount: Int = 0
    private var audioCodecConfig: ByteArray? = null
    private var firstFrameRendered = false

    private var seqNb = 1;

    ///////////
    private lateinit var avfmtCtx:AVFormatContext
    private lateinit var videoStream:AVStream



    interface RtspStatusListener {
        fun onRtspStatusConnecting()
        fun onRtspStatusConnected()
        fun onRtspStatusDisconnected()
        fun onRtspStatusFailedUnauthorized()
        fun onRtspStatusFailed(message: String?)
        fun onRtspFirstFrameRendered()
    }

    private val proxyClientListener = object: RtspClient.RtspClientListener {

        override fun onRtspConnecting() {
           // if (DEBUG) Log.v(TAG, "onRtspConnecting()")
           /* uiHandler.post {
                statusListener?.onRtspStatusConnecting()
            }*/
        }

        override fun onRtspConnected(sdpInfo: RtspClient.SdpInfo) {
           // if (DEBUG) Log.v(TAG, "onRtspConnected()")
            if (sdpInfo.videoTrack != null) {
                videoFrameQueue.clear()
                when (sdpInfo.videoTrack?.videoCodec) {
                    RtspClient.VIDEO_CODEC_H264 -> videoMimeType = "video/avc"
                    RtspClient.VIDEO_CODEC_H265 -> videoMimeType = "video/hevc"
                }

                val sps: ByteArray? = sdpInfo.videoTrack?.sps
                val pps: ByteArray? = sdpInfo.videoTrack?.pps
                // Initialize decoder
                if (sps != null && pps != null) {
                    val data = ByteArray(sps.size + pps.size)
                    sps.copyInto(data, 0, 0, sps.size)
                    pps.copyInto(data, sps.size, 0, pps.size)
                    videoFrameQueue.push(FrameQueue.Frame(data, 0, data.size, 0))

                    spspps = data

                } else {
                }
            }

            onRtspClientConnected()

        }
var nframe =0
        override fun onRtspVideoNalUnitReceived(inputstream:InputStream,datasrc: ByteArray, offset: Int, length: Int, timestamp: Long,type: Byte) {
            if(length > 0){
                videoFrameQueue.push(FrameQueue.Frame(datasrc,offset,length,timestamp))


                //videoFrameQueue.push(FrameQueue.Frame(data, offset, length, timestamp))

            }
        }



        override fun onRtspAudioSampleReceived(data: ByteArray, offset: Int, length: Int, timestamp: Long) {
            if (length > 0) audioFrameQueue.push(FrameQueue.Frame(data, offset, length, timestamp))
        }

        override fun onRtspDisconnected() {
         //   if (DEBUG) Log.v(TAG, "onRtspDisconnected()")
            /*uiHandler.post {
                statusListener?.onRtspStatusDisconnected()
            }*/
        }

        override fun onRtspFailedUnauthorized() {
         //   if (DEBUG) Log.v(TAG, "onRtspFailedUnauthorized()")
            /*uiHandler.post {
                statusListener?.onRtspStatusFailedUnauthorized()
            }*/
        }

        override fun onRtspFailed(message: String?) {
          //  if (DEBUG) Log.v(TAG, "onRtspFailed(message='$message')")
           /* uiHandler.post {
                statusListener?.onRtspStatusFailed(message)
            }*/
        }
    }

   /*private val surfaceCallback = object: SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
        //    if (DEBUG) Log.v(TAG, "surfaceCreated()")
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
         //   if (DEBUG) Log.v(TAG, "surfaceChanged(format=$format, width=$width, height=$height)")
            surfaceWidth = width
            surfaceHeight = height
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
          //  if (DEBUG) Log.v(TAG, "surfaceDestroyed()")
            stopDecoders()
        }
    }*/

    /*constructor(context: Context) : super(context) {
        initView(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView(context, attrs, defStyleAttr)
    }*/

   /* private fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        holder.addCallback(surfaceCallback)
    }*/

    fun init(uri: URI, username: String?, password: String?) {
        init(uri, username, password, null)
    }

    fun init(uri: URI, username: String?, password: String?, userAgent: String?) {
     //   if (DEBUG) Log.v(TAG, "init(uri='$uri', username=$username, password=$password, userAgent='$userAgent')")
        this.uri = uri
        this.username = username
        this.password = password
        this.userAgent = userAgent
    }

    fun start(requestVideo: Boolean, requestAudio: Boolean) {
      //  if (DEBUG) Log.v(TAG, "start(requestVideo=$requestVideo, requestAudio=$requestAudio)")
        if (rtspThread != null) rtspThread?.stopAsync()
        this.requestVideo = requestVideo
        this.requestAudio = requestAudio
        rtspThread = RtspThread()
        rtspThread!!.name = "RTSP IO thread [${getUriName()}]"
        rtspThread!!.start()
    }

    fun stop() {
       // if (DEBUG) Log.v(TAG, "stop()")
        rtspThread?.stopAsync()
        rtspThread = null
    }

    fun isStarted(): Boolean {
        return rtspThread != null
    }

    inner class RtspThread: Thread() {
        private var rtspStopped: AtomicBoolean = AtomicBoolean(false)

        fun stopAsync() {
         //   if (DEBUG) Log.v(TAG, "stopAsync()")
            rtspStopped.set(true)
            // Wake up sleep() code
            interrupt()
        }

        override fun run() {
            onRtspClientStarted()
            val port = if (uri.port == -1) DEFAULT_RTSP_PORT else uri.port
            println(uri.host)
            println(port)
            try {
             //   if (DEBUG) Log.d(TAG, "Connecting to ${uri.host.toString()}:$port...")

                val socket: Socket = if (uri.scheme?.toLowerCase() == "rtsps")
                    NetUtils.createSslSocketAndConnect(uri.host.toString(), port, 5000)
                else
                    NetUtils.createSocketAndConnect(uri.host.toString(), port, 5000)

                // Blocking call until stopped variable is true or connection failed
                val rtspClient = RtspClient.Builder(socket, uri.toString(), rtspStopped, proxyClientListener)
                    .requestVideo(requestVideo)
                    .requestAudio(requestAudio)
                    .withDebug(debug)
                    .withUserAgent(userAgent)
                    .withCredentials(username, password)
                    .build()
                rtspClient.execute()

                NetUtils.closeSocket(socket)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onRtspClientStopped()
        }
    }

    fun setStatusListener(listener: RtspStatusListener) {
      //  if (DEBUG) Log.v(TAG, "setStatusListener()")
        this.statusListener = listener
    }

    private fun onRtspClientStarted() {
       // if (DEBUG) Log.v(TAG, "onRtspClientStarted()")
      //  uiHandler.post { statusListener?.onRtspStatusConnected() }
    }

    private fun onRtspClientConnected() {
      //  if (DEBUG) Log.v(TAG, "onRtspClientConnected()")
        if (videoMimeType.isNotEmpty()) {
            firstFrameRendered = false
            /*val onFrameRenderedListener =
                MediaCodec.OnFrameRenderedListener { _, _, _ ->
                    if (!firstFrameRendered) statusListener?.onRtspFirstFrameRendered()
                    firstFrameRendered = true
                }*/
            val codec: AVCodec = avcodec.avcodec_find_decoder(avcodec.AV_CODEC_ID_H264)
            val context: AVCodecContext = avcodec.avcodec_alloc_context3(codec);
            val opts = AVDictionary()

            avutil.av_opt_set(context.priv_data(), "tune", "fastdecode", 0) //?

            avcodec.avcodec_open2(context,codec, opts)


            //   Log.i(TAG, "Starting video decoder with mime type \"$videoMimeType\"")
            videoDecodeThread = VideoDecodeThread(codec,context,opts,
                framePlace,RtspServer, videoFrameQueue)
            videoDecodeThread!!.name = "RTSP video thread [${getUriName()}]"
            videoDecodeThread!!.start()
        }
        /*if (audioMimeType.isNotEmpty() *//*&& checkAudio!!.isChecked*//*) {
         //   Log.i(TAG, "Starting audio decoder with mime type \"$audioMimeType\"")
            audioDecodeThread = AudioDecodeThread(
                audioMimeType, audioSampleRate, audioChannelCount, audioCodecConfig, audioFrameQueue)
            audioDecodeThread!!.name = "RTSP audio thread [${getUriName()}]"
            audioDecodeThread!!.start()
        }*/
    }

    private fun onRtspClientStopped() {
       // if (DEBUG) Log.v(TAG, "onRtspClientStopped()")
      //  uiHandler.post { statusListener?.onRtspStatusDisconnected() }
        stopDecoders()
        rtspThread = null
    }

    private fun stopDecoders() {
     //   if (DEBUG) Log.v(TAG, "stopDecoders()")
        videoDecodeThread?.stopAsync()
        videoDecodeThread = null
      //  audioDecodeThread?.stopAsync()
      //  audioDecodeThread = null
    }

    private fun getUriName(): String {
        val port = if (uri.port == -1) DEFAULT_RTSP_PORT else uri.port
        return "${uri.host.toString()}:$port"
    }

    companion object {
        private val TAG: String = RtspSurfaceView::class.java.simpleName
        private const val DEBUG = false
        private const val DEFAULT_RTSP_PORT = 554
    }

}
