package xyz.jasenon.classtimetable.config

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.elvishew.xlog.XLog
import kotlinx.coroutines.launch
import xyz.jasenon.classtimetable.ui.theme.ClassTimeTableTheme

/**
 * 设备档案配置页面
 * <p>
 * 用于配置班牌设备的基本信息，包括：
 * <ul>
 *   <li>设备 UUID（仅显示，不可修改）</li>
 *   <li>关联实验室ID（必填）</li>
 *   <li>服务器地址（必填）</li>
 * </ul>
 * </p>
 * <p>
 * 首次启动或配置不完整时，会自动跳转到此页面。
 * 用户完成配置后才能进入主界面。
 * </p>
 *
 * @author Jasenon_ce
 * @see DeviceProfile
 * @since 1.0.0
 */
class DeviceProfileConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val profileManager = DeviceProfileManager.getInstance(this)
        val isFirstConfig = !profileManager.isProfileConfigured()
        
        XLog.tag(TAG).i("打开配置页面, 首次配置=$isFirstConfig")
        
        enableEdgeToEdge()
        setContent {
            ClassTimeTableTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DeviceProfileConfigScreen(
                        profileManager = profileManager,
                        isFirstConfig = isFirstConfig,
                        onConfigComplete = { finish() },
                        onExitApp = { finishAffinity() }
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "DeviceProfileConfig"

        /**
         * 创建启动 Intent
         *
         * @param context 上下文
         * @param isFirstConfig 是否首次配置（决定是否显示退出按钮）
         */
        fun createIntent(context: Context, isFirstConfig: Boolean = false): Intent {
            return Intent(context, DeviceProfileConfigActivity::class.java).apply {
                putExtra("is_first_config", isFirstConfig)
            }
        }

        /**
         * 检查是否需要显示配置页面
         *
         * @param context 上下文
         * @return 如果档案未配置完整返回 true
         */
        fun needShowConfig(context: Context): Boolean {
            return !DeviceProfileManager.getInstance(context).isProfileConfigured()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceProfileConfigScreen(
    profileManager: DeviceProfileManager,
    isFirstConfig: Boolean,
    onConfigComplete: () -> Unit,
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 加载当前档案
    var profile by remember { mutableStateOf<DeviceProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // 表单状态
    var laboratoryIdText by remember { mutableStateOf("") }
    var serverHost by remember { mutableStateOf("") }
    var serverPortText by remember { mutableStateOf("9000") }
    var serverTimeoutText by remember { mutableStateOf("5000") }
    
    // 错误提示
    var laboratoryIdError by remember { mutableStateOf<String?>(null) }
    var serverHostError by remember { mutableStateOf<String?>(null) }
    var serverPortError by remember { mutableStateOf<String?>(null) }
    
    // 加载档案
    LaunchedEffect(Unit) {
        try {
            val loadedProfile = profileManager.loadProfile()
            profile = loadedProfile
            laboratoryIdText = loadedProfile.laboratoryId?.toString() ?: ""
            serverHost = loadedProfile.serverAddress.host
            serverPortText = loadedProfile.serverAddress.port.toString()
            serverTimeoutText = loadedProfile.serverAddress.timeoutMs.toString()
        } catch (e: Exception) {
            XLog.tag("ConfigScreen").e("加载档案失败", e)
            Toast.makeText(context, "加载配置失败: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "班牌设备配置",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp, vertical = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 说明文字
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "请配置班牌设备的基本信息。设备 UUID 已自动生成，实验室ID 和服务器地址需要手动配置。",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // UUID 显示（只读）
                OutlinedTextField(
                    value = profile?.uuid ?: "",
                    onValueChange = { },
                    label = { Text("设备 UUID") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("设备唯一标识，自动生成后不可修改") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 实验室ID输入
                OutlinedTextField(
                    value = laboratoryIdText,
                    onValueChange = { 
                        laboratoryIdText = it.filter { char -> char.isDigit() }
                        laboratoryIdError = null
                    },
                    label = { Text("实验室 ID *") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    isError = laboratoryIdError != null,
                    supportingText = { 
                        if (laboratoryIdError != null) {
                            Text(laboratoryIdError!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("请输入关联的实验室ID（数字）")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 服务器配置区域
                Text(
                    text = "服务器配置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 服务器地址
                OutlinedTextField(
                    value = serverHost,
                    onValueChange = { 
                        serverHost = it
                        serverHostError = null
                    },
                    label = { Text("服务器地址 *") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    isError = serverHostError != null,
                    supportingText = { 
                        if (serverHostError != null) {
                            Text(serverHostError!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("示例: 10.0.2.2 (模拟器) 或实际服务器IP")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 端口和超时时间（并排显示）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = serverPortText,
                        onValueChange = { 
                            serverPortText = it.filter { char -> char.isDigit() }
                            serverPortError = null
                        },
                        label = { Text("端口 *") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        isError = serverPortError != null,
                        supportingText = serverPortError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.weight(1f)
                    )
                    
                    OutlinedTextField(
                        value = serverTimeoutText,
                        onValueChange = { serverTimeoutText = it.filter { char -> char.isDigit() } },
                        label = { Text("超时(ms)") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 退出按钮（仅首次配置时显示）
                    if (isFirstConfig) {
                        OutlinedButton(
                            onClick = onExitApp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("退出应用")
                        }
                    }
                    
                    // 保存按钮
                    Button(
                        onClick = {
                            // 验证输入
                            var hasError = false
                            
                            if (laboratoryIdText.isBlank()) {
                                laboratoryIdError = "实验室ID不能为空"
                                hasError = true
                            } else if (laboratoryIdText.toLongOrNull() == null) {
                                laboratoryIdError = "实验室ID必须是数字"
                                hasError = true
                            }
                            
                            if (serverHost.isBlank()) {
                                serverHostError = "服务器地址不能为空"
                                hasError = true
                            }
                            
                            val port = serverPortText.toIntOrNull()
                            if (port == null || port !in 1..65535) {
                                serverPortError = "端口范围 1-65535"
                                hasError = true
                            }
                            
                            if (hasError) return@Button
                            
                            // 保存配置
                            scope.launch {
                                try {
                                    val currentProfile = profileManager.loadProfile()
                                    currentProfile.laboratoryId = laboratoryIdText.toLong()
                                    currentProfile.serverAddress = ServerAddress(
                                        host = serverHost,
                                        port = port!!,
                                        timeoutMs = serverTimeoutText.toLongOrNull() ?: 5000L
                                    )
                                    
                                    profileManager.saveProfile(currentProfile)
                                    
                                    Toast.makeText(context, "配置保存成功", Toast.LENGTH_SHORT).show()
                                    onConfigComplete()
                                } catch (e: Exception) {
                                    XLog.tag("ConfigScreen").e("保存配置失败", e)
                                    Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("保存配置")
                    }
                }
                
                // 取消按钮（非首次配置时显示）
                if (!isFirstConfig) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onConfigComplete,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("取消")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 600)
@Composable
fun DeviceProfileConfigScreenPreview() {
    ClassTimeTableTheme {
        Surface {
            // 预览无法使用真实的 ProfileManager，这里仅展示布局
            Text("配置页面预览")
        }
    }
}
