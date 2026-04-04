package xyz.jasenon.classtimetable.util

import android.content.Context
import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

/**
 * 从 assets 加载服务端 RSA 公钥，用于加密 REGISTER 包中的 AES 密钥
 * 约定：assets/server_public_key.pem 或 server_public_key_base64.txt
 */
object ServerPublicKeyLoader {

    private const val TAG = "ServerPublicKeyLoader"
    private const val PEM_FILE = "server_public_key.pem"
    private const val BASE64_FILE = "server_public_key_base64.txt"

    @Volatile
    private var cachedPublicKey: PublicKey? = null

    /**
     * 获取服务端公钥；优先从 PEM 文件读取，其次从 Base64 文本读取
     */
    fun getPublicKey(context: Context): PublicKey? {
        if (cachedPublicKey != null) return cachedPublicKey
        synchronized(this) {
            if (cachedPublicKey != null) return cachedPublicKey
            cachedPublicKey = loadFromPem(context) ?: loadFromBase64File(context)
            return cachedPublicKey
        }
    }

    private fun loadFromPem(context: Context): PublicKey? {
        return try {
            context.assets.open(PEM_FILE).use { input ->
                val content = input.bufferedReader().readText()
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replace(Regex("\\s"), "")
                val keyBytes = Base64.decode(content, Base64.DEFAULT)
                val spec = X509EncodedKeySpec(keyBytes)
                KeyFactory.getInstance("RSA").generatePublic(spec)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Load server public key from PEM failed: ${e.message}")
            null
        }
    }

    private fun loadFromBase64File(context: Context): PublicKey? {
        return try {
            context.assets.open(BASE64_FILE).use { input ->
                val base64 = input.bufferedReader().readText().trim().replace(Regex("\\s"), "")
                val keyBytes = Base64.decode(base64, Base64.DEFAULT)
                val spec = X509EncodedKeySpec(keyBytes)
                KeyFactory.getInstance("RSA").generatePublic(spec)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Load server public key from Base64 file failed: ${e.message}")
            null
        }
    }

    fun clearCache() {
        cachedPublicKey = null
    }
}
