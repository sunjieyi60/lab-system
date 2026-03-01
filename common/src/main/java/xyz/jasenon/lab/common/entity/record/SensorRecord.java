package xyz.jasenon.lab.common.entity.record;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("sensor_record")
public class SensorRecord extends BaseRecord {

    /**
     * 地址
     */
    private Integer address;

    /**
     * 设备id
     */
    private Integer selfId;

    /**
     * 温度
     */
    private Double temperature;

    /**
     * 湿度
     */
    private Double humidity;

    /**
     * 光照强度
     */
    private Double light;

    /**
     * 烟雾
     */
    private Integer smoke;

}
