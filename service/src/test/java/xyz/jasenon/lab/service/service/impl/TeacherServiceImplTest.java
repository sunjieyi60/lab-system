package xyz.jasenon.lab.service.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import xyz.jasenon.lab.common.entity.class_time_table.Teacher;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.course.CreateTeacher;
import xyz.jasenon.lab.service.dto.course.DeleteTeacher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * 教师服务测试：验证创建与删除逻辑
 */
class TeacherServiceImplTest {

    @Test
    void testCreateTeacher() {
        TeacherServiceImpl service = Mockito.spy(new TeacherServiceImpl());
        Mockito.doReturn(true).when(service).save(any(Teacher.class));

        CreateTeacher dto = new CreateTeacher();
        dto.setTeacherName("张老师");

        R r = service.createTeacher(dto);
        assertTrue(r.isOk());

        ArgumentCaptor<Teacher> captor = ArgumentCaptor.forClass(Teacher.class);
        Mockito.verify(service).save(captor.capture());
        assertEquals("张老师", captor.getValue().getTeacherName());
    }

    @Test
    void testDeleteTeacher_notFound() {
        TeacherServiceImpl service = Mockito.spy(new TeacherServiceImpl());
        Mockito.doReturn(null).when(service).getById(77L);

        DeleteTeacher dto = new DeleteTeacher();
        dto.setTeacherId(77L);
        R r = service.deleteTeacher(dto);
        assertFalse(r.isOk());
    }

    @Test
    void testDeleteTeacher_found() {
        TeacherServiceImpl service = Mockito.spy(new TeacherServiceImpl());
        Teacher t = new Teacher();
        t.setTeacherName("李老师");
        Mockito.doReturn(t).when(service).getById(7L);
        Mockito.doReturn(true).when(service).removeById(7L);

        DeleteTeacher dto = new DeleteTeacher();
        dto.setTeacherId(7L);
        R r = service.deleteTeacher(dto);
        assertTrue(r.isOk());
    }
}