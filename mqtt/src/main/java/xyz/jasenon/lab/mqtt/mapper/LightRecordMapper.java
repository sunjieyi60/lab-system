package xyz.jasenon.lab.mqtt.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import xyz.jasenon.lab.common.entity.record.LightRecord;

@Mapper
public interface LightRecordMapper extends BaseMapper<LightRecord> {

}
