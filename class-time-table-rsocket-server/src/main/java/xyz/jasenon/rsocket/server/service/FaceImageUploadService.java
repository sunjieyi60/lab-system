package xyz.jasenon.rsocket.server.service;

import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.packet.FileChunkResponse;

/**
 * 人脸图像上传服务接口
 * <p>
 * 提供人脸图像分片上传功能，支持大文件的分片传输。
 * 采用三步上传模式：初始化任务 → 传输分片 → 完成上传。
 * </p>
 *
 * @author Jasenon_ce
 * @see FaceImageUploadServiceImpl
 * @since 1.0.0
 */
public interface FaceImageUploadService {

    /**
     * 初始化上传任务
     * <p>
     * 创建新的分片上传任务，生成任务ID，并通知班牌设备准备接收数据。
     * 任务信息将被持久化到数据库。
     * </p>
     *
     * @param uuid 班牌设备UUID
     * @param faceFeatureName 人脸特征名称，用于标识该人脸数据
     * @param totalChunks 总分片数，表示文件将被分割成多少个分片
     * @param totalSize 文件总大小（字节）
     * @return 任务ID的异步 Mono
     * @throws IllegalStateException 当设备不在线时抛出
     */
    Mono<String> initUploadTask(String uuid, String faceFeatureName, int totalChunks, long totalSize);

    /**
     * 上传分片数据
     * <p>
     * 将单个分片数据发送到班牌设备。通过 RSocket request-response 模式
     * 确保分片可靠传输，并获取设备端的处理响应。
     * </p>
     *
     * @param uuid 班牌设备UUID
     * @param taskId 上传任务ID
     * @param chunkData 分片二进制数据
     * @param chunkIndex 当前分片索引，从0开始
     * @param totalChunks 总分片数
     * @return 分片上传响应的异步 Mono，包含上传进度信息
     * @throws IllegalStateException 当设备不在线时抛出
     */
    Mono<FileChunkResponse> uploadChunk(String uuid, String taskId, byte[] chunkData,
                                        int chunkIndex, int totalChunks);

    /**
     * 标记上传完成
     * <p>
     * 通知班牌设备所有分片已上传完毕，触发设备端的人脸特征提取。
     * 更新数据库中的任务状态为已完成。
     * </p>
     *
     * @param uuid 班牌设备UUID
     * @param taskId 上传任务ID
     * @return 完成操作的异步 Mono
     * @throws IllegalStateException 当设备不在线时抛出
     */
    Mono<Void> completeUpload(String uuid, String taskId);
}
