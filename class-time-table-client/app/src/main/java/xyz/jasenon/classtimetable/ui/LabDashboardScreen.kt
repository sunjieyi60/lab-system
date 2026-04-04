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
 * 这是整个应用的主界面，采用三列布局结构：
 * - 顶部：统一的顶部栏（TopBar）
 * - 左侧列：实验室信息、简介、规章制度
 * - 中间列：实验室课表（主要功能区域）
 * - 右侧列：通知公告、校历、开门方式
 */
@Composable
fun LabDashboardScreen(
    onSwitchInterface: () -> Unit = {},
    onExitSystem: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val courses by RemoteDataObservable.timetableData.collectAsState(initial = emptyList())

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
                .weight(1f)  // 使用 weight 而不是 fillMaxSize，为 Spacer 留出空间
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

        Spacer(modifier = Modifier.fillMaxWidth().height(25.dp))
    }
}

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


