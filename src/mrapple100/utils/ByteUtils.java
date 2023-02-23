package mrapple100.utils;


import org.jetbrains.annotations.NotNull;

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
}
