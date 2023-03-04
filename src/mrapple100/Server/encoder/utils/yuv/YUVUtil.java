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

package mrapple100.Server.encoder.utils.yuv;

import mrapple100.Server.encoder.video.FormatVideoEncoder;

/**
 * Created by pedro on 25/01/17.
 * https://wiki.videolan.org/YUV/#I420
 * <p>
 * Example YUV images 4x4 px.
 * <p>
 * NV21 example:
 * <p>
 * Y1   Y2   Y3   Y4
 * Y5   Y6   Y7   Y8
 * Y9   Y10  Y11  Y12
 * Y13  Y14  Y15  Y16
 * U1   V1   U2   V2
 * U3   V3   U4   V4
 * <p>
 * <p>
 * YV12 example:
 * <p>
 * Y1   Y2   Y3   Y4
 * Y5   Y6   Y7   Y8
 * Y9   Y10  Y11  Y12
 * Y13  Y14  Y15  Y16
 * U1   U2   U3   U4
 * V1   V2   V3   V4
 * <p>
 * <p>
 * YUV420 planar example (I420):
 * <p>
 * Y1   Y2   Y3   Y4
 * Y5   Y6   Y7   Y8
 * Y9   Y10  Y11  Y12
 * Y13  Y14  Y15  Y16
 * V1   V2   V3   V4
 * U1   U2   U3   U4
 * <p>
 * <p>
 * YUV420 semi planar example (NV12):
 * <p>
 * Y1   Y2   Y3   Y4
 * Y5   Y6   Y7   Y8
 * Y9   Y10  Y11  Y12
 * Y13  Y14  Y15  Y16
 * V1   U1   V2   U2
 * V3   U3   V4   U4
 */

public class YUVUtil {


    public static void preAllocateBuffers(int length) {
        NV21Utils.preAllocateBuffers(length);
        YV12Utils.preAllocateBuffers(length);
    }

    public static byte[] NV21toYUV420byColor(byte[] input, int width, int height,
                                             FormatVideoEncoder formatVideoEncoder) {
        switch (formatVideoEncoder) {
            case YUV420PLANAR:
                return NV21Utils.toI420(input, width, height);
            case YUV420SEMIPLANAR:
                return NV21Utils.toNV12(input, width, height);
            default:
                return null;
        }
    }

    public static byte[] rotateNV21(byte[] data, int width, int height, int rotation) {
        switch (rotation) {
            case 0:
                return data;
            case 90:
                return NV21Utils.rotate90(data, width, height);
            case 180:
                return NV21Utils.rotate180(data, width, height);
            case 270:
                return NV21Utils.rotate270(data, width, height);
            default:
                return null;
        }
    }

    public static byte[] YV12toYUV420byColor(byte[] input, int width, int height,
                                             FormatVideoEncoder formatVideoEncoder) {
        switch (formatVideoEncoder) {
            case YUV420PLANAR:
                return YV12Utils.toI420(input, width, height);
            case YUV420SEMIPLANAR:
                return YV12Utils.toNV12(input, width, height);
            default:
                return null;
        }
    }

    public static byte[] rotateYV12(byte[] data, int width, int height, int rotation) {
        switch (rotation) {
            case 0:
                return data;
            case 90:
                return YV12Utils.rotate90(data, width, height);
            case 180:
                return YV12Utils.rotate180(data, width, height);
            case 270:
                return YV12Utils.rotate270(data, width, height);
            default:
                return null;
        }
    }

//  public static Bitmap frameToBitmap(Frame frame, int width, int height, int orientation) {
//    int w = (orientation == 90 || orientation == 270) ? height : width;
//    int h = (orientation == 90 || orientation == 270) ? width : height;
//    int[] argb = NV21Utils.toARGB(rotateNV21(frame.getBuffer(), width, height, orientation), w, h);
//    return Bitmap.createBitmap(argb, w, h, Bitmap.Config.ARGB_8888);
//  }

    public static byte[] ARGBtoYUV420SemiPlanar(int[] input, int width, int height) {
        /*
         * COLOR_FormatYUV420SemiPlanar is NV12
         */
        final int frameSize = width * height;
        byte[] yuv420sp = new byte[width * height * 3 / 2];
        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (input[index] & 0xff000000) >> 24; // a is not used obviously
                R = (input[index] & 0xff0000) >> 16;
                G = (input[index] & 0xff00) >> 8;
                B = (input[index] & 0xff) >> 0;

                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                }

                index++;
            }
        }
        return yuv420sp;
    }

    public static byte[] convertRgbToYuv420(byte[] rgb, int width, int height) {
        byte[] yuv = new byte[width * height * 3 / 2];
        int r, g, b, y, u, v;
        int index = 0;
        int yIndex = 0;
        int uvIndex = width * height;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
// Get the RGB values of the current pixel
                r = rgb[index];
                g = rgb[index + 1];
                b = rgb[index + 2];

// Calculate YUV values
                y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;

// Clamp YUV values to [0, 255]
                y = y < 16 ? 16 : (y > 255 ? 255 : y);
                u = u < 0 ? 0 : (u > 255 ? 255 : u);
                v = v < 0 ? 0 : (v > 255 ? 255 : v);

// Set Y value of current pixel
                yuv[yIndex++] = (byte) y;

// Set U and V values of current pixel
                if (j % 2 == 0 && i % 2 == 0) {
                    yuv[uvIndex++] = (byte) u;
                    yuv[uvIndex++] = (byte) v;
                }

                index += 3;
            }
        }
        return yuv;
    }
}
