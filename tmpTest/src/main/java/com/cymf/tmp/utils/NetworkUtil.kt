@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.cymf.tmp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.util.Patterns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.URL
import kotlin.coroutines.CoroutineContext

object NetworkUtil {

    // ========================================
    // 获取 IP 地址相关
    // ========================================

    /**
     * 方法1：遍历网络接口获取所有 IPv4 地址
     */
    fun getAllIPAddresses(): List<String> {
        val ipList = mutableListOf<String>()
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                if (!networkInterface.isUp) continue

                val inetAddresses = networkInterface.inetAddresses
                while (inetAddresses.hasMoreElements()) {
                    val inetAddress = inetAddresses.nextElement()
                    if (inetAddress.hostAddress.contains(".")) {
                        ipList.add(inetAddress.hostAddress)
                    }
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        return ipList
    }

    /**
     * 方法2：使用 ConnectivityManager 获取当前活动网络的所有 IP 地址
     */
    fun getCurrentIPAddress(context: Context): List<String> {
        val ipList = mutableListOf<String>()
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return ipList

        val linkProperties = connectivityManager.getLinkProperties(network)
        linkProperties?.linkAddresses?.forEach { linkAddress ->
            if (linkAddress.address is Inet4Address) {
                ipList.add(linkAddress.address.hostAddress)
            }
        }

        return ipList
    }

    /**
     * 方法3：执行 shell 命令获取本机 IP（ip route）
     */
    fun getIPAddressByCommand(): String? {
        var reader: BufferedReader? = null
        try {
            val process = Runtime.getRuntime().exec("ip route")
            reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("src") == true) {
                    val parts = line.split(" ")
                    return parts[parts.size - 2]
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            reader?.close()
        }
        return null
    }

    /**
     * 方法4：通过 ifconfig 命令获取本机 IP 地址
     */
    fun getIPAddressByIfconfig(): String? {
        var reader: BufferedReader? = null
        try {
            val process = Runtime.getRuntime().exec("ifconfig")
            reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("inet ") == true && !line.contains("127.0.0.1")) {
                    val parts = line.split(" ")
                    val inetIndex = parts.indexOf("inet")
                    if (inetIndex + 1 < parts.size) {
                        return parts[inetIndex + 1]
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            reader?.close()
        }
        return null
    }

    /**
     * 方法5：读取 ARP 表格中的 IP 地址（与 MAC 对应）
     */
    fun readArpTable(): Map<String, String> {
        val map = mutableMapOf<String, String>()

        try {
            val process = Runtime.getRuntime().exec("cat proc/net/arp")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                if (!line!!.contains("00:00:00:00:00:00") && !line.contains("IP")) {
                    val split = line.split("\\s+".toRegex())
                    if (split.size >= 4) {
                        val mac = split[3]
                        val ip = split[0]
                        map[mac] = ip
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return map
    }

    // ========================================
    // 获取 DNS 和网关信息
    // ========================================

    /**
     * 获取当前网络使用的 DNS 服务器地址列表
     */
    fun getDnsServers(context: Context): List<String> {
        val dnsList = mutableListOf<String>()
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network: Network? = connectivityManager.activeNetwork
        val linkProperties: LinkProperties? = connectivityManager.getLinkProperties(network)
        linkProperties?.dnsServers?.forEach {
            dnsList.add(it.hostAddress ?: it.toString())
        }

        return dnsList
    }
    fun getDnsServersByGetProp(): List<String> {
        val dnsList = mutableListOf<String>()
        var reader: BufferedReader? = null
        try {
            // 执行 getprop | grep dns 命令
            val process = Runtime.getRuntime().exec("getprop")
            reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("dns") == true && line.contains(": ")) {
                    val parts = line.split(": ")
                    if (parts.size >= 2) {
                        var value = parts[1] .trim()
                        // 如果是 IPv6 地址，暂时忽略（可选）
                        if (Patterns.IP_ADDRESS.matcher(value).matches()) {
                            dnsList.add(value)
                        }
                    }
                }
            }

            // 去重 + 排序（可选）
            return dnsList.distinct()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            reader?.close()
        }
        return emptyList()
    }
    /**
     * 获取默认网关地址
     */
    fun getDefaultGateway(context: Context): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = cm.activeNetwork ?: run {
            return "No active network"
        }

        val linkProperties = cm.getLinkProperties(network) ?: run {
            return "Link properties is null"
        }

        // 尝试通过 isDefaultRoute 获取
        var gateway = linkProperties.routes.find { it.isDefaultRoute }?.gateway?.hostAddress
        if (!gateway.isNullOrEmpty()) return gateway

        // 回退：查找 destination 是 0.0.0.0/0 或 ::/0 的路由
        gateway = linkProperties.routes.find {
            it.destination.address.isAnyLocalAddress == true
        }?.gateway?.hostAddress

        if (!gateway.isNullOrEmpty()) return gateway

        // 再次尝试遍历所有路由并排除 IPv6 路由
        for (route in linkProperties.routes) {
            if (route.gateway != null && route.gateway?.isIPv6Address == false) {
                return route.gateway?.hostAddress
            }
        }
        return "Failed to find default gateway"
    }

    private val InetAddress.isIPv6Address: Boolean
        get() = this is Inet6Address && !this.isIPv4CompatibleAddress

    /**
     * 获取子网掩码
     */
    fun getSubnetMask(): String? {
        try {
            val enu = NetworkInterface.getNetworkInterfaces()
            while (enu.hasMoreElements()) {
                val intf = enu.nextElement()
                for (interfaceAddress in intf.interfaceAddresses) {
                    val mask = interfaceAddress.networkPrefixLength
                    if (mask > 0) {
                        val subnetMaskInt = -1 shl (32 - mask)
                        val subnetMask = intToIp(subnetMaskInt)
                        return subnetMask
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    private fun intToIp(ipInt: Int): String {
        return ((ipInt shr 24) and 0xFF).toString() + "." +
                ((ipInt shr 16) and 0xFF) + "." +
                ((ipInt shr 8) and 0xFF) + "." +
                (ipInt and 0xFF)
    }



    fun getDefaultGatewayByIpRoute(): String? {
        var reader: BufferedReader? = null
        try {
            val process = Runtime.getRuntime().exec("ip route")
            reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line?.startsWith("default") == true) {
                    val parts = line.split(" ")
                    return parts[2] // 默认网关地址
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            reader?.close()
        }
        return null
    }

    fun getDefaultGatewayByRoute(): String? {
        var reader: BufferedReader? = null
        try {
            val process = Runtime.getRuntime().exec("route -n")
            reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line?.startsWith("0.0.0.0") == true) {
                    val parts = line.split(" ")
                    return parts.lastOrNull { it.isNotEmpty() } // 默认网关地址
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            reader?.close()
        }
        return null
    }

    fun getDnsServersFromResolvConf(): List<String> {
        val dnsList = mutableListOf<String>()
        try {
            val file = File("/etc/resolv.conf")
            if (file.exists()) {
                val lines = file.readLines()
                for (line in lines) {
                    if (line.startsWith("nameserver")) {
                        val parts = line.split(" ")
                        if (parts.size > 1) {
                            dnsList.add(parts[1])
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return dnsList
    }
    // ========================================
    // 网络请求工具函数
    // ========================================

    /**
     * 挂起函数：执行GET请求并返回结果
     */
    suspend fun httpRequestGet(
        requestUrl: String,
        coroutineContext: CoroutineContext = Dispatchers.IO
    ): String? = withContext(coroutineContext) {
        var connection: HttpURLConnection? = null
        var reader: BufferedReader? = null

        try {
            val url = URL(requestUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }
            reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            return@withContext response.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        } finally {
            reader?.close()
            connection?.disconnect()
        }
    }
}