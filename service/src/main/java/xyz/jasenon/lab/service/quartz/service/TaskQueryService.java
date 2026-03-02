package xyz.jasenon.lab.service.quartz.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import xyz.jasenon.lab.common.command.CommandLine;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.service.quartz.mapper.ActionMapper;
import xyz.jasenon.lab.service.quartz.model.ScheduleConfigRoot;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskQueryService {

    private final ActionMapper actionMapper;
    private final ConfigBatchLoader configBatchLoader;

    /**
     * 高性能查询：实验室维度 + 设备/指令筛选
     *
     * @param labId         实验室ID（必填）
     * @param enable        启用状态筛选（可选）
     * @param deviceType    设备类型筛选（可选）
     * @param deviceId      设备ID筛选（可选）
     * @param commandLine   指令筛选（可选）
     * @param pageNum       页码
     * @param pageSize      每页大小
     */
    public Page<ScheduleConfigRoot> queryByLaboratory(Long labId,
                                                       Boolean enable,
                                                       DeviceType deviceType,
                                                       Long deviceId,
                                                       CommandLine commandLine,
                                                       int pageNum,
                                                       int pageSize) {

        // 步骤1：快速筛选候选任务ID（走索引，性能最优）
        Set<String> candidateTaskIds = actionMapper.selectTaskIdsByFilter(
            labId, enable, deviceType, deviceId, commandLine
        );

        if (CollectionUtils.isEmpty(candidateTaskIds)) {
            return new Page<>(pageNum, pageSize, 0);
        }

        // 如果交集后为空，直接返回
        if (candidateTaskIds.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }

        // 步骤3：分页处理
        List<String> sortedIds = candidateTaskIds.stream()
            .sorted(Collections.reverseOrder())
            .collect(Collectors.toList());

        long total = sortedIds.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, sortedIds.size());

        if (fromIndex >= sortedIds.size()) {
            return new Page<>(pageNum, pageSize, total);
        }

        List<String> pageIds = sortedIds.subList(fromIndex, toIndex);

        // 步骤4：批量组装完整配置
        List<ScheduleConfigRoot> records = configBatchLoader.batchLoadByTaskIds(pageIds);

        // 按原始ID顺序排序
        Map<String, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < pageIds.size(); i++) {
            orderMap.put(pageIds.get(i), i);
        }
        records.sort(Comparator.comparingInt(r -> orderMap.get(r.getTask().getId())));

        Page<ScheduleConfigRoot> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(records);
        return page;
    }
}