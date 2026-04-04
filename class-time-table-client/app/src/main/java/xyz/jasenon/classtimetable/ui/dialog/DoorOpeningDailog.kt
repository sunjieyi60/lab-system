package xyz.jasenon.classtimetable.ui.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 开门方式弹窗（Card 层：仅遮罩与容器）
 *
 * 遮罩关闭逻辑（按你的设想）：
 * - Card 只负责遮罩：全屏半透明、点击遮罩/返回键时调用 [onClose] 关门；不持有业务逻辑。
 * - 将 [setCardVisible] 传给 Provider；Provider 持有并在确认/取消/超时后调用 setCardVisible(false)，Card 内收到后调用 [onClose]，从而由父级将 visible 置 false，实现关门。
 *
 * 本组件只负责：
 * 1. 根据 [visible] 控制整窗显隐与进入/退出动画
 * 2. 全屏半透明遮罩（点击或返回键 → onClose）
 * 3. 居中圆角卡片：标题栏、分割线、内容区（由 Provider.Ui 绘制）
 * 4. 向 Provider 传入 [setCardVisible]，由 Provider 在业务完成后调用以关门
 *
 * @param visible 是否显示弹窗，由父级状态控制；按钮置 true，遮罩点击或 Provider 调用 setCardVisible(false) 后通过 onClose 置 false
 * @param onClose 关门回调；Card 在「点击遮罩/返回」或「Provider 调用 setCardVisible(false)」时调用，父级在此将 visible 置 false
 * @param type 开门方式类型，用于取默认 [uiProvider]
 * @param uiProvider 内容区 UI 提供者
 * @param modifier 内容卡片修饰
 */
@Composable
fun DoorOpeningDialog(
    visible: Boolean,
    onClose: () -> Unit,
    type: DoorOpeningType,
    uiProvider: DoorOpeningUIProvider = DoorOpenUiProviderFactory.get(type),
    modifier: Modifier = uiProvider.modifier
) {
    var timeoutMillis by remember { mutableStateOf(0L) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) +
                scaleIn(initialScale = 0.9f, animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(200)) +
                scaleOut(targetScale = 0.9f, animationSpec = tween(200))
    ) {
        Dialog(
            onDismissRequest = onClose,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            // 遮罩：半透明黑底，点击即关门（由 onClose 通知父级置 false）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = modifier
                        .fillMaxWidth(0.85f)
                        .clickable(enabled = false, onClick = {})
                        .padding(8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiProvider.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${timeoutMillis / 1000}s 后自动关闭",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // Provider 持有 setCardVisible，业务完成后调用 setCardVisible(false) 即关门
                        uiProvider.Ui(
                            visible = visible,
                            onTimeoutUpdate = { timeoutMillis = it },
                            setCardVisible = { if (!it) onClose() }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 1920, heightDp = 1080)
@Composable
private fun DoorOpeningDialogPreview() {
    DoorOpeningDialog(
        visible = true,
        onClose = {},
        type = DoorOpeningType.FACE_RECOGNITION,
        uiProvider = FaceOpenUiProvider(
            title = "人脸识别开门",
            description = "请面向摄像头进行人脸识别"
        )
    )
}
