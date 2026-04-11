package xyz.jasenon.lab.service.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.yulichang.toolkit.JoinWrappers;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.cache.spi.Cache;
import xyz.jasenon.lab.common.Const;
import xyz.jasenon.lab.common.entity.BaseEntity;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.entity.base.LaboratoryUser;
import xyz.jasenon.lab.common.entity.class_time_table.Semester;
import xyz.jasenon.lab.common.entity.class_time_table.WeekType;
import xyz.jasenon.lab.common.entity.device.Device;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.record.SensorRecord;
import xyz.jasenon.lab.common.utils.AsyncExecutor;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.mapper.CourseScheduleMapper;
import xyz.jasenon.lab.service.mapper.LaboratoryUserMapper;
import xyz.jasenon.lab.service.mapper.SemesterMapper;
import xyz.jasenon.lab.service.mapper.record.DeviceMapper;
import xyz.jasenon.lab.service.service.IOverviewService;
import xyz.jasenon.lab.service.strategy.device.record.DeviceRecordFactory;
import xyz.jasenon.lab.service.vo.base.CourseScheduleVo;
import xyz.jasenon.lab.service.vo.device.DeviceRecordVo;
import xyz.jasenon.lab.service.vo.overview.DeviceOverviewVo;
import xyz.jasenon.lab.service.vo.overview.middle.MiddleOverviewVo;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 实验室概览服务实现类
 * <p>
 * 提供实验室总览相关的业务逻辑，包括：
 * <ul>
 *     <li>获取当前用户管理的所有实验室概览信息</li>
 *     <li>获取即将上课的实验室列表</li>
 *     <li>获取左侧边栏设备统计详情</li>
 * </ul>
 * </p>
 *
 * @author Jasenon
 * @see IOverviewService
 * @see MiddleOverviewVo
 * @see DeviceOverviewVo
 */
@Service
@RequiredArgsConstructor
public class OverviewServiceImpl implements IOverviewService, Const.Key, Const.Mysql {

    private final LaboratoryUserMapper laboratoryUserMapper;
    private final CourseScheduleMapper courseScheduleMapper;
    private final SemesterMapper semesterMapper;
    private final Cache cache;
    private final DeviceMapper deviceMapper;

    /**
     * 获取当前用户管理的所有实验室概览信息
     * <p>
     * 包括实验室基本信息、设备状态、课程安排、环境数据等。
     * 根据学期ID和当前时间自动计算周次和单双周，筛选对应课程。
     * </p>
     *
     * @param semesterId 学期ID，用于确定当前教学周次
     * @return 实验室概览信息列表，每个实验室对应一个 {@link MiddleOverviewVo}
     * @throws RuntimeException 当获取设备信息发生异常时抛出
     */
    @Override
    public List<MiddleOverviewVo> getAllLaboratories(Long semesterId) {
        List<MiddleOverviewVo> result = new ArrayList<>();
        
        // 获取当前用户管理的实验室列表
        List<Laboratory> laboratories = laboratories()
                .stream()
                .filter(Objects::nonNull)
                .toList();
        
        // 计算当前星期（1=周一，7=周日）
        int nowWeek = weekDetector();
        
        // 计算当前教学周次及单双周类型
        long passedWeeks = weeksDetector(semesterId);
        WeekType weekType = passedWeeks % 2 == 0 ? WeekType.Double : WeekType.Single;
        List<WeekType> hitWeekType = List.of(WeekType.Both, weekType);

        LocalTime now = LocalTime.now();

        // 查询当前正在上课的课程安排
        List<CourseScheduleVo> vos = courseScheduleMapper.getAllClassingCourseVo(
                semesterId, hitWeekType, passedWeeks, nowWeek, now
        );
        
        // 组装每个实验室的概览信息
        for (Laboratory laboratory : laboratories) {
            MiddleOverviewVo mdv = new MiddleOverviewVo();
            
            // 查找该实验室当前课程
            CourseScheduleVo vo = vos.stream()
                    .filter(csv -> csv.getId().equals(laboratory.getId()))
                    .findFirst()
                    .orElse(null);
            
            // 判断今日是否有课（包括当前未上课但今日有课的情况）
            if (vo == null) {
                mdv.setHasCourse(courseScheduleMapper.existCourse(
                        semesterId, laboratory.getId(), hitWeekType, passedWeeks, nowWeek));
            } else {
                mdv.setHasCourse(true);
            }
            
            mdv.setLaboratory(laboratory);
            mdv.setCourse(vo);
            
            // 异步获取设备信息
            try {
                mdv.setDevices(convert(getAllLaboratoryDevices(laboratory.getId())));
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                throw R.fail("服务异常").convert();
            }
            
            // 获取传感器环境数据（温度、湿度等）
            if (mdv.getDevices().get(DeviceType.Sensor) != null) {
                DeviceRecordVo<SensorRecord> record = DeviceRecordFactory.getDeviceRecordMethod(DeviceType.Sensor)
                        .getRecord(mdv.getDevices().get(DeviceType.Sensor).getDevices().get(0).getId());
                mdv.setEnv(record);
            }

            result.add(mdv);
        }
        return result;
    }

    /**
     * 获取正在上课的实验室列表（已弃用）
     *
     * @param semesterId 学期ID
     * @return 空列表
     * @deprecated 该方法已弃用，请使用 {@link #getAllLaboratories(Long)}
     */
    @Override
    @Deprecated
    public List<MiddleOverviewVo> getLaboratoriesOnClassing(Long semesterId) {
        return List.of();
    }

    /**
     * 获取即将上课的实验室列表
     * <p>
     * 查询未来45分钟内即将开始上课的实验室。
     * 根据学期ID和当前时间自动计算周次和单双周，筛选对应课程。
     * </p>
     *
     * @param semesterId 学期ID，用于确定当前教学周次
     * @return 即将上课的实验室概览信息列表
     * @throws RuntimeException 当获取设备信息发生异常时抛出
     */
    @Override
    public List<MiddleOverviewVo> getLaboratoriesSoonClassing(Long semesterId) {
        List<MiddleOverviewVo> result = new ArrayList<>();
        
        // 获取当前用户管理的实验室列表
        List<Laboratory> laboratories = laboratories()
                .stream()
                .filter(Objects::nonNull)
                .toList();
        
        // 计算当前星期
        int nowWeek = weekDetector();
        
        // 计算当前教学周次及单双周类型
        long passedWeeks = weeksDetector(semesterId);
        WeekType weekType = passedWeeks % 2 == 0 ? WeekType.Double : WeekType.Single;
        List<WeekType> hitWeekType = List.of(WeekType.Both, weekType);

        // 计算45分钟后的时间
        LocalTime soon = LocalTime.now().plus(Duration.ofMinutes(45L));
        
        // 查询即将上课的课程安排
        List<CourseScheduleVo> vos = courseScheduleMapper.getSoonCourseVo(
                semesterId, hitWeekType, passedWeeks, nowWeek, soon
        );

        // 提取有课的实验室ID集合
        Set<Long> laboratoryIds = vos.stream()
                .map(CourseScheduleVo::getLaboratoryId)
                .collect(Collectors.toSet());

        // 过滤出有课的实验室
        laboratories = laboratories.stream()
                .filter(l -> laboratoryIds.contains(l.getId()))
                .toList();
        
        // 组装概览信息
        for (Laboratory laboratory : laboratories) {
            MiddleOverviewVo mdv = new MiddleOverviewVo();
            
            // 查找该实验室即将开始的课程
            CourseScheduleVo vo = vos.stream()
                    .filter(csv -> csv.getId().equals(laboratory.getId()))
                    .findFirst()
                    .orElse(null);
            
            // 判断今日是否有课
            if (vo == null) {
                mdv.setHasCourse(courseScheduleMapper.existCourse(
                        semesterId, laboratory.getId(), hitWeekType, passedWeeks, nowWeek));
            } else {
                mdv.setHasCourse(true);
            }
            
            mdv.setLaboratory(laboratory);
            mdv.setCourse(vo);
            
            // 异步获取设备信息
            try {
                mdv.setDevices(convert(getAllLaboratoryDevices(laboratory.getId())));
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                throw R.fail("服务异常").convert();
            }
            
            // 获取传感器环境数据
            if (mdv.getDevices().get(DeviceType.Sensor) != null) {
                DeviceRecordVo<SensorRecord> record = DeviceRecordFactory.getDeviceRecordMethod(DeviceType.Sensor)
                        .getRecord(mdv.getDevices().get(DeviceType.Sensor).getDevices().get(0).getId());
                mdv.setEnv(record);
            }

            result.add(mdv);
        }
        return result;
    }

    /**
     * 获取无课的实验室列表（已弃用）
     *
     * @param semesterId 学期ID
     * @return 空列表
     * @deprecated 该方法已弃用
     */
    @Override
    @Deprecated
    public List<MiddleOverviewVo> getLaboratoriesWithoutClass(Long semesterId) {
        return List.of();
    }

    /**
     * 获取左侧边栏设备统计详情
     * <p>
     * 统计当前用户管理的所有实验室的设备总数和在线数，按设备类型分组。
     * 在线状态通过检查设备是否在Redis中有心跳记录来判断。
     * </p>
     *
     * @return 设备类型到设备概览的映射，包含总数和在线数
     * @throws RuntimeException 当异步获取设备信息发生异常时抛出
     */
    @Override
    public Map<DeviceType, DeviceOverviewVo> getLeftbarDetail() {
        // 获取当前用户管理的所有实验室ID
        List<Long> laboratoryIds = laboratories().stream()
                .map(BaseEntity::getId)
                .toList();
        
        // 获取所有设备并按类型分组
        Map<DeviceType, List<Device>> devices = getAllLaboratoryDevices(laboratoryIds);
        
        try {
            return convert(devices);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 课程节次信息
     * <p>
     * 记录每节课的开始和结束时间
     * </p>
     *
     * @param section 节次编号（1-11）
     * @param startTime 开始时间
     * @param endTime 结束时间
     */
    private record SectionInfo(int section, LocalTime startTime, LocalTime endTime) {
        @Override
        public String toString() {
            return String.format("第%d节(%02d:%02d~%02d:%02d)", 
                    section, startTime.getHour(), startTime.getMinute(), 
                    endTime.getHour(), endTime.getMinute());
        }
    }

    /**
     * 课程时间表（全天11节课）
     * <p>
     * 根据学校课程安排，定义了全天11节课的时间段：
     * <ul>
     *     <li>第1节: 08:00~08:45</li>
     *     <li>第2节: 08:55~09:40</li>
     *     <li>第3节: 10:00~10:45</li>
     *     <li>第4节: 10:55~11:40</li>
     *     <li>第5节: 14:10~14:55</li>
     *     <li>第6节: 15:05~15:50</li>
     *     <li>第7节: 16:00~16:45</li>
     *     <li>第8节: 16:55~17:40</li>
     *     <li>第9节: 18:40~19:25</li>
     *     <li>第10节: 19:30~20:15</li>
     *     <li>第11节: 20:20~21:05</li>
     * </ul>
     * </p>
     */
    private static final List<SectionInfo> COURSE_SCHEDULE = List.of(
            new SectionInfo(1, LocalTime.of(8, 0), LocalTime.of(8, 45)),
            new SectionInfo(2, LocalTime.of(8, 55), LocalTime.of(9, 40)),
            new SectionInfo(3, LocalTime.of(10, 0), LocalTime.of(10, 45)),
            new SectionInfo(4, LocalTime.of(10, 55), LocalTime.of(11, 40)),
            new SectionInfo(5, LocalTime.of(14, 10), LocalTime.of(14, 55)),
            new SectionInfo(6, LocalTime.of(15, 5), LocalTime.of(15, 50)),
            new SectionInfo(7, LocalTime.of(16, 0), LocalTime.of(16, 45)),
            new SectionInfo(8, LocalTime.of(16, 55), LocalTime.of(17, 40)),
            new SectionInfo(9, LocalTime.of(18, 40), LocalTime.of(19, 25)),
            new SectionInfo(10, LocalTime.of(19, 30), LocalTime.of(20, 15)),
            new SectionInfo(11, LocalTime.of(20, 20), LocalTime.of(21, 5))
    );

    /**
     * 节次检测器（指定时间）
     * <p>
     * 根据指定时间判断处于第几节课。
     * </p>
     *
     * @param time 指定时间
     * @return 当前节次（1-11），非上课时间返回0
     */
    private int sectionDetector(LocalTime time) {
        for (SectionInfo section : COURSE_SCHEDULE) {
            // 检查时间是否在该节课的时间段内（包含开始时间，不包含结束时间）
            if (!time.isBefore(section.startTime()) && time.isBefore(section.endTime())) {
                return section.section();
            }
        }
        return 0; // 非上课时间
    }

    /**
     * 获取当前学期详细信息
     * <p>
     * 返回格式：第X周星期X第X节
     * </p>
     *
     * @param semesterId 学期ID
     * @return 学期详细信息字符串
     */
    @Override
    public String NowSemesterDetailInfo(Long semesterId) {
        return MessageFormat.format("第{0}周星期{1}第{2}节", 
                weeksDetector(semesterId), 
                weekDetector(), 
                sectionDetector(LocalTime.now()));
    }

    /**
     * 获取当前用户管理的实验室列表
     * <p>
     * 通过关联表查询当前登录用户有权限管理的所有实验室。
     * 使用 groupBy 去重处理同一实验室的多条权限记录。
     * </p>
     *
     * @return 实验室实体列表
     */
    private List<Laboratory> laboratories() {
        Long userId = StpUtil.getLoginIdAsLong();
        return laboratoryUserMapper.selectJoinList(Laboratory.class,
                JoinWrappers.lambda(LaboratoryUser.class)
                        .eq(LaboratoryUser::getUserId, userId)
                        // groupBy去重
                        .groupBy(LaboratoryUser::getLaboratoryId)
                        .leftJoin(Laboratory.class, Laboratory::getId, LaboratoryUser::getLaboratoryId)
                        .selectAll(Laboratory.class)
        );
    }

    /**
     * 计算当前教学周次
     * <p>
     * 使用缓存避免频繁查询数据库。根据学期开始日期和当前日期计算已过去的周数。
     * </p>
     *
     * @param semesterId 学期ID
     * @return 当前教学周次（从1开始）
     * @throws RuntimeException 当学期不存在时抛出
     */
    private Long weeksDetector(Long semesterId) {
        // 一级缓存，从缓存获取学期信息
        Semester semester = cache.get(
                semesterInfo(semesterId),
                () -> semesterMapper.selectOne(
                        new LambdaQueryWrapper<Semester>()
                                .eq(Semester::getId, semesterId)
                                .last(LOCK)
                ),
                Semester.class
        );
        
        if (semester == null) {
            throw R.badRequest("目标学期不存在").convert();
        }
        
        LocalDate now = LocalDate.now();
        var weeks = ChronoUnit.WEEKS.between(now, semester.getStartDate());
        return weeks + 1;
    }

    /**
     * 获取当前星期
     *
     * @return 星期几（1=周一，7=周日）
     */
    private Integer weekDetector() {
        return LocalDate.now().get(WeekFields.ISO.dayOfWeek());
    }

    /**
     * 获取单个实验室的所有设备
     *
     * @param laboratoryId 实验室ID
     * @return 设备按类型分组的映射
     */
    private Map<DeviceType, List<Device>> getAllLaboratoryDevices(long laboratoryId) {
        return deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getBelongToLaboratoryId, laboratoryId)
        ).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Device::getDeviceType));
    }

    /**
     * 获取多个实验室的所有设备
     *
     * @param laboratoryIds 实验室ID列表，不能为空
     * @return 设备按类型分组的映射
     * @throws RuntimeException 当实验室ID列表为空时抛出
     */
    private Map<DeviceType, List<Device>> getAllLaboratoryDevices(@NotEmpty List<Long> laboratoryIds) {
        if (laboratoryIds == null || laboratoryIds.isEmpty()) {
            throw R.badRequest("实验室id列表不能为空").convert();
        }
        return deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .in(Device::getBelongToLaboratoryId, laboratoryIds)
        ).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Device::getDeviceType));
    }

    /**
     * 转换设备分组为概览视图
     * <p>
     * 使用异步并发处理提高性能，每个设备类型独立统计在线状态。
     * 在线状态通过检查Redis中的心跳记录判断。
     * </p>
     *
     * @param map 设备按类型分组的映射
     * @return 设备类型到概览的映射
     * @throws ExecutionException   当异步任务执行异常时抛出
     * @throws InterruptedException 当线程被中断时抛出
     * @throws TimeoutException     当异步任务超时时抛出
     */
    private Map<DeviceType, DeviceOverviewVo> convert(Map<DeviceType, List<Device>> map) 
            throws ExecutionException, InterruptedException, TimeoutException {
        Map<DeviceType, CompletableFuture<DeviceOverviewVo>> tasks = new HashMap<>();
        
        // 为每种设备类型创建异步任务统计在线状态
        for (Map.Entry<DeviceType, List<Device>> entry : map.entrySet()) {
            CompletableFuture<DeviceOverviewVo> future = AsyncExecutor.runAsyncIO(
                    () -> async(entry.getKey(), entry.getValue())
            );
            tasks.put(entry.getKey(), future);
        }
        
        // 等待所有异步任务完成（最多5秒超时）
        Map<DeviceType, DeviceOverviewVo> result = new HashMap<>();
        for (Map.Entry<DeviceType, CompletableFuture<DeviceOverviewVo>> entry : tasks.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get(5, TimeUnit.SECONDS));
        }
        return result;
    }

    /**
     * 异步统计指定类型设备的在线状态
     * <p>
     * 通过查询Redis中的设备心跳记录来判断设备是否在线。
     * </p>
     *
     * @param type    设备类型
     * @param devices 设备列表
     * @return 设备概览信息，包含总数和在线数
     */
    private DeviceOverviewVo async(DeviceType type, List<Device> devices) {
        // 从缓存获取该类型设备的所有在线心跳记录key
        Set<String> keys = cache.keys(type.getRedisPrefix() + "*")
                .stream()
                .map(s -> s.replace(type.getRedisPrefix(), ""))
                .collect(Collectors.toSet());
        
        // 统计在线设备数量
        int online = devices.stream()
                .filter(d -> keys.contains(Long.toString(d.getId())))
                .toList()
                .size();
        
        int total = devices.size();
        
        DeviceOverviewVo vo = new DeviceOverviewVo();
        vo.setDevices(devices);
        vo.setOnline(online);
        vo.setTotal(total);
        return vo;
    }

}
