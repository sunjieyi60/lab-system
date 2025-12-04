package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.class_time_table.Teacher;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.course.CreateTeacher;
import xyz.jasenon.lab.service.dto.course.DeleteTeacher;
import xyz.jasenon.lab.service.mapper.TeacherMapper;
import xyz.jasenon.lab.service.service.ITeacherService;

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
    public R createTeacher(CreateTeacher createTeacher) {
        Teacher teacher = new Teacher();
        teacher.setTeacherName(createTeacher.getTeacherName());
        this.save(teacher);
        return R.success("教师创建成功");
    }

    /**
     * 删除教师：不存在则返回失败
     */
    @Override
    public R deleteTeacher(DeleteTeacher deleteTeacher) {
        Teacher teacher = this.getById(deleteTeacher.getTeacherId());
        if (teacher == null) {
            return R.fail("教师不存在");
        }
        this.removeById(deleteTeacher.getTeacherId());
        return R.success("教师删除成功");
    }
}
