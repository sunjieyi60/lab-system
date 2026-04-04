package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.protocol.ClientSend;
import xyz.jasenon.rsocket.core.protocol.Message;

/**
 * 文件分片传输响应 - Client 返回给 Server
 */
@Getter
@Setter
public class FileChunkResponse extends Message implements ClientSend {

    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private String taskId;

    /** 已接收的分片索引 */
    private int receivedChunkIndex;

    /** 接收状态 */
    private boolean success;

    /** 处理进度 0-100 */
    private int progress;

    @Override
    public String route() {
        return "face.upload.chunk.response";
    }
}
