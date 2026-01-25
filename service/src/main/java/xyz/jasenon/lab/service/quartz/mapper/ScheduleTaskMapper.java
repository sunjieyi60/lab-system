package xyz.jasenon.lab.service.quartz.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.jasenon.lab.service.quartz.model.ScheduleTask;

@Mapper
public interface ScheduleTaskMapper extends MPJBaseMapper<ScheduleTask> {
}
