package xyz.jasenon.classtimetable.ui.dialog

import android.icu.util.TimeUnit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 开门方式相关类型与抽象提供者定义
 *
 * 本文件定义：
 * 1. [DoorOpeningType]：开门方式枚举，与右侧列按钮、工厂注册一一对应
 * 2. [DoorOpeningUIProvider]：开门方式 UI 提供者抽象基类，约定 Ui() 签名与关门能力
 *
 * 架构约定：
 * - 父级（如 RightColumn）持有 visible 状态，点击按钮置 true 显示对应弹窗
 * - [DoorOpeningDialog] 仅负责遮罩层与居中卡片容器，内容区由各 Provider 的 Ui() 绘制
 * - Provider 必须持有由 Card 传入的 [setCardVisible]，
 *   在确认、取消、超时等逻辑完成后调用 setCardVisible(false) 实现关门
 */
// ==================== 开门方式类型枚举 ====================

/**
 * 开门方式类型枚举
 *
 * 与右侧列「开门方式」面板中的按钮一一对应，亦用于 [DoorOpenUiProviderFactory] 的注册与查找。
 * 新增开门方式时需在此添加枚举值，并在 [DoorOpenUiProviderInitializer] 中注册对应 Provider。
 */
enum class DoorOpeningType {
    /** 人脸识别开门：调用摄像头与 FaceSearchEngine 进行人脸比对 */
    FACE_RECOGNITION,

    /** 密码开门：数字键盘输入，与配置中的密码比对 */
    PASSWORD,

    /** 二维码开门：预留，可接入扫码 SDK */
    QR_CODE,

    /** 刷卡开门：预留，可接入读卡器 */
    CARD
}

// ==================== 开门方式 UI 提供者抽象基类 ====================

/**
 * 开门方式 UI 提供者抽象基类
 *
 * 职责：
 * - 定义弹窗内「实际业务 UI」的绘制接口 [Ui]
 * - 提供标题、描述、默认 Modifier，供 [DoorOpeningDialog] 展示标题栏与布局
 *
 * 与 Card 的协作：
 * - Card（[DoorOpeningDialog]）只负责：全屏半透明遮罩、居中圆角卡片、标题栏、剩余时间展示
 * - 卡片内容区域完全由子类 [Ui] 绘制（如密码键盘、人脸预览等）
 * - Card 将 [setCardVisible] 传入 [Ui]，Provider 在业务完成（确认/取消/超时）后必须调用
 *   setCardVisible(false)，从而触发父级将 visible 置 false，实现关门
 *
 * @param title 弹窗标题，显示在卡片顶部
 * @param description 简要说明，可由子类在内容区展示
 * @param modifier 内容区默认尺寸与修饰，如宽度、高度，供 Dialog 内 Card 使用
 */
abstract class DoorOpeningUIProvider(
    val title: String,
    val description: String,
    val modifier: Modifier,
) {
    /**
     * 绘制本开门方式的内容 UI（数字键盘、人脸预览等）
     *
     * 由 [DoorOpeningDialog] 在卡片内容区调用，子类在此实现具体交互与逻辑。
     * 遮罩关闭逻辑：Card 只负责遮罩，将 [setCardVisible] 传入；Provider 持有并在业务完成后调用 setCardVisible(false) 实现关门。
     *
     * @param visible 当前弹窗是否显示（由父级状态传入，仅用于只读）
     * @param onTimeoutUpdate 剩余时间（毫秒）回调；Provider 在倒计时变化时调用，用于标题栏「Xs 后自动关闭」展示
     * @param setCardVisible 由 Card 传入的可见性控制；Provider 在确认成功、用户取消、超时等任一完成路径上必须调用 setCardVisible(false) 以关闭遮罩/弹窗
     */
    @Composable
    abstract fun Ui(
        visible: Boolean,
        onTimeoutUpdate: (Long) -> Unit,
        setCardVisible: (Boolean) -> Unit
    )

    /** 确认成功时的扩展钩子，子类可做埋点或后续请求 */
    abstract fun onConfirm(): Unit

    /** 用户取消时的扩展钩子，子类可做埋点 */
    abstract fun onCancel(): Unit

    /**
     * 将配置中的「保持时间」数值与单位转换为毫秒
     *
     * @param value 数值，null 时按 30 处理
     * @param unit 时间单位，null 时按秒处理
     * @return 对应的毫秒数
     */
    fun toMillis(value: Int?, unit: TimeUnit?): Long {
        val v = value ?: 30
        return when (unit ?: TimeUnit.SECOND) {
            TimeUnit.SECOND -> v * 1000L
            TimeUnit.MINUTE -> v * 60000L
            TimeUnit.HOUR -> v * 3600000L
            TimeUnit.DAY -> v * 86400000L
            else -> v * 1000L
        }
    }
}



