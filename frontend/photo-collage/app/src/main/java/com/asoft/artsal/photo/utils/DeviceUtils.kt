package com.asoft.artsal.photo.utils

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.media.MediaDrm
import android.media.UnsupportedSchemeException
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.TelephonyManager
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


object DeviceUtils {

    const val NETWORK_CLASS_UNKNOWN = 0

    const val NETWORK_CLASS_2G = "2G"

    const val NETWORK_CLASS_3G = "3G"

    const val NETWORK_CLASS_4G = "4G"

    const val NETWORK_CLASS_WIFI = "wifi"

    const val HEIGHT_FOLD = 2050
    const val WIDTH_FOLD = 1812


    fun getUniqueDeviceId(): String? {
        val uuid = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
        var mediaDrm: MediaDrm? = null
        try {
            mediaDrm = MediaDrm(uuid)
            val widevineId: ByteArray =
                mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            val md: MessageDigest = MessageDigest.getInstance("SHA-256")
            md.update(widevineId)
            return bytesToHex(md.digest())
        } catch (e: UnsupportedSchemeException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mediaDrm?.close()
            } else {
                mediaDrm?.release()
            }
        }
        return null
    }

    private val digits = "0123456789ABCDEF"

    private fun bytesToHex(byteArray: ByteArray): String {
        val hexChars = CharArray(byteArray.size * 2)
        for (i in byteArray.indices) {
            val v = byteArray[i].toInt() and 0xff
            hexChars[i * 2] = digits[v shr 4]
            hexChars[i * 2 + 1] = digits[v and 0xf]
        }
        return String(hexChars)
    }

    fun getLocalIpAddress(): String? {
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf: NetworkInterface = en.nextElement()
                val enumIpAddr: Enumeration<InetAddress> = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress: InetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.getHostAddress()
                    }
                }
            }
        } catch (ex: SocketException) {
            ex.printStackTrace()
        }
        return null
    }

    fun mGetNetworkClass(context: Context): String {
        // ConnectionManager instance
        val mConnectivityManager =
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val mInfo = mConnectivityManager.activeNetworkInfo
        // If not connected, "-" will be displayed
        if (mInfo == null || !mInfo.isConnected) return "-"

        // If Connected to Wifi
        if (mInfo.type == ConnectivityManager.TYPE_WIFI) return "wifi"

        // If Connected to Mobile
        if (mInfo.type == ConnectivityManager.TYPE_MOBILE) {
            return when (mInfo.subtype) {
                TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT,
                TelephonyManager.NETWORK_TYPE_IDEN, TelephonyManager.NETWORK_TYPE_GSM -> "2G"
                TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD,
                TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "3G"
                TelephonyManager.NETWORK_TYPE_LTE, TelephonyManager.NETWORK_TYPE_IWLAN, 19 -> "4G"
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                else -> "?"
            }
        }
        return "?"
    }
}