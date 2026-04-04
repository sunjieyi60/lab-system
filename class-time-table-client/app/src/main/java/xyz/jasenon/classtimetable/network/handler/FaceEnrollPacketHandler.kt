package xyz.jasenon.classtimetable.network.handler

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.ai.face.core.engine.FaceAISDKEngine
import com.ai.face.faceSearch.search.FaceSearchFeatureManger
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import xyz.jasenon.classtimetable.network.TioClientManager
import xyz.jasenon.classtimetable.protocol.CheckSumCalculator
import xyz.jasenon.classtimetable.protocol.CommandType
import xyz.jasenon.classtimetable.protocol.PacketFlags
import xyz.jasenon.classtimetable.protocol.QosLevel
import xyz.jasenon.classtimetable.protocol.SeqIdGenerator
import xyz.jasenon.classtimetable.protocol.SmartBoardPacket

/**
 * FACE_SEND 包处理器（Server 发出 -> 班牌本地特征库插入一条新特征）
 * 接收 payload = JSON(requestId, faceId, tag 人脸名称, group, imageBase64)，
 * 转 Bitmap -> faceAILib 提取特征 -> 录入本地（tag 即人脸名称），
 * 录入成功或失败后均回复 FACE_SEND_ACK 给 Server，Web 一次请求内得知插入结果。
 */
class FaceEnrollPacketHandler(private val context: Context) : PacketHandler {

    private val gson = Gson()

    override fun handle(packet: SmartBoardPacket): Boolean {
        if (packet.cmdType != CommandType.FACE_SEND) return false
        val payload = packet.payload ?: return true
        if (payload.isEmpty()) return true

        val json = String(payload, Charsets.UTF_8)
        val request = gson.fromJson(json, FaceEnrollPayload::class.java)
            ?: run {
                sendResult(requestId = null, faceId = null, success = false, errorMessage = "payload 解析失败")
                return true
            }

        val requestId = request.requestId
        val faceId = request.faceId?.takeIf { it.isNotBlank() }
            ?: run {
                sendResult(requestId, null, false, "faceId 为空")
                return true
            }

        val imageBase64 = request.imageBase64?.replace(Regex("\\s"), "") ?: ""
        if (imageBase64.isEmpty()) {
            sendResult(requestId, faceId, false, "imageBase64 为空")
            return true
        }

        val imageBytes = try {
            Base64.decode(imageBase64, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "FACE_SEND Base64 解码失败", e)
            sendResult(requestId, faceId, false, "Base64 解码失败")
            return true
        }

        if (imageBytes.isEmpty()) {
            sendResult(requestId, faceId, false, "图片数据为空")
            return true
        }

        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: run {
                sendResult(requestId, faceId, false, "Bitmap 解码失败")
                return true
            }

        val appContext = context.applicationContext
        val feature = try {
            FaceAISDKEngine.getInstance(context = appContext).croppedBitmap2Feature(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "FACE_SEND 提取人脸特征失败", e)
            sendResult(requestId, faceId, false, "提取特征失败: ${e.message}")
            return true
        }

        if (feature.isNullOrEmpty()) {
            sendResult(requestId, faceId, false, "无法从图片中提取人脸特征")
            return true
        }

        val tagName = request.tag ?: ""
        try {
            FaceSearchFeatureManger.getInstance(context = appContext).insertFaceFeature(
                faceId,
                feature,
                System.currentTimeMillis(),
                tagName,
                request.group ?: "default_group"
            )
            Log.d(TAG, "FACE_SEND 录入成功, faceId=$faceId, tag=$tagName")
            sendResult(requestId, faceId, true, null)
        } catch (e: Exception) {
            Log.e(TAG, "FACE_SEND insertFaceFeature 失败, faceId=$faceId", e)
            sendResult(requestId, faceId, false, "录入失败: ${e.message}")
        }

        return true
    }

    private fun sendResult(requestId: String?, faceId: String?, success: Boolean, errorMessage: String?) {
        val result = FaceEnrollResultPayload(
            requestId = requestId,
            success = success,
            faceId = faceId,
            errorMessage = errorMessage
        )
        val body = gson.toJson(result).toByteArray(Charsets.UTF_8)
        val resultPacket = SmartBoardPacket()
        resultPacket.setCmdType(CommandType.FACE_SEND_ACK)
        resultPacket.setVersion(0x01.toByte())
        resultPacket.setSeqId(SeqIdGenerator().nextSeqId())
        resultPacket.setQos(QosLevel.AT_LEAST_ONCE.value)
        resultPacket.setFlags(PacketFlags.NONE)
        resultPacket.setReserved(0.toByte())
        resultPacket.setPayload(body)
        resultPacket.setLength(body.size)
        try {
            TioClientManager.getInstance(context).sendPacket(resultPacket)
        } catch (e: Exception) {
            Log.e(TAG, "回复 FACE_SEND_ACK 失败", e)
        }
    }

    /**
     * 与后端 FaceEnrollRequestDTO 一致的 payload 结构（tag 即人脸名称）
     */
    private data class FaceEnrollPayload(
        @SerializedName("requestId") val requestId: String? = null,
        @SerializedName("faceId") val faceId: String? = null,
        @SerializedName("tag") val tag: String? = null,
        @SerializedName("group") val group: String? = null,
        @SerializedName("imageBase64") val imageBase64: String? = null
    )

    private data class FaceEnrollResultPayload(
        @SerializedName("requestId") val requestId: String? = null,
        @SerializedName("success") val success: Boolean = false,
        @SerializedName("faceId") val faceId: String? = null,
        @SerializedName("errorMessage") val errorMessage: String? = null
    )

    companion object {
        private const val TAG = "FaceEnrollPacketHandler"
    }
}
