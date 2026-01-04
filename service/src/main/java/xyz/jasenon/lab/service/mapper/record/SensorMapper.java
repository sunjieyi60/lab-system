package xyz.jasenon.lab.service.mapper.record;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.jasenon.lab.common.entity.device.Sensor;

@Mapper
public interface SensorMapper extends BaseMapper<Sensor> {

}

