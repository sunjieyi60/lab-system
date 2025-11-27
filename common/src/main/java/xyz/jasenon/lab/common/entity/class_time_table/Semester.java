package xyz.jasenon.lab.common.entity.class_time_table;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.BaseEntity;

import java.time.LocalDate;

@Getter
@Setter
public class Semester extends BaseEntity {

    /**
     * 学期名称，2025-2026-1 存在正则(\\d{4})-(\\d{4}) 第(\\d+)学年
     */
    private String name;
    
    /**
     * 学期开始时间
     */
    private LocalDate startDate;

    /**
     * 学期结束时间
     */
    private LocalDate endDate;
}
