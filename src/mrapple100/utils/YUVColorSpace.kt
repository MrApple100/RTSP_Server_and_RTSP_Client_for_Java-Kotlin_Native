package mrapple100.utils

import java.awt.color.ColorSpace

class YUVColorSpace : ColorSpace(YUV, 3) {
    override fun fromCIEXYZ(colorvalue: FloatArray?): FloatArray? {
        return null
    }

    override fun fromRGB(rgbvalue: FloatArray): FloatArray {
        val y = 0.299f * rgbvalue[0] + 0.587f * rgbvalue[1] + 0.114f * rgbvalue[2]
        val u = -0.14713f * rgbvalue[0] - 0.28886f * rgbvalue[1] + 0.436f * rgbvalue[2]
        val v = 0.615f * rgbvalue[0] - 0.51498f * rgbvalue[1] - 0.10001f * rgbvalue[2]
        return floatArrayOf(y, u, v)
    }

    override fun toCIEXYZ(colorvalue: FloatArray?): FloatArray? {
        return null
    }

    override fun toRGB(yuvvalue: FloatArray): FloatArray {
        val r = yuvvalue[0] + 1.13983f * yuvvalue[2]
        val g = yuvvalue[0] - 0.39465f * yuvvalue[1] - 0.58060f * yuvvalue[2]
        val b = yuvvalue[0] + 2.03211f * yuvvalue[1]
        return floatArrayOf(r, g, b)
    }

    private object Holder {
        val INSTANCE = YUVColorSpace()
    }

    companion object {
        private const val serialVersionUID = 1L
        const val YUV = 1
        val instance: YUVColorSpace
            get() = Holder.INSTANCE
    }
}