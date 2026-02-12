package xyz.jasenon.lab.service.vo.analysis;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 能耗统计：汇总行（总能耗、实验室/单位/智能空开汇总、100%）。
 */
@Getter
@Setter
@Accessors(chain = true)
public class EnergyConsumptionSummaryVo {

    /**
     * 时间范围
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
     * 智能空开汇总展示，如 "16-202-1..." 或 "全部"
     */
    private String switchSummary;

    /**
     * 总能耗（Kwh）
     */
    private BigDecimal totalKwh;

    /**
     * 占比，汇总行为 "100%"
     */
    private String proportion;
}
