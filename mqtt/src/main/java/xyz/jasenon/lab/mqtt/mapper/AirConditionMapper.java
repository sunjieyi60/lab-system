package xyz.jasenon.lab.mqtt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.jasenon.lab.common.entity.device.AirCondition;

@Mapper
public interface AirConditionMapper extends BaseMapper<AirCondition> {

}

