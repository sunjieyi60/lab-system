package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.class_time_table.Course;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.course.CreateCourse;
import xyz.jasenon.lab.service.dto.course.DeleteCourse;
import xyz.jasenon.lab.service.dto.course.EditCourse;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
public interface ICourseService extends IService<Course> {

    R createCourse(CreateCourse createCourse);

    R deleteCourse(DeleteCourse deleteCourse);

    R editCourse(EditCourse editCourse);

}
