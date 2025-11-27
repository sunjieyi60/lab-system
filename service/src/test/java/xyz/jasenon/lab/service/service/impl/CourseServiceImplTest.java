package xyz.jasenon.lab.service.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import xyz.jasenon.lab.common.entity.class_time_table.Course;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.course.CreateCourse;
import xyz.jasenon.lab.service.dto.course.DeleteCourse;
import xyz.jasenon.lab.service.dto.course.EditCourse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * 课程服务测试：验证创建、编辑（BeanUtil CopyOptions）、删除的行为
 */
class CourseServiceImplTest {

    @Test
    void testCreateCourse_usesDtoValues() {
        CourseServiceImpl service = Mockito.spy(new CourseServiceImpl());
        Mockito.doReturn(true).when(service).save(any(Course.class));

        CreateCourse dto = new CreateCourse()
                .courseName("高等数学")
                .volume(120)
                .grade("2023级");

        R r = service.createCourse(dto);
        assertTrue(r.isOk());

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        Mockito.verify(service).save(captor.capture());
        Course saved = captor.getValue();
        assertEquals("高等数学", saved.getCourseName());
        assertEquals(120, saved.getVolumn());
        assertEquals("2023级", saved.getGrade());
    }

    @Test
    void testEditCourse_beanUtilCopyOptionsIgnoreNull() {
        CourseServiceImpl service = Mockito.spy(new CourseServiceImpl());
        Course existing = new Course();
        existing.setCourseName("线性代数");
        existing.setVolumn(100);
        existing.setGrade("2022级");

        Mockito.doReturn(existing).when(service).getById(1L);
        Mockito.doReturn(true).when(service).updateById(any(Course.class));

        EditCourse edit = new EditCourse();
        edit.setCourseId(1L);
        edit.setCourseName(null); // 应忽略
        edit.setVolume(150);      // 应更新
        edit.setGrade("2024级");  // 应更新

        R r = service.editCourse(edit);
        assertTrue(r.isOk());
        assertEquals("线性代数", existing.getCourseName());
        assertEquals(150, existing.getVolumn());
        assertEquals("2024级", existing.getGrade());
    }

    @Test
    void testDeleteCourse_notFound() {
        CourseServiceImpl service = Mockito.spy(new CourseServiceImpl());
        Mockito.doReturn(null).when(service).getById(99L);

        DeleteCourse dto = new DeleteCourse();
        dto.setCourseId(99L);
        R r = service.deleteCourse(dto);
        assertFalse(r.isOk());
    }
}