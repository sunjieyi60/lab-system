package xyz.jasenon.lab.mqtt.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import xyz.jasenon.lab.common.entity.device.Access;

@Mapper
public interface AccessMapper extends BaseMapper<Access> {

}