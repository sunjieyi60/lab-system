package xyz.jasenon.rsocket.core.rsocket.client.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.cache.UploadTaskCache;
import xyz.jasenon.rsocket.core.packet.FileChunkPacket;
import xyz.jasenon.rsocket.core.packet.FileChunkResponse;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;

/**
 * 文件分片处理器 - Client 端
 *
 * 处理 Server 发来的 UPLOAD_FACE_IMAGE 命令
 * 通过 taskId 从缓存查找关联的 faceFeatureName，避免每次传输
 */
@Slf4j
@Component
public class FileChunkHandler implements Handler {

    @Override
    public Command command() {
        return Command.UPLOAD_FACE_IMAGE;
    }

    @Override
    public Mono<Message> handle(Message message) {
        return Mono.fromCallable(() -> {
            if (!(message instanceof FileChunkPacket)) {
                return Message.error(command(), Status.C10001, "消息类型错误");
            }

            FileChunkPacket packet = (FileChunkPacket) message;
            String taskId = packet.getTaskId();
            int chunkIndex = packet.getChunkIndex();
            int totalChunks = packet.getTotalChunks();
            byte[] data = packet.getData();
            boolean lastChunk = packet.isLastChunk();

            // 从缓存查找任务信息（优化：不传faceFeatureName，通过taskId查找）
            UploadTaskCache.TaskInfo taskInfo = UploadTaskCache.get(taskId);
            if (taskInfo == null) {
                log.error("未找到任务缓存: taskId={}", taskId);
                return Message.error(command(), Status.C10001, "任务不存在或已过期");
            }

            String faceFeatureName = taskInfo.getFaceFeatureName();
            String tempDir = taskInfo.getTempDir();

            log.debug("收到文件分片: taskId={}, faceFeatureName={}, chunkIndex={}, size={}, lastChunk={}",
                    taskId, faceFeatureName, chunkIndex, data != null ? data.length : 0, lastChunk);

            // TODO: Client 端实现
            // 1. 保存分片到临时目录: tempDir + "/chunk_" + chunkIndex
            // 2. 如果是最后一片，可以合并文件

            // 计算进度
            int progress = (chunkIndex + 1) * 100 / totalChunks;

            // 返回响应
            FileChunkResponse response = new FileChunkResponse();
            response.setTaskId(taskId);
            response.setReceivedChunkIndex(chunkIndex);
            response.setSuccess(true);
            response.setProgress(progress);
            response.setStatus(Status.C10000, "分片接收成功");

            return response;
        });
    }
}
