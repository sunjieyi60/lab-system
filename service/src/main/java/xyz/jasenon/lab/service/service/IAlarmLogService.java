package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.log.AlarmLog;
import xyz.jasenon.lab.service.dto.log.AlarmLogQueryDto;

/**
 * 报警日志服务接口。
 */
public interface IAlarmLogService extends IService<AlarmLog> {

    /**
     * 按条件分页查询报警日志。
     */
    Page<AlarmLog> pageAlarmLog(AlarmLogQueryDto query, Long pageNum, Long pageSize);
}

