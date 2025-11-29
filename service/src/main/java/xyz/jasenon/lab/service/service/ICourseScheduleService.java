package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.entity.class_time_table.CourseSchedule;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.course.CreateCourseSchedule;
import xyz.jasenon.lab.service.dto.course.DeleteCourseSchedule;
import xyz.jasenon.lab.service.vo.CourseScheduleVo;

import java.util.List;
import java.util.Map;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
public interface ICourseScheduleService extends IService<CourseSchedule> {

    R createCourseSchedule(CreateCourseSchedule createCourseSchedule);

    R deleteCourseSchedule(DeleteCourseSchedule deleteCourseSchedule);

    R<Map<Long,List<CourseScheduleVo>>> listCourseSchedule(List<Long> laboratoryIds);

    R<List<Laboratory>> listLaboratory();
}
