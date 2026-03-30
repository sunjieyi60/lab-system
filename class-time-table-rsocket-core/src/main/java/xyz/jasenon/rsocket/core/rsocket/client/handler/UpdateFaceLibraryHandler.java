package xyz.jasenon.rsocket.core.rsocket.client.handler;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.packet.UpdateFaceLibraryRequest;
import xyz.jasenon.rsocket.core.packet.UpdateFaceLibraryResponse;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Status;


import java.util.List;

/**
 * 更新人脸库处理器
 * 
 * 处理人脸库更新请求，更新设备本地人脸库
 * 
 * 注意：server -> client 的消息使用 command
 */
@Slf4j
public class UpdateFaceLibraryHandler implements Handler {

    @Override
    public Command command() {
        return Command.UPDATE_FACE_LIBRARY;
    }

    @Override
    public Mono<Message> handle(Message message) {
        // 类型检查
        if (!(message instanceof UpdateFaceLibraryRequest)) {
            log.error("UpdateFaceLibraryHandler 收到错误的消息类型: {}", message.getClass().getName());
            return Mono.just(Message.error(Command.UPDATE_FACE_LIBRARY, Status.C10001,
                    "消息类型错误: 期望 UpdateFaceLibraryRequest，实际 " + message.getClass().getSimpleName(), null));
        }

        UpdateFaceLibraryRequest request = (UpdateFaceLibraryRequest) message;
        UpdateFaceLibraryRequest.UpdateType updateType = request.getUpdateType();
        Long libraryVersion = request.getLibraryVersion();
        List<UpdateFaceLibraryRequest.FaceItem> faces = request.getFaces();
        List<String> deletedFaceIds = request.getDeletedFaceIds();

        log.info("处理人脸库更新请求: type={}, version={}, faces={}, deleted={}",
                updateType, libraryVersion,
                faces != null ? faces.size() : 0,
                deletedFaceIds != null ? deletedFaceIds.size() : 0);

        try {
            // 参数校验
            if (updateType == null) {
                return Mono.just(Message.error(Command.UPDATE_FACE_LIBRARY, Status.C10001, "更新类型不能为空", null));
            }

            int processedCount = faces != null ? faces.size() : 0;

            // 构造响应（server -> client，使用 command）
            UpdateFaceLibraryResponse response = new UpdateFaceLibraryResponse();
            response.setCommand(Command.UPDATE_FACE_LIBRARY);
            response.setSuccess(true);
            response.setCode(0);
            response.setProcessedCount(processedCount);
            response.setCurrentVersion(libraryVersion);
            response.setUpdateTime(System.currentTimeMillis());
            response.setStatus(Status.C10000, "人脸库更新成功");
            response.setTimestamp(System.currentTimeMillis());

            log.info("人脸库更新处理完成: processedCount={}", processedCount);
            return Mono.just(response);

        } catch (Exception e) {
            log.error("人脸库更新处理失败", e);
            UpdateFaceLibraryResponse response = new UpdateFaceLibraryResponse();
            response.setCommand(Command.UPDATE_FACE_LIBRARY);
            response.setSuccess(false);
            response.setCode(500);
            response.setStatus(Status.C10001, "人脸库更新失败", e.getMessage());
            response.setTimestamp(System.currentTimeMillis());
            return Mono.just(response);
        }
    }
}
