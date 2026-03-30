package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.ServerSend;
import xyz.jasenon.rsocket.core.protocol.Status;
import xyz.jasenon.lab.common.entity.class_time_table.CourseSchedule;


import java.util.List;

/**
 * 更新课表请求
 * 
 * 服务器推送新课表数据
 * 继承 Message，简化设计
 */
@Getter
@Setter
public class UpdateScheduleRequest extends Message implements ServerSend {

    private static final long serialVersionUID = 1L;

    /**
     * 课表数据
     */
    private List<CourseSchedule> schedules;

    /**
     * 生效时间
     */
    private Long effectiveTime;

    /**
     * 请求时间
     */
    private Long requestTime;

    /**
     * 创建课表更新请求 Message
     */
    public static UpdateScheduleRequest create(List<CourseSchedule> schedules, Long effectiveTime) {
        UpdateScheduleRequest request = new UpdateScheduleRequest();
        // client -> server 使用 route
        request.setRoute(Const.Route.DEVICE_SCHEDULE_UPDATE);
        request.setStatus(Status.C10000);
        request.setSchedules(schedules);
        request.setEffectiveTime(effectiveTime);
        request.setRequestTime(System.currentTimeMillis());
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    @Override
    public Command command() {
        return Command.UPDATE_SCHEDULE;
    }
}
