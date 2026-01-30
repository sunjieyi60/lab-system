package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.log.AlarmLog;
import xyz.jasenon.lab.service.dto.log.AlarmLogQueryDto;
import xyz.jasenon.lab.service.mapper.AlarmLogMapper;
import xyz.jasenon.lab.service.service.IAlarmLogService;

/**
 * 报警日志服务实现。
 */
@Service
public class AlarmLogServiceImpl extends ServiceImpl<AlarmLogMapper, AlarmLog> implements IAlarmLogService {

    @Override
    public Page<AlarmLog> pageAlarmLog(AlarmLogQueryDto query, Long pageNum, Long pageSize) {
        LambdaQueryWrapper<AlarmLog> wrapper = new LambdaQueryWrapper<>();
        if (query.getStartTime() != null) {
            wrapper.ge(AlarmLog::getAlarmTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(AlarmLog::getAlarmTime, query.getEndTime());
        }
        if (query.getCategories() != null && !query.getCategories().isEmpty()) {
            wrapper.in(AlarmLog::getCategory, query.getCategories());
        }
        if (query.getAlarmTypes() != null && !query.getAlarmTypes().isEmpty()) {
            wrapper.in(AlarmLog::getAlarmType, query.getAlarmTypes());
        }
        if (query.getRoom() != null && !query.getRoom().isEmpty()) {
            wrapper.eq(AlarmLog::getRoom, query.getRoom());
        }
        wrapper.orderByDesc(AlarmLog::getAlarmTime);

        long pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long ps = pageSize == null || pageSize < 1 ? 10 : pageSize;
        return this.page(new Page<>(pn, ps), wrapper);
    }
}

