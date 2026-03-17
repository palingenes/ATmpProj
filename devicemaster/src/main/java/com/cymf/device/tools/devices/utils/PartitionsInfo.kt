package com.cymf.device.tools.devices.utils

import android.text.TextUtils

import com.cymf.device.tools.devices.beans.PartitionsBean

/**
 * 分区信息
 */
object PartitionsInfo {
    val partitionsInfo: MutableList<PartitionsBean?>
        get() {
            val lines =
                CommandUtils.exec("mount")
            val df =
                parseDf()
            val list: MutableList<PartitionsBean?> =
                ArrayList<PartitionsBean?>()
            for (line in lines) {
                val args =
                    line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (args.size < 4) {
                    continue
                }
                val mount: String? = args[0]
                val path: String? = args[2]
                val type: String? = args[4]
                val rws: String? = args[5]
                var rw = ""
                if (!TextUtils.isEmpty(rws)) {
                    if (rws!!.startsWith("(rw")) {
                        rw = "read-write"
                    } else if (rws.startsWith("(ro")) {
                        rw = "read-only"
                    } else {
                        rw = "UNKNOWN"
                    }
                }
                val bean = PartitionsBean()
                bean.setPath(path)
                bean.setMount(mount)
                bean.setFs(type)
                bean.setMod(rw)
                if (df.containsKey(mount)) {
                    val partitionsBean = df.get(mount)
                    bean.setRatio(partitionsBean!!.getRatio())
                    bean.setUsed(partitionsBean.getUsed())
                    bean.setSize(partitionsBean.getSize())
                }
                list.add(bean)
            }
            return list
        }

    /**
     * 解析 df 数据
     *
     */
    private fun parseDf(): MutableMap<String?, PartitionsBean?> {
        val dfs = CommandUtils.exec("df -h")
        val map: MutableMap<String?, PartitionsBean?> = HashMap<String?, PartitionsBean?>()
        for (line in dfs) {
            if (line.startsWith("Filesystem")) {
                continue
            }
            val args = line.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (args.size < 5) {
                continue
            }
            val path: String? = args[0]
            val size: String? = args[1]
            val used: String? = args[2]
            val ratio = args[4]
            val mount: String? = args[5]
            val bean = PartitionsBean()
            bean.setPath(path)
            bean.setMount(mount)
            bean.setSize(size)
            bean.setUsed(used)
            var i = 0
            try {
                i = ratio.replace("%", "").toInt()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bean.setRatio(i)
            map.put(path, bean)
        }
        return map
    }
}
