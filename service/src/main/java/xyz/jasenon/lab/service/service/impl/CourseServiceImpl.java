package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.class_time_table.Course;
import xyz.jasenon.lab.service.mapper.CourseMapper;
import xyz.jasenon.lab.service.service.ICourseService;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.course.CreateCourse;
import xyz.jasenon.lab.service.dto.course.DeleteCourse;
import xyz.jasenon.lab.service.dto.course.EditCourse;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements ICourseService {

    @Override
    public R createCourse(CreateCourse createCourse) {
        // 创建课程：参考项目风格，使用流式DTO取值进行字段注入
        Course course = new Course();
        course.setCourseName(createCourse.getCourseName());
        course.setVolumn(createCourse.getVolume());
        course.setGrade(createCourse.getGrade());
        this.save(course);
        return R.success("课程创建成功");
    }

    @Override
    public R deleteCourse(DeleteCourse deleteCourse) {
        Course course = this.getById(deleteCourse.getCourseId());
        if (course == null) {
            return R.fail("课程不存在");
        }
        this.removeById(deleteCourse.getCourseId());
        return R.success("课程删除成功");
    }

    @Override
    public R editCourse(EditCourse editCourse) {
        Course course = this.getById(editCourse.getCourseId());
        if (course == null) {
            return R.fail("课程不存在");
        }
        // 编辑课程：参考 LaboratoryServiceImpl，使用 Hutool BeanUtil + CopyOptions 忽略空值与只读字段
        CopyOptions copyOptions = CopyOptions.create()
                .setIgnoreProperties("id", "createTime")
                .ignoreNullValue();

        Course edit = new Course();
        edit.setCourseName(editCourse.getCourseName());
        edit.setVolumn(editCourse.getVolume());
        edit.setGrade(editCourse.getGrade());

        BeanUtil.copyProperties(edit, course, copyOptions);
        this.updateById(course);
        return R.success("课程修改成功");
    }

    @Override
    public R<List<Course>> listCourse() {
        return R.success(list());
    }
}
