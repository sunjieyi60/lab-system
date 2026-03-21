package xyz.jasenon.rsocket.core.packet;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.MessageAdaptor;
import xyz.jasenon.rsocket.core.protocol.Message;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * 更新人脸库请求
 * 
 * 服务器推送人脸库更新
 */
@Getter
@Setter
public class UpdateFaceLibraryRequest implements MessageAdaptor<UpdateFaceLibraryRequest,UpdateFaceLibraryResponse>,  Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 更新类型：FULL/INCREMENTAL
     */
    private UpdateType updateType;

    /**
     * 人脸库版本
     */
    private Long libraryVersion;

    /**
     * 新增/更新的人脸列表
     */
    private List<FaceItem> faces;

    /**
     * 需要删除的人脸ID列表
     */
    private List<String> deletedFaceIds;

    /**
     * 请求时间
     */
    private Instant requestTime;

    public enum UpdateType {
        /**
         * 全量更新
         */
        FULL,
        /**
         * 增量更新
         */
        INCREMENTAL
    }

    /**
     * 人脸项
     */
    @Getter
    @Setter
    public static class FaceItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String faceId;
        private String name;
        private byte[] faceData;
    }

    @Override
    public Message adaptor() {
        return Message.builder()
                .route(Const.Route.DEVICE_FACE_UPDATE)
                .type(Message.Type.REQUEST_RESPONSE)
                .data(this)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public Class<UpdateFaceLibraryResponse> getResponseType() {
        return UpdateFaceLibraryResponse.class;
    }

}
