package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.class_time_table.Teacher;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.course.CreateTeacher;
import xyz.jasenon.lab.service.dto.course.DeleteTeacher;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
public interface ITeacherService extends IService<Teacher> {

    R createTeacher(CreateTeacher createTeacher);

    R deleteTeacher(DeleteTeacher deleteTeacher);
}
