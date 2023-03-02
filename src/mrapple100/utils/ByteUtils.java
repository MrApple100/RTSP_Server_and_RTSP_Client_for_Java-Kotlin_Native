package mrapple100.utils;


import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.ShortPointer;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.jetbrains.annotations.NotNull;

import static org.bytedeco.opencv.global.opencv_core.*;

public class ByteUtils {

    // int memcmp ( const void * ptr1, const void * ptr2, size_t num );
    public static boolean memcmp(
            @NotNull byte[] source1,
            int offsetSource1,
            @NotNull byte[] source2,
            int offsetSource2,
            int num) {
        if (source1.length - offsetSource1 < num)
            return false;
        if (source2.length - offsetSource2 < num)
            return false;

        for (int i = 0; i < num; i++) {
            if (source1[offsetSource1 + i] != source2[offsetSource2 + i])
                return false;
        }
        return true;
    }

    public static byte[] copy(@NotNull byte[] src) {
        byte[] dest = new byte[src.length];
        System.arraycopy(src, 0, dest, 0, src.length);
        return dest;
    }

    public static byte[] iplImageToByteArray(IplImage image) {
        int width = image.width();
        int height = image.height();
        int numChannels = image.nChannels();
        int depth = image.depth();
        BytePointer imageData = image.imageData();
        int imageStep = image.widthStep();
        byte[] bytes = new byte[width * height * numChannels];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int c = 0; c < numChannels; c++) {
                    int value;
                    if (depth == IPL_DEPTH_8U) {
                        value = imageData.get(y * imageStep + x * numChannels + c) & 0xFF;
                    } else if (depth == IPL_DEPTH_16U) {
                        ShortPointer imageDataShort = new ShortPointer(imageData);
                        value = imageDataShort.get(y * (imageStep / 2) + x * numChannels + c) & 0xFFFF;
                    } else if (depth == IPL_DEPTH_32F) {
                        FloatPointer imageDataFloat = new FloatPointer(imageData);
                        value = (int) imageDataFloat.get(y * (imageStep / 4) + x * numChannels + c);
                    } else if (depth == IPL_DEPTH_64F) {
                        DoublePointer imageDataDouble = new DoublePointer(imageData);
                        value = (int) imageDataDouble.get(y * (imageStep / 8) + x * numChannels + c);
                    } else {
                        throw new IllegalArgumentException("Unsupported IplImage depth: " + depth);
                    }
                    bytes[(y * width + x) * numChannels + c] = (byte) value;
                }
            }
        }
        return bytes;
    }
}
