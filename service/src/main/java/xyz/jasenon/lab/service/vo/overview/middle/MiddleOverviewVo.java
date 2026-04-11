package xyz.jasenon.lab.service.vo.overview.middle;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.entity.record.SensorRecord;
import xyz.jasenon.lab.service.vo.base.CourseScheduleVo;
import xyz.jasenon.lab.service.vo.device.DeviceRecordVo;
import xyz.jasenon.lab.service.vo.overview.DeviceOverviewVo;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MiddleOverviewVo {

    /**
     * 实验室信息
     */
    private Laboratory laboratory;
    /**
     * 设备信息  按类型
     */
    private Map<DeviceType, DeviceOverviewVo> devices;
    /**
     * 课程信息
     */
    private CourseScheduleVo course;
    /**
     * 是否今日有课
     */
    private Boolean hasCourse;
    /**
     * 环境信息
     */
    private DeviceRecordVo<SensorRecord> env;

}
