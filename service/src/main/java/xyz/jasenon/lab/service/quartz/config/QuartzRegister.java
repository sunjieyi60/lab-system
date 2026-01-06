package xyz.jasenon.lab.service.quartz.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.service.quartz.service.TaskRuntimeService;

@Component
public class QuartzRegister implements InitializingBean {

    @Autowired
    private TaskRuntimeService taskRuntimeService;

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
