package xyz.jasenon.lab.service.service;


import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.service.vo.overview.DeviceOverviewVo;
import xyz.jasenon.lab.service.vo.overview.middle.MiddleOverviewVo;

import java.util.List;
import java.util.Map;

public interface IOverviewService {

    List<MiddleOverviewVo> getAllLaboratories(Long semesterId);

    @Deprecated
    List<MiddleOverviewVo> getLaboratoriesOnClassing(Long semesterId);

    List<MiddleOverviewVo> getLaboratoriesSoonClassing(Long semesterId);

    @Deprecated
    List<MiddleOverviewVo> getLaboratoriesWithoutClass(Long semesterId);

    Map<DeviceType, DeviceOverviewVo> getLeftbarDetail();

    String NowSemesterDetailInfo(Long semesterId);

}
