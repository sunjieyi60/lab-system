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
 * 功能区域说明：
 * - 左侧：切换界面和退出系统按钮
 * - 中间：标题和Logo
 * - 右侧：日期时间和天气信息
 * 
 * 使用的 Compose 组件：
 * - Row: 水平布局容器，用于排列左右两侧的内容
 * - Button: Material3 按钮组件，用于操作按钮
 * - Text: 文本显示组件
 * - Box: 通用布局容器，用于定位和叠加元素
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

                Spacer(modifier = Modifier.width(48.dp).height(48.dp))

                Button(
                    onClick = onSwitchInterface,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("切换界面")
                }
                
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
                // Logo占位符（实际应用中应使用实际Logo图片）
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
                DateTimeInLineText()
                WeatherInfoInLineText()
            }
        }
    }
}

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

