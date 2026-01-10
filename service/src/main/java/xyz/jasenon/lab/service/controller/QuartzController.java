package xyz.jasenon.lab.service.controller;

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
@RestController
@RequestMapping("/quartz")
@CrossOrigin("*")
@RequiredArgsConstructor
public class QuartzController {

    private final ConfigLoader configLoader;
    private final QuartzRegister quartzRegister;

    @PostMapping("/create")
    public R createConfig(ScheduleConfigRoot scheduleConfigRoot){
        Result<Boolean> res = configLoader.configCreate(scheduleConfigRoot);
        return res.getData() ? R.success("创建成功") : R.fail(res.getMessage());
    }

}
