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

package mrapple100.Server.rtplibrary.rtsp;

import com.pedro.rtsp.rtsp.VideoCodec;
import mrapple100.Server.MediaBufferInfo;
import mrapple100.Server.encoder.utils.CodecUtil;
import mrapple100.Server.rtplibrary.base.Camera1Base;
import mrapple100.Server.rtsp.rtsp.Protocol;
import mrapple100.Server.rtsp.rtsp.RtspClient;
import mrapple100.Server.rtsp.utils.ConnectCheckerRtsp;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;


public class RtspCamera1 extends Camera1Base {

  private final RtspClient rtspClient;

  public RtspCamera1(ConnectCheckerRtsp connectCheckerRtsp) {
    super();
    rtspClient = new RtspClient(connectCheckerRtsp);
  }

  /**
   * Internet protocol used.
   *
   * @param protocol Could be Protocol.TCP or Protocol.UDP.
   */
  public void setProtocol(Protocol protocol) {
    rtspClient.setProtocol(protocol);
  }

  public void resizeCache(int newSize) throws RuntimeException {
    rtspClient.resizeCache(newSize);
  }


  public void setVideoCodec(VideoCodec videoCodec) {
    videoEncoder.setType(videoCodec == VideoCodec.H265 ? CodecUtil.H265_MIME : CodecUtil.H264_MIME);
  }

  @Override
  public void setAuthorization(String user, String password) {
    rtspClient.setAuthorization(user, password);
  }


  @Override
  protected void startStreamRtp(String url) {
    rtspClient.setOnlyVideo(!audioInitialized);
    rtspClient.connect(url);
  }

  @Override
  protected void stopStreamRtp() {
    rtspClient.disconnect();
  }

  public void setReTries(int reTries) {
    rtspClient.setReTries(reTries);
  }


  protected boolean shouldRetry(String reason) {
    return rtspClient.shouldRetry(reason);
  }

  public void reConnect(long delay, @Nullable String backupUrl) {
    rtspClient.reConnect(delay, backupUrl);
  }

  public boolean hasCongestion() {
    return rtspClient.hasCongestion();
  }


  @Override
  protected void onSpsPpsVpsRtp(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps) {
    rtspClient.setVideoInfo(sps, pps, vps);
  }

  @Override
  protected void getH264DataRtp(ByteBuffer h264Buffer, MediaBufferInfo info) {
    rtspClient.sendVideo(h264Buffer, info);
  }

  @Override
  public void setLogs(boolean enable) {
    rtspClient.setLogs(enable);
  }

  @Override
  public void setCheckServerAlive(boolean enable) {
    rtspClient.setCheckServerAlive(enable);
  }


}
