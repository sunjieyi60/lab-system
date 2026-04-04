-- 测试数据

-- 学期数据
INSERT INTO semester (id, name, start_date, end_date) VALUES
(1, '2025-2026学年第一学期', '2025-09-01', '2026-01-15');

-- 课程数据
INSERT INTO course (id, course_name, volumn, grade) VALUES
(1, '高等数学', 60, '2024级'),
(2, '大学英语', 50, '2024级'),
(3, '程序设计基础', 45, '2024级');

-- 课表数据
-- 高等数学 - 实验室101 - 周一、周三 第1-2节 第1-16周
INSERT INTO course_schedule (id, semester_id, laboratory_id, week_type, start_week, end_week, start_time, end_time, weekdays, course_id, teacher_id, dept_id, start_section, end_section) VALUES
(1, 1, 101, 2, 1, 16, '08:00:00', '09:40:00', '[1,3]', 1, 1, 1, 1, 2);

-- 大学英语 - 实验室102 - 周二、周四 第3-4节 第1-8周
INSERT INTO course_schedule (id, semester_id, laboratory_id, week_type, start_week, end_week, start_time, end_time, weekdays, course_id, teacher_id, dept_id, start_section, end_section) VALUES
(2, 1, 102, 2, 1, 8, '10:00:00', '11:40:00', '[2,4]', 2, 2, 2, 3, 4);

-- 程序设计基础 - 实验室101 - 周五 第5-6节 第9-16周
INSERT INTO course_schedule (id, semester_id, laboratory_id, week_type, start_week, end_week, start_time, end_time, weekdays, course_id, teacher_id, dept_id, start_section, end_section) VALUES
(3, 1, 101, 2, 9, 16, '14:00:00', '15:40:00', '[5]', 3, 3, 1, 5, 6);

-- 重置序列，确保后续插入的 ID 不会冲突
ALTER TABLE semester ALTER COLUMN id RESTART WITH 100;
ALTER TABLE course ALTER COLUMN id RESTART WITH 100;
ALTER TABLE course_schedule ALTER COLUMN id RESTART WITH 100;
