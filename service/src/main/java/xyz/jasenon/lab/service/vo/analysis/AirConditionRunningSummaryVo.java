package xyz.jasenon.lab.service.vo.analysis;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 空调运行时长统计：汇总行（总时长、实验室/单位/内机汇总、占比 100%）。
 */
@Getter
@Setter
@Accessors(chain = true)
public class AirConditionRunningSummaryVo {

    /**
     * 时间范围文案
     */
    private String timeRange;

    /**
     * 实验室编号汇总，如 "16-202、16-205"
     */
    private String laboratoryNos;

    /**
     * 所属单位名称（多单位时取首个或汇总）
     */
    private String deptName;

    /**
     * 空调内机汇总展示，如 "16-202-1..." 或 "全部"
     */
    private String acUnitSummary;

    /**
     * 运行总时长（小时）
     */
    private BigDecimal totalHours;

    /**
     * 占比，汇总行为 "100%"
     */
    private String proportion;
}
