package xyz.jasenon.lab.service.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import jakarta.validation.constraints.NotEmpty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.jasenon.lab.common.entity.class_time_table.CourseSchedule;
import xyz.jasenon.lab.common.entity.class_time_table.WeekType;
import xyz.jasenon.lab.service.vo.base.CourseScheduleVo;

import java.time.LocalTime;
import java.util.List;


/**
 * @author Jasenon_ce
 * @date 2025/9/18
 */
@Mapper
public interface CourseScheduleMapper extends MPJBaseMapper<CourseSchedule> {

    List<CourseSchedule> getAllClassingCourse(
            @Param("semesterId") long semesterId,@Param("laboratoryId") long laboratoryId,
            @NotEmpty @Param("weekTypes") List<WeekType> weekTypes,@Param("passedWeeks") long passedWeeks,
            @Param("weekNow") int weekNow,@Param("now") LocalTime now);

    List<CourseScheduleVo> getAllClassingCourseVo(
            @Param("semesterId") long semesterId,
            @NotEmpty @Param("weekTypes") List<WeekType> weekTypes,@Param("passedWeeks") long passedWeeks,
            @Param("weekNow") int weekNow,@Param("now") LocalTime now);

    boolean existCourse(
            @Param("semesterId") long semesterId, @Param("laboratoryId") long laboratoryId,
            @Param("weekTypes") List<WeekType> weekTypes, @Param("passedWeeks") long passedWeeks,
            @Param("weekNow") int weekNow);

    List<CourseScheduleVo> getSoonCourseVo(
            @Param("semesterId") long semesterId, @Param("weekTypes") List<WeekType> weekTypes, @Param("passedWeeks") long passedWeeks,
            @Param("weekNow") int weekNow, @Param("now") LocalTime soon);
}
