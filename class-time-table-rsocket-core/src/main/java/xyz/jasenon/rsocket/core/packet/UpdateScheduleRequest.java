package xyz.jasenon.rsocket.core.packet;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;
import xyz.jasenon.rsocket.core.protocol.Message;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.class_time_table.CourseSchedule;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * 更新课表请求
 * 
 * 服务器推送新课表数据
 */
@Getter
@Setter
public class UpdateScheduleRequest implements MessageAdaptor<UpdateScheduleRequest, UpdateScheduleResponse>,  Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 课表数据
     */
    private List<CourseSchedule> schedules;

    /**
     * 生效时间
     */
    private Instant effectiveTime;

    /**
     * 请求时间
     */
    private Instant requestTime;

    @Override
    public Message<UpdateScheduleRequest> adaptor() {
        return Message.<UpdateScheduleRequest>builder()
                .route(Const.Route.DEVICE_SCHEDULE_UPDATE)
                .type(Message.Type.REQUEST_RESPONSE)
                .data(this)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public Class<UpdateScheduleResponse> getResponseType() {
        return UpdateScheduleResponse.class;
    }

}
