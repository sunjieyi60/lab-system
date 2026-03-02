package xyz.jasenon.lab.service.quartz.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.jasenon.lab.common.command.CommandLine;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.service.quartz.model.Action;

import java.util.List;
import java.util.Set;

@Mapper
public interface ActionMapper extends MPJBaseMapper<Action> {

    /**
     * 根据设备/指令筛选获取任务ID集合
     */
    Set<String> selectTaskIdsByFilter(@Param("labId") Long labId,
                                       @Param("enable") Boolean enable,
                                       @Param("deviceType") DeviceType deviceType,
                                       @Param("deviceId") Long deviceId,
                                       @Param("commandLine") CommandLine commandLine);

    /**
     * 批量查询多个任务的动作
     */
    List<Action> selectListByTaskIds(@Param("taskIds") List<String> taskIds);
}
