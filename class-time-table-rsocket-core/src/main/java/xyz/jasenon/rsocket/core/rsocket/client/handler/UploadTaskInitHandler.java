package xyz.jasenon.rsocket.core.rsocket.client.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.cache.UploadTaskCache;
import xyz.jasenon.rsocket.core.packet.UploadTaskInitRequest;
import xyz.jasenon.rsocket.core.packet.UploadTaskInitResponse;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;

/**
 * 上传任务初始化处理器 - Client 端
 *
 * 处理 Server 发来的 INIT_UPLOAD_TASK 命令
 * Client 创建本地任务目录，缓存 taskId -> faceFeatureName 映射
 */
@Slf4j
@Component
public class UploadTaskInitHandler implements Handler {

    @Override
    public Command command() {
        return Command.INIT_UPLOAD_TASK;
    }

    @Override
    public Mono<Message> handle(Message message) {
        return Mono.fromCallable(() -> {
            if (!(message instanceof UploadTaskInitRequest)) {
                return Message.error(command(), Status.C10001, "消息类型错误");
            }

            UploadTaskInitRequest request = (UploadTaskInitRequest) message;
            String taskId = request.getTaskId();
            String faceFeatureName = request.getFaceFeatureName();
            int totalChunks = request.getTotalChunks();
            long totalSize = request.getTotalSize();

            log.info("收到上传任务初始化请求: taskId={}, faceFeatureName={}, totalChunks={}, totalSize={}",
                    taskId, faceFeatureName, totalChunks, totalSize);

            // TODO: Client 端实现
            // 1. 创建临时目录存储分片
            String tempDir = "/sdcard/face_upload/" + taskId; // Android 示例路径

            // 2. 缓存任务信息（关键：后续分片通过taskId查找）
            UploadTaskCache.put(taskId, new UploadTaskCache.TaskInfo(
                    faceFeatureName, totalChunks, totalSize, tempDir
            ));

            // 3. 返回响应
            UploadTaskInitResponse response = new UploadTaskInitResponse();
            response.setTaskId(taskId);
            response.setSuccess(true);
            response.setStatus(Status.C10000, "任务初始化成功");

            return response;
        });
    }
}
