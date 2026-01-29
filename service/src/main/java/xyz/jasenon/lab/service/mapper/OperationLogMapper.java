package xyz.jasenon.lab.service.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.jasenon.lab.common.entity.log.OperationLog;

/**
 * 操作日志 Mapper。
 */
@Mapper
public interface OperationLogMapper extends MPJBaseMapper<OperationLog> {
}

