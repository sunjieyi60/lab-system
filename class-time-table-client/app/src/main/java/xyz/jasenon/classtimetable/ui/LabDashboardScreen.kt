package xyz.jasenon.classtimetable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.jasenon.classtimetable.network.observe.RemoteDataObservable

/**
 * 实验室管理主界面
 *
 * 整个应用的主界面，采用三列布局结构：
 * - 顶部：统一的顶部栏（[TopBar]）
 * - 左侧列：实验室信息、简介、规章制度（[LeftColumn]）
 * - 中间列：实验室课表（[MiddleColumn]）
 * - 右侧列：通知公告、校历、开门方式（[RightColumn]）
 *
 * ## 数据流
 *
 * ```
 * RemoteDataObservable.timetableData (StateFlow)
 *              │
 *              ▼ (collectAsState)
 * LabDashboardScreen.courses
 *              │
 *              ▼ (参数传递)
 * MiddleColumn (课程表展示)
 * ```
 *
 * ## 布局结构
 *
 * ```
 * ┌─────────────────────────────────────────────────────────┐
 * │                        TopBar                           │
 * ├──────────────┬────────────────────────────┬─────────────┤
 * │              │                            │             │
 * │  LeftColumn  │       MiddleColumn         │ RightColumn │
 * │  (weight=1)  │       (weight=2)           │ (weight=1)  │
 * │              │                            │             │
 * │  实验室信息   │        课程表              │  通知公告    │
 * │  实验室简介   │                            │  校历       │
 * │  规章制度   │                            │  开门方式    │
 * │              │                            │             │
 * └──────────────┴────────────────────────────┴─────────────┘
 * ```
 *
 * ## 响应式设计
 *
 * 使用 Compose 的响应式数据流：
 * - 通过 [collectAsState] 收集 [RemoteDataObservable.timetableData]
 * - 数据变化时自动触发重组，刷新课程表
 *
 * @param onSwitchInterface 切换界面回调，点击"切换界面"按钮时触发
 * @param onExitSystem 退出系统回调，点击"退出系统"按钮时触发
 * @param modifier Compose 修饰符，用于外部传入的布局调整
 *
 * @see TopBar
 * @see LeftColumn
 * @see MiddleColumn
 * @see RightColumn
 */
@Composable
fun LabDashboardScreen(
    onSwitchInterface: () -> Unit = {},
    onExitSystem: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    /**
     * 课程列表状态
     *
     * 使用 [collectAsState] 收集 [RemoteDataObservable.timetableData]，
     * 当数据变化时自动触发重组。
     *
     * initial = emptyList() 确保首次组合时有默认值
     */
    val courses by RemoteDataObservable.timetableData.collectAsState(initial = emptyList())

    /**
     * 根布局：垂直排列
     */
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部栏区域
        TopBar(
            onSwitchInterface = onSwitchInterface,
            onExitSystem = onExitSystem,
            modifier = Modifier.fillMaxWidth()
        )

        // 主内容区域：三列布局
        Row(
            modifier = Modifier
                .weight(1f)  // 使用 weight 分配剩余空间
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 左侧列：实验室信息相关
            LeftColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            // 中间列：实验室课表（观察 RemoteDataObservable.timetableData 实现动态刷新）
            MiddleColumn(
                courses = courses,
                currentWeek = 1,
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            )

            // 右侧列：通知公告、校历、开门方式
            RightColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // 底部间距
        Spacer(modifier = Modifier.fillMaxWidth().height(25.dp))
    }
}

/**
 * LabDashboardScreen 预览
 *
 * 在 Android Studio 的设计视图中预览主界面布局。
 * 尺寸：1920x1080（典型的 1080p 横屏显示器）
 */
@Preview(showBackground = true, widthDp = 1920, heightDp = 1080)
@Composable
fun LabDashboardScreenPreview() {
    MaterialTheme {
        LabDashboardScreen(
            onSwitchInterface = { },
            onExitSystem = { },
            modifier = Modifier.fillMaxSize()
        )
    }
}
