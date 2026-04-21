package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.entity.class_time_table.CourseSchedule;
import xyz.jasenon.lab.service.dto.course.CreateCourseSchedule;
import xyz.jasenon.lab.service.dto.course.DeleteCourseSchedule;
import xyz.jasenon.lab.service.dto.course.EditCourseSchedule;
import xyz.jasenon.lab.service.vo.base.CourseScheduleVo;

import java.util.List;
import java.util.Map;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
public interface ICourseScheduleService extends IService<CourseSchedule> {

    CourseSchedule createCourseSchedule(CreateCourseSchedule createCourseSchedule);

    void deleteCourseSchedule(DeleteCourseSchedule deleteCourseSchedule);

    /**
     * 编辑课程表：全量更新
     */
    CourseSchedule editCourseSchedule(EditCourseSchedule editCourseSchedule);

    /**
     * 删除指定实验室下的全部课程表记录
     */
    void deleteCourseScheduleByLaboratoryId(Long laboratoryId);

    Map<Long,List<CourseScheduleVo>> listCourseSchedule(List<Long> laboratoryIds);

    List<Laboratory> listLaboratory();

    boolean checkConflect(CourseSchedule insertOrUpdate, CourseSchedule ...olds);

    boolean checkConflect(CourseSchedule insertOrUpdate, List<CourseSchedule> olds);
}
