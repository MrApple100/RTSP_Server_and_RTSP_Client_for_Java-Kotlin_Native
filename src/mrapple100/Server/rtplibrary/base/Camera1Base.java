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

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.pedro.encoder.Frame;
import com.pedro.encoder.audio.AudioEncoder;
import com.pedro.encoder.audio.GetAacData;
import com.pedro.encoder.input.audio.CustomAudioEffect;
import com.pedro.encoder.input.audio.GetMicrophoneData;
import com.pedro.encoder.input.audio.MicrophoneManager;
import com.pedro.encoder.input.audio.MicrophoneManagerManual;
import com.pedro.encoder.input.audio.MicrophoneMode;
import com.pedro.encoder.input.video.Camera1ApiManager;
import com.pedro.encoder.input.video.CameraCallbacks;
import com.pedro.encoder.input.video.CameraHelper;
import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.encoder.input.video.GetCameraData;
import com.pedro.encoder.utils.CodecUtil;
import com.pedro.encoder.video.FormatVideoEncoder;
import com.pedro.encoder.video.GetVideoData;
import com.pedro.encoder.video.VideoEncoder;
import com.pedro.rtplibrary.base.Camera2Base;
import com.pedro.rtplibrary.base.recording.BaseRecordController;
import com.pedro.rtplibrary.base.recording.RecordController;
import com.pedro.rtplibrary.util.AndroidMuxerRecordController;
import com.pedro.rtplibrary.util.FpsListener;
import com.pedro.rtplibrary.view.GlInterface;
import com.pedro.rtplibrary.view.LightOpenGlView;
import com.pedro.rtplibrary.view.OffScreenGlThread;
import com.pedro.rtplibrary.view.OpenGlView;
import mrapple100.Server.MediaBufferInfo;
import mrapple100.Server.encoder.Frame;
import mrapple100.Server.encoder.input.video.Camera1ApiManager;
import mrapple100.Server.encoder.input.video.CameraCallbacks;
import mrapple100.Server.encoder.input.video.CameraHelper;
import mrapple100.Server.encoder.input.video.GetCameraData;
import mrapple100.Server.encoder.utils.CodecUtil;
import mrapple100.Server.encoder.video.FormatVideoEncoder;
import mrapple100.Server.encoder.video.GetVideoData;
import mrapple100.Server.encoder.video.VideoEncoder;
import mrapple100.Server.rtplibrary.util.FpsListener;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

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

  private final GetCameraData getCameraData;
  protected VideoEncoder videoEncoder;
  private boolean streaming = false;
  protected boolean audioInitialized = false;
  private boolean onPreview = false;
  private int previewWidth, previewHeight;
  private final FpsListener fpsListener = new FpsListener();

  public Camera1Base() {
    cameraManager = new Camera1ApiManager(this);
    init();
  }




  private void init() {
    videoEncoder = new VideoEncoder(this);
  }



  public void setCameraCallbacks(CameraCallbacks callbacks) {
    cameraManager.setCameraCallbacks(callbacks);
  }


  /**
   * @param callback get fps while record or stream
   */
  public void setFpsListener(FpsListener.Callback callback) {
    fpsListener.setCallback(callback);
  }

  /**
   * @return true if success, false if fail (not supported or called before start camera)
   */
  public boolean enableFaceDetection(Camera1ApiManager.FaceDetectorCallback faceDetectorCallback) {
    return cameraManager.enableFaceDetection(faceDetectorCallback);
  }

  public void disableFaceDetection() {
    cameraManager.disableFaceDetection();
  }

  public boolean isFaceDetectionEnabled() {
    return cameraManager.isFaceDetectionEnabled();
  }

  /**
   * @return true if success, false if fail (not supported or called before start camera)
   */
  public boolean enableVideoStabilization() {
    return cameraManager.enableVideoStabilization();
  }

  public void disableVideoStabilization() {
    cameraManager.disableVideoStabilization();
  }

  public boolean isVideoStabilizationEnabled() {
    return cameraManager.isVideoStabilizationEnabled();
  }

  /**
   * Use getCameraFacing instead
   */
  @Deprecated
  public boolean isFrontCamera() {
    return cameraManager.getCameraFacing() == CameraHelper.Facing.FRONT;
  }

  public CameraHelper.Facing getCameraFacing() {
    return cameraManager.getCameraFacing();
  }

  public void enableLantern() throws Exception {
    cameraManager.enableLantern();
  }

  public void disableLantern() {
    cameraManager.disableLantern();
  }

  public boolean isLanternEnabled() {
    return cameraManager.isLanternEnabled();
  }

  public void enableAutoFocus() {
    cameraManager.enableAutoFocus();
  }

  public void disableAutoFocus() {
    cameraManager.disableAutoFocus();
  }

  public boolean isAutoFocusEnabled() {
    return cameraManager.isAutoFocusEnabled();
  }

  /**
   * Basic auth developed to work with Wowza. No tested with other server
   *
   * @param user auth.
   * @param password auth.
   */
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
      stopPreview();
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
    return prepareVideo(1920, 1080, 30, 1024*8000, 0);
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
  @RequiresApi(api = Build.VERSION_CODES.O)
  public void startRecord(@NonNull final FileDescriptor fd,
      @Nullable RecordController.Listener listener) throws IOException {
    recordController.startRecord(fd, listener);
    if (!streaming) {
      startEncoders();
    } else if (videoEncoder.isRunning()) {
      requestKeyFrame();
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  public void startRecord(@NonNull final FileDescriptor fd) throws IOException {
    startRecord(fd, null);
  }


  /**
   * Stop record MP4 video started with @startRecord. If you don't call it file will be unreadable.
   */
  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  public void stopRecord() {
    recordController.stopRecord();
    if (!streaming) stopStream();
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  public void replaceView(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      replaceGlInterface(new OffScreenGlThread(context));
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  public void replaceView(OpenGlView openGlView) {
    replaceGlInterface(openGlView);
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  public void replaceView(LightOpenGlView lightOpenGlView) {
    replaceGlInterface(lightOpenGlView);
  }

  /**
   * Replace glInterface used on fly. Ignored if you use SurfaceView or TextureView
   */
  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  private void replaceGlInterface(GlInterface glInterface) {
    if (this.glInterface != null && Build.VERSION.SDK_INT >= 18) {
      if (isStreaming() || isRecording() || isOnPreview()) {
        Point size = this.glInterface.getEncoderSize();
        cameraManager.stop();
        this.glInterface.removeMediaCodecSurface();
        this.glInterface.stop();
        this.glInterface = glInterface;
        this.glInterface.init();
        this.glInterface.setEncoderSize(size.x, size.y);
        this.glInterface.setRotation(0);
        this.glInterface.start();
        if (isStreaming() || isRecording()) {
          this.glInterface.addMediaCodecSurface(videoEncoder.getInputSurface());
        }
        cameraManager.setSurfaceTexture(glInterface.getSurfaceTexture());
        cameraManager.setRotation(videoEncoder.getRotation());
        cameraManager.start(videoEncoder.getWidth(), videoEncoder.getHeight(),
            videoEncoder.getFps());
      } else {
        this.glInterface = glInterface;
        this.glInterface.init();
      }
    }
  }

  /**
   * Start camera preview. Ignored, if stream or preview is started.
   *
   * @param cameraFacing front or back camera. Like: {@link com.pedro.encoder.input.video.CameraHelper.Facing#BACK}
   * {@link com.pedro.encoder.input.video.CameraHelper.Facing#FRONT}
   * @param width of preview in px.
   * @param height of preview in px.
   * @param rotation camera rotation (0, 90, 180, 270). Recommended: {@link
   * com.pedro.encoder.input.video.CameraHelper#getCameraOrientation(Context)}
   */
  public void startPreview(CameraHelper.Facing cameraFacing, int width, int height, int fps, int rotation) {
    if (!isStreaming() && !onPreview && !(glInterface instanceof OffScreenGlThread)) {
      previewWidth = width;
      previewHeight = height;
      videoEncoder.setFps(fps);
      videoEncoder.setRotation(rotation);
      if (glInterface != null && Build.VERSION.SDK_INT >= 18) {
        if (videoEncoder.getRotation() == 90 || videoEncoder.getRotation() == 270) {
          glInterface.setEncoderSize(height, width);
        } else {
          glInterface.setEncoderSize(width, height);
        }
        glInterface.setRotation(0);
        glInterface.setFps(fps);
        glInterface.start();
        cameraManager.setSurfaceTexture(glInterface.getSurfaceTexture());
      }
      cameraManager.setRotation(rotation);
      cameraManager.start(cameraFacing, width, height, videoEncoder.getFps());
      onPreview = true;
    } else if (!isStreaming() && !onPreview && glInterface instanceof OffScreenGlThread) {
      // if you are using background mode startPreview only work to indicate
      // that you want start with front or back camera
      cameraManager.setCameraFacing(cameraFacing);
    } else {
      Log.e(TAG, "Streaming or preview started, ignored");
    }
  }

  /**
   * Start camera preview. Ignored, if stream or preview is started.
   *
   * @param cameraId camera id.
   * {@link com.pedro.encoder.input.video.CameraHelper.Facing#FRONT}
   * @param width of preview in px.
   * @param height of preview in px.
   * @param rotation camera rotation (0, 90, 180, 270). Recommended: {@link
   * com.pedro.encoder.input.video.CameraHelper#getCameraOrientation(Context)}
   */
  public void startPreview(int cameraId, int width, int height, int fps, int rotation) {
    if (!isStreaming() && !onPreview && !(glInterface instanceof OffScreenGlThread)) {
      previewWidth = width;
      previewHeight = height;
      videoEncoder.setFps(fps);
      videoEncoder.setRotation(rotation);
      if (glInterface != null && Build.VERSION.SDK_INT >= 18) {
        if (videoEncoder.getRotation() == 90 || videoEncoder.getRotation() == 270) {
          glInterface.setEncoderSize(height, width);
        } else {
          glInterface.setEncoderSize(width, height);
        }
        glInterface.setRotation(0);
        glInterface.setFps(fps);
        glInterface.start();
        cameraManager.setSurfaceTexture(glInterface.getSurfaceTexture());
      }
      cameraManager.setRotation(rotation);
      cameraManager.start(cameraId, width, height, videoEncoder.getFps());
      onPreview = true;
    } else if (!isStreaming() && !onPreview && glInterface instanceof OffScreenGlThread) {
      // if you are using background mode startPreview only work to indicate
      // that you want start with front or back camera
      cameraManager.setCameraSelect(cameraId);
    } else {
      Log.e(TAG, "Streaming or preview started, ignored");
    }
  }

  public void startPreview(CameraHelper.Facing cameraFacing, int width, int height, int rotation) {
    startPreview(cameraFacing, width, height, videoEncoder.getFps(), rotation);
  }

  public void startPreview(int cameraFacing, int width, int height, int rotation) {
    startPreview(cameraFacing, width, height, videoEncoder.getFps(), rotation);
  }

  public void startPreview(CameraHelper.Facing cameraFacing, int width, int height) {
    startPreview(cameraFacing, width, height, CameraHelper.getCameraOrientation(context));
  }

  public void startPreview(int cameraFacing, int width, int height) {
    startPreview(cameraFacing, width, height, CameraHelper.getCameraOrientation(context));
  }

  public void startPreview(CameraHelper.Facing cameraFacing, int rotation) {
    startPreview(cameraFacing, videoEncoder.getWidth(), videoEncoder.getHeight(), rotation);
  }

  public void startPreview(CameraHelper.Facing cameraFacing) {
    startPreview(cameraFacing, videoEncoder.getWidth(), videoEncoder.getHeight());
  }

  public void startPreview(int cameraFacing) {
    startPreview(cameraFacing, videoEncoder.getWidth(), videoEncoder.getHeight());
  }

  public void startPreview(int width, int height) {
    startPreview(getCameraFacing(), width, height);
  }

  public void startPreview() {
    startPreview(getCameraFacing());
  }

  /**
   * Stop camera preview. Ignored if streaming or already stopped. You need call it after
   *
   * @stopStream to release camera properly if you will close activity.
   */
  public void stopPreview() {
    if (!isStreaming()
        && !isRecording()
        && onPreview
        && !(glInterface instanceof OffScreenGlThread)) {
      if (glInterface != null && Build.VERSION.SDK_INT >= 18) {
        glInterface.stop();
      }
      cameraManager.stop();
      onPreview = false;
      previewWidth = 0;
      previewHeight = 0;
    } else {
      Log.e(TAG, "Streaming or preview stopped, ignored");
    }
  }

  /**
   * Change preview orientation can be called while stream.
   *
   * @param orientation of the camera preview. Could be 90, 180, 270 or 0.
   */
  public void setPreviewOrientation(int orientation) {
    cameraManager.setPreviewOrientation(orientation);
  }

  /**
   * Set zoomIn or zoomOut to camera.
   *
   * @param event motion event. Expected to get event.getPointerCount() > 1
   */
  public void setZoom(MotionEvent event) {
    cameraManager.setZoom(event);
  }

  /**
   * Set zoomIn or zoomOut to camera.
   * Use this method if you use a zoom slider.
   *
   * @param level Expected to be >= 1 and <= max zoom level
   * @see Camera2Base#getZoom()
   */
  public void setZoom(int level) {
    cameraManager.setZoom(level);
  }

  /**
   * Return current zoom level
   *
   * @return current zoom level
   */
  public float getZoom() {
    return cameraManager.getZoom();
  }

  /**
   * Return max zoom level
   *
   * @return max zoom level range
   */
  public int getMaxZoom() {
    return cameraManager.getMaxZoom();
  }

  /**
   * Return min zoom level
   *
   * @return min zoom level range
   */
  public int getMinZoom() {
    return cameraManager.getMinZoom();
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  public void startStreamAndRecord(String url, String path, RecordController.Listener listener) throws IOException {
    startStream(url);
    recordController.startRecord(path, listener);
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  public void startStreamAndRecord(String url, String path) throws IOException {
    startStreamAndRecord(url, path, null);
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
    if (!recordController.isRunning()) {
      startEncoders();
    } else {
      requestKeyFrame();
    }
    startStreamRtp(url);
    onPreview = true;
  }

  private void startEncoders() {
    videoEncoder.start();
    if (audioInitialized) audioEncoder.start();
    prepareGlView();
    if (audioInitialized) microphoneManager.start();
    cameraManager.setRotation(videoEncoder.getRotation());
    if (!cameraManager.isRunning() && videoEncoder.getWidth() != previewWidth
        || videoEncoder.getHeight() != previewHeight) {
      cameraManager.start(videoEncoder.getWidth(), videoEncoder.getHeight(), videoEncoder.getFps());
    }
    onPreview = true;
  }

  public void requestKeyFrame() {
    if (videoEncoder.isRunning()) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        videoEncoder.requestKeyframe();
      } else {
        if (glInterface != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
          glInterface.removeMediaCodecSurface();
        }
        videoEncoder.reset();
        if (glInterface != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
          glInterface.addMediaCodecSurface(videoEncoder.getInputSurface());
        }
      }
    }
  }

  private void prepareGlView() {
    if (glInterface != null && Build.VERSION.SDK_INT >= 18) {
      glInterface.setFps(videoEncoder.getFps());
      if (videoEncoder.getRotation() == 90 || videoEncoder.getRotation() == 270) {
        glInterface.setEncoderSize(videoEncoder.getHeight(), videoEncoder.getWidth());
      } else {
        glInterface.setEncoderSize(videoEncoder.getWidth(), videoEncoder.getHeight());
      }
      glInterface.setRotation(0);
      if (!cameraManager.isRunning() && videoEncoder.getWidth() != previewWidth
          || videoEncoder.getHeight() != previewHeight) {
        glInterface.start();
      }
      if (videoEncoder.getInputSurface() != null) {
        glInterface.addMediaCodecSurface(videoEncoder.getInputSurface());
      }
      cameraManager.setSurfaceTexture(glInterface.getSurfaceTexture());
    }
  }

  protected abstract void stopStreamRtp();

  /**
   * Stop stream started with @startStream.
   */
  public void stopStream() {
    if (streaming) {
      streaming = false;
      stopStreamRtp();
    }
    if (!recordController.isRecording()) {
      if (audioInitialized) microphoneManager.stop();
      if (glInterface != null && Build.VERSION.SDK_INT >= 18) {
        glInterface.removeMediaCodecSurface();
        if (glInterface instanceof OffScreenGlThread) {
          glInterface.stop();
          cameraManager.stop();
          onPreview = false;
        }
      }
      videoEncoder.stop();
      if (audioInitialized) audioEncoder.stop();
      recordController.resetFormats();
    }
  }

  /**
   * Retries to connect with the given delay. You can pass an optional backupUrl
   * if you'd like to connect to your backup server instead of the original one.
   * Given backupUrl replaces the original one.
   */
  public boolean reTry(long delay, String reason, @Nullable String backupUrl) {
    boolean result = shouldRetry(reason);
    if (result) {
      requestKeyFrame();
      reConnect(delay, backupUrl);
    }
    return result;
  }

  public boolean reTry(long delay, String reason) {
    return reTry(delay, reason, null);
  }

  protected abstract boolean shouldRetry(String reason);

  public abstract void setReTries(int reTries);

  protected abstract void reConnect(long delay, @Nullable String backupUrl);

  //cache control
  public abstract boolean hasCongestion();

  public abstract void resizeCache(int newSize) throws RuntimeException;


  /**
   * Get supported preview resolutions of back camera in px.
   *
   * @return list of preview resolutions supported by back camera
   */
  public List<Camera.Size> getResolutionsBack() {
    return cameraManager.getPreviewSizeBack();
  }

  /**
   * Get supported preview resolutions of front camera in px.
   *
   * @return list of preview resolutions supported by front camera
   */
  public List<Camera.Size> getResolutionsFront() {
    return cameraManager.getPreviewSizeFront();
  }

  public List<int[]> getSupportedFps() {
    return cameraManager.getSupportedFps();
  }


  /**
   * Get mute state of microphone.
   *
   * @return true if muted, false if enabled
   */
  public boolean isAudioMuted() {
    return microphoneManager.isMuted();
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
   * Set video bitrate of H264 in bits per second while stream.
   *
   * @param bitrate H264 in bits per second.
   */
  @RequiresApi(api = Build.VERSION_CODES.KITKAT)
  public void setVideoBitrateOnFly(int bitrate) {
    videoEncoder.setVideoBitrateOnFly(bitrate);
  }

  /**
   * Set limit FPS while stream. This will be override when you call to prepareVideo method. This
   * could produce a change in iFrameInterval.
   *
   * @param fps frames per second
   */
  public void setLimitFPSOnFly(int fps) {
    videoEncoder.setFps(fps);
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
  public void inputYUVData(Frame frame) {
    videoEncoder.inputYUVData(frame);
  }




  public abstract void setLogs(boolean enable);

  public abstract void setCheckServerAlive(boolean enable);
}