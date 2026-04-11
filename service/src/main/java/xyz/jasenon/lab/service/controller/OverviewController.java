package xyz.jasenon.lab.service.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.utils.DiyResponseEntity;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.service.IOverviewService;
import xyz.jasenon.lab.service.vo.overview.DeviceOverviewVo;
import xyz.jasenon.lab.service.vo.overview.middle.MiddleOverviewVo;

import java.util.List;
import java.util.Map;

/**
 * 实验室概览控制器
 * <p>
 * 提供实验室总览相关的 RESTful API，包括：
 * <ul>
 *     <li>获取所有实验室概览信息</li>
 *     <li>获取即将上课的实验室列表</li>
 *     <li>获取左侧边栏设备统计</li>
 * </ul>
 * </p>
 *
 * @author Jasenon
 * @see IOverviewService
 * @see MiddleOverviewVo
 * @see DeviceOverviewVo
 */
@Api(tags = "实验室概览")
@RestController
@RequestMapping("/overview")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class OverviewController {

    private final IOverviewService overviewService;

    @Autowired
    public OverviewController(IOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    /**
     * 获取所有实验室概览信息
     * <p>
     * 返回当前用户管理的所有实验室的完整概览，包括：
     * <ul>
     *     <li>实验室基本信息</li>
     *     <li>设备状态统计（在线/总数）</li>
     *     <li>当前课程安排</li>
     *     <li>环境传感器数据（温度、湿度等）</li>
     *     <li>今日是否有课标识</li>
     * </ul>
     * </p>
     *
     * @param semesterId 学期ID，用于确定当前教学周次和课程安排
     * @return 实验室概览信息列表
     */
    @ApiOperation(value = "获取所有实验室概览", 
                  notes = "获取当前用户管理的所有实验室的完整概览信息，包括设备状态、课程安排、环境数据等")
    @GetMapping("/laboratories")
    public DiyResponseEntity<R<List<MiddleOverviewVo>>> getAllLaboratories(
            @ApiParam(value = "学期ID", required = true, example = "1")
            @RequestParam Long semesterId) {
        List<MiddleOverviewVo> result = overviewService.getAllLaboratories(semesterId);
        return DiyResponseEntity.of(R.success(result));
    }

    /**
     * 获取即将上课的实验室列表
     * <p>
     * 查询未来45分钟内即将开始上课的实验室列表。
     * 用于提前准备上课环境、开启设备等场景。
     * </p>
     *
     * @param semesterId 学期ID，用于确定当前教学周次和课程安排
     * @return 即将上课的实验室概览信息列表
     */
    @ApiOperation(value = "获取即将上课的实验室", 
                  notes = "查询未来45分钟内即将开始上课的实验室列表")
    @GetMapping("/laboratories/soon")
    public DiyResponseEntity<R<List<MiddleOverviewVo>>> getLaboratoriesSoonClassing(
            @ApiParam(value = "学期ID", required = true, example = "1")
            @RequestParam Long semesterId) {
        List<MiddleOverviewVo> result = overviewService.getLaboratoriesSoonClassing(semesterId);
        return DiyResponseEntity.of(R.success(result));
    }

    /**
     * 获取左侧边栏设备统计详情
     * <p>
     * 统计当前用户管理的所有实验室的设备总数和在线数，按设备类型分组返回。
     * 设备类型包括：空调、照明、门禁、传感器、断路器等。
     * 在线状态通过检查设备心跳记录判断。
     * </p>
     *
     * @return 设备类型到概览信息的映射，包含总数和在线数
     */
    @ApiOperation(value = "获取设备统计概览", 
                  notes = "获取左侧边栏设备统计详情，按设备类型分组统计总数和在线数")
    @GetMapping("/devices/statistics")
    public DiyResponseEntity<R<Map<DeviceType, DeviceOverviewVo>>> getLeftbarDetail() {
        Map<DeviceType, DeviceOverviewVo> result = overviewService.getLeftbarDetail();
        return DiyResponseEntity.of(R.success(result));
    }

}
