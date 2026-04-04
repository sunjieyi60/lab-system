package xyz.jasenon.classtimetable.dto

import java.time.LocalTime
import xyz.jasenon.classtimetable.constants.WeekType

/**
 * 课程表DTO
 * 使用Kotlin自动生成的getter/setter
 * 
 * @author Jasenon_ce
 * @date 2025/11/29
 */
data class CourseScheduleDto(
    /**
     * 课程表ID
     */
    val id: Long? = null,

    /**
     * 课程ID
     */
    val courseId: Long? = null,

    /**
     * 课程名称
     */
    val courseName: String? = null,

    /**
     * 教师ID
     */
    val teacherId: Long? = null,

    /**
     * 教师名称
     */
    val teacherName: String? = null,

    /**
     * 部门id
     */
    val deptId: Long? = null,

    /**
     * 部门名称
     */
    val deptName: String? = null,

    /**
     * 学期ID
     */
    val semesterId: Long? = null,

    /**
     * 学期名称
     */
    val semesterName: String? = null,

    /**
     * 实验室ID
     */
    val laboratoryId: Long? = null,

    /**
     * 实验室名称
     */
    val laboratoryName: String? = null,

    /**
     * 周类型
     */
    val weekType: WeekType? = null,

    /**
     * 开始周
     */
    val startWeek: Int? = null,

    /**
     * 结束周
     */
    val endWeek: Int? = null,

    /**
     * 开始时间
     */
    val startTime: LocalTime? = null,

    /**
     * 结束时间
     */
    val endTime: LocalTime? = null,

    /**
     * 星期几
     */
    val weekdays: MutableList<Int?>? = null,

    /**
     * 开始节次
     */
    val startSection: Int? = null,

    /**
     * 结束节次
     */
    val endSection: Int? = null,

    /**
     * 备注信息
     */
    val mark: String? = null
)
