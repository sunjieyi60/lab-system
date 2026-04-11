package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.class_time_table.Teacher;
import xyz.jasenon.lab.service.dto.course.CreateTeacher;
import xyz.jasenon.lab.service.dto.course.DeleteTeacher;
import xyz.jasenon.lab.service.dto.course.EditTeacher;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
public interface ITeacherService extends IService<Teacher> {

    Teacher createTeacher(CreateTeacher createTeacher);

    void deleteTeacher(DeleteTeacher deleteTeacher);

    Teacher editTeacher(EditTeacher editTeacher);

    List<Teacher> getAllTeacher();
}
