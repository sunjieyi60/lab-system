package xyz.jasenon.rsocket.core.packet;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;
import xyz.jasenon.rsocket.core.protocol.Message;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * 更新人脸库响应
 * 
 * 设备返回人脸库更新结果
 */
@Getter
@Setter
public class UpdateFaceLibraryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 结果码
     */
    private Integer code;

    /**
     * 成功处理的人脸数量
     */
    private Integer processedCount;

    /**
     * 设备当前人脸库版本
     */
    private Long currentVersion;

    /**
     * 更新时间
     */
    private Instant updateTime;

    public static UpdateFaceLibraryResponse success(int count, Long version) {
        UpdateFaceLibraryResponse response = new UpdateFaceLibraryResponse();
        response.setSuccess(true);
        response.setCode(0);
        response.setProcessedCount(count);
        response.setCurrentVersion(version);
        response.setUpdateTime(Instant.now());
        return response;
    }

    public static UpdateFaceLibraryResponse fail(Integer code, String message) {
        UpdateFaceLibraryResponse response = new UpdateFaceLibraryResponse();
        response.setSuccess(false);
        response.setCode(code);
        return response;
    }

}
