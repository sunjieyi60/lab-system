package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.ServerSend;

/**
 * 上传任务初始化请求
 *
 * 在初始化时传递完整信息，后续分片只传 taskId
 */
@Getter
@Setter
public class UploadTaskInitRequest extends Message implements ServerSend {

    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private String taskId;

    /** 人脸特征名称 - 只在初始化传递 */
    private String faceFeatureName;

    /** 总分片数 */
    private int totalChunks;

    /** 文件总大小 */
    private long totalSize;

    @Override
    public Command command() {
        return Command.INIT_UPLOAD_TASK;
    }
}
