package mrapple100.utils

import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.*

class YuvConverter {
    companion object {
        fun createARGBImage2(bytes: ByteArray, width: Int, height: Int): BufferedImage? {
            val buffer = DataBufferByte(bytes, bytes.size)
            val colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB)
            val numBits = intArrayOf(8, 8, 8, 8)
            val colorModel = ComponentColorModel(colorSpace, numBits, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE)
            return BufferedImage(colorModel, Raster.createInterleavedRaster(buffer, width, height, width * 4, 4, intArrayOf(1, 2, 3, 0), null), false, null)
        }
    }
}