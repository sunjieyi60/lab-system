package xyz.jasenon.rsocket.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.rsocket.core.model.FaceImageEntity;
import xyz.jasenon.rsocket.core.packet.FileChunkResponse;
import xyz.jasenon.rsocket.server.mapper.FaceImageMapper;
import xyz.jasenon.rsocket.server.service.FaceImageUploadService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/face-image")
@RequiredArgsConstructor
public class FaceImageController {

    private final FaceImageUploadService uploadService;
    private final FaceImageMapper faceImageMapper;

    /**
     * 1. 初始化分片上传任务
     * POST /api/face-image/upload/init
     *
     * @param uuid 班牌UUID
     * @param faceFeatureName 人脸特征名称
     * @param totalChunks 总分片数
     * @param totalSize 文件总大小
     * @return 任务ID
     */
    @PostMapping("/upload/init")
    public Mono<R<Map<String, String>>> initUpload(
            @RequestParam String uuid,
            @RequestParam String faceFeatureName,
            @RequestParam int totalChunks,
            @RequestParam long totalSize) {

        return uploadService.initUploadTask(uuid, faceFeatureName, totalChunks, totalSize)
                .map(taskId -> R.success(Map.of("taskId", taskId), "上传任务初始化成功"));
    }

    /**
     * 2. 分片上传
     * POST /api/face-image/upload/chunk
     * Content-Type: multipart/form-data
     *
     * @param uuid 班牌UUID
     * @param taskId 任务ID
     * @param chunk 分片文件
     * @param chunkIndex 分片索引
     * @param totalChunks 总分片数
     * @return 上传进度响应
     */
    @PostMapping("/upload/chunk")
    public Mono<R<FileChunkResponse>> uploadChunk(
            @RequestParam String uuid,
            @RequestParam String taskId,
            @RequestParam("chunk") MultipartFile chunk,
            @RequestParam int chunkIndex,
            @RequestParam int totalChunks) {

        return Mono.fromCallable(chunk::getBytes)
                .flatMap(bytes -> uploadService.uploadChunk(uuid, taskId, bytes, chunkIndex, totalChunks))
                .map(R::success);
    }

    /**
     * 3. 标记上传完成
     * POST /api/face-image/upload/complete
     *
     * @param uuid 班牌UUID
     * @param taskId 任务ID
     * @return 完成状态
     */
    @PostMapping("/upload/complete")
    public Mono<R<Void>> completeUpload(
            @RequestParam String uuid,
            @RequestParam String taskId) {

        return uploadService.completeUpload(uuid, taskId)
                .then(Mono.just(R.success(null, "上传完成")));
    }

    /**
     * 查询人脸库列表
     * GET /api/face-image/list/{uuid}
     */
    @GetMapping("/list/{uuid}")
    public R<List<FaceImageEntity>> listByUuid(@PathVariable String uuid) {
        List<FaceImageEntity> list = faceImageMapper.selectList(
                new LambdaQueryWrapper<FaceImageEntity>()
                        .eq(FaceImageEntity::getUuid, uuid)
                        .orderByDesc(FaceImageEntity::getCreateTime)
        );
        return R.success(list);
    }

    /**
     * 删除人脸
     * DELETE /api/face-image/{id}
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        faceImageMapper.deleteById(id);
        return R.success(null, "删除成功");
    }
}
