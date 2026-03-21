package xyz.jasenon.rsocket.core.protocol;

/**
 * 消息适配器接口 - 建立请求/响应类型安全契约
 * 
 * 核心设计价值：
 * 
 * 1. 类型安全契约
 *    - 将请求类型 T 和响应类型 R 绑定在一起
 *    - 编译期就能确定：发送 T 会收到 R
 *    - 避免强制转换和类型擦除问题
 * 
 * 2. 封装消息元数据
 *    - 路由（route）封装在实现类中
 *    - 消息类型（Type）由实现类决定
 *    - 调用方无需关心这些细节
 * 
 * 3. 与 SendStrategy/Client/Server 配合
 *    - 支持类型安全的发送方法：<T, R> Mono<Message<R>> send(MessageAdaptor<T, R> adaptor)
 *    - 自动类型推导，无需手动指定 Class<R>
 * 
 * 使用示例：
 * <pre>
 * // 1. 实现 Adaptor
 * public class OpenDoorRequest implements MessageAdaptor<OpenDoorRequest, OpenDoorResponse> {
 *     
 *     @Override
 *     public Message<OpenDoorRequest> adaptor() {
 *         return Message.<OpenDoorRequest>builder()
 *                 .route("door/open")          // 路由封装在这里
 *                 .type(Message.Type.REQUEST_RESPONSE)
 *                 .data(this)
 *                 .build();
 *     }
 *     
 *     @Override
 *     public Class<OpenDoorResponse> getResponseType() {
 *         return OpenDoorResponse.class;       // 声明响应类型
 *     }
 * }
 * 
 * // 2. 类型安全发送
 * OpenDoorRequest request = new OpenDoorRequest();
 * 
 * // 返回类型是 Mono<Message<OpenDoorResponse>>，不是 Message<?>！
 * Mono<Message<OpenDoorResponse>> response = client.send(request);
 * 
 * response.subscribe(msg -> {
 *     OpenDoorResponse data = msg.getData();  // 无需强制转换！
 * });
 * </pre>
 * 
 * @param <T> 请求数据类型
 * @param <R> 响应数据类型（用于类型安全）
 */
public interface MessageAdaptor<T, R> {

    /**
     * 将当前对象转换为 Message
     * 
     * 实现要点：
     * - 设置正确的 route（用于 RSocket 路由）
     * - 设置正确的 type（REQUEST_RESPONSE / FIRE_AND_FORGET 等）
     * - 将 this 作为 data
     * 
     * @return Message 对象
     */
    Message<T> adaptor();

    /**
     * 获取响应类型
     * 
     * 作用：让框架知道应该将响应反序列化为什么类型
     * 
     * @return 响应数据类型 Class 对象
     */
    Class<R> getResponseType();
}
