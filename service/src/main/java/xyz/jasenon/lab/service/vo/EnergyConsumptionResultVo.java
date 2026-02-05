package xyz.jasenon.lab.service.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 能耗统计接口返回：总能耗、列表、饼图、汇总行。
 */
@Getter
@Setter
@Accessors(chain = true)
public class EnergyConsumptionResultVo {

    /**
     * 总能耗（Kwh）
     */
    private BigDecimal totalKwh;

    /**
     * 列表行（列表显示）
     */
    private List<EnergyConsumptionRowVo> list;

    /**
     * 饼图扇区（饼图显示）
     */
    private List<EnergyConsumptionChartSegmentVo> chartSegments;

    /**
     * 汇总行：时间范围、实验室/单位/智能空开汇总、总能耗、100%
     */
    private EnergyConsumptionSummaryVo summaryRow;
}
