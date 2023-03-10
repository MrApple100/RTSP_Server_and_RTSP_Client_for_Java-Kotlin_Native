package mrapple100.Server.rtspserver


import com.pedro.rtsp.rtsp.VideoCodec
import com.pedro.rtspserver.RtspServer
import mrapple100.Server.MediaBufferInfo
import mrapple100.Server.encoder.utils.CodecUtil
import mrapple100.Server.rtplibrary.base.Camera1Base
import mrapple100.Server.rtsp.utils.ConnectCheckerRtsp
import java.nio.ByteBuffer
import javax.swing.JLabel

/**
 * Created by pedro on 13/02/19.
 */
open class RtspServerCamera1 : Camera1Base {

    private val frameAfterPlace: JLabel
    private val rtspServer: RtspServer

  constructor(frame:JLabel, connectCheckerRtsp: ConnectCheckerRtsp, port: Int) : super() {
      frameAfterPlace = frame
      rtspServer = RtspServer(connectCheckerRtsp, port)
  }


fun getFrameAfterPlace():JLabel = frameAfterPlace

  fun setVideoCodec(videoCodec: VideoCodec) {
    videoEncoder.type =
      if (videoCodec == VideoCodec.H265) CodecUtil.H265_MIME else CodecUtil.H264_MIME
  }

  fun getNumClients(): Int = rtspServer.getNumClients()

  fun getEndPointConnection(): String = "rtsp://${rtspServer.serverIp}:${rtspServer.port}/cast/2"

  override fun setAuthorization(user: String, password: String) {
    rtspServer.setAuth(user, password)
  }

  fun startStream() {
    super.startStream("")
    rtspServer.startServer()
  }


  override fun startStreamRtp(url: String) { //unused
  }

  override fun stopStreamRtp() {
    rtspServer.stopServer()
  }

  override fun onSpsPpsVpsRtp(sps: ByteBuffer, pps: ByteBuffer, vps: ByteBuffer?) {
    val newSps = sps.duplicate()
    val newPps = pps.duplicate()
    val newVps = vps?.duplicate()
    rtspServer.setVideoInfo(newSps, newPps, newVps)
  }

  override fun getH264DataRtp(h264Buffer: ByteBuffer, info: MediaBufferInfo) {
    rtspServer.sendVideo(h264Buffer, info)
  }

  override fun setLogs(enable: Boolean) {
    rtspServer.setLogs(enable)
  }

  override fun setCheckServerAlive(enable: Boolean) {
  }

//  /**
//   * Unused functions
//   */
//  @Throws(RuntimeException::class)
//  override fun resizeCache(newSize: Int) {
//  }
//
//  override fun shouldRetry(reason: String?): Boolean = false
//
//  override fun hasCongestion(): Boolean = rtspServer.hasCongestion()
//
//  override fun setReTries(reTries: Int) {
//  }
//
//  override fun reConnect(delay: Long, backupUrl: String?) {
//  }

   fun getCacheSize(): Int = 0

   fun getSentAudioFrames(): Long = 0

   fun getSentVideoFrames(): Long = 0

   fun getDroppedAudioFrames(): Long = 0

   fun getDroppedVideoFrames(): Long = 0

   fun resetSentAudioFrames() {}


   fun resetSentVideoFrames() {}


   fun resetDroppedAudioFrames() {}


   fun resetDroppedVideoFrames() {}
}