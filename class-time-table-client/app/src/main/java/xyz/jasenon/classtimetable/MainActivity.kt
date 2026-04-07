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
import com.ai.face.core.engine.FaceAISDKEngine
import com.ai.face.faceSearch.search.FaceSearchFeatureManger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import xyz.jasenon.classtimetable.config.AppConfigManager
import xyz.jasenon.classtimetable.ui.component.date_time.DateTimeObservable
import xyz.jasenon.classtimetable.ui.component.weather_ifno.WeatherPusher
import xyz.jasenon.classtimetable.ui.LabDashboardScreen
import xyz.jasenon.classtimetable.ui.dialog.DoorOpenUiProviderInitializer
import xyz.jasenon.classtimetable.ui.theme.ClassTimeTableTheme
import java.io.IOException

/**
 * 应用主 Activity
 *
 * 实验室智慧班牌的主入口，负责：
 * - 权限管理（相机权限请求）
 * - 人脸识别初始化（默认人脸注册）
 * - 应用组件初始化（配置、天气、时间）
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
 * initializeAfterPermissions() ◄───────────────┘
 *    │
 *    ├──► registerDefaultFace() (IO 线程)
 *    │
 *    ├──► DoorOpenUiProviderInitializer.initialize()
 *    │
 *    ├──► AppConfigManager.loadFallbackAndPush()
 *    │
 *    ├──► WeatherPusher.start()
 *    │
 *    ├──► DateTimeObservable.start()
 *    │
 *    └──► setContent { LabDashboardScreen() }
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
 * RSocket Client 已准备就绪，可以通过以下方式初始化：
 * ```kotlin
 * lifecycleScope.launch {
 *     RSocketClientManager.getInstance(applicationContext).connect()
 * }
 * ```
 *
 * @see LabDashboardScreen
 * @see AppConfigManager
 * @see WeatherPusher
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
            initializeAfterPermissions()
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
     * - 已授权：直接调用 [initializeAfterPermissions]
     * - 未授权：启动权限请求流程
     */
    private fun checkAndRequestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                initializeAfterPermissions()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * 权限授予后的初始化
     *
     * 在获得相机权限后执行所有初始化操作：
     *
     * 1. **注册默认人脸** ([registerDefaultFace])
     *    - 首次启动时从 assets 加载默认人脸图片
     *    - 使用 FaceAISDK 提取特征并注册到本地数据库
     *
     * 2. **初始化开门 UI 提供器** ([DoorOpenUiProviderInitializer.initialize])
     *    - 注册人脸/密码开门 UI 的依赖注入
     *
     * 3. **加载应用配置** ([AppConfigManager.loadFallbackAndPush])
     *    - 从 SharedPreferences 或 assets 加载兜底配置
     *    - 推送到 [ConfigObservable] 供其他组件使用
     *
     * 4. **启动天气推送** ([WeatherPusher.start])
     *    - 推送 Mock 天气数据到 [RemoteDataObservable]
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

        // 加载应用配置
        val configManager = AppConfigManager(applicationContext)
        configManager.loadFallbackAndPush()

        // 启动天气推送（Mock 数据）
        WeatherPusher.start(applicationScope)

        // 启动日期时间更新
        DateTimeObservable.start(applicationScope)

        // 注意：RSocket Client 连接可以在这里初始化
        // lifecycleScope.launch {
        //     RSocketClientManager.getInstance(applicationContext).connect()
        // }

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
                        onExitSystem = { finish() }
                    )
                }
            }
        }
    }

    /**
     * 注册默认人脸
     *
     * 首次启动应用时，将 assets 中的默认人脸图片注册到 FaceAISDK。
     * 用于演示和测试人脸识别功能。
     *
     * ## 执行流程
     *
     * 1. 检查 SharedPreferences 中的 "isFirstRun" 标记
     * 2. 如果是首次运行：
     *    - 从 assets 加载 default_face.jpg
     *    - 使用 [FaceAISDKEngine] 提取人脸特征
     *    - 使用 [FaceSearchFeatureManger] 插入到本地数据库
     *    - 标记 "isFirstRun" 为 false
     *
     * ## 协程上下文
     * - 在 [Dispatchers.IO] 上执行，避免阻塞主线程
     * - 涉及文件读取和 AI 计算，耗时操作
     *
     * @throws IOException assets 文件读取失败时抛出
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

                    if (!feature.isNullOrEmpty()) {
                        FaceSearchFeatureManger.getInstance(context = applicationContext)
                            .insertFaceFeature(
                                "default_user",
                                feature,
                                System.currentTimeMillis(),
                                "default",
                                "default_group"
                            )
                        prefs.edit().putBoolean("isFirstRun", false).apply()
                        Log.d("MainActivity", "默认人脸注册成功")
                    } else {
                        Log.e("MainActivity", "无法从默认图片中提取人脸特征")
                    }
                }
            } catch (e: IOException) {
                Log.e("MainActivity", "加载默认人脸图片失败", e)
            }
        }
    }
}

/**
 * LabDashboardScreen 预览
 *
 * 在 Android Studio 的设计视图中预览主界面。
 * 尺寸：1920x1080（典型的 1080p 横屏显示器）
 */
@Preview(showBackground = true, widthDp = 1920, heightDp = 1080)
@Composable
fun LabDashboardPreview() {
    ClassTimeTableTheme {
        LabDashboardScreen()
    }
}
