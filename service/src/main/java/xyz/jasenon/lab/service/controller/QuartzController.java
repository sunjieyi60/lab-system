package xyz.jasenon.lab.service.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.quartz.check.Result;
import xyz.jasenon.lab.service.quartz.config.QuartzRegister;
import xyz.jasenon.lab.service.quartz.model.ScheduleConfigRoot;
import xyz.jasenon.lab.service.quartz.service.ConfigLoader;

/**
 * @author Jasenon_ce
 * @date 2026/1/10
 */
@Api("定时任务")
@RestController
@RequestMapping("/quartz")
@CrossOrigin("*")
@RequiredArgsConstructor
public class QuartzController {

    private final ConfigLoader configLoader;
    private final QuartzRegister quartzRegister;

    @PostMapping("/create")
    @ApiOperation("创建定时任务")
    public R createConfig(ScheduleConfigRoot scheduleConfigRoot){
        Result<Boolean> res = configLoader.configCreate(scheduleConfigRoot);
        return res.getData() ? R.success("创建成功") : R.fail(res.getMessage());
    }

}
