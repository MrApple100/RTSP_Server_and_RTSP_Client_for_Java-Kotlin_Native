
package mrapple100.Server.rtsp.rtp.sockets

import mrapple100.Server.rtsp.rtsp.Protocol
import mrapple100.Server.rtsp.rtsp.RtpFrame
import java.io.IOException
import java.io.OutputStream

/**
 * Created by pedro on 7/11/18.
 */
abstract class BaseRtpSocket {

  protected val TAG = "BaseRtpSocket"

  companion object {
    @JvmStatic
    fun getInstance(protocol: Protocol, videoSourcePort: Int, audioSourcePort: Int): BaseRtpSocket {
      return if (protocol === Protocol.TCP) {
        RtpSocketTcp()
      } else {
        RtpSocketUdp(videoSourcePort, audioSourcePort)
      }
    }
  }

  @Throws(IOException::class)
  abstract fun setDataStream(outputStream: OutputStream, host: String)

  @Throws(IOException::class)
  abstract fun sendFrame(rtpFrame: RtpFrame, isEnableLogs: Boolean)

  abstract fun close()
}