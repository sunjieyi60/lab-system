package xyz.jasenon.classtimetable.config

import android.content.Context
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileNotFoundException

/**
 * 设备档案管理器
 * <p>
 * 管理班牌设备的本地档案配置，包括：
 * <ul>
 *   <li>设备 UUID（首次启动生成，终身不变）</li>
 *   <li>关联实验室ID</li>
 *   <li>服务器地址</li>
 * </ul>
 * </p>
 * <p>
 * <strong>设计原则：</strong>
 * <ul>
 *   <li>取消默认兜底配置，配置不存在宁可失败</li>
 *   <li>首次启动时自动生成 UUID，但其他配置必须用户手动设置</li>
 *   <li>UUID 一旦生成就不可修改</li>
 *   <li>运行时配置（Config）由服务端下发，不保存在本地</li>
 * </ul>
 * </p>
 *
 * @author Jasenon_ce
 * @see DeviceProfile
 * @since 1.0.0
 */
class DeviceProfileManager private constructor(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * 档案文件存储路径
     */
    private val profileFile: File by lazy {
        File(context.filesDir, PROFILE_FILE_NAME)
    }

    /**
     * 加载设备档案
     * <p>
     * 加载流程：
     * <ol>
     *   <li>检查档案文件是否存在</li>
     *   <li>如果存在，读取并解析</li>
     *   <li>如果不存在，生成默认档案（仅含 UUID）并保存</li>
     * </ol>
     * </p>
     *
     * @return 设备档案
     * @throws ProfileLoadException 当档案文件损坏或读取失败时抛出
     */
    @Throws(ProfileLoadException::class)
    fun loadProfile(): DeviceProfile {
        return try {
            if (!profileFile.exists()) {
                XLog.tag(TAG).i("档案文件不存在，创建默认档案")
                createDefaultProfile()
            } else {
                val json = profileFile.readText()
                val profile = gson.fromJson(json, DeviceProfile::class.java)
                    ?: throw ProfileLoadException("档案文件解析结果为 null")
                
                // 验证 UUID 有效性
                if (profile.uuid.isBlank()) {
                    throw ProfileLoadException("档案中 UUID 为空")
                }
                
                XLog.tag(TAG).i("档案加载成功: uuid=${profile.uuid}, laboratoryId=${profile.laboratoryId}")
                profile
            }
        } catch (e: FileNotFoundException) {
            XLog.tag(TAG).e("档案文件不存在", e)
            createDefaultProfile()
        } catch (e: Exception) {
            XLog.tag(TAG).e("档案加载失败", e)
            throw ProfileLoadException("档案加载失败: ${e.message}", e)
        }
    }

    /**
     * 检查档案是否已存在
     */
    fun profileExists(): Boolean {
        return profileFile.exists()
    }

    /**
     * 检查档案是否已完整配置
     * <p>需要满足：laboratoryId 不为空，服务器地址有效</p>
     *
     * @return 如果已完整配置返回 true，否则返回 false
     */
    fun isProfileConfigured(): Boolean {
        return try {
            val profile = loadProfile()
            profile.isConfigured()
        } catch (e: Exception) {
            XLog.tag(TAG).e("检查档案配置状态失败", e)
            false
        }
    }

    /**
     * 保存设备档案
     * <p>
     * <strong>注意：</strong>UUID 一旦设置不可修改。如果已存在档案，
     * 新档案的 UUID 必须与现有档案一致。
     * </p>
     *
     * @param profile 要保存的档案
     * @throws ProfileSaveException 当保存失败或 UUID 不一致时抛出
     */
    @Throws(ProfileSaveException::class)
    fun saveProfile(profile: DeviceProfile) {
        try {
            // 如果已存在档案，检查 UUID 一致性
            if (profileFile.exists()) {
                val existingProfile = loadProfile()
                if (existingProfile.uuid != profile.uuid) {
                    throw ProfileSaveException(
                        "UUID 不可修改: 现有=${existingProfile.uuid}, 新=${profile.uuid}"
                    )
                }
            }

            // 验证档案有效性
            if (profile.uuid.isBlank()) {
                throw ProfileSaveException("UUID 不能为空")
            }

            // 保存到文件
            val json = gson.toJson(profile)
            profileFile.writeText(json)
            
            XLog.tag(TAG).i("档案保存成功: uuid=${profile.uuid}")
            
            // 通知观察者
            DeviceProfileObservable.updateProfile(profile)
        } catch (e: ProfileSaveException) {
            throw e
        } catch (e: Exception) {
            XLog.tag(TAG).e("档案保存失败", e)
            throw ProfileSaveException("档案保存失败: ${e.message}", e)
        }
    }

    /**
     * 更新实验室ID
     * <p>这是允许修改的字段之一</p>
     *
     * @param laboratoryId 新的实验室ID
     * @throws ProfileSaveException 当更新失败时抛出
     */
    @Throws(ProfileSaveException::class)
    fun updateLaboratoryId(laboratoryId: Long) {
        val profile = loadProfile()
        profile.laboratoryId = laboratoryId
        saveProfile(profile)
        XLog.tag(TAG).i("实验室ID更新成功: $laboratoryId")
    }

    /**
     * 更新服务器地址
     * <p>这是允许修改的字段之一</p>
     *
     * @param serverAddress 新的服务器地址
     * @throws ProfileSaveException 当更新失败时抛出
     */
    @Throws(ProfileSaveException::class)
    fun updateServerAddress(serverAddress: ServerAddress) {
        val profile = loadProfile()
        profile.serverAddress = serverAddress
        saveProfile(profile)
        XLog.tag(TAG).i("服务器地址更新成功: ${serverAddress.toAddressString()}")
    }

    /**
     * 获取当前设备 UUID
     * <p>如果档案不存在，会创建默认档案并返回 UUID</p>
     *
     * @return 设备 UUID
     */
    fun getUuid(): String {
        return try {
            loadProfile().uuid
        } catch (e: Exception) {
            XLog.tag(TAG).e("获取 UUID 失败，创建默认档案", e)
            createDefaultProfile().uuid
        }
    }

    /**
     * 获取服务器地址
     *
     * @return 服务器地址配置
     * @throws ProfileLoadException 当档案不存在或读取失败时抛出
     */
    @Throws(ProfileLoadException::class)
    fun getServerAddress(): ServerAddress {
        return loadProfile().serverAddress
    }

    /**
     * 创建默认档案
     * <p>生成新的 UUID 并保存到文件</p>
     *
     * @return 创建的默认档案
     */
    private fun createDefaultProfile(): DeviceProfile {
        val profile = DeviceProfile.createDefault()
        try {
            val json = gson.toJson(profile)
            profileFile.writeText(json)
            XLog.tag(TAG).i("默认档案创建成功: uuid=${profile.uuid}")
            DeviceProfileObservable.updateProfile(profile)
        } catch (e: Exception) {
            XLog.tag(TAG).e("默认档案创建失败", e)
            throw ProfileSaveException("默认档案创建失败: ${e.message}", e)
        }
        return profile
    }

    /**
     * 删除档案（谨慎使用）
     * <p>主要用于重置设备或测试场景</p>
     */
    fun clearProfile() {
        if (profileFile.exists()) {
            profileFile.delete()
            XLog.tag(TAG).w("档案已删除")
        }
        DeviceProfileObservable.clear()
    }

    companion object {
        private const val TAG = "DeviceProfileManager"
        private const val PROFILE_FILE_NAME = "device_profile.json"

        @Volatile
        private var INSTANCE: DeviceProfileManager? = null

        @JvmStatic
        fun getInstance(context: Context): DeviceProfileManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DeviceProfileManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        @JvmStatic
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}

/**
 * 档案加载异常
 */
class ProfileLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 档案保存异常
 */
class ProfileSaveException(message: String, cause: Throwable? = null) : Exception(message, cause)
