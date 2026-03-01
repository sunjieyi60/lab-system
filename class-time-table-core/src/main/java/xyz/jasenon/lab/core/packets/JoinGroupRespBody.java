package xyz.jasenon.lab.core.packets;

import xyz.jasenon.lab.core.ImStatus;

public class JoinGroupRespBody extends RespBody {

    public JoinGroupRespBody() {
        super();
    }

    public JoinGroupRespBody(Command command) {
        super(command);
    }

    public JoinGroupRespBody(Command command, ImStatus status){
        super(command,status);
    }

    public static JoinGroupRespBody success(){
        JoinGroupRespBody resp = new JoinGroupRespBody(Command.COMMAND_JOIN_GROUP_RESP, ImStatus.C10011);
        return resp;
    }

    public static JoinGroupRespBody failed(String msg){
        JoinGroupRespBody resp = new JoinGroupRespBody(Command.COMMAND_JOIN_GROUP_RESP, ImStatus.C10012);
        resp.setMsg(msg);
        return resp;
    }

    public static JoinGroupRespBody failed(String msg, ImStatus status){
        JoinGroupRespBody resp = new JoinGroupRespBody(Command.COMMAND_JOIN_GROUP_RESP,status);
        resp.setMsg(msg);
        return resp;
    }

}
