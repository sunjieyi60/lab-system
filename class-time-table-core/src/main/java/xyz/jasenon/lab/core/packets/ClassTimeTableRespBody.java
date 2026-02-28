package xyz.jasenon.lab.core.packets;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.core.ImStatus;

/**
 * @author Jasenon_ce
 * @date 2026/2/28
 */
@Getter
@Setter
public class ClassTimeTableRespBody extends RespBody{

    private static final long serialVersionUID = -1L;

    private ClassTimeTable classTimeTable;

    public ClassTimeTableRespBody() {}

    public ClassTimeTableRespBody(Command command){
        super(command);
    }

    public ClassTimeTableRespBody(Command command, ImStatus status){
        super(command, status);
    }

    public static ClassTimeTableRespBody success(ClassTimeTable classTimeTable){
        ClassTimeTableRespBody classTimeTableRespBody = new ClassTimeTableRespBody(Command.COMMAND_GET_CLASS_TIME_TABLE_RESP);
        classTimeTableRespBody.setClassTimeTable(classTimeTable);
        return classTimeTableRespBody;
    }

    public static ClassTimeTableRespBody failed(ImStatus status, String msg){
        ClassTimeTableRespBody classTimeTableRespBody = new ClassTimeTableRespBody(Command.COMMAND_GET_CLASS_TIME_TABLE_RESP, status);
        classTimeTableRespBody.setMsg(msg);
        return classTimeTableRespBody;
    }


}
