package xyz.jasenon.rsocket.core.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上传任务缓存 - Client 端使用
 *
 * 缓存 taskId 与任务信息的映射，避免每次分片都传输完整信息
 */
public class UploadTaskCache {

    /**
     * 任务信息
     */
    public static class TaskInfo {
        private final String faceFeatureName;
        private final int totalChunks;
        private final long totalSize;
        private final String tempDir;

        public TaskInfo(String faceFeatureName, int totalChunks, long totalSize, String tempDir) {
            this.faceFeatureName = faceFeatureName;
            this.totalChunks = totalChunks;
            this.totalSize = totalSize;
            this.tempDir = tempDir;
        }

        public String getFaceFeatureName() { return faceFeatureName; }
        public int getTotalChunks() { return totalChunks; }
        public long getTotalSize() { return totalSize; }
        public String getTempDir() { return tempDir; }
    }

    private static final Map<String, TaskInfo> TASK_CACHE = new ConcurrentHashMap<>();

    /**
     * 缓存任务信息
     */
    public static void put(String taskId, TaskInfo taskInfo) {
        TASK_CACHE.put(taskId, taskInfo);
    }

    /**
     * 获取任务信息
     */
    public static TaskInfo get(String taskId) {
        return TASK_CACHE.get(taskId);
    }

    /**
     * 移除任务信息
     */
    public static void remove(String taskId) {
        TASK_CACHE.remove(taskId);
    }

    /**
     * 检查任务是否存在
     */
    public static boolean contains(String taskId) {
        return TASK_CACHE.containsKey(taskId);
    }
}
