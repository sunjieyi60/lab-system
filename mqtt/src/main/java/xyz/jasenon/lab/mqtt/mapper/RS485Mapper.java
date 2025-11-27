package xyz.jasenon.lab.mqtt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.jasenon.lab.common.entity.device.gateway.RS485Gateway;

@Mapper
public interface RS485Mapper extends BaseMapper<RS485Gateway> {
    
}
