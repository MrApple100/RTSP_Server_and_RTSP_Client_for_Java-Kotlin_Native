
package mrapple100.Server.rtsp.rtp.packets

import mrapple100.Server.rtsp.rtsp.RtpFrame

/**
 * Created by pedro on 7/11/18.
 */
interface VideoPacketCallback {
  fun onVideoFrameCreated(rtpFrame: RtpFrame)
}