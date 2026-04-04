package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.protocol.ClientSend;
import xyz.jasenon.rsocket.core.protocol.Message;

/**
 * 上传任务初始化响应
 */
@Getter
@Setter
public class UploadTaskInitResponse extends Message implements ClientSend {

    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private String taskId;

    /** 是否成功 */
    private boolean success;

    @Override
    public String route() {
        return "face.upload.init.response";
    }
}
