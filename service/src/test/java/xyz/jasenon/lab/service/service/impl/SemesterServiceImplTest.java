package xyz.jasenon.lab.service.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import xyz.jasenon.lab.common.entity.class_time_table.Semester;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.course.CreateSemester;
import xyz.jasenon.lab.service.dto.course.DeleteSemester;
import xyz.jasenon.lab.service.dto.course.EditSemester;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * 学期服务测试：验证创建与编辑（BeanUtil CopyOptions）逻辑
 */
class SemesterServiceImplTest {

    @Test
    void testCreateSemester_usesDtoValues() {
        SemesterServiceImpl service = Mockito.spy(new SemesterServiceImpl());
        Mockito.doReturn(true).when(service).save(any(Semester.class));

        CreateSemester dto = new CreateSemester()
                .setName("2025-2026 第1学年")
                .setStartDate(LocalDate.of(2025, 9, 1))
                .setEndDate(LocalDate.of(2026, 1, 15));

        R r = service.createSemester(dto);
        assertTrue(r.isOk());

        ArgumentCaptor<Semester> captor = ArgumentCaptor.forClass(Semester.class);
        Mockito.verify(service).save(captor.capture());
        Semester saved = captor.getValue();
        assertEquals("2025-2026 第1学年", saved.getName());
        assertEquals(LocalDate.of(2025, 9, 1), saved.getStartDate());
        assertEquals(LocalDate.of(2026, 1, 15), saved.getEndDate());
    }

    @Test
    void testEditSemester_beanUtilCopyOptionsIgnoreNull() {
        SemesterServiceImpl service = Mockito.spy(new SemesterServiceImpl());
        Semester existing = new Semester();
        existing.setName("2024-2025 第2学年");
        existing.setStartDate(LocalDate.of(2024, 2, 25));
        existing.setEndDate(LocalDate.of(2024, 7, 10));

        Mockito.doReturn(existing).when(service).getById(1L);
        Mockito.doReturn(true).when(service).updateById(any(Semester.class));

        EditSemester edit = new EditSemester()
                .setSemesterId(1L)
                .setName(null) // 应忽略
                .setStartDate(LocalDate.of(2025, 9, 1))
                .setEndDate(LocalDate.of(2026, 1, 15));

        R r = service.editSemester(edit);
        assertTrue(r.isOk());
        assertEquals("2024-2025 第2学年", existing.getName());
        assertEquals(LocalDate.of(2025, 9, 1), existing.getStartDate());
        assertEquals(LocalDate.of(2026, 1, 15), existing.getEndDate());
    }

    @Test
    void testDeleteSemester_notFound() {
        SemesterServiceImpl service = Mockito.spy(new SemesterServiceImpl());
        Mockito.doReturn(null).when(service).getById(88L);

        DeleteSemester dto = new DeleteSemester();
        dto.setSemesterId(88L);
        R r = service.deleteSemester(dto);
        assertFalse(r.isOk());
    }
}