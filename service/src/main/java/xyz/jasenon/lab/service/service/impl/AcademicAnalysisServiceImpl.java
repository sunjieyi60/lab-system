package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.analysis.AnalysisQueryDto;
import xyz.jasenon.lab.service.mapper.AnalysisMapper;
import xyz.jasenon.lab.service.mapper.LaboratoryMapper;
import xyz.jasenon.lab.service.service.IAcademicAnalysisService;
import xyz.jasenon.lab.service.vo.analysis.AnalysisChartVo;
import xyz.jasenon.lab.service.vo.analysis.DimensionPointVo;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据分析服务实现：为前端图表提供课程数、学时数、人学时数及按周次/星期/节次的分布数据。
 */
@Service
public class AcademicAnalysisServiceImpl implements IAcademicAnalysisService {

    // 周次维度：最小周
    private static final int WEEK_MIN = 1;
    // 周次维度：最大周
    private static final int WEEK_MAX = 18;
    // 星期维度展示文案
    private static final String[] WEEKDAY_LABELS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    // 节次维度展示文案
    private static final String[] SECTION_LABELS = {"1-2", "3-4", "5-6", "7-8", "9-10", "11-12"};
    // Redis 图表缓存 Key 前缀，完整 Key 为 prefix + semesterId:deptId:labIds
    private static final String CACHE_KEY_PREFIX = "analysis:chart:";
    // 图表缓存过期时间
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    @Autowired
    private AnalysisMapper analysisMapper;
    @Autowired
    private LaboratoryMapper laboratoryMapper;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 按筛选条件获取图表数据：先查 Redis 缓存，未命中则从 DB 聚合后写入缓存并返回。
     * @param query 学年学期、楼栋、部门、实验室（均为可选）
     * @return 总数（课程数/学时数/人学时数）及 byWeek / byWeekday / bySection 分布
     */
    @Override
    public R<AnalysisChartVo> getChartData(AnalysisQueryDto query) {
        List<Long> labIds = resolveLabIds(query);
        String cacheKey = CACHE_KEY_PREFIX + buildCacheKey(query.getSemesterId(), query.getDeptId(), labIds);

        RBucket<String> bucket = redissonClient.getBucket(cacheKey);
        String cached = bucket.get();
        if (cached != null && !cached.isEmpty()) {
            try {
                AnalysisChartVo vo = objectMapper.readValue(cached, AnalysisChartVo.class);
                return R.success(vo, "获取成功(缓存)");
            } catch (JsonProcessingException e) {
                // 反序列化失败则忽略缓存，继续查库
            }
        }

        AnalysisChartVo vo = aggregateAndBuildVo(query.getSemesterId(), query.getDeptId(), labIds);
        try {
            bucket.set(objectMapper.writeValueAsString(vo), CACHE_TTL);
        } catch (JsonProcessingException e) {
            // 缓存写入失败不影响返回
        }
        return R.success(vo, labIds != null && labIds.isEmpty() ? "无数据" : "获取成功");
    }

    /**
     * 使所有图表缓存失效。排课/课程表增删改后调用，保证下次图表请求从 DB 重新聚合。
     * 清理失败不抛异常，不影响主流程。
     */
    @Override
    public void invalidateChartCache() {
        try {
            redissonClient.getKeys().deleteByPattern(CACHE_KEY_PREFIX + "*");
        } catch (Exception e) {
            // 缓存清理失败不影响业务，仅记录或忽略
        }
    }

    /**
     * 根据查询条件解析实验室 ID 列表：楼栋优先，再与 laboratoryIds 取交集；无指定时返回 null（表示不按实验室过滤）。
     * @param query 含 buildingId、laboratoryIds
     * @return 实验室 ID 列表，或 null；空列表表示有楼栋/实验室条件但无匹配实验室
     */
    private List<Long> resolveLabIds(AnalysisQueryDto query) {
        List<Long> labIds = null;
        if (query.getBuildingId() != null) {
            List<Laboratory> labs = laboratoryMapper.selectList(
                    new LambdaQueryWrapper<Laboratory>()
                            .eq(Laboratory::getBelongToBuilding, query.getBuildingId())
            );
            labIds = labs.stream().map(Laboratory::getId).toList();
            if (labIds.isEmpty()) {
                return labIds;
            }
        }
        if (query.getLaboratoryIds() != null && !query.getLaboratoryIds().isEmpty()) {
            labIds = labIds == null ? query.getLaboratoryIds() : labIds.stream()
                    .filter(query.getLaboratoryIds()::contains)
                    .toList();
        }
        return labIds;
    }

    /** 生成缓存 Key 后缀：semesterId:deptId:labIds（null 或空用 "-" 表示） */
    private String buildCacheKey(Long semesterId, Long deptId, List<Long> labIds) {
        String labStr = labIds == null || labIds.isEmpty()
                ? "-"
                : labIds.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
        return (semesterId != null ? semesterId : "-") + ":"
                + (deptId != null ? deptId : "-") + ":"
                + labStr;
    }

    /**
     * 从 DB 聚合三维度数据并补齐为完整维度（1-18 周、周一～周日、6 个节次），缺失项填 0。
     * @param semesterId 学期 ID，可为 null
     * @param deptId     部门 ID，可为 null
     * @param labIds     实验室 ID 列表，空列表时直接返回空图表
     */
    private AnalysisChartVo aggregateAndBuildVo(Long semesterId, Long deptId, List<Long> labIds) {
        if (labIds != null && labIds.isEmpty()) {
            return emptyChartVo();
        }

        List<DimensionPointVo> weekRows = analysisMapper.aggByWeek(semesterId, deptId, labIds);
        List<DimensionPointVo> weekdayRows = analysisMapper.aggByWeekday(semesterId, deptId, labIds);
        List<DimensionPointVo> sectionRows = analysisMapper.aggBySectionSlot(semesterId, deptId, labIds);

        // label 可能为 Integer（周次/星期/节次下标）或 String，统一转为 Integer 作为 map key
        Map<Integer, long[]> byWeek = toMap(weekRows, obj -> obj instanceof Number ? ((Number) obj).intValue() : 0);
        Map<Integer, long[]> byWeekday = toMap(weekdayRows, obj -> obj instanceof Number ? ((Number) obj).intValue() : 0);
        Map<Integer, long[]> bySection = toMap(sectionRows, obj -> obj instanceof Number ? ((Number) obj).intValue() : 0);

        long totalCourseCount = 0;
        long totalSectionCount = 0;
        long totalStudentSectionCount = 0;
        for (long[] v : byWeek.values()) {
            totalCourseCount += v[0];
            totalSectionCount += v[1];
            totalStudentSectionCount += v[2];
        }

        return new AnalysisChartVo()
                .setTotalCourseCount(totalCourseCount)
                .setTotalSectionCount(totalSectionCount)
                .setTotalStudentSectionCount(totalStudentSectionCount)
                .setByWeek(fullByWeek(byWeek))
                .setByWeekday(fullByWeekday(byWeekday))
                .setBySection(fullBySection(bySection));
    }

    /** 将聚合结果转为 dimension -> [courseCount, sectionCount, studentSectionCount]，label 经 labelToInt 转为 key */
    private static Map<Integer, long[]> toMap(List<DimensionPointVo> rows, java.util.function.Function<Object, Integer> labelToInt) {
        Map<Integer, long[]> map = new HashMap<>();
        for (DimensionPointVo row : rows) {
            if (row == null) continue;
            Integer k = labelToInt.apply(row.getLabel());
            long courseCount = row.getCourseCount() != null ? row.getCourseCount() : 0L;
            long sectionCount = row.getSectionCount() != null ? row.getSectionCount() : 0L;
            long studentSectionCount = row.getStudentSectionCount() != null ? row.getStudentSectionCount() : 0L;
            map.put(k, new long[]{courseCount, sectionCount, studentSectionCount});
        }
        return map;
    }

    /** 返回全 0 的图表 VO（无实验室或无匹配实验室时使用） */
    private static AnalysisChartVo emptyChartVo() {
        Map<Integer, long[]> empty = new HashMap<>();
        return new AnalysisChartVo()
                .setTotalCourseCount(0L)
                .setTotalSectionCount(0L)
                .setTotalStudentSectionCount(0L)
                .setByWeek(fullByWeek(empty))
                .setByWeekday(fullByWeekday(empty))
                .setBySection(fullBySection(empty));
    }

    /** 按 1～18 周补齐周次维度，缺失周次填 0 */
    private static List<DimensionPointVo> fullByWeek(Map<Integer, long[]> map) {
        List<DimensionPointVo> list = new ArrayList<>(WEEK_MAX - WEEK_MIN + 1);
        for (int w = WEEK_MIN; w <= WEEK_MAX; w++) {
            long[] v = map.get(w);
            list.add(new DimensionPointVo(
                    w,
                    v != null ? v[0] : 0L,
                    v != null ? v[1] : 0L,
                    v != null ? v[2] : 0L
            ));
        }
        return list;
    }

    /** 按周一～周日补齐星期维度，缺失填 0，label 为中文星期名 */
    private static List<DimensionPointVo> fullByWeekday(Map<Integer, long[]> map) {
        List<DimensionPointVo> list = new ArrayList<>(WEEKDAY_LABELS.length);
        for (int i = 0; i < WEEKDAY_LABELS.length; i++) {
            int wd = i + 1;
            long[] v = map.get(wd);
            list.add(new DimensionPointVo(
                    WEEKDAY_LABELS[i],
                    v != null ? v[0] : 0L,
                    v != null ? v[1] : 0L,
                    v != null ? v[2] : 0L
            ));
        }
        return list;
    }

    /** 按 6 个节次（1-2～11-12）补齐节次维度，缺失填 0 */
    private static List<DimensionPointVo> fullBySection(Map<Integer, long[]> map) {
        List<DimensionPointVo> list = new ArrayList<>(SECTION_LABELS.length);
        for (int i = 0; i < SECTION_LABELS.length; i++) {
            long[] v = map.get(i);
            list.add(new DimensionPointVo(
                    SECTION_LABELS[i],
                    v != null ? v[0] : 0L,
                    v != null ? v[1] : 0L,
                    v != null ? v[2] : 0L
            ));
        }
        return list;
    }
}
