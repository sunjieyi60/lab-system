-- 测试数据

-- 学期数据
INSERT INTO semester (id, name, start_date, end_date) VALUES
(1, '2025-2026学年第一学期', '2025-09-01', '2026-01-15');

-- 课程数据
INSERT INTO course (id, course_name, volumn, grade) VALUES
(1, '高等数学', 60, '2024级'),
(2, '大学英语', 50, '2024级'),
(3, '程序设计基础', 45, '2024级');

-- 重置序列，确保后续插入的 ID 不会冲突
ALTER TABLE semester ALTER COLUMN id RESTART WITH 100;
ALTER TABLE course ALTER COLUMN id RESTART WITH 100;
ALTER TABLE course_schedule ALTER COLUMN id RESTART WITH 100;
