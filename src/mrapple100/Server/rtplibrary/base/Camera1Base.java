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

package mrapple100.Server.rtplibrary.base;

import mrapple100.Server.MediaBufferInfo;
import mrapple100.Server.encoder.Frame;
import mrapple100.Server.encoder.input.video.GetCameraData;
import mrapple100.Server.encoder.utils.CodecUtil;
import mrapple100.Server.encoder.video.FormatVideoEncoder;
import mrapple100.Server.encoder.video.GetVideoData;
import mrapple100.Server.encoder.video.VideoEncoder;
import mrapple100.Server.rtplibrary.util.FpsListener;
import mrapple100.Server.rtspserver.RtspServerCamera1;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Wrapper to stream with camera1 api and microphone. Support stream with SurfaceView, TextureView
 * and OpenGlView(Custom SurfaceView that use OpenGl). SurfaceView and TextureView use buffer to
 * buffer encoding mode for H264 and OpenGlView use Surface to buffer mode(This mode is generally
 * better because skip buffer processing).
 *
 * API requirements:
 * SurfaceView and TextureView mode: API 16+.
 * OpenGlView: API 18+.
 *
 * Created by pedro on 7/07/17.
 */

public abstract class Camera1Base
    implements GetCameraData, GetVideoData {

  private static final String TAG = "Camera1Base";

  //private final GetCameraData getCameraData;
  protected VideoEncoder videoEncoder;
  private boolean streaming = false;
  protected boolean audioInitialized = false;
  private boolean onPreview = false;
  private int previewWidth, previewHeight;
  private final FpsListener fpsListener = new FpsListener();

  public Camera1Base() {
    init();
  }




  private void init() {
    videoEncoder = new VideoEncoder(this);
  }

  //public void setCameraCallbacks(CameraCallbacks callbacks) {
   // cameraManager.setCameraCallbacks(callbacks);
 // }


  /**
   * @param callback get fps while record or stream
   */
  public void setFpsListener(FpsListener.Callback callback) {
    fpsListener.setCallback(callback);
  }

//  /**
//   * @return true if success, false if fail (not supported or called before start camera)
//   */
//  public boolean enableFaceDetection(Camera1ApiManager.FaceDetectorCallback faceDetectorCallback) {
//    return cameraManager.enableFaceDetection(faceDetectorCallback);
//  }
//
//  public void disableFaceDetection() {
//    cameraManager.disableFaceDetection();
//  }
//
//  public boolean isFaceDetectionEnabled() {
//    return cameraManager.isFaceDetectionEnabled();
//  }

//  /**
//   * @return true if success, false if fail (not supported or called before start camera)
//   */
//  public boolean enableVideoStabilization() {
//    return cameraManager.enableVideoStabilization();
//  }
//
//  public void disableVideoStabilization() {
//    cameraManager.disableVideoStabilization();
//  }
//
//  public boolean isVideoStabilizationEnabled() {
//    return cameraManager.isVideoStabilizationEnabled();
//  }

//  /**
//   * Use getCameraFacing instead
//   */
//  @Deprecated
//  public boolean isFrontCamera() {
//    return cameraManager.getCameraFacing() == CameraHelper.Facing.FRONT;
//  }
//
//  public CameraHelper.Facing getCameraFacing() {
//    return cameraManager.getCameraFacing();
//  }
//
//  public void enableLantern() throws Exception {
//    cameraManager.enableLantern();
//  }

//  public void disableLantern() {
//    cameraManager.disableLantern();
//  }
//
//  public boolean isLanternEnabled() {
//    return cameraManager.isLanternEnabled();
//  }
//
//  public void enableAutoFocus() {
//    cameraManager.enableAutoFocus();
//  }
//
//  public void disableAutoFocus() {
//    cameraManager.disableAutoFocus();
//  }
//
//  public boolean isAutoFocusEnabled() {
//    return cameraManager.isAutoFocusEnabled();
//  }
//
//  /**
//   * Basic auth developed to work with Wowza. No tested with other server
//   *
//   * @param user auth.
//   * @param password auth.
//   */
  public abstract void setAuthorization(String user, String password);

  /**
   * Call this method before use @startStream. If not you will do a stream without video. NOTE:
   * Rotation with encoder is silence ignored in some devices.
   *
   * @param width resolution in px.
   * @param height resolution in px.
   * @param fps frames per second of the stream.
   * @param bitrate H264 in bps.
   * @param rotation could be 90, 180, 270 or 0. You should use CameraHelper.getCameraOrientation
   * with SurfaceView or TextureView and 0 with OpenGlView or LightOpenGlView. NOTE: Rotation with
   * encoder is silence ignored in some devices.
   * @return true if success, false if you get a error (Normally because the encoder selected
   * doesn't support any configuration seated or your device hasn't a H264 encoder).
   */
  public boolean prepareVideo(int width, int height, int fps, int bitrate, int iFrameInterval,
      int rotation, int avcProfile, int avcProfileLevel) {
    if (onPreview && width != previewWidth || height != previewHeight
        || fps != videoEncoder.getFps() || rotation != videoEncoder.getRotation()) {
      //stopPreview();
      onPreview = true;
    }
    FormatVideoEncoder formatVideoEncoder = FormatVideoEncoder.YUV420Dynamical;
    return videoEncoder.prepareVideoEncoder(width, height, fps, bitrate, rotation, iFrameInterval,
        formatVideoEncoder, avcProfile, avcProfileLevel);
  }

  /**
   * backward compatibility reason
   */
  public boolean prepareVideo(int width, int height, int fps, int bitrate, int iFrameInterval,
      int rotation) {
    return prepareVideo(width, height, fps, bitrate, iFrameInterval, rotation, -1, -1);
  }

  public boolean prepareVideo(int width, int height, int fps, int bitrate, int rotation) {
    return prepareVideo(width, height, fps, bitrate, 0, rotation);
  }

  /**
   * Same to call: rotation = 0; if (Portrait) rotation = 90; prepareVideo(640, 480, 30, 1200 *
   * 1024, false, rotation);
   *
   * @return true if success, false if you get a error (Normally because the encoder selected
   * doesn't support any configuration seated or your device hasn't a H264 encoder).
   */
  public boolean prepareVideo() {
   // int rotation = CameraHelper.getCameraOrientation(context);
    return prepareVideo(640, 480, 30, 1024*1200, 0);
  }



  /**
   * @param forceVideo force type codec used. FIRST_COMPATIBLE_FOUND, SOFTWARE, HARDWARE
   * @param forceAudio force type codec used. FIRST_COMPATIBLE_FOUND, SOFTWARE, HARDWARE
   */
  public void setForce(CodecUtil.Force forceVideo, CodecUtil.Force forceAudio) {
    videoEncoder.setForce(forceVideo);
  }

  /**
   * Starts recording a MP4 video.
   *
   * @param fd Where the file will be saved.
   * @throws IOException If initialized before a stream.
   */
  public void startRecord(final FileDescriptor fd) throws IOException {
    if (!streaming) {
      startEncoders();
    } else if (videoEncoder.isRunning()) {
//      requestKeyFrame();
    }
  }



  public void startStreamAndRecord(String url, String path) throws IOException {
    startStream(url);
  }


  protected abstract void startStreamRtp(String url);

  /**
   * Need be called after @prepareVideo or/and @prepareAudio. This method override resolution of
   *
   * @param url of the stream like: protocol://ip:port/application/streamName
   *
   * RTSP: rtsp://192.168.1.1:1935/live/pedroSG94 RTSPS: rtsps://192.168.1.1:1935/live/pedroSG94
   * RTMP: rtmp://192.168.1.1:1935/live/pedroSG94 RTMPS: rtmps://192.168.1.1:1935/live/pedroSG94
   * @startPreview to resolution seated in @prepareVideo. If you never startPreview this method
   * startPreview for you to resolution seated in @prepareVideo.
   */
  public void startStream(String url) {
    streaming = true;
    startEncoders();
    startStreamRtp(url);
    onPreview = true;
  }

  private void startEncoders() {
    videoEncoder.start();

    onPreview = true;
  }

//  public void requestKeyFrame() {
//    if (videoEncoder.isRunning()) {
//      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//        videoEncoder.requestKeyframe();
//      } else {
//        if (glInterface != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//          glInterface.removeMediaCodecSurface();
//        }
//        videoEncoder.reset();
//        if (glInterface != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//          glInterface.addMediaCodecSurface(videoEncoder.getInputSurface());
//        }
//      }
//    }
//  }

//  private void prepareGlView() {
//    if (glInterface != null && Build.VERSION.SDK_INT >= 18) {
//      glInterface.setFps(videoEncoder.getFps());
//      if (videoEncoder.getRotation() == 90 || videoEncoder.getRotation() == 270) {
//        glInterface.setEncoderSize(videoEncoder.getHeight(), videoEncoder.getWidth());
//      } else {
//        glInterface.setEncoderSize(videoEncoder.getWidth(), videoEncoder.getHeight());
//      }
//      glInterface.setRotation(0);
//      if (!cameraManager.isRunning() && videoEncoder.getWidth() != previewWidth
//          || videoEncoder.getHeight() != previewHeight) {
//        glInterface.start();
//      }
//      if (videoEncoder.getInputSurface() != null) {
//        glInterface.addMediaCodecSurface(videoEncoder.getInputSurface());
//      }
//      cameraManager.setSurfaceTexture(glInterface.getSurfaceTexture());
//    }
//  }

  protected abstract void stopStreamRtp();

  /**
   * Stop stream started with @startStream.
   */
  public void stopStream() {
    if (streaming) {
      streaming = false;
      stopStreamRtp();
    }
      videoEncoder.stop();

  }






  public int getBitrate() {
    return videoEncoder.getBitRate();
  }

  public int getResolutionValue() {
    return videoEncoder.getWidth() * videoEncoder.getHeight();
  }

  public int getStreamWidth() {
    return videoEncoder.getWidth();
  }

  public int getStreamHeight() {
    return videoEncoder.getHeight();
  }




  /**
   * Get stream state.
   *
   * @return true if streaming, false if not streaming.
   */
  public boolean isStreaming() {
    return streaming;
  }

  /**
   * Get preview state.
   *
   * @return true if enabled, false if disabled.
   */
  public boolean isOnPreview() {
    return onPreview;
  }



  protected abstract void onSpsPpsVpsRtp(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps);

  @Override
  public void onSpsPpsVps(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps) {
    onSpsPpsVpsRtp(sps.duplicate(), pps.duplicate(), vps != null ? vps.duplicate() : null);
  }

  protected abstract void getH264DataRtp(ByteBuffer h264Buffer, MediaBufferInfo info);

  @Override
  public void getVideoData(ByteBuffer h264Buffer, MediaBufferInfo info) {
    fpsListener.calculateFps();

    if (streaming) getH264DataRtp(h264Buffer, info);
  }

  @Override
  public void inputYUVData(RtspServerCamera1 rtspServerCamera1,Frame frame) {
    videoEncoder.inputYUVData(rtspServerCamera1,frame);
  }




  public abstract void setLogs(boolean enable);

  public abstract void setCheckServerAlive(boolean enable);
}