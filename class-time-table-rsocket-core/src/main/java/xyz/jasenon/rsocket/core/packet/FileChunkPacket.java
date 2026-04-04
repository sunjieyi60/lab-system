package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.ServerSend;

/**
 * 文件分片传输包 - Server 发送给 Client
 *
 * 优化：只传 taskId，Client 通过 taskId 查找关联的人脸信息
 */
@Getter
@Setter
public class FileChunkPacket extends Message implements ServerSend {

    private static final long serialVersionUID = 1L;

    /** 任务ID - 用于关联人脸信息 */
    private String taskId;

    /** 分片索引 */
    private int chunkIndex;

    /** 总分片数 */
    private int totalChunks;

    /** 分片数据 */
    private byte[] data;

    /** 数据长度 */
    private int dataLength;

    /** 是否为最后一片 */
    private boolean lastChunk;

    @Override
    public Command command() {
        return Command.UPLOAD_FACE_IMAGE;
    }
}
