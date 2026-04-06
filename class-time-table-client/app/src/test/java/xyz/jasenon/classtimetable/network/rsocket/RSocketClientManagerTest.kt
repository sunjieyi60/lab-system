package xyz.jasenon.classtimetable.network.rsocket

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.SystemPrinter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import xyz.jasenon.classtimetable.network.rsocket.model.SetUp
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = android.app.Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class RSocketClientManagerTest {

    companion object {
        private const val TEST_HOST = "127.0.0.1"
        private const val TEST_PORT = 7001
        // 将超时时间增加，但在测试中我们依靠 join 而不是 advanceTime
        private const val CONNECTION_TIMEOUT_MS = 10000L
        private const val TAG = "RSocketTest"
    }

    private lateinit var context: Context
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        Dispatchers.setMain(testDispatcher)

        XLog.init(LogConfiguration.Builder()
            .logLevel(LogLevel.ALL)
            .tag(TAG)
            .build(),
            AndroidPrinter(),
            SystemPrinter()
        )

        XLog.d(">>> [SETUP] 清理并准备环境")
        RSocketClientManager.destroyInstance()
    }

    @After
    fun tearDown() {
        XLog.d(">>> [TEARDOWN] 测试结束")
        Dispatchers.resetMain()
        RSocketClientManager.destroyInstance()
    }

    @Test
    fun `connect to localhost_7001 should handle connection result`() = runTest(timeout = 60.seconds) {
        XLog.d(">>> [TEST START] 验证连接流程")
        val manager = RSocketClientManager.getInstance(context)
        val states = mutableListOf<RSocketConnectionState>()

        // 监听状态流转
        val stateCollectionJob = launch {
            manager.connectionState.collect { state ->
                XLog.i(">>> [状态机流转] 当前状态: $state")
                states.add(state)
            }
        }

        XLog.d(">>> [STEP 1] 调用 connect() 函数 (127.0.0.1:7001)")

        // 关键修改：使用 withContext(Dispatchers.IO) 确保 IO 任务不被虚拟时钟瞬间跳过
        // 且不再手动调用 advanceTimeBy
        val connectJob = launch {
            try {
                withContext(Dispatchers.IO) {
                    manager.connect(TEST_HOST, TEST_PORT, maxRetries = 1, SetUp(UUID.randomUUID().toString()))
                }
                XLog.d(">>> [STEP 2] connect() 挂起函数已正常返回")
            } catch (e: Exception) {
                XLog.e(">>> [ERROR] connect() 过程中抛出异常: ${e.message}", e)
            }
        }

        // 等待连接任务结束（或者被 runTest 的 60s 超时强制终止）
        connectJob.join()

        XLog.i(">>> [FINISH] 测试结果验证. 总状态流转数: ${states.size}, 最终状态: ${manager.connectionState.value}")

        assertTrue("状态应该至少包含 Connecting 及其后续流转", states.size > 1)

        stateCollectionJob.cancel()
    }

    @Test
    fun `disconnect should change state to Disconnected`() = runTest {
        XLog.d(">>> [TEST START] 验证主动断开逻辑")
        val manager = RSocketClientManager.getInstance(context)

        XLog.d(">>> [STEP 1] 发起异步连接...")
        val connectJob = launch(Dispatchers.IO) {
            manager.connect(TEST_HOST, TEST_PORT, maxRetries = 1)
        }

        // 稍微等待一下让它进入 Connecting 状态
        delay(100)

        XLog.d(">>> [STEP 2] 立即执行 disconnect()")
        manager.disconnect()

        val state = manager.connectionState.value
        XLog.i(">>> [STATE] 断开后的最终状态: $state")

        assertTrue("状态必须为 Disconnected", state is RSocketConnectionState.Disconnected)
        connectJob.cancel()
    }
}