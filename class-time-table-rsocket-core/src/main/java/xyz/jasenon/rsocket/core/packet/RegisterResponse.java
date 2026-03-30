package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.ServerSend;
import xyz.jasenon.rsocket.core.model.Config;


/**
 * 设备注册响应 - 继承 Message
 * 
 * 服务器向客户端发送注册结果
 */
@Getter
@Setter
public class RegisterResponse extends Message implements ServerSend<RegisterResponse> {

    private static final long serialVersionUID = 1L;

    /**
     * 班牌唯一编号
     */
    private String uuid;

    /**
     * 班牌配置
     */
    private Config config;

    /**
     * 创建注册成功响应
     */
    public static RegisterResponse success(String uuid, Config config) {
        RegisterResponse response = new RegisterResponse();
        response.setUuid(uuid);
        response.setConfig(config);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    /**
     * 创建注册失败响应
     */
    public static RegisterResponse fail(String uuid, String errorMessage) {
        RegisterResponse response = new RegisterResponse();
        response.setUuid(uuid);
        response.setTimestamp(System.currentTimeMillis());
        // 错误信息可以通过 data 字段或其他方式传递
        return response;
    }

    @Override
    public Command command() {
        return Command.REGISTER;
    }

    public RegisterResponse(){
         init(this);
    }
}
