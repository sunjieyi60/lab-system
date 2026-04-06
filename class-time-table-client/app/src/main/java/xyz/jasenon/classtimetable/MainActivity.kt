package xyz.jasenon.classtimetable

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ai.face.core.engine.FaceAISDKEngine
import com.ai.face.faceSearch.search.FaceSearchFeatureManger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import xyz.jasenon.classtimetable.config.DeviceProfileConfigActivity
import xyz.jasenon.classtimetable.config.DeviceProfileManager
import xyz.jasenon.classtimetable.config.DeviceProfileObservable
import xyz.jasenon.classtimetable.config.DeviceRuntimeConfigObservable
import xyz.jasenon.classtimetable.network.rsocket.RSocketClientManager
import xyz.jasenon.classtimetable.ui.component.date_time.DateTimeObservable
import xyz.jasenon.classtimetable.ui.LabDashboardScreen
import xyz.jasenon.classtimetable.ui.dialog.DoorOpenUiProviderInitializer
import xyz.jasenon.classtimetable.ui.theme.ClassTimeTableTheme
import java.io.IOException
import androidx.core.content.edit

/**
 * 应用主 Activity
 *
 * 实验室智慧班牌的主入口，负责：
 * - 权限管理（相机权限请求）
 * - 配置检查（首次启动或配置不完整时跳转配置页面）
 * - 人脸识别初始化（默认人脸注册）
 * - 应用组件初始化（设备档案、时间）
 * - 建立 RSocket 长连接（同步课表与天气）
 * - 设置 Compose UI 内容
 *
 * ## 初始化流程
 *
 * ```
 * onCreate()
 *    │
 *    ▼
 * checkAndRequestCameraPermission() ──► 权限拒绝 ──► Toast 提示
 *    │                                          │
 *    │ 权限授予                                  │
 *    ▼                                          │
 * checkDeviceProfile() ◄────────────────────────┘
 *    │
 *    ├──► 配置不完整 ──► 打开 DeviceProfileConfigActivity
 *    │                          │
 *    │                          │ 配置完成返回
 *    │◄─────────────────────────┘
 *    │
 *    ▼
 * initializeAfterPermissions()
 *    │
 *    ├──► registerDefaultFace() (IO 线程)
 *    │
 *    ├──► DoorOpenUiProviderInitializer.initialize()
 *    │
 *    ├──► DeviceProfileManager.loadProfile() + 推送到 Observable
 *    │
 *    ├──► DateTimeObservable.start()
 *    │
 *    ├──► RSocketClientManager.connect() (异步连接服务器)
 *    │
 *    └──► setContent { LabDashboardScreen() }
 * ```
 * <p>
 * <strong>注意：</strong>天气与课表数据通过 RSocket 从服务端实时获取，并推送到 RemoteDataObservable。
 * </p>
 *
 * ## 配置页面入口
 *
 * 用户可以在 LabDashboardScreen 的设置菜单中进入配置页面：
 * ```kotlin
 * val intent = DeviceProfileConfigActivity.createIntent(context, isFirstConfig = false)
 * context.startActivity(intent)
 * ```
 *
 * ## 协程使用
 *
 * - [applicationScope]: 应用级别的 CoroutineScope，用于初始化和后台任务
 *   - SupervisorJob: 子协程失败不影响其他协程
 *   - Dispatchers.Main: 主线程调度
 * - [registerDefaultFace]: 使用 [Dispatchers.IO] 在后台线程执行
 *
 * ## RSocket 初始化
 *
 * RSocket Client 可以通过以下方式初始化：
 * ```kotlin
 * lifecycleScope.launch {
 *     val serverAddress = DeviceProfileObservable.getCurrentServerAddress()
 *     RSocketClientManager.getInstance(applicationContext).connect(
 *         customHost = serverAddress?.host,
 *         customPort = serverAddress?.port
 *     )
 * }
 * ```
 *
 * @see LabDashboardScreen
 * @see DeviceProfileManager
 * @see DeviceProfileConfigActivity
 * @see RSocketClientManager
 * @see DateTimeObservable
 */
class MainActivity : ComponentActivity() {

    /**
     * 应用级别的 CoroutineScope
     *
     * 使用 [SupervisorJob] 确保一个子协程失败不会影响其他协程。
     * 使用 [Dispatchers.Main] 主线程调度，用于 UI 相关的初始化任务。
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * 权限请求 Launcher
     *
     * 使用 [ActivityResultContracts.RequestPermission] 请求相机权限。
     * 用户响应后回调，根据结果决定是否初始化。
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            checkDeviceProfile()
        } else {
            Toast.makeText(this, "相机权限是正常使用人脸识别功能所必需的", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Activity 创建回调
     *
     * 应用入口点，首先检查并请求相机权限。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestCameraPermission()
    }

    /**
     * 检查并请求相机权限
     *
     * 检查当前权限状态：
     * - 已授权：调用 [checkDeviceProfile] 检查设备档案
     * - 未授权：启动权限请求流程
     */
    private fun checkAndRequestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                checkDeviceProfile()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * 检查设备档案配置
     *
     * 检查设备档案是否已完整配置（UUID、laboratoryId、服务器地址）：
     * - 已配置：继续初始化流程
     * - 未配置：打开配置页面
     */
    private fun checkDeviceProfile() {
        val profileManager = DeviceProfileManager.getInstance(this)
        
        if (profileManager.isProfileConfigured()) {
            // 档案已配置，继续初始化
            initializeAfterPermissions()
        } else {
            // 档案未配置，打开配置页面
            Log.d(TAG, "设备档案未配置，打开配置页面")
            val intent = DeviceProfileConfigActivity.createIntent(this, isFirstConfig = true)
            startActivity(intent)
        }
    }

    /**
     * 权限授予且档案配置完成后的初始化
     *
     * 在获得相机权限且档案配置完整后执行所有初始化操作：
     *
     * 1. **注册默认人脸** ([registerDefaultFace])
     *    - 首次启动时从 assets 加载默认人脸图片
     *    - 使用 FaceAISDK 提取特征并注册到本地数据库
     *
     * 2. **初始化开门 UI 提供器** ([DoorOpenUiProviderInitializer.initialize])
     *    - 注册人脸/密码开门 UI 的依赖注入
     *
     * 3. **加载设备档案** ([DeviceProfileManager.loadProfile])
     *    - 加载本地档案并推送到 [DeviceProfileObservable]
     *    - RSocketClientManager 会监听此 Observable 获取服务器地址
     *
     * 4. **建立 RSocket 连接**
     *    - 获取配置的服务器地址并尝试建立长连接，用于同步课表和天气
     *
     * 5. **启动日期时间更新** ([DateTimeObservable.start])
     *    - 定时更新日期时间显示
     *
     * 6. **设置 Compose UI**
     *    - 启用 Edge-to-Edge 显示
     *    - 设置 [LabDashboardScreen] 为根布局
     */
    private fun initializeAfterPermissions() {
        // 在后台线程注册默认人脸
        applicationScope.launch(Dispatchers.IO) {
            registerDefaultFace()
        }

        // 初始化 UI 提供器
        DoorOpenUiProviderInitializer.initialize()

        // 加载设备档案并推送到 Observable
        val profileManager = DeviceProfileManager.getInstance(applicationContext)
        try {
            val profile = profileManager.loadProfile()
            DeviceProfileObservable.updateProfile(profile)
            Log.d(TAG, "设备档案已加载: uuid=${profile.uuid}, laboratoryId=${profile.laboratoryId}")
        } catch (e: Exception) {
            Log.e(TAG, "加载设备档案失败", e)
            Toast.makeText(this, "加载配置失败，请重新配置", Toast.LENGTH_LONG).show()
            profileManager.clearProfile()
            return
        }

        // 启动 RSocket Client 连接
        lifecycleScope.launch {
            val serverAddress = DeviceProfileObservable.getCurrentServerAddress()
            RSocketClientManager.getInstance(applicationContext).connect(
                customHost = serverAddress?.host,
                customPort = serverAddress?.port
            )
        }

        // 启动日期时间更新
        DateTimeObservable.start(applicationScope)

        // 启用 Edge-to-Edge 显示
        enableEdgeToEdge()

        // 设置 Compose UI
        setContent {
            ClassTimeTableTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LabDashboardScreen(
                        onSwitchInterface = {},
                        onExitSystem = { finish() },
                        onOpenSettings = { openSettings() }
                    )
                }
            }
        }
    }

    /**
     * 打开设置页面
     */
    private fun openSettings() {
        val intent = DeviceProfileConfigActivity.createIntent(this, isFirstConfig = false)
        startActivity(intent)
    }

    /**
     * 注册默认人脸
     *
     * 首次启动应用时，将 assets 中的默认人脸图片注册到 FaceAISDK。
     */
    private fun registerDefaultFace() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("isFirstRun", true)

        if (isFirstRun) {
            try {
                assets.open("default_face.jpg").use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val feature = FaceAISDKEngine.getInstance(context = applicationContext)
                        .croppedBitmap2Feature(bitmap)

                    if (feature.isNotEmpty()) {
                        FaceSearchFeatureManger.getInstance(context = applicationContext)
                            .insertFaceFeature(
                                "default_user",
                                feature,
                                System.currentTimeMillis(),
                                "default",
                                "default_group"
                            )
                        prefs.edit { putBoolean("isFirstRun", false) }
                        Log.d(TAG, "默认人脸注册成功")
                    } else {
                        Log.e(TAG, "无法从默认图片中提取人脸特征")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "加载默认人脸图片失败", e)
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

/**
 * LabDashboardScreen 预览
 */
@Preview(showBackground = true, widthDp = 1920, heightDp = 1080)
@Composable
fun LabDashboardPreview() {
    ClassTimeTableTheme {
        LabDashboardScreen()
    }
}
