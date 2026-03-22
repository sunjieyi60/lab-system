package xyz.jasenon.rsocket.core.rsocket.client.handler;

import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Command;

/**
 * 消息处理器接口
 * 
 * 实现类负责：
 * 1. 返回支持的 Command 类型
 * 2. 将 Message 转换为对应的业务实体进行处理（业务实体继承自 Message）
 * 3. 返回处理结果 Message（通常是业务响应实体，也继承自 Message）
 * 
 * 使用示例：
 * <pre>
 * public class RegisterHandler implements Handler {
 *     @Override
 *     public Command command() {
 *         return Command.REGISTER;
 *     }
 *     
 *     @Override
 *     public Mono<Message> handler(Message message) {
 *         // 直接类型转换，因为业务实体继承自 Message
 *         if (!(message instanceof RegisterRequest)) {
 *             return Mono.just(Message.error(400, "消息类型错误"));
 *         }
 *         RegisterRequest request = (RegisterRequest) message;
 *         
 *         // 处理业务逻辑...
 *         
 *         // 返回响应实体（也继承自 Message）
 *         RegisterResponse response = new RegisterResponse();
 *         response.setCode(200);
 *         response.setMsg("注册成功");
 *         return Mono.just(response);
 *     }
 * }
 * </pre>
 */
public interface Handler {

    /**
     * 获取该处理器支持的 Command 类型
     * 
     * @return Command 类型
     */
    Command command();

    /**
     * 处理消息
     * 
     * 实现类中需要将 Message 转换为具体的业务实体：
     * 例如：RegisterRequest request = (RegisterRequest) message;
     * 
     * 注意：业务实体（如 RegisterRequest, RegisterResponse）都继承自 Message
     * 不需要通过 data 字段获取业务数据
     * 
     * @param message 接收到的消息（实际是业务实体的父类）
     * @return 处理结果的 Mono（返回的业务实体也继承自 Message）
     */
    Mono<Message> handler(Message message);

}
