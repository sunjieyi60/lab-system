package xyz.jasenon.lab.service.quartz.model;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.record.BaseRecord;
import xyz.jasenon.lab.service.vo.device.DeviceRecordVo;

import java.util.Map;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Getter
@Setter
public class Data {

    /**
     * 数据ID
     */
    private String id;

    /**
     * 任务组id
     */
    private String scheduleTaskId;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 设备类型
     */
    private DeviceType deviceType;

    /**
     * 数据 Object<? extends BaseRecord>
     */
    @TableField(exist = false)
    private DeviceRecordVo<? extends BaseRecord> value;

    public Map<String,Object> value2Map(){
        return BeanUtil.beanToMap(value.getData());
    }

}
