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
     * 初始化分片上传任务
     * <p>
     * 创建一个新的分片上传任务，生成任务ID并通知班牌设备准备接收数据。
     * 这是分片上传流程的第一步。
     * </p>
     *
     * @param uuid 班牌设备UUID，用于标识目标设备
     * @param faceFeatureName 人脸特征名称，用于标识该人脸数据
     * @param totalChunks 总分片数，表示文件将被分割成多少个分片
     * @param totalSize 文件总大小（字节）
     * @return 包含任务ID的统一响应，任务ID用于后续分片上传
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
     * 分片上传
     * <p>
     * 上传单个分片数据到班牌设备。支持 multipart/form-data 格式。
     * 这是分片上传流程的第二步，需要按顺序上传所有分片。
     * </p>
     *
     * @param uuid 班牌设备UUID
     * @param taskId 上传任务ID，由初始化接口返回
     * @param chunk 分片文件数据，multipart/form-data 格式
     * @param chunkIndex 当前分片索引，从0开始
     * @param totalChunks 总分片数
     * @return 包含上传进度信息的响应
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
     * 标记上传完成
     * <p>
     * 通知班牌设备所有分片已上传完毕，触发设备端的人脸特征提取和保存。
     * 这是分片上传流程的最后一步。
     * </p>
     *
     * @param uuid 班牌设备UUID
     * @param taskId 上传任务ID
     * @return 操作完成状态的响应
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
     * <p>
     * 获取指定班牌设备的人脸图像数据列表，按创建时间倒序排列。
     * </p>
     *
     * @param uuid 班牌设备UUID
     * @return 该设备关联的人脸图像实体列表
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
     * 删除人脸数据
     * <p>
     * 根据ID删除指定的人脸图像记录。
     * </p>
     *
     * @param id 人脸图像记录ID
     * @return 删除成功响应
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        faceImageMapper.deleteById(id);
        return R.success(null, "删除成功");
    }
}
