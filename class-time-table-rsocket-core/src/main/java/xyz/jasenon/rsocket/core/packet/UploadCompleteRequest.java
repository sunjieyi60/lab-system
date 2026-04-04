package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.ServerSend;

/**
 * 上传完成请求
 *
 * 只传 taskId，Client 通过 taskId 找到关联信息完成处理
 */
@Getter
@Setter
public class UploadCompleteRequest extends Message implements ServerSend {

    private static final long serialVersionUID = 1L;

    /** 任务ID - 用于查找关联的人脸信息 */
    private String taskId;

    @Override
    public Command command() {
        return Command.COMPLETE_UPLOAD_TASK;
    }
}
