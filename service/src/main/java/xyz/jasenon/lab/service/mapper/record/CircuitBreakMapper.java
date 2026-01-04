package xyz.jasenon.lab.service.mapper.record;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.jasenon.lab.common.entity.device.CircuitBreak;

@Mapper
public interface CircuitBreakMapper extends BaseMapper<CircuitBreak> {

}

