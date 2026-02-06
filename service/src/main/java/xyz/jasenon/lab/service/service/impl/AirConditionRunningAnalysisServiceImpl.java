package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.base.Dept;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.entity.device.AirCondition;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.record.AirConditionRecord;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.analysis.AirConditionRunningQueryDto;
import xyz.jasenon.lab.service.mapper.DeptMapper;
import xyz.jasenon.lab.service.mapper.LaboratoryMapper;
import xyz.jasenon.lab.service.mapper.record.AirConditionMapper;
import xyz.jasenon.lab.service.mapper.record.AirConditionRecordMapper;
import xyz.jasenon.lab.service.service.IAirConditionRunningAnalysisService;
import xyz.jasenon.lab.service.strategy.device.ex.AirConditionQ;
import xyz.jasenon.lab.service.vo.AirConditionRunningChartSegmentVo;
import xyz.jasenon.lab.service.vo.AirConditionRunningResultVo;
import xyz.jasenon.lab.service.vo.AirConditionRunningRowVo;
import xyz.jasenon.lab.service.vo.AirConditionRunningSummaryVo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 空调运行时长统计：遍历 Redis 统计在线状态，开启时间区间从 DB 取并切分到查询区间，
 * 每个设备起线程任务，线程安全 List 拼接结果。
 */
@Slf4j
@Service
public class AirConditionRunningAnalysisServiceImpl implements IAirConditionRunningAnalysisService {

    private static final String REDIS_KEY_PATTERN = "AirCondition:Record:*";
    /** 与前端表格一致：日期与时间用 - 连接，小时不补零，如 2025-09-01-8:00 至 2025-09-01-21:00 */
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd-H:mm");
    private static final int PARALLEL_THREADS = 8;

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private AirConditionMapper airConditionMapper;
    @Autowired
    private AirConditionRecordMapper airConditionRecordMapper;
    @Autowired
    private LaboratoryMapper laboratoryMapper;
    @Autowired
    private DeptMapper deptMapper;
    @Autowired
    private AirConditionQ airConditionQ;

    private final ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_THREADS);

    @Override
    public R<AirConditionRunningResultVo> getRunningStats(AirConditionRunningQueryDto query) {
        if (query.getStartTime() == null || query.getEndTime() == null || !query.getStartTime().isBefore(query.getEndTime())) {
            return R.fail("请填写有效的开始、结束时间");
        }
        List<Long> labIds = resolveLabIds(query);
        List<AirCondition> devices = listDevices(labIds, query.getDeviceIds());
        if (devices.isEmpty()) {
            return R.success(buildEmptyResult(query), "无符合条件的空调设备");
        }

        Map<Long, AirConditionRecord> redisStateByDeviceId = traverseRedisForOnlineState();
        Map<Long, Laboratory> labMap = loadLabMap(devices);
        Map<Long, String> deptNameMap = loadDeptNameMap(labMap);

        String timeRangeStr = formatTimeRange(query.getStartTime(), query.getEndTime());
        LocalDateTime queryStart = query.getStartTime();
        LocalDateTime queryEnd = query.getEndTime();

        ConcurrentLinkedQueue<AirConditionRunningRowVo> resultQueue = new ConcurrentLinkedQueue<>();
        List<AirCondition> deviceList = devices;
        List<Callable<Void>> tasks = deviceList.stream().map(device -> (java.util.concurrent.Callable<Void>) () -> {
            try {
                double hours = computeRunningHours(device, queryStart, queryEnd, redisStateByDeviceId.get(device.getId()));
                if (hours <= 0) return null;
                Laboratory lab = labMap.get(device.getBelongToLaboratoryId());
                String deptName = lab != null && lab.getBelongToDepts() != null && !lab.getBelongToDepts().isEmpty()
                        ? deptNameMap.getOrDefault(lab.getBelongToDepts().get(0), "")
                        : "";
                AirConditionRunningRowVo row = new AirConditionRunningRowVo()
                        .setTimeRange(timeRangeStr)
                        .setLaboratoryNo(lab != null ? lab.getLaboratoryId() : "")
                        .setDeptName(deptName)
                        .setAcUnitName(device.getDeviceName() != null ? device.getDeviceName() : ("设备" + device.getId()))
                        .setDurationHours(BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP));
                resultQueue.add(row);
            } catch (Exception e) {
                log.warn("空调运行时长统计单设备异常 deviceId={}", device.getId(), e);
            }
            return null;
        }).toList();
        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("空调运行时长统计被中断", e);
        }

        List<AirConditionRunningRowVo> list = new ArrayList<>(resultQueue);
        BigDecimal totalHours = list.stream()
                .map(AirConditionRunningRowVo::getDurationHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalHours.compareTo(BigDecimal.ZERO) == 0) {
            return R.success(buildEmptyResult(query), "该时间范围内无运行记录");
        }
        for (AirConditionRunningRowVo row : list) {
            BigDecimal p = row.getDurationHours().divide(totalHours, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            row.setProportion(p.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%");
        }
        List<AirConditionRunningChartSegmentVo> chartSegments = list.stream()
                .map(r -> new AirConditionRunningChartSegmentVo()
                        .setName(r.getAcUnitName())
                        .setHours(r.getDurationHours())
                        .setProportion(r.getDurationHours().divide(totalHours, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(1, RoundingMode.HALF_UP)))
                        .collect(Collectors.toList());

        AirConditionRunningSummaryVo summaryRow = buildSummaryRow(timeRangeStr, list, totalHours);

        AirConditionRunningResultVo vo = new AirConditionRunningResultVo()
                .setTotalHours(totalHours)
                .setList(list)
                .setChartSegments(chartSegments)
                .setSummaryRow(summaryRow);
        return R.success(vo);
    }

    /**
     * 遍历 Redis 统计在线状态：key 为 AirCondition:Record:{deviceId}，值为当前一条记录（含 createTime、isOpen）。
     */
    private Map<Long, AirConditionRecord> traverseRedisForOnlineState() {
        Map<Long, AirConditionRecord> map = new HashMap<>();
        try {
            Iterable<String> keysIter = redissonClient.getKeys().getKeysByPattern(REDIS_KEY_PATTERN);
            String prefix = DeviceType.AirCondition.getRedisPrefix();
            for (String key : keysIter) {
                String suffix = key.substring(prefix.length());
                try {
                    long deviceId = Long.parseLong(suffix);
                    RBucket<AirConditionRecord> bucket = redissonClient.getBucket(key);
                    AirConditionRecord record = bucket.get();
                    if (record != null) {
                        map.put(deviceId, record);
                    }
                } catch (NumberFormatException ignored) {

                }
            }
        } catch (Exception e) {
            log.warn("遍历 Redis 空调在线状态异常", e);
        }
        return map;
    }

    /**
     * 从 DB 按 device_id 取该设备在 [queryStart, queryEnd] 内的记录，
     * 合并 Redis 当前开启状态，得到开启时间区间并切分到查询区间内，汇总时长（小时）。
     */
    private double computeRunningHours(AirCondition device, LocalDateTime queryStart, LocalDateTime queryEnd, AirConditionRecord redisRecord) {
        List<AirConditionRecord> records = airConditionRecordMapper.selectList(
                new LambdaQueryWrapper<AirConditionRecord>()
                        .eq(AirConditionRecord::getDeviceId, device.getId())
                        .ge(AirConditionRecord::getCreateTime, queryStart)
                        .le(AirConditionRecord::getCreateTime, queryEnd)
                        .orderByAsc(AirConditionRecord::getCreateTime)
        );
        List<double[]> intervals = buildOpenIntervals(records, queryStart, queryEnd, redisRecord);
        return clipAndSumHours(intervals, queryStart, queryEnd);
    }

    /**
     * 根据有序记录构建开启区间 [startSeconds, endSeconds]（相对某个基准的秒数或直接用 LocalDateTime）。
     * 为简单用时间戳秒数计算时长。
     */
    private List<double[]> buildOpenIntervals(List<AirConditionRecord> records, LocalDateTime queryStart, LocalDateTime queryEnd, AirConditionRecord redisRecord) {
        List<double[]> out = new ArrayList<>();
        if (records.isEmpty()) {
        if (redisRecord != null && Boolean.TRUE.equals(redisRecord.getIsOpen()) && redisRecord.getCreateTime() != null) {
            LocalDateTime start = redisRecord.getCreateTime().isBefore(queryStart) ? queryStart : redisRecord.getCreateTime();
            LocalDateTime end = queryEnd;
            if (start.isBefore(end)) {
                out.add(toSecondsPair(start, end));
            }
        }
            return out;
        }
        LocalDateTime intervalStart = null;
        for (AirConditionRecord r : records) {
            if (Boolean.TRUE.equals(r.getIsOpen())) {
                if (intervalStart == null) {
                    intervalStart = queryStart;
                }
            } else {
                if (intervalStart != null) {
                    out.add(toSecondsPair(intervalStart, r.getCreateTime()));
                    intervalStart = null;
                }
            }
        }
        if (intervalStart != null) {
            out.add(toSecondsPair(intervalStart, queryEnd));
        }
        if (redisRecord != null && Boolean.TRUE.equals(redisRecord.getIsOpen()) && redisRecord.getCreateTime() != null) {
            boolean covered = out.stream().anyMatch(p -> p[1] >= toSeconds(redisRecord.getCreateTime()));
            if (!covered) {
                LocalDateTime start = redisRecord.getCreateTime().isBefore(queryStart) ? queryStart : redisRecord.getCreateTime();
                out.add(toSecondsPair(start, queryEnd));
            }
        }
        return out;
    }

    private static double toSeconds(LocalDateTime t) {
        return t.atZone(ZoneId.systemDefault()).toEpochSecond() + t.getNano() / 1e9;
    }

    private static double[] toSecondsPair(LocalDateTime start, LocalDateTime end) {
        return new double[]{toSeconds(start), toSeconds(end)};
    }

    private static double clipAndSumHours(List<double[]> intervals, LocalDateTime queryStart, LocalDateTime queryEnd) {
        double qs = toSeconds(queryStart), qe = toSeconds(queryEnd);
        double sum = 0;
        for (double[] p : intervals) {
            double s = Math.max(p[0], qs);
            double e = Math.min(p[1], qe);
            if (s < e) sum += (e - s) / 3600.0;
        }
        return sum;
    }

    private List<Long> resolveLabIds(AirConditionRunningQueryDto query) {
        List<Laboratory> labs;
        if (query.getBuildingId() != null) {
            labs = laboratoryMapper.selectList(new LambdaQueryWrapper<Laboratory>()
                    .eq(Laboratory::getBelongToBuilding, query.getBuildingId()));
        } else {
            labs = laboratoryMapper.selectList(null);
        }
        if (query.getDeptId() != null && !labs.isEmpty()) {
            labs = labs.stream()
                    .filter(l -> l.getBelongToDepts() != null && l.getBelongToDepts()
                            .contains(query.getDeptId())).toList();
        }
        if (query.getLaboratoryIds() != null && !query.getLaboratoryIds().isEmpty()) {
            Set<Long> set = new HashSet<>(query.getLaboratoryIds());
            labs = labs.stream().filter(l -> set.contains(l.getId())).toList();
        }
        return labs.stream().map(Laboratory::getId).toList();
    }

    private List<AirCondition> listDevices(List<Long> labIds, List<Long> deviceIds) {
        if (labIds.isEmpty()) return List.of();
        List<AirCondition> list = airConditionQ.list(labIds);
        if (deviceIds != null && !deviceIds.isEmpty()) {
            Set<Long> set = new HashSet<>(deviceIds);
            list = list.stream().filter(d -> set.contains(d.getId())).toList();
        }
        return list;
    }

    private Map<Long, Laboratory> loadLabMap(List<AirCondition> devices) {
        Set<Long> labIds = devices.stream()
                .map(AirCondition::getBelongToLaboratoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (labIds.isEmpty()) return Map.of();
        List<Laboratory> labs = laboratoryMapper.selectBatchIds(labIds);
        return labs.stream().collect(Collectors.toMap(Laboratory::getId, l -> l, (a, b) -> a));
    }

    private Map<Long, String> loadDeptNameMap(Map<Long, Laboratory> labMap) {
        Set<Long> deptIds = new HashSet<>();
        for (Laboratory lab : labMap.values()) {
            if (lab.getBelongToDepts() != null) deptIds.addAll(lab.getBelongToDepts());
        }
        if (deptIds.isEmpty()) return Map.of();
        List<Dept> depts = deptMapper.selectBatchIds(deptIds);
        return depts.stream()
                .collect(Collectors.toMap(Dept::getId, Dept::getDeptName, (a, b) -> a));
    }

    private String formatTimeRange(LocalDateTime start, LocalDateTime end) {
        return start.format(FMT) + " 至 " + end.format(FMT);
    }

    private AirConditionRunningSummaryVo buildSummaryRow(String timeRangeStr, List<AirConditionRunningRowVo> list, BigDecimal totalHours) {
        String laboratoryNos = list.stream()
                .map(AirConditionRunningRowVo::getLaboratoryNo)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.joining("、"));
        String deptName = list.isEmpty() ? "" : (list.get(0).getDeptName() != null ? list.get(0).getDeptName() : "");
        String acUnitSummary = list.isEmpty() ? "全部"
                : list.stream().map(AirConditionRunningRowVo::getAcUnitName).filter(Objects::nonNull).min(String::compareTo)
                        .map(s -> s + "...").orElse("全部");
        return new AirConditionRunningSummaryVo()
                .setTimeRange(timeRangeStr)
                .setLaboratoryNos(laboratoryNos.isEmpty() ? "" : laboratoryNos)
                .setDeptName(deptName)
                .setAcUnitSummary(acUnitSummary)
                .setTotalHours(totalHours)
                .setProportion("100%");
    }

    private AirConditionRunningResultVo buildEmptyResult(AirConditionRunningQueryDto query) {
        return new AirConditionRunningResultVo()
                .setTotalHours(BigDecimal.ZERO)
                .setList(List.of())
                .setChartSegments(List.of())
                .setSummaryRow(null);
    }
}
