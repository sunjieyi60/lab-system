package xyz.jasenon.classtimetable.network.register

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import xyz.jasenon.classtimetable.config.AesConfigHelper
import xyz.jasenon.classtimetable.config.AppConfig
import xyz.jasenon.classtimetable.util.ServerPublicKeyLoader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom
import java.util.Collections
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 * 构建 REGISTER 包 payload：设备信息 AES 加密，AES 密钥 RSA 加密，与后端 RegisterRequestDTO 一致
 * 构建前会生成 AES 密钥并写入 [AesConfigHelper.negotiatedKey]，供 REGISTER_ACK 解密使用
 */
object RegisterPayloadBuilder {

    private const val TAG = "RegisterPayloadBuilder"
    private const val AES_KEY_SIZE = 16
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"

    private val gson = Gson()
    private val secureRandom = SecureRandom()

    /**
     * 构建 REGISTER 包 body（JSON 字符串的 UTF-8 字节），并设置 [AesConfigHelper.negotiatedKey]
     * @return REGISTER 包 payload，失败返回 null
     */
    fun buildPayload(context: Context, config: AppConfig): ByteArray? {
        val publicKey = ServerPublicKeyLoader.getPublicKey(context)
            ?: run {
                Log.e(TAG, "Server public key not found in assets (server_public_key.pem or server_public_key_base64.txt)")
                return null
            }

        val deviceId = config.deviceId?.takeIf { it.isNotBlank() } ?: ""
        val deviceName = if (deviceId.isBlank()) UUID.randomUUID().toString() else (deviceId)
        val ip = getLocalIpAddress() ?: "0.0.0.0"
        val mac = getMacAddress() ?: "00:00:00:00:00:00"

        val deviceInfo = DeviceInfoDTO(
            deviceId = deviceId.ifBlank { null },
            deviceName = deviceName,
            ipAddress = ip,
            macAddress = mac
//            version = Build.VERSION.RELEASE,
//            deviceType = "android"
        )
        val deviceInfoJson = gson.toJson(deviceInfo)

        val aesKeyBytes = ByteArray(AES_KEY_SIZE).apply { secureRandom.nextBytes(this) }
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")
        val iv = ByteArray(GCM_IV_LENGTH).apply { secureRandom.nextBytes(this) }

        val encryptedDeviceInfo = encryptAesGcm(deviceInfoJson.toByteArray(Charsets.UTF_8), aesKey, iv)
            ?: run {
                Log.e(TAG, "AES encrypt device info failed")
                return null
            }
        val encryptedAesKey = encryptRsa(aesKeyBytes, publicKey)
            ?: run {
                Log.e(TAG, "RSA encrypt AES key failed")
                return null
            }

        AesConfigHelper.negotiatedKey = aesKeyBytes

        val request = RegisterRequestDTO(
            encryptedDeviceInfo = Base64.encodeToString(encryptedDeviceInfo, Base64.NO_WRAP),
            encryptedAesKey = Base64.encodeToString(encryptedAesKey, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            timestamp = System.currentTimeMillis(),
            nonce = UUID.randomUUID().toString()
        )
        val requestJson = gson.toJson(request)
        return requestJson.toByteArray(Charsets.UTF_8)
    }

    private fun encryptAesGcm(plain: ByteArray, key: SecretKeySpec, iv: ByteArray): ByteArray? {
        return try {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            cipher.doFinal(plain)
        } catch (e: Exception) {
            Log.e(TAG, "AES-GCM encrypt failed", e)
            null
        }
    }

    private fun encryptRsa(plain: ByteArray, publicKey: java.security.PublicKey): ByteArray? {
        return try {
            val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            cipher.doFinal(plain)
        } catch (e: Exception) {
            Log.e(TAG, "RSA encrypt failed", e)
            null
        }
    }

    private fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()?.flatMap { ni ->
                ni.inetAddresses.toList().filter { !it.isLoopbackAddress && it is Inet4Address }
                    .map { it.hostAddress ?: "" }
            }?.firstOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "Get local IP failed", e)
            null
        }
    }

    fun getMacAddress(): String {
        try {
            val all: ArrayList<NetworkInterface> =
                Collections.list<NetworkInterface>(NetworkInterface.getNetworkInterfaces())
            for (nif in all) {
                if (!nif.getName().equals("wlan0", ignoreCase = true)) continue
                val macBytes = nif.getHardwareAddress()
                if (macBytes == null) {
                    return ""
                }
                val res1 = StringBuilder()
                for (b in macBytes) {
                    res1.append(String.format("%02X:", b))
                }
                if (res1.length > 0) {
                    res1.deleteCharAt(res1.length - 1)
                }
                return res1.toString()
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
        return "未获取到设备Mac地址"
    }
}
