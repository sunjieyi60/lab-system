package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.class_time_table.Teacher;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.course.CreateTeacher;
import xyz.jasenon.lab.service.dto.course.DeleteTeacher;
import xyz.jasenon.lab.service.dto.course.EditTeacher;
import xyz.jasenon.lab.service.mapper.TeacherMapper;
import xyz.jasenon.lab.service.service.ITeacherService;

import java.util.List;
import java.util.Objects;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Service
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements ITeacherService {
    /**
     * 创建教师：使用DTO提供的字段进行注入，保持与项目统一风格
     */
    @Override
    public Teacher createTeacher(CreateTeacher createTeacher) {
        Teacher teacher = new Teacher();
        teacher.setTeacherName(createTeacher.getTeacherName());
        this.save(teacher);
        return teacher;
    }

    /**
     * 删除教师：不存在则返回失败
     */
    @Override
    public void deleteTeacher(DeleteTeacher deleteTeacher) {
        Teacher teacher = this.getById(deleteTeacher.getTeacherId());
        if (teacher == null) {
            throw R.fail("教师不存在").convert();
        }
        this.removeById(deleteTeacher.getTeacherId());
    }

    @Override
    public Teacher editTeacher(EditTeacher editTeacher) {
        Teacher teacher = this.getById(editTeacher.getTeacherId());
        if (Objects.nonNull(teacher)){
            teacher.setTeacherName(editTeacher.getTeacherName());
            updateById(teacher);
            return teacher;
        } else {
            throw R.fail("相关教师不存在").convert();
        }
    }

    @Override
    public List<Teacher> getAllTeacher() {
        List<Teacher> teachers = this.list();
        return teachers;
    }
}
