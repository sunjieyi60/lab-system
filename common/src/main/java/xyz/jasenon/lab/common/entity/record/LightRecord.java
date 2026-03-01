package xyz.jasenon.lab.common.entity.record;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("light_record")
public class LightRecord extends BaseRecord {

    /**
     * 地址
     */
    private Integer address;

    /**
     * 设备id
     */
    private Integer selfId;

    /**
     * 是否开启
     */
    private Boolean isOpen;

    /**
     * 是否锁定
     */
    private Boolean isLock;

}
