package xyz.jasenon.classtimetable.config

import android.util.Log
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * REGISTER_ACK payload 的 AES-GCM 解密
 *
 * 协商 AES 密钥后（REGISTER 时由 [xyz.jasenon.classtimetable.network.register.RegisterPayloadBuilder] 写入 [negotiatedKey]），
 * 服务器下发的配置为 IV(12) + AES-GCM(ciphertext)；解密后反序列化并合并到 [AppConfig]。
 */
object AesConfigHelper {

    private const val TAG = "AesConfigHelper"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_LENGTH = 16
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    /** REGISTER 时协商得到的 AES 密钥（16 字节），由 RegisterPayloadBuilder 写入 */
    @Volatile
    var negotiatedKey: ByteArray? = null
        set(value) {
            field = value?.take(KEY_LENGTH)?.toByteArray()
        }

    /**
     * 使用协商密钥解密 payload，得到 UTF-8 JSON 字符串
     * 约定：payload = IV(12 字节) + AES-GCM(ciphertext)
     *
     * @param encryptedPayload REGISTER_ACK 的 payload（IV + AES-GCM 密文）
     * @return 解密后的 JSON 字符串，失败返回 null
     */
    fun decryptToJson(encryptedPayload: ByteArray): String? {
        if (encryptedPayload.size <= GCM_IV_LENGTH) return null
        val key = negotiatedKey ?: run {
            Log.w(TAG, "negotiatedKey not set, REGISTER may not have been sent yet")
            return null
        }
        return try {
            val iv = encryptedPayload.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = encryptedPayload.copyOfRange(GCM_IV_LENGTH, encryptedPayload.size)
            val keySpec = SecretKeySpec(key, "AES")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "AES-GCM decrypt failed", e)
            null
        }
    }
}
