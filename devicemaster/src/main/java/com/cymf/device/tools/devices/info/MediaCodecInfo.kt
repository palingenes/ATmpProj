package com.cymf.device.tools.devices.info

import android.media.MediaCodecList
import androidx.core.util.Pair

/**
 * 编解码器
 */
object MediaCodecInfo {
    val codeCInfo: ArrayList<Pair<String?, String?>?>
        get() {
            val list =
                ArrayList<Pair<String?, String?>?>()
            val codecCount = MediaCodecList.getCodecCount()
            for (i in 0..<codecCount) {
                val codecInfo = MediaCodecList.getCodecInfoAt(i)
                if (codecInfo != null) {
                    val types = codecInfo.getSupportedTypes()
                    if (types != null) {
                        for (type in types) {
                            list.add(
                                androidx.core.util.Pair<String?, String?>(
                                    codecInfo.getName(),
                                    type
                                )
                            )
                        }
                    }
                }
            }
            return list
        }
}
