-- 基础设置
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- building
CREATE TABLE IF NOT EXISTS building (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  building_name VARCHAR(64) NOT NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- dept
CREATE TABLE IF NOT EXISTS dept (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dept_name VARCHAR(64) NOT NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- dept_building
CREATE TABLE IF NOT EXISTS dept_building (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dept_id BIGINT NOT NULL,
  building_id BIGINT NOT NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0,
  INDEX idx_dept_building_dept_id (dept_id),
  INDEX idx_dept_building_building_id (building_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- dept_user
CREATE TABLE IF NOT EXISTS dept_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dept_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0,
  INDEX idx_dept_user_dept_id (dept_id),
  INDEX idx_dept_user_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- laboratory
CREATE TABLE IF NOT EXISTS laboratory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  laboratory_id VARCHAR(64) NOT NULL,
  laboratory_name VARCHAR(128) NOT NULL,
  belong_to_depts JSON NULL,
  belong_to_building BIGINT NULL,
  security_level VARCHAR(32) NULL,
  class_capacity INT NULL,
  area INT NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0,
  INDEX idx_laboratory_building (belong_to_building)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- laboratory_manager
CREATE TABLE IF NOT EXISTS laboratory_manager (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  laboratory_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0,
  INDEX idx_lab_manager_lab_id (laboratory_id),
  INDEX idx_lab_manager_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- laboratory_user
CREATE TABLE IF NOT EXISTS laboratory_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  laboratory_id BIGINT NOT NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0,
  INDEX idx_lab_user_lab_id (laboratory_id),
  INDEX idx_lab_user_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- user
CREATE TABLE IF NOT EXISTS user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(64) NOT NULL,
  real_name VARCHAR(64) NULL,
  phone VARCHAR(16) NULL,
  email VARCHAR(128) NULL,
  create_by BIGINT NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0,
  INDEX idx_user_phone (phone),
  INDEX idx_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- device（统一设备表，承载多态字段）
CREATE TABLE IF NOT EXISTS device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_name VARCHAR(128) NULL,
  device_type VARCHAR(32) NOT NULL, -- 存储枚举名：AirCondition/Light/Access/Sensor/CircuitBreak
  belong_to_laboratory_id BIGINT NULL,
  address INT NULL,
  self_id INT NULL,
  rs485_gateway_id BIGINT NULL,
  socket_gateway_id BIGINT NULL,
  group_id VARCHAR(64) NULL,
  is_lock TINYINT(1) NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0,
  INDEX idx_device_lab (belong_to_laboratory_id),
  INDEX idx_device_type (device_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- rs485_gateway
CREATE TABLE IF NOT EXISTS rs485_gateway (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  gateway_name VARCHAR(64) NOT NULL,
  send_topic VARCHAR(128) NOT NULL,
  accept_topic VARCHAR(128) NOT NULL,
  belong_to_laboratory_id BIGINT NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- socket_gateway
CREATE TABLE IF NOT EXISTS socket_gateway (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  mac VARCHAR(32) NULL,
  ip VARCHAR(64) NULL,
  belong_to_laboratory_id BIGINT NULL,
  gateway_name VARCHAR(64) NOT NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- course
CREATE TABLE IF NOT EXISTS course (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_name VARCHAR(128) NOT NULL,
  volumn INT NOT NULL,
  grade VARCHAR(16) NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- semester
CREATE TABLE IF NOT EXISTS semester (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- teacher
CREATE TABLE IF NOT EXISTS teacher (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_name VARCHAR(64) NOT NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- course_schedule（包含 Schedule 基类字段）
CREATE TABLE IF NOT EXISTS course_schedule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  semester_id BIGINT NOT NULL,
  laboratory_id BIGINT NOT NULL,
  week_type TINYINT NOT NULL,       -- WeekType@EnumValue 映射整型：Single(0)、Double(1)、Both(2)
  start_week INT NOT NULL,
  end_week INT NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  weekdays JSON NOT NULL,           -- JacksonTypeHandler 建议存储 JSON 数组
  course_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  dept_id BIGINT NULL,
  start_section INT NULL,
  end_section INT NULL,
  mark TEXT NULL,
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0,
  INDEX idx_cs_semester (semester_id),
  INDEX idx_cs_lab (laboratory_id),
  INDEX idx_cs_teacher (teacher_id),
  INDEX idx_cs_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- access_record
CREATE TABLE IF NOT EXISTS access_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  create_time DATETIME NULL,
  rs485_id BIGINT NULL,
  address INT NULL,
  self_id INT NULL,
  is_open TINYINT(1) NULL,
  is_lock TINYINT(1) NULL,
  lock_status INT NULL,
  delay_time INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- air_condition_record
CREATE TABLE IF NOT EXISTS air_condition_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  create_time DATETIME NULL,
  rs485_id BIGINT NULL,
  address INT NULL,
  self_id INT NULL,
  is_open TINYINT(1) NULL,
  mode VARCHAR(16) NULL,
  temperature INT NULL,
  speed VARCHAR(16) NULL,
  room_temperature INT NULL,
  error_code INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- circuit_break_record
CREATE TABLE IF NOT EXISTS circuit_break_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  create_time DATETIME NULL,
  rs485_id BIGINT NULL,
  address INT NULL,
  is_open TINYINT(1) NULL,
  is_fix TINYINT(1) NULL,
  is_lock TINYINT(1) NULL,
  voltage FLOAT NULL,
  current FLOAT NULL,
  power FLOAT NULL,
  energy FLOAT NULL,
  leakage FLOAT NULL,
  temperature FLOAT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- light_record
CREATE TABLE IF NOT EXISTS light_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  create_time DATETIME NULL,
  rs485_id BIGINT NULL,
  address INT NULL,
  self_id INT NULL,
  is_open TINYINT(1) NULL,
  is_lock TINYINT(1) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- sensor_record
CREATE TABLE IF NOT EXISTS sensor_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  create_time DATETIME NULL,
  rs485_id BIGINT NULL,
  address INT NULL,
  self_id INT NULL,
  temperature DOUBLE NULL,
  humidity DOUBLE NULL,
  light DOUBLE NULL,
  smoke INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- user_permission
CREATE TABLE IF NOT EXISTS user_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  permission VARCHAR(64) NOT NULL,  -- 枚举名
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  deleted TINYINT(1) DEFAULT 0,
  INDEX idx_up_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 依赖：所有表已存在（来自 db.sql），引擎 InnoDB，字段类型匹配

ALTER TABLE dept_building
  ADD CONSTRAINT fk_dept_building_dept
    FOREIGN KEY (dept_id) REFERENCES dept(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_dept_building_building
    FOREIGN KEY (building_id) REFERENCES building(id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE dept_user
  ADD CONSTRAINT fk_dept_user_dept
    FOREIGN KEY (dept_id) REFERENCES dept(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_dept_user_user
    FOREIGN KEY (user_id) REFERENCES user(id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE laboratory
  ADD CONSTRAINT fk_laboratory_building
    FOREIGN KEY (belong_to_building) REFERENCES building(id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE laboratory_manager
  ADD CONSTRAINT fk_lab_manager_lab
    FOREIGN KEY (laboratory_id) REFERENCES laboratory(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_lab_manager_user
    FOREIGN KEY (user_id) REFERENCES user(id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE laboratory_user
  ADD CONSTRAINT fk_lab_user_lab
    FOREIGN KEY (laboratory_id) REFERENCES laboratory(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_lab_user_user
    FOREIGN KEY (user_id) REFERENCES user(id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE device
  ADD CONSTRAINT fk_device_lab
    FOREIGN KEY (belong_to_laboratory_id) REFERENCES laboratory(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_device_rs485
    FOREIGN KEY (rs485_gateway_id) REFERENCES rs485_gateway(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_device_socket
    FOREIGN KEY (socket_gateway_id) REFERENCES socket_gateway(id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE rs485_gateway
  ADD CONSTRAINT fk_rs485_lab
    FOREIGN KEY (belong_to_laboratory_id) REFERENCES laboratory(id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE socket_gateway
  ADD CONSTRAINT fk_socket_lab
    FOREIGN KEY (belong_to_laboratory_id) REFERENCES laboratory(id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE course_schedule
  ADD CONSTRAINT fk_cs_semester
    FOREIGN KEY (semester_id) REFERENCES semester(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_cs_lab
    FOREIGN KEY (laboratory_id) REFERENCES laboratory(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_cs_course
    FOREIGN KEY (course_id) REFERENCES course(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_cs_teacher
    FOREIGN KEY (teacher_id) REFERENCES teacher(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_cs_dept
    FOREIGN KEY (dept_id) REFERENCES dept(id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE access_record
  ADD CONSTRAINT fk_access_record_rs485
    FOREIGN KEY (rs485_id) REFERENCES rs485_gateway(id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE air_condition_record
  ADD CONSTRAINT fk_air_record_rs485
    FOREIGN KEY (rs485_id) REFERENCES rs485_gateway(id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE circuit_break_record
  ADD CONSTRAINT fk_cb_record_rs485
    FOREIGN KEY (rs485_id) REFERENCES rs485_gateway(id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE light_record
  ADD CONSTRAINT fk_light_record_rs485
    FOREIGN KEY (rs485_id) REFERENCES rs485_gateway(id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE sensor_record
  ADD CONSTRAINT fk_sensor_record_rs485
    FOREIGN KEY (rs485_id) REFERENCES rs485_gateway(id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE user_permission
  ADD CONSTRAINT fk_user_permission_user
    FOREIGN KEY (user_id) REFERENCES user(id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- 可选：用户创建者
ALTER TABLE user
  ADD CONSTRAINT fk_user_created_by
    FOREIGN KEY (create_by) REFERENCES user(id)
    ON DELETE RESTRICT ON UPDATE CASCADE;
    
SET FOREIGN_KEY_CHECKS = 1;