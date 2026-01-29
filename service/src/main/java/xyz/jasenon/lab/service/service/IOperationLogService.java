package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.log.OperationLog;
import xyz.jasenon.lab.service.dto.log.OperationLogQueryDto;

/**
 * 操作日志服务接口。
 */
public interface IOperationLogService extends IService<OperationLog> {

    /**
     * 按条件分页查询操作日志。
     */
    Page<OperationLog> pageOperationLog(OperationLogQueryDto query, Long pageNum, Long pageSize);
}

