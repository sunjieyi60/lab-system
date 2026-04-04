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
import xyz.jasenon.classtimetable.network.TioClientManager
import xyz.jasenon.classtimetable.ui.LabDashboardScreen
import xyz.jasenon.classtimetable.ui.dialog.DoorOpenUiProviderInitializer
import xyz.jasenon.classtimetable.ui.theme.ClassTimeTableTheme
import java.io.IOException

class MainActivity : ComponentActivity() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            initializeAfterPermissions()
        } else {
            Toast.makeText(this, "相机权限是正常使用人脸识别功能所必需的", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestCameraPermission()
    }

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

    private fun initializeAfterPermissions() {
        applicationScope.launch(Dispatchers.IO) {
            registerDefaultFace()
        }

        DoorOpenUiProviderInitializer.initialize()
        val configManager = AppConfigManager(applicationContext)
        configManager.loadFallbackAndPush()
        WeatherPusher.start(applicationContext, applicationScope)
        DateTimeObservable.start(applicationScope)
        TioClientManager.getInstance(applicationContext)

        enableEdgeToEdge()
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

    private fun registerDefaultFace() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("isFirstRun", true)

        if (isFirstRun) {
            try {
                assets.open("default_face.jpg").use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val feature = FaceAISDKEngine.getInstance(context = applicationContext).croppedBitmap2Feature(bitmap)

                    if (!feature.isNullOrEmpty()) {
                        FaceSearchFeatureManger.getInstance(context = applicationContext).insertFaceFeature(
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

@Preview(showBackground = true, widthDp = 1920, heightDp = 1080)
@Composable
fun LabDashboardPreview() {
    ClassTimeTableTheme {
        LabDashboardScreen()
    }
}
