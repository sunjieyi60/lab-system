package xyz.jasenon.classtimetable.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jasenon.classtimetable.ui.dialog.DoorOpeningDialog
import xyz.jasenon.classtimetable.ui.dialog.DoorOpeningType

/**
 * 右侧列组件
 *
 * 实验室辅助功能区，包含三个垂直排列的功能面板：
 * 1. [NoticesCard] 通知公告面板：显示系统通知和公告信息
 * 2. [AcademicCalendarCard] 校历面板：显示学校校历信息
 * 3. [DoorOpeningMethodsCard] 开门方式面板：提供四种开门方式的按钮
 *
 * ## 布局特点
 *
 * - 使用 [Column] 垂直排列三个面板
 * - 每个面板使用 [Modifier.weight(1f)] 均分可用高度
 * - 面板之间使用 [Arrangement.spacedBy] 设置间距
 *
 * ## 数据来源
 *
 * 当前为静态 Mock 数据，后续可从 [RemoteDataObservable] 获取：
 * - 通知公告：从服务器获取
 * - 校历：从服务器获取
 * - 开门方式：本地功能
 *
 * @param modifier Compose 修饰符，用于外部传入的布局调整
 *
 * @see NoticesCard
 * @see AcademicCalendarCard
 * @see DoorOpeningMethodsCard
 */
@Composable
fun RightColumn(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 通知公告面板 - 均分高度
        NoticesCard(
            modifier = Modifier.weight(1f)
        )

        // 校历面板 - 均分高度
        AcademicCalendarCard(
            modifier = Modifier.weight(1f)
        )

        // 开门方式面板 - 均分高度
        DoorOpeningMethodsCard(
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 通知公告卡片
 *
 * 显示系统通知和公告信息。
 *
 * ## UI 设计
 *
 * - 标题居中显示"通知公告"
 * - 内容区域预留，当前显示"暂无通知公告"
 * - 后续可替换为可滚动的列表
 *
 * @param modifier Compose 修饰符，用于外部传入的布局调整
 */
@Composable
fun NoticesCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "通知公告",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 通知内容（实际应用中应从数据源获取）
            Text(
                text = "暂无通知公告",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 校历卡片
 *
 * 显示学校校历信息。
 *
 * ## UI 设计
 *
 * - 标题居中显示"校历"
 * - 内容区域预留，当前显示"暂无校历信息"
 * - 后续可替换为日历组件
 *
 * @param modifier Compose 修饰符，用于外部传入的布局调整
 */
@Composable
fun AcademicCalendarCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "校历",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 校历内容（实际应用中应从数据源获取）
            Text(
                text = "暂无校历信息",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 开门方式卡片
 *
 * 提供四种开门方式的按钮：
 * - 人脸识别开门
 * - 密码开门
 * - 二维码开门
 * - 刷卡开门
 *
 * ## UI 设计
 *
 * - 2x2 网格布局，使用 Row + Column 实现
 * - 每个按钮使用 [DoorOpeningButton] 组件
 * - 点击按钮弹出对应的开门对话框
 *
 * ## 交互逻辑
 *
 * 1. 点击"人脸识别开门" → 弹出人脸识别对话框
 * 2. 点击"密码开门" → 弹出密码输入对话框
 * 3. 点击"二维码开门"/"刷卡开门" → 预留功能
 *
 * @param modifier Compose 修饰符，用于外部传入的布局调整
 *
 * @see DoorOpeningButton
 * @see DoorOpeningDialog
 */
@Composable
fun DoorOpeningMethodsCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "开门方式",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 对话框状态
            var showPwdDialog by remember { mutableStateOf(false) }
            var showFaceDialog by remember { mutableStateOf(false) }

            // 开门方式按钮网格（2x2布局）
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 第一行：人脸识别和密码开门
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DoorOpeningButton(
                        text = "人脸识别开门",
                        onClick = { showFaceDialog = true },
                        modifier = Modifier.weight(1f)
                    )

                    DoorOpeningButton(
                        text = "密码开门",
                        onClick = { showPwdDialog = true},
                        modifier = Modifier.weight(1f)
                    )
                }

                // 第二行：二维码和刷卡开门
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DoorOpeningButton(
                        text = "二维码开门",
                        onClick = { /* 处理二维码开门 */ },
                        modifier = Modifier.weight(1f)
                    )
                    DoorOpeningButton(
                        text = "刷卡开门",
                        onClick = { /* 处理刷卡开门 */ },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 密码开门对话框
                DoorOpeningDialog(
                    visible = showPwdDialog,
                    onClose = { showPwdDialog = false },
                    type = DoorOpeningType.PASSWORD,
                )

                // 人脸识别开门对话框
                DoorOpeningDialog(
                    visible = showFaceDialog,
                    onClose = { showFaceDialog = false },
                    type = DoorOpeningType.FACE_RECOGNITION,
                )
            }
        }
    }
}

/**
 * 开门方式按钮组件
 *
 * 统一的开门方式按钮样式，使用 Material3 [Button]。
 *
 * ## UI 设计
 *
 * - 固定高度 80dp
 * - 主色背景，白色文字
 * - 圆角 8dp
 * - 20sp 字体大小
 *
 * @param text 按钮文本
 * @param onClick 点击回调
 * @param modifier Compose 修饰符
 */
@Composable
fun DoorOpeningButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(80.dp)
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 20.sp
        )
    }
}

// ============== 预览 ==============

@Preview(showBackground = true, widthDp = 300, heightDp = 800)
@Composable
fun RightColumnPreview() {
    MaterialTheme {
        RightColumn(
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 200)
@Composable
fun NoticesCardPreview() {
    MaterialTheme {
        NoticesCard()
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 200)
@Composable
fun AcademicCalendarCardPreview() {
    MaterialTheme {
        AcademicCalendarCard()
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 300)
@Composable
fun DoorOpeningMethodsCardPreview() {
    MaterialTheme {
        DoorOpeningMethodsCard()
    }
}

@Preview(showBackground = true, widthDp = 150, heightDp = 80)
@Composable
fun DoorOpeningButtonPreview() {
    MaterialTheme {
        DoorOpeningButton(
            text = "人脸识别开门",
            onClick = { }
        )
    }
}
