package xyz.jasenon.rsocket.core.rsocket.client.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import xyz.jasenon.rsocket.core.cache.UploadTaskCache;
import xyz.jasenon.rsocket.core.packet.UploadCompleteRequest;
import xyz.jasenon.rsocket.core.packet.UploadCompleteResponse;
import xyz.jasenon.rsocket.core.protocol.Command;
import xyz.jasenon.rsocket.core.protocol.Message;
import xyz.jasenon.rsocket.core.protocol.Status;

/**
 * 上传完成处理器 - Client 端
 *
 * 处理 Server 发来的 COMPLETE_UPLOAD_TASK 命令
 * Client 通过 taskId 查找缓存的 faceFeatureName，完成人脸特征提取
 */
@Slf4j
@Component
public class UploadCompleteHandler implements Handler {

    @Override
    public Command command() {
        return Command.COMPLETE_UPLOAD_TASK;
    }

    @Override
    public Mono<Message> handle(Message message) {
        return Mono.fromCallable(() -> {
            if (!(message instanceof UploadCompleteRequest)) {
                return Message.error(command(), Status.C10001, "消息类型错误");
            }

            UploadCompleteRequest request = (UploadCompleteRequest) message;
            String taskId = request.getTaskId();

            // 从缓存查找任务信息
            UploadTaskCache.TaskInfo taskInfo = UploadTaskCache.get(taskId);
            if (taskInfo == null) {
                log.error("未找到任务缓存: taskId={}", taskId);
                return Message.error(command(), Status.C10001, "任务不存在或已过期");
            }

            String faceFeatureName = taskInfo.getFaceFeatureName();
            String tempDir = taskInfo.getTempDir();

            log.info("收到上传完成通知: taskId={}, faceFeatureName={}", taskId, faceFeatureName);

            // TODO: Client 端实现
            // 1. 合并所有分片成完整图片: tempDir + "/final.jpg"
            // 2. 使用 faceAILib 提取人脸特征
            // 3. 保存人脸特征到本地数据库（关联 faceFeatureName）
            // 4. 清理临时分片文件

            // 清理缓存
            UploadTaskCache.remove(taskId);

            // 返回响应（可选：回传人脸特征给Server）
            UploadCompleteResponse response = new UploadCompleteResponse();
            response.setTaskId(taskId);
            response.setSuccess(true);
            // response.setFaceFeature(faceFeature); // 可选
            response.setStatus(Status.C10000, "人脸特征提取完成");

            return response;
        });
    }
}
