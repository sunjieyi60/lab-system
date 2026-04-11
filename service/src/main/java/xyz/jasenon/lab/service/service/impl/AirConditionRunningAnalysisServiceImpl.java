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
import xyz.jasenon.lab.service.vo.analysis.AirConditionRunningChartSegmentVo;
import xyz.jasenon.lab.service.vo.analysis.AirConditionRunningResultVo;
import xyz.jasenon.lab.service.vo.analysis.AirConditionRunningRowVo;
import xyz.jasenon.lab.service.vo.analysis.AirConditionRunningSummaryVo;

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
 * 空调运行时长统计：以 MySQL 历史开关记录为主，可选用 Redis 补段（条件收紧、有时长上限）。
 * 区间内无 MySQL 记录时不使用 Redis 推导，该设备时长计为 0。
 */
@Slf4j
@Service
public class AirConditionRunningAnalysisServiceImpl implements IAirConditionRunningAnalysisService {

    private static final String REDIS_KEY_PATTERN = "AirCondition:Record:*";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int PARALLEL_THREADS = 8;
    /** Redis 补段单段最长小时数，防止单设备拉高整段统计 */
    private static final double REDIS_SEGMENT_MAX_HOURS = 24.0;

    /** 单设备运行时长及数据来源标记 */
    private static final class RunningHoursResult {
        final double hours;
        final boolean hadMysqlData;
        final boolean usedRedisSegment;

        RunningHoursResult(double hours, boolean hadMysqlData, boolean usedRedisSegment) {
            this.hours = hours;
            this.hadMysqlData = hadMysqlData;
            this.usedRedisSegment = usedRedisSegment;
        }
    }

    /** 开启区间列表 + 是否使用了 Redis 补段 */
    private static final class IntervalsResult {
        final List<double[]> intervals;
        final boolean usedRedisSegment;

        IntervalsResult(List<double[]> intervals, boolean usedRedisSegment) {
            this.intervals = intervals;
            this.usedRedisSegment = usedRedisSegment;
        }
    }

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
    public AirConditionRunningResultVo getRunningStats(AirConditionRunningQueryDto query) {
        if (query.getStartTime() == null || query.getEndTime() == null || !query.getStartTime().isBefore(query.getEndTime())) {
            throw R.fail("请填写有效的开始、结束时间").convert();
        }
        List<Long> labIds = resolveLabIds(query);
        List<AirCondition> devices = listDevices(labIds, query.getDeviceIds());
        if (devices.isEmpty()) {
            return buildEmptyResult(query);
        }

        Map<Long, AirConditionRecord> redisStateByDeviceId = traverseRedisForOnlineState();
        Map<Long, Laboratory> labMap = loadLabMap(devices);
        Map<Long, String> deptNameMap = loadDeptNameMap(labMap);

        String timeRangeStr = formatTimeRange(query.getStartTime(), query.getEndTime());
        LocalDateTime queryStart = query.getStartTime();
        LocalDateTime queryEnd = query.getEndTime();

        ConcurrentLinkedQueue<AirConditionRunningRowVo> resultQueue = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Boolean> noDataFlags = new ConcurrentLinkedQueue<>();
        List<AirCondition> deviceList = devices;
        List<Callable<Void>> tasks = deviceList.stream().map(device -> (java.util.concurrent.Callable<Void>) () -> {
            try {
                RunningHoursResult res = computeRunningHours(device, queryStart, queryEnd, redisStateByDeviceId.get(device.getId()));
                if (!res.hadMysqlData) {
                    noDataFlags.add(true);
                }
                if (res.hours <= 0) return null;
                Laboratory lab = labMap.get(device.getBelongToLaboratoryId());
                String deptName = lab != null && lab.getBelongToDepts() != null && !lab.getBelongToDepts().isEmpty()
                        ? deptNameMap.getOrDefault(lab.getBelongToDepts().get(0), "")
                        : "";
                AirConditionRunningRowVo row = new AirConditionRunningRowVo()
                        .setTimeRange(timeRangeStr)
                        .setLaboratoryNo(lab != null ? lab.getLaboratoryId() : "")
                        .setDeptName(deptName)
                        .setAcUnitName(device.getDeviceName() != null ? device.getDeviceName() : ("设备" + device.getId()))
                        .setDurationHours(BigDecimal.valueOf(res.hours).setScale(2, RoundingMode.HALF_UP))
                        .setDataSource(res.usedRedisSegment ? "mysql_and_redis" : "mysql");
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
            return buildEmptyResult(query);
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

        String warningMessage = noDataFlags.isEmpty() ? null : "部分设备该时段无历史开关记录，未计入统计";

        AirConditionRunningResultVo vo = new AirConditionRunningResultVo()
                .setTotalHours(totalHours)
                .setList(list)
                .setChartSegments(chartSegments)
                .setSummaryRow(summaryRow)
                .setWarningMessage(warningMessage);
        return vo;
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
     * 从 DB 取该设备在 [queryStart, queryEnd] 内的记录，构建开启区间并汇总时长。
     * 如果查询时间段内最后一条记录是开启状态，会查 Redis 确认当前状态。
     * 如果查询时间段内无 MySQL 记录，会查 Redis，如果 Redis 是开启状态则从 queryStart 到 queryEnd 算时长。
     */
    private RunningHoursResult computeRunningHours(AirCondition device, LocalDateTime queryStart, LocalDateTime queryEnd, AirConditionRecord redisRecord) {
        List<AirConditionRecord> records = airConditionRecordMapper.selectList(
                new LambdaQueryWrapper<AirConditionRecord>()
                        .eq(AirConditionRecord::getDeviceId, device.getId())
                        .ge(AirConditionRecord::getCreateTime, queryStart)
                        .le(AirConditionRecord::getCreateTime, queryEnd)
                        .orderByAsc(AirConditionRecord::getCreateTime)
        );
        IntervalsResult ir = buildOpenIntervals(records, queryStart, queryEnd, redisRecord);
        double hours = clipAndSumHours(ir.intervals, queryStart, queryEnd);
        return new RunningHoursResult(hours, !records.isEmpty(), ir.usedRedisSegment);
    }

    /**
     * 根据有序记录构建开启区间。
     * 1. 如果查询时间段内无 MySQL 记录，查 Redis：如果 Redis 是开启状态，从 queryStart（或 Redis 记录的 createTime）到 queryEnd 算时长。
     * 2. 如果最后一条记录是开启状态，查 Redis 确认当前状态：
     *    - 如果 Redis 是开启或没有数据，从最后一条开启记录的时间到 MySQL 中最新的一条开启记录的时间点算时长
     *    - 如果 Redis 是关闭，从最后一条开启记录的时间到 Redis 记录的关闭时间算时长（如果关闭时间在查询区间内）
     */
    private IntervalsResult buildOpenIntervals(List<AirConditionRecord> records, LocalDateTime queryStart, LocalDateTime queryEnd, AirConditionRecord redisRecord) {
        List<double[]> out = new ArrayList<>();
        boolean usedRedisSegment = false;
        
        // 情况1：查询时间段内无 MySQL 记录，查 Redis
        if (records.isEmpty()) {
            if (redisRecord != null && Boolean.TRUE.equals(redisRecord.getIsOpen()) && redisRecord.getCreateTime() != null) {
                LocalDateTime start = redisRecord.getCreateTime().isBefore(queryStart) ? queryStart : redisRecord.getCreateTime();
                if (start.isBefore(queryEnd)) {
                    out.add(toSecondsPair(start, queryEnd));
                    usedRedisSegment = true;
                }
            }
            return new IntervalsResult(out, usedRedisSegment);
        }
        
        // 情况2：有 MySQL 记录，遍历构建开启区间
        LocalDateTime intervalStart = null;
        LocalDateTime lastOpenRecordTime = null; // 记录最后一条开启记录的时间
        for (AirConditionRecord r : records) {
            if (Boolean.TRUE.equals(r.getIsOpen())) {
                if (intervalStart == null && r.getCreateTime() != null) {
                    intervalStart = r.getCreateTime();
                }
                if (r.getCreateTime() != null) {
                    lastOpenRecordTime = r.getCreateTime(); // 更新最后一条开启记录的时间
                }
            } else {
                if (intervalStart != null && r.getCreateTime() != null) {
                    out.add(toSecondsPair(intervalStart, r.getCreateTime()));
                    intervalStart = null;
                }
            }
        }
        
        // 情况3：最后一条记录是开启状态，查 Redis 确认当前状态
        if (intervalStart != null && lastOpenRecordTime != null) {
            if (redisRecord != null && redisRecord.getCreateTime() != null) {
                LocalDateTime redisTime = redisRecord.getCreateTime();
                if (Boolean.TRUE.equals(redisRecord.getIsOpen())) {
                    // Redis 是开启状态：从最后一条开启记录的时间到 MySQL 中最新的一条开启记录的时间点
                    out.add(toSecondsPair(intervalStart, lastOpenRecordTime));
                    usedRedisSegment = true;
                } else {
                    // Redis 是关闭状态：从最后一条开启记录的时间到 Redis 记录的关闭时间（如果关闭时间在查询区间内）
                    if (!redisTime.isBefore(queryStart) && !redisTime.isAfter(queryEnd) && redisTime.isAfter(intervalStart)) {
                        out.add(toSecondsPair(intervalStart, redisTime));
                    } else {
                        // Redis 关闭时间不在查询区间内，只算到 MySQL 中最新的一条开启记录的时间点
                        out.add(toSecondsPair(intervalStart, lastOpenRecordTime));
                    }
                    usedRedisSegment = true;
                }
            } else {
                // Redis 没有数据：从最后一条开启记录的时间到 MySQL 中最新的一条开启记录的时间点
                out.add(toSecondsPair(intervalStart, lastOpenRecordTime));
            }
        }
        
        return new IntervalsResult(out, usedRedisSegment);
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
        if (query.getDeptId() != null && query.getDeptId() != 0 && !labs.isEmpty()) {
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
                .setSummaryRow(null)
                .setWarningMessage("该时间范围内无运行记录，仅基于 MySQL 历史开关数据统计");
    }
}
