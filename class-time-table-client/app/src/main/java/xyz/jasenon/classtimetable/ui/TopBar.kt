package xyz.jasenon.classtimetable.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jasenon.classtimetable.R
import xyz.jasenon.classtimetable.ui.component.date_time.DateTimeInLineText
import xyz.jasenon.classtimetable.ui.component.weather_ifno.WeatherInfoInLineText

/**
 * 顶部栏组件
 *
 * 应用的全局顶部导航栏，包含三个功能区域：
 * - 左侧：操作按钮（切换界面、退出系统）
 * - 中间：Logo 和标题
 * - 右侧：日期时间和天气信息
 *
 * ## 布局结构
 *
 * ```
 * ┌────────────────────────────────────────────────────────────────────────┐
 * │ [切换界面] [退出系统]     Logo | 本科生院 | 计算机基础实验教学中心     [时间] │
 * │                           （标题区域）                                    │
 * │                                                                       [天气] │
 * └────────────────────────────────────────────────────────────────────────┘
 *          左侧区域              中间区域                          右侧区域
 *           320dp              自适应宽度                          自适应
 * ```
 *
 * ## 组件说明
 *
 * | 区域 | 组件 | 说明 |
 * |------|------|------|
 * | 左侧 | 切换界面按钮 | 触发界面切换功能 |
 * | 左侧 | 退出系统按钮 | 触发应用退出 |
 * | 中间 | Logo | 学校/机构 Logo 图片 |
 * | 中间 | 标题 | "本科生院 \| 计算机基础实验教学中心" |
 * | 右侧 | [DateTimeInLineText] | 实时日期时间显示 |
 * | 右侧 | [WeatherInfoInLineText] | 天气信息显示 |
 *
 * @param onSwitchInterface 切换界面回调，点击"切换界面"按钮时触发
 * @param onExitSystem 退出系统回调，点击"退出系统"按钮时触发
 * @param modifier Compose 修饰符
 *
 * @see DateTimeInLineText
 * @see WeatherInfoInLineText
 */
@Composable
fun TopBar(
    onSwitchInterface: () -> Unit,
    onExitSystem: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：操作按钮区域
            Row(
                modifier = Modifier.width(320.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 占位 Spacer，保持布局平衡
                Spacer(modifier = Modifier.width(48.dp).height(48.dp))

                // 切换界面按钮
                Button(
                    onClick = onSwitchInterface,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("切换界面")
                }

                // 退出系统按钮
                Button(
                    onClick = onExitSystem,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("退出系统")
                }
            }

            // 中间：标题和Logo区域
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo
                Box(
                    modifier = Modifier
                        .size(88.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo"
                    )
                }

                // 标题文本
                Text(
                    text = "本科生院 | 计算机基础实验教学中心",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 40.sp,
                    letterSpacing = 8.sp
                )
            }

            // 右侧：日期时间和天气信息区域
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 日期时间显示
                DateTimeInLineText()
                // 天气信息显示
                WeatherInfoInLineText()
            }
        }
    }
}

/**
 * TopBar 预览
 *
 * 在 Android Studio 的设计视图中预览顶部栏布局。
 */
@Preview(showBackground = true, widthDp = 1920, heightDp = 100)
@Composable
fun TopBarPreview() {
    MaterialTheme {
        TopBar(
            onSwitchInterface = { },
            onExitSystem = { },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
