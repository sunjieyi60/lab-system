package xyz.jasenon.lab.common.entity.record;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("access_record")
public class AccessRecord extends BaseRecord {

    /**
     * 门禁地址 
     */
    private Integer address;

    /**
     * 门禁状态 1:开,0:关
     */
    private Boolean isOpen;

    /**
     * 门禁通电状态: 1:通电,0:断电
     */
    private Boolean isLock;

    /**
     * 门禁锁定状态: 
     */
    private Integer lockStatus;

    /**
     * 延时时间
     * 单位: 秒
     */
    private Integer delayTime;


}
