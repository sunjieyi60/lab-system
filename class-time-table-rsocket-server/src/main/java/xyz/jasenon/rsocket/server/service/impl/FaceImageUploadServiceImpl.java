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

/**
 * 人脸图像上传服务实现类
 * <p>
 * 提供人脸图像分片上传的完整实现，采用三步上传模式：
 * <ol>
 *   <li>初始化任务：创建任务记录，通知设备准备接收</li>
 *   <li>分片传输：通过 RSocket 逐个传输分片数据</li>
     *   <li>完成上传：通知设备处理，保存人脸特征</li>
 * </ol>
 * 使用 RSocket request-response 模式确保可靠传输。
 * </p>
 *
 * @author Jasenon_ce
 * @see FaceImageUploadService
 * @see AbstractConnectionManager
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaceImageUploadServiceImpl implements FaceImageUploadService {

    /** RSocket 连接管理器，用于与班牌设备通信 */
    private final AbstractConnectionManager connectionManager;

    /** 人脸图像数据访问层 */
    private final FaceImageMapper faceImageMapper;

    /**
     * {@inheritDoc}
     * <p>
     * 初始化上传任务的完整流程：
     * <ol>
     *   <li>生成唯一任务ID</li>
     *   <li>创建数据库记录，保存任务信息</li>
     *   <li>检查设备在线状态</li>
     *   <li>通过 RSocket 通知设备准备接收数据</li>
     * </ol>
     * </p>
     *
     * @throws IllegalStateException 当设备不在线或连接已关闭时抛出
     */
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

    /**
     * {@inheritDoc}
     * <p>
     * 上传单个分片的完整流程：
     * <ol>
     *   <li>获取设备 RSocket 连接</li>
     *   <li>构建设备分片数据包</li>
     *   <li>通过 RSocket 发送分片并等待响应</li>
     *   <li>解析设备响应并返回上传进度</li>
     * </ol>
     * </p>
     *
     * @throws IllegalStateException 当设备不在线时抛出
     */
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

    /**
     * {@inheritDoc}
     * <p>
     * 完成上传任务的完整流程：
     * <ol>
     *   <li>获取设备 RSocket 连接</li>
     *   <li>发送完成通知到设备</li>
     *   <li>解析设备响应，获取人脸特征数据</li>
     *   <li>更新数据库中的任务状态和人脸特征</li>
     * </ol>
     * </p>
     *
     * @throws IllegalStateException 当设备不在线时抛出
     */
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
