package xyz.jasenon.rsocket.core.rsocket.client.handler;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.packet.OpenDoorRequest;
import xyz.jasenon.rsocket.core.packet.OpenDoorResponse;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Status;

import java.time.Instant;

/**
 * 开门处理器
 * 
 * 处理开门请求，控制设备开门
 * 
 * 注意：server -> client 的消息使用 command
 */
@Slf4j
public class OpenDoorHandler implements Handler {

    @Override
    public Command command() {
        return Command.OPEN_DOOR;
    }

    @Override
    public Mono<Message> handler(Message message) {
        // 类型检查
        if (!(message instanceof OpenDoorRequest)) {
            log.error("OpenDoorHandler 收到错误的消息类型: {}", message.getClass().getName());
            return Mono.just(Message.error(Command.OPEN_DOOR, Status.C10001,
                    "消息类型错误: 期望 OpenDoorRequest，实际 " + message.getClass().getSimpleName(), null));
        }

        OpenDoorRequest request = (OpenDoorRequest) message;
        OpenDoorRequest.OpenType type = request.getType_();
        String verifyInfo = request.getVerifyInfo();
        Integer duration = request.getDuration();

        log.info("处理开门请求: type={}, verifyInfo={}, duration={}", type, verifyInfo, duration);

        try {
            // 参数校验
            if (type == null) {
                return Mono.just(Message.error(Command.OPEN_DOOR, Status.C10001, "开门类型不能为空", null));
            }

            // 构造响应（server -> client，使用 command）
            OpenDoorResponse response = new OpenDoorResponse();
            response.setCommand(Command.OPEN_DOOR);
            response.setSuccess(true);
            response.setCode(0);
            response.setMessageText("开门成功");
            response.setOpenTime(Instant.now());
            response.setStatus(Status.C10000, "开门成功");
            response.setTimestamp(Instant.now());

            log.info("开门请求处理完成: type={}, success=true", type);
            return Mono.just(response);

        } catch (Exception e) {
            log.error("开门请求处理失败", e);
            OpenDoorResponse response = new OpenDoorResponse();
            response.setCommand(Command.OPEN_DOOR);
            response.setSuccess(false);
            response.setCode(500);
            response.setMessageText("开门失败: " + e.getMessage());
            response.setStatus(Status.C10001, "开门失败", e.getMessage());
            response.setTimestamp(Instant.now());
            return Mono.just(response);
        }
    }
}
