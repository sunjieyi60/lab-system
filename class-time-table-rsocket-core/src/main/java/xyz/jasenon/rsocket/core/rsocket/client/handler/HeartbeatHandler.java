package xyz.jasenon.rsocket.core.rsocket.client.handler;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.packet.Heartbeat;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Status;



/**
 * 心跳处理器
 * 
 * 处理心跳请求，返回心跳响应
 * 
 * 注意：server -> client 的消息使用 command
 */
@Slf4j
public class HeartbeatHandler implements Handler {

    @Override
    public Command command() {
        return Command.HEARTBEAT;
    }

    @Override
    public Mono<Message> handle(Message message) {
        // 类型检查
        if (!(message instanceof Heartbeat)) {
            log.error("HeartbeatHandler 收到错误的消息类型: {}", message.getClass().getName());
            return Mono.just(Message.error(Command.HEARTBEAT, Status.C10001,
                    "消息类型错误: 期望 Heartbeat，实际 " + message.getClass().getSimpleName(), null));
        }

        Heartbeat request = (Heartbeat) message;
        String uuid = request.getUuid();
        Integer interval = request.getInterval();

        log.debug("处理心跳请求: uuid={}, interval={}", uuid, interval);

        // 构造心跳响应（server -> client，使用 command）
        Heartbeat response = new Heartbeat();
        response.setCommand(Command.HEARTBEAT);
        response.setUuid(uuid);
        response.setInterval(interval);
        response.setStatus(Status.C10000, "心跳成功");
        response.setTimestamp(System.currentTimeMillis());

        return Mono.just(response);
    }
}
