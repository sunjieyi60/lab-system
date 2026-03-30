package xyz.jasenon.rsocket.core.rsocket.client.handler;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.packet.RebootRequest;
import xyz.jasenon.rsocket.core.packet.RebootResponse;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Status;



/**
 * 设备重启处理器
 * 
 * 处理设备重启请求
 * 
 * 注意：server -> client 的消息使用 command
 */
@Slf4j
public class RebootHandler implements Handler {

    @Override
    public Command command() {
        return Command.REBOOT;
    }

    @Override
    public Mono<Message> handle(Message message) {
        // 类型检查
        if (!(message instanceof RebootRequest)) {
            log.error("RebootHandler 收到错误的消息类型: {}", message.getClass().getName());
            return Mono.just(Message.error(Command.REBOOT, Status.C10001,
                    "消息类型错误: 期望 RebootRequest，实际 " + message.getClass().getSimpleName(), null));
        }

        RebootRequest request = (RebootRequest) message;
        Integer delaySeconds = request.getDelaySeconds();
        String reason = request.getReason();

        log.info("处理设备重启请求: delaySeconds={}, reason={}", delaySeconds, reason);

        try {
            // 构造响应（server -> client，使用 command）
            long rebootTime = System.currentTimeMillis() + (delaySeconds != null ? delaySeconds : 0) * 1000L;
            RebootResponse response = new RebootResponse();
            response.setCommand(Command.REBOOT);
            response.setAccepted(true);
            response.setRebootTime(rebootTime);
            response.setMessageText("设备将在指定时间重启");
            response.setStatus(Status.C10000, "重启请求已接受");
            response.setTimestamp(System.currentTimeMillis());

            log.info("设备重启请求已接受，预计重启时间: {}", rebootTime);
            return Mono.just(response);

        } catch (Exception e) {
            log.error("处理设备重启请求失败", e);
            RebootResponse response = new RebootResponse();
            response.setCommand(Command.REBOOT);
            response.setAccepted(false);
            response.setMessageText("重启请求被拒绝: " + e.getMessage());
            response.setStatus(Status.C10001, "重启失败", e.getMessage());
            response.setTimestamp(System.currentTimeMillis());
            return Mono.just(response);
        }
    }
}
