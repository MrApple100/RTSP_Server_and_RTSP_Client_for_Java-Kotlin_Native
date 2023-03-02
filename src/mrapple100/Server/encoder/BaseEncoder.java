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

package mrapple100.Server.encoder;


import mrapple100.Server.MediaBufferInfo;
import mrapple100.Server.encoder.utils.CodecUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by pedro on 18/09/19.
 */
public abstract class BaseEncoder implements EncoderCallback {

  protected String TAG = "BaseEncoder";
  private final MediaBufferInfo bufferInfo = new MediaBufferInfo();
  protected BlockingQueue<Frame> queue = new ArrayBlockingQueue<>(1);
  protected BlockingQueue<ByteBuffer> queuebb = new ArrayBlockingQueue<>(1);

  protected static long presentTimeUs;
  protected volatile boolean running = false;
  protected boolean isBufferMode = true;
  protected CodecUtil.Force force = CodecUtil.Force.FIRST_COMPATIBLE_FOUND;
  private long oldTimeStamp = 0L;
  protected boolean shouldReset = true;

  public void restart() {
    start(false);
    initCodec();
  }

  public void start() {
    if (presentTimeUs == 0) {
      presentTimeUs = System.nanoTime() / 1000;
    }
    start(true);
    initCodec();
  }


  private void initCodec() {
      new Thread(new Runnable() {
        @Override
        public void run() {
          while (running) {
            try {
              getDataFromEncoder();
            } catch (IllegalStateException | InterruptedException e) {
              //Log.i(TAG, "Encoding error", e);
              reloadCodec();
            }
          }
        }
      }).start();

    running = true;
  }

  public abstract void reset();

  public abstract void start(boolean resetTs);

  protected abstract void stopImp();

  protected void fixTimeStamp(MediaBufferInfo info) {
    if (oldTimeStamp > info.presentationTimeUs) {
      info.presentationTimeUs = oldTimeStamp;
    } else {
      oldTimeStamp = info.presentationTimeUs;
    }
  }

  private void reloadCodec() {
    //Sometimes encoder crash, we will try recover it. Reset encoder a time if crash
    if (shouldReset) {
    //  //Log.e(TAG, "Encoder crashed, trying to recover it");
      reset();
    }
  }

  public void stop() {
    stop(true);
  }

  public void stop(boolean resetTs) {
    if (resetTs) {
      presentTimeUs = 0;
    }
    running = false;
    stopImp();

    queue.clear();
    queue = new ArrayBlockingQueue<>(1);

    oldTimeStamp = 0L;
  }


  //here
  protected void getDataFromEncoder() throws IllegalStateException, InterruptedException {

        inputAvailable();



        outputAvailable(bufferInfo);

  }

  protected abstract Frame getInputFrame() throws InterruptedException;

  protected abstract long calculatePts(Frame frame, long presentTimeUs);

  private void processInput() throws IllegalStateException {
    try {
      Frame frame = getInputFrame();
      while (frame == null) frame = getInputFrame();
//      byteBuffer.clear();
//      int size = Math.max(0, Math.min(frame.getSize(), byteBuffer.remaining()) - frame.getOffset());
//      byteBuffer.put(frame.getBuffer(), frame.getOffset(), size);
//      long pts = calculatePts(frame, presentTimeUs);
      queuebb.offer( ByteBuffer.wrap(frame.getBuffer()));
//      mediaCodec.queueInputBuffer(inBufferIndex, 0, size, pts, 0);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (NullPointerException | IndexOutOfBoundsException e) {
      //Log.i(TAG, "Encoding error", e);
    }
  }

  protected abstract void checkBuffer(@NotNull ByteBuffer byteBuffer,
      @NotNull MediaBufferInfo bufferInfo);

  protected abstract void sendBuffer(@NotNull ByteBuffer byteBuffer,
      @NotNull MediaBufferInfo bufferInfo);

  private void processOutput(@NotNull ByteBuffer byteBuffer,@NotNull MediaBufferInfo bufferInfo) throws IllegalStateException {
    checkBuffer(byteBuffer, bufferInfo);
    sendBuffer(byteBuffer, bufferInfo);
  }

  public void setForce(CodecUtil.Force force) {
    this.force = force;
  }

  public boolean isRunning() {
    return running;
  }

  @Override
  public void inputAvailable()
      throws IllegalStateException {


    processInput();
  }

  @Override
  public void outputAvailable(@NotNull MediaBufferInfo bufferInfo) throws IllegalStateException, InterruptedException {
    ByteBuffer byteBuffer;
      byteBuffer = queuebb.take();
      //need convert byteBuffer(yuv420) to h264

    processOutput(byteBuffer, bufferInfo);
  }

}
