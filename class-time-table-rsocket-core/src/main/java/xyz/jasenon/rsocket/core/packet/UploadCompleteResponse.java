package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.protocol.ClientSend;
import xyz.jasenon.rsocket.core.protocol.Message;

/**
 * 上传完成响应
 */
@Getter
@Setter
public class UploadCompleteResponse extends Message implements ClientSend {

    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private String taskId;

    /** 是否成功 */
    private boolean success;

    /** 提取的人脸特征数据（回传给Server） */
    private byte[] faceFeature;

    @Override
    public String route() {
        return "face.upload.complete.response";
    }
}
