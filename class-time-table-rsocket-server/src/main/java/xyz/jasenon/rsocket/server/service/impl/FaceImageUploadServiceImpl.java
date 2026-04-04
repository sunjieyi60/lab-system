package xyz.jasenon.rsocket.server.service.impl;

import com.alibaba.fastjson2.JSON;
import io.rsocket.util.ByteBufPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import xyz.jasenon.rsocket.core.model.FaceImageEntity;
import xyz.jasenon.rsocket.core.packet.*;
import xyz.jasenon.rsocket.core.protocol.Status;
import xyz.jasenon.rsocket.core.rsocket.AbstractConnectionManager;
import xyz.jasenon.rsocket.server.mapper.FaceImageMapper;
import xyz.jasenon.rsocket.server.service.FaceImageUploadService;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaceImageUploadServiceImpl implements FaceImageUploadService {

    private final AbstractConnectionManager connectionManager;
    private final FaceImageMapper faceImageMapper;

    @Override
    public Mono<String> initUploadTask(String uuid, String faceFeatureName, int totalChunks, long totalSize) {
        return Mono.fromCallable(() -> {
            // 1. 生成任务ID
            String taskId = UUID.randomUUID().toString().replace("-", "");

            // 2. 保存任务到数据库（关联taskId与faceFeatureName）
            FaceImageEntity entity = new FaceImageEntity();
            entity.setUuid(uuid);
            entity.setFaceFeatureName(faceFeatureName);
            entity.setTaskId(taskId);
            entity.setStatus("UPLOADING");
            entity.setImageCount(0);
            entity.setImageUrls(new ArrayList<>());
            faceImageMapper.insert(entity);

            // 3. 获取设备连接
            RSocketRequester requester = connectionManager.getRequester(uuid);
            if (requester == null || requester.isDisposed()) {
                throw new IllegalStateException("设备不在线: " + uuid);
            }

            // 4. 构建初始化请求（传递完整信息）
            UploadTaskInitRequest request = new UploadTaskInitRequest();
            request.setTaskId(taskId);
            request.setFaceFeatureName(faceFeatureName);
            request.setTotalChunks(totalChunks);
            request.setTotalSize(totalSize);
            request.setStatus(Status.C10000);

            // 5. 通知Client初始化任务
            return requester.rsocket()
                    .requestResponse(ByteBufPayload.create(JSON.toJSONBytes(request)))
                    .map(payload -> {
                        payload.release();
                        log.info("Client 任务初始化成功: taskId={}", taskId);
                        return taskId;
                    })
                    .block();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<FileChunkResponse> uploadChunk(String uuid, String taskId, byte[] chunkData,
                                               int chunkIndex, int totalChunks) {
        return Mono.fromCallable(() -> {
            // 1. 获取设备连接
            RSocketRequester requester = connectionManager.getRequester(uuid);
            if (requester == null) {
                throw new IllegalStateException("设备不在线: " + uuid);
            }

            // 2. 构建分片包（只传 taskId，不传 faceFeatureName）
            FileChunkPacket packet = new FileChunkPacket();
            packet.setTaskId(taskId);
            packet.setChunkIndex(chunkIndex);
            packet.setTotalChunks(totalChunks);
            packet.setData(chunkData);
            packet.setDataLength(chunkData.length);
            packet.setLastChunk(chunkIndex == totalChunks - 1);
            packet.setStatus(Status.C10000);

            // 3. 发送分片并等待响应
            return requester.rsocket()
                    .requestResponse(ByteBufPayload.create(JSON.toJSONBytes(packet)))
                    .map(payload -> {
                        ByteBuffer buffer = payload.getData();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        payload.release();
                        FileChunkResponse response = JSON.parseObject(bytes, FileChunkResponse.class);
                        log.debug("分片上传完成: taskId={}, chunkIndex={}, progress={}%",
                                taskId, chunkIndex, response.getProgress());
                        return response;
                    })
                    .block();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> completeUpload(String uuid, String taskId) {
        return Mono.fromCallable(() -> {
            // 1. 获取设备连接
            RSocketRequester requester = connectionManager.getRequester(uuid);
            if (requester == null) {
                throw new IllegalStateException("设备不在线: " + uuid);
            }

            // 2. 构建完成请求（只传 taskId）
            UploadCompleteRequest request = new UploadCompleteRequest();
            request.setTaskId(taskId);
            request.setStatus(Status.C10000);

            // 3. 通知Client完成（Client通过taskId查找关联的faceFeatureName）
            return requester.rsocket()
                    .requestResponse(ByteBufPayload.create(JSON.toJSONBytes(request)))
                    .flatMap(payload -> {
                        ByteBuffer buffer = payload.getData();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        payload.release();

                        UploadCompleteResponse response = JSON.parseObject(bytes, UploadCompleteResponse.class);

                        // 4. 更新数据库状态
                        FaceImageEntity entity = faceImageMapper.selectOne(
                                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FaceImageEntity>()
                                        .eq(FaceImageEntity::getTaskId, taskId)
                        );
                        if (entity != null) {
                            entity.setStatus("COMPLETED");
                            // 可选：保存Client回传的人脸特征
                            if (response.getFaceFeature() != null) {
                                entity.setFaceFeature(response.getFaceFeature());
                            }
                            faceImageMapper.updateById(entity);
                            log.info("上传任务完成: taskId={}, faceFeatureName={}",
                                    taskId, entity.getFaceFeatureName());
                        }
                        return Mono.empty();
                    })
                    .block();
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
