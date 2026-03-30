package xyz.jasenon.rsocket.core.rsocket.client.handler;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.packet.UpdateScheduleRequest;
import xyz.jasenon.rsocket.core.packet.UpdateScheduleResponse;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Status;
import xyz.jasenon.lab.common.entity.class_time_table.CourseSchedule;


import java.util.List;

/**
 * 更新课表处理器
 * 
 * 处理课表更新请求，更新设备本地课表
 * 
 * 注意：server -> client 的消息使用 command
 */
@Slf4j
public class UpdateScheduleHandler implements Handler {

    @Override
    public Command command() {
        return Command.UPDATE_SCHEDULE;
    }

    @Override
    public Mono<Message> handle(Message message) {
        // 类型检查
        if (!(message instanceof UpdateScheduleRequest)) {
            log.error("UpdateScheduleHandler 收到错误的消息类型: {}", message.getClass().getName());
            return Mono.just(Message.error(Command.UPDATE_SCHEDULE, Status.C10001,
                    "消息类型错误: 期望 UpdateScheduleRequest，实际 " + message.getClass().getSimpleName(), null));
        }

        UpdateScheduleRequest request = (UpdateScheduleRequest) message;
        List<CourseSchedule> schedules = request.getSchedules();

        log.info("处理课表更新请求: schedulesCount={}", schedules != null ? schedules.size() : 0);

        try {
            // 构造响应（server -> client，使用 command）
            UpdateScheduleResponse response = new UpdateScheduleResponse();
            response.setCommand(Command.UPDATE_SCHEDULE);
            response.setSuccess(true);
            response.setUpdateTime(System.currentTimeMillis());
            response.setStatus(Status.C10000, "课表更新成功");
            response.setTimestamp(System.currentTimeMillis());

            log.info("课表更新处理完成");
            return Mono.just(response);

        } catch (Exception e) {
            log.error("课表更新处理失败", e);
            UpdateScheduleResponse response = new UpdateScheduleResponse();
            response.setCommand(Command.UPDATE_SCHEDULE);
            response.setSuccess(false);
            response.setStatus(Status.C10001, "课表更新失败", e.getMessage());
            response.setTimestamp(System.currentTimeMillis());
            return Mono.just(response);
        }
    }
}
