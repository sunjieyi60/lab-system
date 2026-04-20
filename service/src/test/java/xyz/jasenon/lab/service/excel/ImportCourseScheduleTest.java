package xyz.jasenon.lab.service.excel;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;
import xyz.jasenon.lab.common.entity.class_time_table.CourseSchedule;
import xyz.jasenon.lab.service.ServiceApplication;
import xyz.jasenon.lab.service.mapper.CourseMapper;
import xyz.jasenon.lab.service.mapper.CourseScheduleMapper;
import xyz.jasenon.lab.service.mapper.TeacherMapper;

import java.io.*;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ServiceApplication.class)
@ActiveProfiles("test")
class ImportCourseScheduleTest {

    @Autowired
    private ImportCourseSchedule importCourseSchedule;

    @Autowired
    private CourseScheduleMapper courseScheduleMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private CourseMapper courseMapper;

    /**
     * 普通 File 强制转换为 MultipartFile（不依赖 spring-test 的 MockMultipartFile）
     */
    public static class FileMultipartFile implements MultipartFile {
        private final File file;
        private final String name;
        private final String originalFilename;
        private final String contentType;

        public FileMultipartFile(File file, String name, String originalFilename, String contentType) {
            this.file = file;
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return file.length() == 0;
        }

        @Override
        public long getSize() {
            return file.length();
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(file.toPath());
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.copy(file.toPath(), dest.toPath());
        }
    }

    private MultipartFile toMultipartFile(File file) {
        return new FileMultipartFile(
                file,
                "file",
                file.getName(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
    }

    @Test
    @SneakyThrows
    void testImportSuccess() {
        // TODO: 替换为实际测试文件路径（文件内需包含正确格式数据，如：软件工程<>1-16周[1-2节]<>A101<>张三）
        String path = "/Users/Zhuanz/Documents/206.xls";
        File file = new File(path);
        assertTrue(file.exists(), "测试文件不存在，请替换 path 为实际 Excel 路径");

        long courseCountBefore = courseMapper.selectCount(null);
        long teacherCountBefore = teacherMapper.selectCount(null);
        long scheduleCountBefore = courseScheduleMapper.selectCount(null);

        MultipartFile multipartFile = toMultipartFile(file);
        ImportResult result = importCourseSchedule.importCourse(multipartFile, 1L, 1L);

        assertNotNull(result);
        assertTrue(result.getOk() > 0, "至少应有一条成功记录");

        long courseCountAfter = courseMapper.selectCount(null);
        long teacherCountAfter = teacherMapper.selectCount(null);
        long scheduleCountAfter = courseScheduleMapper.selectCount(null);

        assertTrue(courseCountAfter > courseCountBefore, "course 表应新增记录");
        assertTrue(scheduleCountAfter > scheduleCountBefore, "course_schedule 表应新增记录");
    }
}
