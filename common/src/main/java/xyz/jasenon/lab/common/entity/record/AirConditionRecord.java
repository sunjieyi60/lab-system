package xyz.jasenon.lab.common.entity.record;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("air_condition_record")
public class AirConditionRecord extends BaseRecord {

    /**
     * 地址
     */
    private Integer address;

    /**
     * 设备id
     */
    private Integer selfId;

    /**
     * 是否开启: 1:是,0:否
     */
    private Boolean isOpen;

    /**
     * 模式: 1:制冷,2:制热,3:除湿,4:送风,5:自动
     */
    private String mode;

    /**
     * 温度: 单位为摄氏度,范围在16-30度之间
     */
    private Integer temperature;

    /**
     * 风速: 1:低,2:中,3:高
     */
    private String speed;

    /**
     * 房间温度: 单位为摄氏度,范围在16-30度之间
     */
    private Integer roomTemperature;

    /**
     * 错误码: 0:正常,1:设备故障,2:通信失败
     */
    private Integer errorCode;

}
