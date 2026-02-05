package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.entity.class_time_table.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.course.CreateCourseSchedule;
import xyz.jasenon.lab.service.dto.course.DeleteCourseSchedule;
import xyz.jasenon.lab.service.mapper.CourseScheduleMapper;
import xyz.jasenon.lab.service.mapper.LaboratoryMapper;
import xyz.jasenon.lab.service.service.IAnalysisService;
import xyz.jasenon.lab.service.service.ICourseScheduleService;
import xyz.jasenon.lab.service.vo.CourseScheduleVo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 * 课程课表服务实现类
 *
 * 设计说明：
 * - 创建课程课表时执行冲突检测，确保同一学期内教师或实验室的排课不重叠
 * - 冲突维度包含：实验室或教师、上课星期、周次区间、单双周类型、时间段/节次
 * - 删除操作在不存在时返回明确错误信息
 */
@Service
public class CourseScheduleServiceImpl extends ServiceImpl<CourseScheduleMapper, CourseSchedule> implements ICourseScheduleService {

    @Autowired
    private LaboratoryMapper laboratoryMapper;
    @Autowired
    private IAnalysisService analysisService;

    @Override
    public R createCourseSchedule(CreateCourseSchedule createCourseSchedule) {
        CourseSchedule courseSchedule = new CourseSchedule();
        courseSchedule.setSemesterId(createCourseSchedule.getSemesterId());
        courseSchedule.setLaboratoryId(createCourseSchedule.getLaboratoryId());
        courseSchedule.setWeekType(createCourseSchedule.getWeekType());
        courseSchedule.setStartWeek(createCourseSchedule.getStartWeek());
        courseSchedule.setEndWeek(createCourseSchedule.getEndWeek());
        courseSchedule.setStartTime(createCourseSchedule.getStartTime());
        courseSchedule.setEndTime(createCourseSchedule.getEndTime());
        courseSchedule.setWeekdays(createCourseSchedule.getWeekdays());
        courseSchedule.setCourseId(createCourseSchedule.getCourseId());
        courseSchedule.setTeacherId(createCourseSchedule.getTeacherId());
        courseSchedule.setDeptId(createCourseSchedule.getBelongToDeptId());
        courseSchedule.setStartSection(createCourseSchedule.getStartSection());
        courseSchedule.setEndSection(createCourseSchedule.getEndSection());
        courseSchedule.setMark(createCourseSchedule.getMark());

        // 同一学期范围内扫描潜在冲突对象，降低查询成本并符合业务语义
        List<CourseSchedule> schedules = this.lambdaQuery()
                .eq(CourseSchedule::getSemesterId, courseSchedule.getSemesterId())
                .list();

        // 冲突检测：存在任意一个冲突即阻止创建
        for (CourseSchedule ex : schedules) {
            if (hasConflict(courseSchedule, ex)) {
                return R.fail("课程安排冲突");
            }
        }

        this.save(courseSchedule);
        analysisService.invalidateChartCache();
        return R.success("课程安排创建成功");
    }

    @Override
    public R deleteCourseSchedule(DeleteCourseSchedule deleteCourseSchedule) {
        CourseSchedule courseSchedule = this.getById(deleteCourseSchedule.getCourseScheduleId());
        if (courseSchedule == null) {
            return R.fail("课程表不存在");
        }
        this.removeById(deleteCourseSchedule.getCourseScheduleId());
        analysisService.invalidateChartCache();
        return R.success("课程表删除成功");
    }

    @Override
    public R<Map<Long, List<CourseScheduleVo>>> listCourseSchedule(List<Long> laboratoryIds) {
        List<CourseScheduleVo> all = new ArrayList<>();
        for (Long laboratoryId : laboratoryIds) {
            List<CourseScheduleVo> list = baseMapper.selectJoinList(CourseScheduleVo.class,
                    new MPJLambdaWrapper<CourseSchedule>()
                            .selectAll(CourseSchedule.class)
                            .selectAs(Course::getId, CourseScheduleVo::getCourseId)
                            .selectAs(Course::getCourseName, CourseScheduleVo::getCourseName)
                            .selectAs(Teacher::getId, CourseScheduleVo::getTeacherId)
                            .selectAs(Teacher::getTeacherName, CourseScheduleVo::getTeacherName)
                            .selectAs(Semester::getId, CourseScheduleVo::getSemesterId)
                            .selectAs(Semester::getName, CourseScheduleVo::getSemesterName)
                            .selectAs(Laboratory::getId, CourseScheduleVo::getLaboratoryId)
                            .selectAs(Laboratory::getLaboratoryName, CourseScheduleVo::getLaboratoryName)
                            .leftJoin(Course.class, Course::getId, CourseSchedule::getCourseId)
                            .leftJoin(Teacher.class, Teacher::getId, CourseSchedule::getTeacherId)
                            .leftJoin(Semester.class, Semester::getId, CourseSchedule::getSemesterId)
                            .leftJoin(Laboratory.class, Laboratory::getId, CourseSchedule::getLaboratoryId)
                            .eq(CourseSchedule::getLaboratoryId, laboratoryId)
            );
            all.addAll(list);
        }
        Map<Long, List<CourseScheduleVo>> map = all.stream().collect(Collectors.toMap(CourseScheduleVo::getLaboratoryId, List::of));
        return R.success(map,"获取成功");
    }

    @Override
    public R<List<Laboratory>> listLaboratory() {
        List<Laboratory> lists = laboratoryMapper.selectList(new LambdaQueryWrapper<Laboratory>());
        return R.success(lists,"获取成功");
    }

    /**
     * 核心冲突检测逻辑
     * 规则：
     * 1) 主体重叠（同实验室或同教师）
     * 2) 上课星期有交集
     * 3) 周次区间存在交集
     * 4) 单/双/单双周在交集区间内匹配
     * 5) 时间段或节次重叠
     */
    private boolean hasConflict(CourseSchedule a, CourseSchedule b) {
        boolean sameLab = a.getLaboratoryId().equals(b.getLaboratoryId());
        boolean sameTeacher = a.getTeacherId().equals(b.getTeacherId());
        if (!(sameLab || sameTeacher)) {
            return false;
        }
        if (a.getWeekdays() == null || b.getWeekdays() == null) {
            return false;
        }
        boolean weekdaysIntersect = a.getWeekdays().stream().anyMatch(b.getWeekdays()::contains);
        if (!weekdaysIntersect) {
            return false;
        }
        int interStart = Math.max(a.getStartWeek(), b.getStartWeek());
        int interEnd = Math.min(a.getEndWeek(), b.getEndWeek());
        if (interStart > interEnd) {
            return false;
        }
        boolean weekTypeIntersect = weekTypeOverlap(a.getWeekType(), b.getWeekType(), interStart, interEnd);
        if (!weekTypeIntersect) {
            return false;
        }
        boolean timeOverlap = a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime());
        boolean sectionOverlap = a.getStartSection() <= b.getEndSection() && b.getStartSection() <= a.getEndSection();
        return timeOverlap || sectionOverlap;
    }

    /**
     * 单/双/单双周类型是否在交集区间内重叠
     */
    private boolean weekTypeOverlap(WeekType t1, WeekType t2, int start, int end) {
        if (t1 == WeekType.Both || t2 == WeekType.Both) {
            return true;
        }
        if (t1 == WeekType.Single && t2 == WeekType.Double) {
            return false;
        }
        if (t1 == WeekType.Double && t2 == WeekType.Single) {
            return false;
        }
        if (t1 == WeekType.Single && t2 == WeekType.Single) {
            int firstOdd = (start % 2 == 1) ? start : start + 1;
            return firstOdd <= end;
        }
        if (t1 == WeekType.Double && t2 == WeekType.Double) {
            int firstEven = (start % 2 == 0) ? start : start + 1;
            return firstEven <= end;
        }
        return false;
    }
}
