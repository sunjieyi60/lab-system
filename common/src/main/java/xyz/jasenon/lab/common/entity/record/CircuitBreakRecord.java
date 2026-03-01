package xyz.jasenon.lab.common.entity.record;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("circuit_break_record")
public class CircuitBreakRecord extends BaseRecord {

    /**
     * 地址
     */
    private Integer address;

    /**
     * 设备断开
     */
    private Boolean isOpen;

    /**
     * 设备是否维修
     */
    private Boolean isFix;

    /**
     * 设备是否锁定
     */
    private Boolean isLock;

    /**
     * 设备电压
     */
    private Float voltage;

    /**
     * 设备电流
     */
    private Float current;

    /**
     * 设备功率
     */
    private Float power;

    /**
     * 设备能耗量
     */
    private Float energy;

    /**
     * 设备漏电
     */
    private Float leakage;

    /**
     * 线温
     */
    private Float temperature;

}
