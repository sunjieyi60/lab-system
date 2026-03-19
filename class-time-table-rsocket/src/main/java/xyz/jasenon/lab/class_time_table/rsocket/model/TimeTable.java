package xyz.jasenon.lab.class_time_table.rsocket.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 课表信息
 */
@Data
@Builder
public class TimeTable implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 课表 ID
     */
    private String id;
    
    /**
     * 课程名称
     */
    private String courseName;
    
    /**
     * 教师名称
     */
    private String teacherName;
    
    /**
     * 教室/实验室
     */
    private String roomName;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 星期几（1-7）
     */
    private Integer dayOfWeek;
    
    /**
     * 第几节课
     */
    private Integer section;
    
    /**
     * 班级/学生列表
     */
    private List<String> classGroups;
    
    /**
     * 备注
     */
    private String remark;
}
