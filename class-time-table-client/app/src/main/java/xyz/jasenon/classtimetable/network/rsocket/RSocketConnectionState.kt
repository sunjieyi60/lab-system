package xyz.jasenon.classtimetable.network.rsocket

/**
 * RSocket 连接状态密封类
 *
 * 表示 RSocket 连接的各种状态，用于 [RSocketClientManager.connectionState] 状态流。
 * 使用密封类（sealed class）确保状态枚举的完整性检查。
 *
 * ## 状态流转图
 *
 * ```
 * Disconnected
 *      │
 *      ▼ (调用 connect)
 * Connecting ───────────────────────────┐
 *      │                                │
 *      ▼ (连接成功)                     │ (连接失败)
 * Connected                             │
 *      │                                │
 *      ▼ (连接断开)                     │
 * ConnectionClosed ──► Reconnecting ────┘
 *      │                      │
 *      │ (重试用尽)            │ (重试成功)
 *      ▼                      ▼
 * ConnectionFailed ◄─── (重试中)
 * ```
 *
 * ## 使用示例
 *
 * ```kotlin
 * rsocketManager.connectionState.collect { state ->
 *     when (state) {
 *         is RSocketConnectionState.Disconnected -> {
 *             // 显示"未连接"状态
 *         }
 *         is RSocketConnectionState.Connecting -> {
 *             // 显示"连接中"进度条
 *         }
 *         is RSocketConnectionState.Connected -> {
 *             // 显示"已连接"，启用功能按钮
 *         }
 *         is RSocketConnectionState.Reconnecting -> {
 *             // 显示"重连中: ${state.attempt}/${state.maxAttempts}"
 *         }
 *         is RSocketConnectionState.ConnectionFailed -> {
 *             // 显示"连接失败: ${state.error.message}"
 *         }
 *         is RSocketConnectionState.ConnectionClosed -> {
 *             // 显示"连接已关闭"
 *         }
 *     }
 * }
 * ```
 */
sealed class RSocketConnectionState {

    /**
     * 未连接状态
     *
     * 初始状态或调用 [RSocketClientManager.disconnect] 后的状态。
     * 表示当前没有活跃的 RSocket 连接。
     */
    data object Disconnected : RSocketConnectionState()

    /**
     * 连接中状态
     *
     * 调用 [RSocketClientManager.connect] 后进入此状态，
     * 表示正在尝试建立 TCP 连接和 RSocket 握手。
     *
     * 此状态持续直到：
     * - 连接成功 → [Connected]
     * - 连接失败 → [Reconnecting] 或 [ConnectionFailed]
     */
    data object Connecting : RSocketConnectionState()

    /**
     * 已连接状态
     *
     * RSocket 连接已成功建立，可以正常发送和接收数据。
     * 此状态下 [RSocketClientManager.getRequestHandler] 返回非 null。
     *
     * 此状态持续直到：
     * - 连接断开 → [ConnectionClosed]
     * - 网络异常 → [Reconnecting]
     * - 主动断开 → [Disconnected]
     */
    data object Connected : RSocketConnectionState()

    /**
     * 连接失败状态
     *
     * 连接建立失败且重试用尽，或发生不可恢复的错误。
     *
     * @property error 导致连接失败的异常，可用于错误提示和日志记录
     */
    data class ConnectionFailed(val error: Throwable) : RSocketConnectionState()

    /**
     * 连接关闭状态
     *
     * 连接正常关闭（收到关闭帧或 TCP 断开）。
     * 可能是服务器主动关闭或网络问题导致。
     *
     * @property reason 关闭原因描述，可为 null
     */
    data class ConnectionClosed(val reason: String? = null) : RSocketConnectionState()

    /**
     * 重连中状态
     *
     * 连接断开后，内置重连机制正在尝试重新建立连接。
     *
     * @property attempt 当前重试次数（从 1 开始）
     * @property maxAttempts 最大重试次数
     */
    data class Reconnecting(
        val attempt: Int,
        val maxAttempts: Int
    ) : RSocketConnectionState()
}
