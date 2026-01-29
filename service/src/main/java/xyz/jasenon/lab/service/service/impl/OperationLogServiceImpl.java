package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.log.OperationLog;
import xyz.jasenon.lab.service.dto.log.OperationLogQueryDto;
import xyz.jasenon.lab.service.mapper.OperationLogMapper;
import xyz.jasenon.lab.service.service.IOperationLogService;

/**
 * 操作日志服务实现。
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements IOperationLogService {

    @Override
    public Page<OperationLog> pageOperationLog(OperationLogQueryDto query, Long pageNum, Long pageSize) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (query.getStartTime() != null) {
            wrapper.ge(OperationLog::getOperateTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(OperationLog::getOperateTime, query.getEndTime());
        }
        if (query.getLogType() != null && !query.getLogType().isEmpty()) {
            wrapper.eq(OperationLog::getLogType, query.getLogType());
        }
        if (query.getAccount() != null && !query.getAccount().isEmpty()) {
            wrapper.eq(OperationLog::getOperatorAccount, query.getAccount());
        }
        if (query.getName() != null && !query.getName().isEmpty()) {
            wrapper.like(OperationLog::getOperatorName, query.getName());
        }
        if (query.getRoom() != null && !query.getRoom().isEmpty()) {
            wrapper.eq(OperationLog::getRoom, query.getRoom());
        }
        if (query.getDevice() != null && !query.getDevice().isEmpty()) {
            wrapper.eq(OperationLog::getDevice, query.getDevice());
        }
        wrapper.orderByDesc(OperationLog::getOperateTime);

        long pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long ps = pageSize == null || pageSize < 1 ? 10 : pageSize;
        return this.page(new Page<>(pn, ps), wrapper);
    }
}

