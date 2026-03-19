package xyz.jasenon.lab.core.packets;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.jasenon.lab.core.ImStatus;

@NoArgsConstructor
@Data
public class BitmapRespBody extends RespBody {

    private String uuid;

    public BitmapRespBody(ImStatus status,String uuid, String msg){
        super(Command.COMMAND_BITMAP_PUSH_RESP, status);
        this.uuid = uuid;
        if (StrUtil.isNotBlank(msg)) this.msg = msg;
    }

    public static BitmapRespBody success(String uuid, String msg){
        BitmapRespBody resp = new BitmapRespBody(ImStatus.C10026, uuid, msg);
        return resp;
    }

    public static BitmapRespBody fail(String uuid, String msg){
        BitmapRespBody resp = new BitmapRespBody(ImStatus.C10027, uuid, msg);
        return resp;
    }

    public static BitmapRespBody fail(ImStatus status, String uuid, String msg){
        BitmapRespBody resp = new BitmapRespBody(status, uuid, msg);
        return resp;
    }


}
