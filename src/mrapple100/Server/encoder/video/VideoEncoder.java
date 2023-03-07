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

package mrapple100.Server.encoder.video;

import javafx.util.Pair;
import mrapple100.Server.DecodeUtil;
import mrapple100.Server.MediaBufferInfo;
import mrapple100.Server.encoder.BaseEncoder;
import mrapple100.Server.encoder.Frame;
import mrapple100.Server.encoder.input.video.FpsLimiter;
import mrapple100.Server.encoder.utils.CodecUtil;
import mrapple100.Server.encoder.utils.yuv.YUVUtil;
import mrapple100.Server.rtspserver.RtspServerCamera1;
import mrapple100.utils.MediaCodec;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;
import static org.bytedeco.ffmpeg.global.avutil.av_opt_set;

/**
 * Created by pedro on 19/01/17.
 * This class need use same resolution, fps and imageFormat that Camera1ApiManagerGl
 */

public class VideoEncoder extends BaseEncoder {

  private final GetVideoData getVideoData;
  private boolean spsPpsSetted = false;
  private boolean forceKey = false;
  //video data necessary to send after requestKeyframe.
  private ByteBuffer oldSps, oldPps, oldVps;

  private int width = 640;
  private int height = 480;
  private int fps = 30;
  private int bitRate = 1200 * 1024; //in kbps
  private int rotation = 0;
  private int iFrameInterval = 0;


  //for disable video
  private final FpsLimiter fpsLimiter = new FpsLimiter();
  private String type = CodecUtil.H264_MIME;
  private FormatVideoEncoder formatVideoEncoder = FormatVideoEncoder.YUV420Dynamical;
  private int avcProfile = -1;
  private int avcProfileLevel = -1;

  public VideoEncoder(GetVideoData getVideoData) {
    this.getVideoData = getVideoData;
    TAG = "VideoEncoder";
  }

  public boolean prepareVideoEncoder(int width, int height, int fps, int bitRate, int rotation,
      int iFrameInterval, FormatVideoEncoder formatVideoEncoder) {
    return prepareVideoEncoder(width, height, fps, bitRate, rotation, iFrameInterval,
        formatVideoEncoder, -1, -1);
  }

  /**
   * Prepare encoder with custom parameters
   */
  public boolean prepareVideoEncoder(int width, int height, int fps, int bitRate, int rotation,
      int iFrameInterval, FormatVideoEncoder formatVideoEncoder, int avcProfile,
      int avcProfileLevel) {
    this.width = width;
    this.height = height;
    this.fps = fps;
    this.bitRate = bitRate;
    this.rotation = rotation;
    this.iFrameInterval = iFrameInterval;
    this.formatVideoEncoder = formatVideoEncoder;
    this.avcProfile = avcProfile;
    this.avcProfileLevel = avcProfileLevel;
    isBufferMode = true;


    codec = avcodec_find_encoder(AV_CODEC_ID_H264);
    c = avcodec_alloc_context3(codec);
    c.bit_rate(bitRate);
    c.width(width);
    c.height(height);
    c.time_base().num(1).den(fps);
   // c.framerate().num(fps).den(1);
    c.gop_size(1);
    //c.max_b_frames(0);
    c.pix_fmt(AV_PIX_FMT_YUV420P);
    avcodec_open2(c, codec, new AVDictionary());

    //  av_opt_set(c.priv_data(),"preset","ultrafast",0);

//     // Log.i(TAG, "Prepare video info: " + this.formatVideoEncoder.name() + ", " + resolution);
//      videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
//          this.formatVideoEncoder.getFormatCodec());
//      videoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
//      videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
//      videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
//      videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);

      // Rotation by encoder.
      // Removed because this is ignored by most encoders, producing different results on different devices
      //  videoFormat.setInteger(MediaFormat.KEY_ROTATION, rotation);

     // this.stop();
      return true;

  }

  @Override
  public void start(boolean resetTs) {
    forceKey = false;
    shouldReset = resetTs;
    spsPpsSetted = false;
    if (resetTs) {
      fpsLimiter.setFPS(fps);
    }
    if (formatVideoEncoder != FormatVideoEncoder.SURFACE) {
      YUVUtil.preAllocateBuffers(width * height * 3 / 2);
    }
   //// Log.i(TAG, "started");
  }

  @Override
  protected void stopImp() {
    spsPpsSetted = false;
    oldSps = null;
    oldPps = null;
    oldVps = null;
   //// Log.i(TAG, "stopped");
  }

  @Override
  public void reset() {
    stop(false);
    prepareVideoEncoder(width, height, fps, bitRate, rotation, iFrameInterval, formatVideoEncoder,
        avcProfile, avcProfileLevel);
    restart();
  }


  /**
   * Prepare encoder with default parameters
   */
  public boolean prepareVideoEncoder() {
    return prepareVideoEncoder(width, height, fps, bitRate, rotation, iFrameInterval,
        formatVideoEncoder, avcProfile, avcProfileLevel);
  }

//  public void setVideoBitrateOnFly(int bitrate) {
//    if (isRunning()) {
//      this.bitRate = bitrate;
//      Bundle bundle = new Bundle();
//      bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bitrate);
//      try {
//        codec.setParameters(bundle);
//      } catch (IllegalStateException e) {
//       // Log.e(TAG, "encoder need be running", e);
//      }
//    }
//  }

//  public void requestKeyframe() {
//    if (isRunning()) {
//      if (spsPpsSetted) {
//        Bundle bundle = new Bundle();
//        bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
//        try {
//          codec.setParameters(bundle);
//          getVideoData.onSpsPpsVps(oldSps, oldPps, oldVps);
//        } catch (IllegalStateException e) {
//         // Log.e(TAG, "encoder need be running", e);
//        }
//      } else {
//        //You need wait until encoder generate first frame.
//        forceKey = true;
//      }
//    }
//  }


  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int getRotation() {
    return rotation;
  }

  public void setFps(int fps) {
    this.fps = fps;
  }

  public void setRotation(int rotation) {
    this.rotation = rotation;
  }

  public int getFps() {
    return fps;
  }

  public int getBitRate() {
    return bitRate;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

//этот метод нужно поместить туда где появляется кадры
  public void inputYUVData(RtspServerCamera1 rtspServerCamera1,Frame frame) {
    //initial codec H264

    //open codec
    //avcodec_open2(c,codec,new AVDictionary());
    //initial media-writer ffmpeg

    this.rtspServer = rtspServerCamera1;

    if ( !queue.offer(frame)) {
      System.out.println("frame discarded");
    }
  }
public static byte[] imageToByteArray(BufferedImage image) {
  try {
    byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
    int size = image.getWidth()*image.getHeight()*3;
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(size);
    outputStream.write(pixels);
    return outputStream.toByteArray();
  } catch (IOException e) {
    e.printStackTrace();
  }
  return null;
}
  /*private void sendSPSandPPS(MediaFormat mediaFormat) {
    //H265
    if (type.equals(CodecUtil.H265_MIME)) {
      List<ByteBuffer> byteBufferList = extractVpsSpsPpsFromH265(mediaFormat.getByteBuffer("csd-0"));
      oldSps = byteBufferList.get(1);
      oldPps = byteBufferList.get(2);
      oldVps = byteBufferList.get(0);
      getVideoData.onSpsPpsVps(oldSps, oldPps, oldVps);
      //H264
    } else {
      oldSps = mediaFormat.getByteBuffer("csd-0");
      oldPps = mediaFormat.getByteBuffer("csd-1");
      oldVps = null;
      getVideoData.onSpsPpsVps(oldSps, oldPps, oldVps);
    }
  }*/

  /**
   * decode sps and pps if the encoder never call to MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
   */
  private Pair<ByteBuffer, ByteBuffer> decodeSpsPpsFromBuffer(ByteBuffer outputBuffer, int length) {
    byte[] csd = new byte[length];
    outputBuffer.get(csd, 0, length);
    outputBuffer.rewind();
    int i = 0;
    int spsIndex = -1;
    int ppsIndex = -1;
    while (i < length - 4) {
      if (csd[i] == 0 && csd[i + 1] == 0 && csd[i + 2] == 0 && csd[i + 3] == 1) {
        if (spsIndex == -1) {
          spsIndex = i;
        } else {
          ppsIndex = i;
          break;
        }
      }
      i++;
    }
    if (spsIndex != -1 && ppsIndex != -1) {
      byte[] sps = new byte[ppsIndex];
      System.arraycopy(csd, spsIndex, sps, 0, ppsIndex);
      byte[] pps = new byte[length - ppsIndex];
      System.arraycopy(csd, ppsIndex, pps, 0, length - ppsIndex);
      return new Pair<>(ByteBuffer.wrap(sps), ByteBuffer.wrap(pps));
    }
    return null;
  }

  /**
   * You need find 0 0 0 1 byte sequence that is the initiation of vps, sps and pps
   * buffers.
   *
   * @param csd0byteBuffer get in mediacodec case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
   * @return list with vps, sps and pps
   */
  private List<ByteBuffer> extractVpsSpsPpsFromH265(ByteBuffer csd0byteBuffer) {
    List<ByteBuffer> byteBufferList = new ArrayList<>();
    int vpsPosition = -1;
    int spsPosition = -1;
    int ppsPosition = -1;
    int contBufferInitiation = 0;
    int length = csd0byteBuffer.remaining();
    byte[] csdArray = new byte[length];
    csd0byteBuffer.get(csdArray, 0, length);
    csd0byteBuffer.rewind();
    for (int i = 0; i < csdArray.length; i++) {
      if (contBufferInitiation == 3 && csdArray[i] == 1) {
        if (vpsPosition == -1) {
          vpsPosition = i - 3;
        } else if (spsPosition == -1) {
          spsPosition = i - 3;
        } else {
          ppsPosition = i - 3;
        }
      }
      if (csdArray[i] == 0) {
        contBufferInitiation++;
      } else {
        contBufferInitiation = 0;
      }
    }
    byte[] vps = new byte[spsPosition];
    byte[] sps = new byte[ppsPosition - spsPosition];
    byte[] pps = new byte[csdArray.length - ppsPosition];
    for (int i = 0; i < csdArray.length; i++) {
      if (i < spsPosition) {
        vps[i] = csdArray[i];
      } else if (i < ppsPosition) {
        sps[i - spsPosition] = csdArray[i];
      } else {
        pps[i - ppsPosition] = csdArray[i];
      }
    }
    byteBufferList.add(ByteBuffer.wrap(vps));
    byteBufferList.add(ByteBuffer.wrap(sps));
    byteBufferList.add(ByteBuffer.wrap(pps));
    return byteBufferList;
  }

  @Override
  protected Frame getInputFrame() throws InterruptedException {
    Frame frame = queue.take();
    if (frame == null) return null;
    if (fpsLimiter.limitFPS()) return getInputFrame();
   // byte[] buffer = frame.getBuffer();
    boolean isYV12 = frame.getFormat() == 20;//ImageFormat.YV12;

//    int orientation = frame.isFlip() ? frame.getOrientation() + 180 : frame.getOrientation();
//    if (orientation >= 360) orientation -= 360;

//    frame.setBuffer(buffer);
    return frame;
  }

  @Override
  protected long calculatePts(Frame frame, long presentTimeUs) {
    return System.nanoTime() / 1000 - presentTimeUs;
  }


  @Override
  protected void checkBuffer(@NotNull ByteBuffer byteBuffer, @NotNull MediaBufferInfo bufferInfo) {
      forceKey = false;
     // requestKeyframe();
    bufferInfo.size=byteBuffer.array().length;
    bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
    bufferInfo.offset = 0;
    bufferInfo.presentationTimeUs = System.currentTimeMillis();
    fixTimeStamp(bufferInfo);
    if (!spsPpsSetted && type.equals(CodecUtil.H264_MIME)) {
      System.out.println("formatChanged not called, doing manual sps/pps extraction..."+bufferInfo.toString()+" "+spsPpsSetted);

      Pair<ByteBuffer, ByteBuffer> buffers = decodeSpsPpsFromBuffer(byteBuffer.duplicate(), byteBuffer.array().length);
      if (buffers != null) {
        System.out.println("manual sps/pps extraction success");
        oldSps = buffers.getKey();
        oldPps = buffers.getValue();
        oldVps = null;
       // System.out.println("SPS "+DecodeUtil.byteArrayToHexString(oldSps.array()));
       // System.out.println("PPS "+DecodeUtil.byteArrayToHexString(oldPps.array()));

        getVideoData.onSpsPpsVps(oldSps, oldPps, oldVps);
        spsPpsSetted = true;
      } else {
       // Log.e(TAG, "manual sps/pps extraction failed");
      }
    } else if (!spsPpsSetted && type.equals(CodecUtil.H265_MIME)) {
     // Log.i(TAG, "formatChanged not called, doing manual vps/sps/pps extraction...");
      List<ByteBuffer> byteBufferList = extractVpsSpsPpsFromH265(byteBuffer);
      if (byteBufferList.size() == 3) {
       // Log.i(TAG, "manual vps/sps/pps extraction success");
        oldSps = byteBufferList.get(1);
        oldPps = byteBufferList.get(2);
        oldVps = byteBufferList.get(0);
        getVideoData.onSpsPpsVps(oldSps, oldPps, oldVps);
        spsPpsSetted = true;
      } else {
       // Log.e(TAG, "manual vps/sps/pps extraction failed");
      }
    }
    if (formatVideoEncoder == FormatVideoEncoder.SURFACE) {
      bufferInfo.presentationTimeUs = System.nanoTime() / 1000 - presentTimeUs;
    }
  }

  @Override
  protected void sendBuffer(@NotNull ByteBuffer byteBuffer,
      @NotNull MediaBufferInfo bufferInfo) {
    //System.out.println("HERE");
    getVideoData.getVideoData(byteBuffer, bufferInfo);
  }
}
