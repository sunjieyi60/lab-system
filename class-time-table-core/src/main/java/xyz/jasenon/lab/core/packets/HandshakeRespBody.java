package xyz.jasenon.lab.core.packets;

import lombok.Data;
import xyz.jasenon.lab.core.ImStatus;

@Data
public class HandshakeRespBody extends RespBody{

    private Config config;

    public HandshakeRespBody(Command command){
        super(command);
    }

    public HandshakeRespBody(Command command, ImStatus imStatus){
        super(command, imStatus);
    }

    public static HandshakeRespBody success(Config config){
        HandshakeRespBody handshakeRespBody = new HandshakeRespBody(Command.COMMAND_HANDSHAKE_RESP, ImStatus.C10024);
        handshakeRespBody.setConfig(config);
        return handshakeRespBody;
    }

    public static HandshakeRespBody fail(){
        HandshakeRespBody handshakeRespBody = new HandshakeRespBody(Command.COMMAND_HANDSHAKE_RESP, ImStatus.C10025);
        return handshakeRespBody;
    }

    public static HandshakeRespBody fail(String msg){
        HandshakeRespBody handshakeRespBody = new HandshakeRespBody(Command.COMMAND_HANDSHAKE_RESP, ImStatus.C10025);
        handshakeRespBody.setMsg(msg);
        return handshakeRespBody;
    }

}
