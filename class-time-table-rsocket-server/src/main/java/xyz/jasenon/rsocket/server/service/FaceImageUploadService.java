package xyz.jasenon.rsocket.server.service;

import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.packet.FileChunkResponse;

public interface FaceImageUploadService {

    /**
     * 初始化上传任务
     * @param uuid 班牌UUID
     * @param faceFeatureName 人脸特征名称
     * @param totalChunks 总分片数
     * @param totalSize 文件总大小
     * @return 任务ID
     */
    Mono<String> initUploadTask(String uuid, String faceFeatureName, int totalChunks, long totalSize);

    /**
     * 发送分片到client（通过request-response）
     * @param uuid 班牌UUID
     * @param taskId 任务ID
     * @param chunkData 分片数据
     * @param chunkIndex 分片索引
     * @param totalChunks 总分片数
     * @return 客户端响应
     */
    Mono<FileChunkResponse> uploadChunk(String uuid, String taskId, byte[] chunkData,
                                        int chunkIndex, int totalChunks);

    /**
     * 标记上传完成
     * @param uuid 班牌UUID
     * @param taskId 任务ID
     * @return 客户端处理结果
     */
    Mono<Void> completeUpload(String uuid, String taskId);
}
