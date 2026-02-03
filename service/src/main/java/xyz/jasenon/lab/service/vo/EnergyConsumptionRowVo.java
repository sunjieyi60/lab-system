package xyz.jasenon.lab.service.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 能耗统计：单行（列表显示）。
 */
@Getter
@Setter
@Accessors(chain = true)
public class EnergyConsumptionRowVo {

    /**
     * 时间范围文案，如 "2025-09-01 8:00 至 2025-09-01 21:00"
     */
    private String timeRange;

    /**
     * 实验室编号
     */
    private String laboratoryNo;

    /**
     * 所属单位名称
     */
    private String deptName;

    /**
     * 开关名称/编号
     */
    private String switchName;

    /**
     * 开关总耗电量（Kwh）
     */
    private BigDecimal energyKwh;

    /**
     * 占比（百分比），如 "77.8"
     */
    private String proportion;
}
