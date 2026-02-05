package xyz.jasenon.lab.class_time_table;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClassTimeTableApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClassTimeTableApplication.class, args);
    }

}
