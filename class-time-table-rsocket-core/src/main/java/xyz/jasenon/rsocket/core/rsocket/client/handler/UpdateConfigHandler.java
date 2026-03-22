package xyz.jasenon.rsocket.core.rsocket.client.handler;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.packet.UpdateConfigRequest;
import xyz.jasenon.rsocket.core.packet.UpdateConfigResponse;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Status;

import java.time.Instant;

/**
 * 更新配置处理器
 * 
 * 处理配置更新请求，更新设备本地配置
 * 
 * 注意：server -> client 的消息使用 command
 */
@Slf4j
public class UpdateConfigHandler implements Handler {

    @Override
    public Command command() {
        return Command.UPDATE_CONFIG;
    }

    @Override
    public Mono<Message> handler(Message message) {
        // 类型检查
        if (!(message instanceof UpdateConfigRequest)) {
            log.error("UpdateConfigHandler 收到错误的消息类型: {}", message.getClass().getName());
            return Mono.just(Message.error(Command.UPDATE_CONFIG, Status.C10001,
                    "消息类型错误: 期望 UpdateConfigRequest，实际 " + message.getClass().getSimpleName(), null));
        }

        UpdateConfigRequest request = (UpdateConfigRequest) message;
        Long version = request.getVersion();
        Boolean immediate = request.getImmediate();

        log.info("处理配置更新请求: version={}, immediate={}", version, immediate);

        try {
            // 参数校验
            if (version == null) {
                return Mono.just(Message.error(Command.UPDATE_CONFIG, Status.C10001, "配置版本号不能为空", null));
            }

            // 构造响应（server -> client，使用 command）
            UpdateConfigResponse response = new UpdateConfigResponse();
            response.setCommand(Command.UPDATE_CONFIG);
            response.setSuccess(true);
            response.setUpdateTime(Instant.now());
            response.setStatus(Status.C10000, "配置更新成功");
            response.setTimestamp(Instant.now());

            log.info("配置更新处理完成: version={}", version);
            return Mono.just(response);

        } catch (Exception e) {
            log.error("配置更新处理失败: version={}", version, e);
            UpdateConfigResponse response = new UpdateConfigResponse();
            response.setCommand(Command.UPDATE_CONFIG);
            response.setSuccess(false);
            response.setStatus(Status.C10001, "配置更新失败", e.getMessage());
            response.setTimestamp(Instant.now());
            return Mono.just(response);
        }
    }
}
