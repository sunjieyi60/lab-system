package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.ClientSend;
import xyz.jasenon.rsocket.core.protocol.Message;

import java.io.Serial;
import java.io.Serializable;


/**
 * 开门响应
 * 
 * 设备返回开门执行结果
 * 继承 Message，简化设计
 */
@Getter
@Setter
public class OpenDoorResponse extends Message implements ClientSend, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 结果码：0成功，其他失败
     */
    private Integer code;

    /**
     * 结果消息
     */
    private String messageText;

    /**
     * 实际开门时间
     */
    private Long openTime;

    public static OpenDoorResponse success() {
        OpenDoorResponse response = new OpenDoorResponse();
        response.setSuccess(true);
        response.setCode(0);
        response.setMessageText("开门成功");
        response.setOpenTime(System.currentTimeMillis());
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    public static OpenDoorResponse fail(Integer code, String message) {
        OpenDoorResponse response = new OpenDoorResponse();
        response.setSuccess(false);
        response.setCode(code);
        response.setMessageText(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    @Override
    public String route() {
        return Const.Route.DEVICE_DOOR_OPEN;
    }
}
