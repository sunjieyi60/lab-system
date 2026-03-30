package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.ClientSend;
import xyz.jasenon.rsocket.core.protocol.Message;



/**
 * 更新课表响应
 * 
 * 设备返回课表更新结果
 * 继承 Message，简化设计
 */
@Getter
@Setter
public class UpdateScheduleResponse extends Message implements ClientSend {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 更新时间
     */
    private Long updateTime;

    public static UpdateScheduleResponse success(Long version) {
        UpdateScheduleResponse response = new UpdateScheduleResponse();
        response.setSuccess(true);
        response.setUpdateTime(System.currentTimeMillis());
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    public static UpdateScheduleResponse fail(Integer code, String message) {
        UpdateScheduleResponse response = new UpdateScheduleResponse();
        response.setSuccess(false);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    @Override
    public String route() {
        return Const.Route.DEVICE_SCHEDULE_UPDATE;
    }
}
