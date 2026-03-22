package xyz.jasenon.rsocket.core.packet;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.rsocket.core.Const;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.ServerSend;
import xyz.jasenon.rsocket.core.protocol.Status;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * 更新人脸库请求
 * 
 * 服务器推送人脸库更新
 * 继承 Message，简化设计
 */
@Getter
@Setter
public class UpdateFaceLibraryRequest extends Message implements ServerSend {

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
        @Serial
        private static final long serialVersionUID = 1L;

        private String faceId;
        private String name;
        private byte[] faceData;
    }

    /**
     * 创建人脸库更新请求 Message
     */
    public static UpdateFaceLibraryRequest create(UpdateType updateType, Long libraryVersion, 
                                                   List<FaceItem> faces, List<String> deletedFaceIds) {
        UpdateFaceLibraryRequest request = new UpdateFaceLibraryRequest();
        // client -> server 使用 route
        request.setRoute(Const.Route.DEVICE_FACE_UPDATE);
        request.setStatus(Status.C10000);
        request.setUpdateType(updateType);
        request.setLibraryVersion(libraryVersion);
        request.setFaces(faces);
        request.setDeletedFaceIds(deletedFaceIds);
        request.setRequestTime(Instant.now());
        request.setTimestamp(Instant.now());
        return request;
    }

    @Override
    public Command command() {
        return Command.UPDATE_FACE_LIBRARY;
    }
}
