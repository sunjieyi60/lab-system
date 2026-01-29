package xyz.jasenon.lab.service.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.jasenon.lab.common.entity.log.AlarmLog;

/**
 * 报警日志 Mapper。
 */
@Mapper
public interface AlarmLogMapper extends MPJBaseMapper<AlarmLog> {
}

