package xyz.jasenon.lab.mqtt.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import xyz.jasenon.lab.common.entity.device.CircuitBreak;

@Mapper
public interface CircuitBreakMapper extends BaseMapper<CircuitBreak> {

}

