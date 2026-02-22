package xyz.jasenon.lab.class_time_table;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 智慧班牌应用主类
 * 
 * <p>当前版本：</p>
 * <ul>
 *   <li>基于 tio-protocol 的 QoS 机制</li>
 *   <li>保留 t-io TCP 长连接基础设施</li>
 *   <li>业务逻辑待重新规划</li>
 * </ul>
 * 
 * @author Jasenon_ce
 */
@SpringBootApplication
@EnableConfigurationProperties
public class ClassTimeTableApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClassTimeTableApplication.class, args);
    }

}
