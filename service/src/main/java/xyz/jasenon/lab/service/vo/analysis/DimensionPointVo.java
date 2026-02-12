package xyz.jasenon.lab.service.vo.analysis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 一个维度上的一个点：维度值 + 课程数 / 学时数 / 人学时数。
 * 前端可直接用于柱状图 series.data 或 xAxis + 多系列。
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class DimensionPointVo {

    /**
     * 维度取值，如周次 1、星期 1、节次 "1-2"
     */
    private Object label;

    /**
     * 课程数（排课条数）
     */
    private Long courseCount;

    /**
     学时数（节数之和）
     */
    private Long sectionCount;

    /**
     * 人学时数（节数 × 选课人数）
     */
    private Long studentSectionCount;
}
