package xyz.jasenon.lab.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.Date;

@Slf4j
public class DateTest {

    @Test
    public void todayInWeek(){
        Integer week = new Date().toInstant().atZone(ZoneId.of("Asia/Shanghai")).get(WeekFields.SUNDAY_START.dayOfWeek());
        log.info("今天是周:{}", week);
    }

}
