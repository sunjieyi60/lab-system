package xyz.jasenon.rsocket.core.rsocket.client.handler;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.packet.RegisterRequest;
import xyz.jasenon.rsocket.core.packet.RegisterResponse;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Status;

import java.time.Instant;

/**
 * 设备注册处理器
 * 
 * 处理设备注册请求，返回注册响应
 * 
 * 注意：server -> client 的消息使用 command
 */
@Slf4j
public class RegisterHandler implements Handler {

    @Override
    public Command command() {
        return Command.REGISTER;
    }

    @Override
    public Mono<Message> handler(Message message) {
        // 类型检查
        if (!(message instanceof RegisterRequest)) {
            log.error("RegisterHandler 收到错误的消息类型: {}", message.getClass().getName());
            // server -> client 错误，使用 command
            return Mono.just(Message.error(Command.REGISTER, Status.C10001,
                    "消息类型错误: 期望 RegisterRequest，实际 " + message.getClass().getSimpleName(), null));
        }

        RegisterRequest request = (RegisterRequest) message;
        String uuid = request.getUuid();
        Long laboratoryId = request.getLaboratoryId();

        log.info("处理设备注册请求: uuid={}, laboratoryId={}", uuid, laboratoryId);

        try {
            // 参数校验
            if (uuid == null || uuid.isEmpty()) {
                log.warn("注册请求缺少 uuid");
                return Mono.just(Message.error(Command.REGISTER, Status.C10001, "uuid 不能为空", null));
            }

            // 构造注册响应（server -> client，使用 command）
            RegisterResponse response = new RegisterResponse();
            response.setCommand(Command.REGISTER);
            response.setUuid(uuid);
            response.setStatus(Status.C10000, "注册成功");
            response.setTimestamp(Instant.now());

            log.info("设备 {} 注册处理完成", uuid);
            return Mono.just(response);

        } catch (Exception e) {
            log.error("设备 {} 注册处理失败", uuid, e);
            // server -> client 错误，使用 command
            return Mono.just(Message.error(Command.REGISTER, Status.C10001, "注册失败", e.getMessage()));
        }
    }
}
