package xyz.jasenon.lab.service.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 数据分析接口返回：三个总数 + 按周次/星期/节次的分布，供前端画图。
 */
@Getter
@Setter
@Accessors(chain = true)
public class AnalysisChartVo {

    /**
     * 课程数（排课条数）
     */
    private Long totalCourseCount;

    /**
     * 学时数（节数之和）
     */
    private Long totalSectionCount;

    /**
     * 人学时数（节数×选课人数）
     */
    private Long totalStudentSectionCount;

    /**
     * 按周次分布（label=周次 1..N）
     */
    private List<DimensionPointVo> byWeek;

    /**
     * 按星期几分布（label=1..7 周一至周日）
     */
    private List<DimensionPointVo> byWeekday;

    /**
     * 按节次分布（label 如 "1-2","3-4" 或节次号）
     */
    private List<DimensionPointVo> bySection;
}
