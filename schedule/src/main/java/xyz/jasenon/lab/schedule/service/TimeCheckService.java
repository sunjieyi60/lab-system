package xyz.jasenon.lab.schedule.service;

import org.springframework.stereotype.Service;
import xyz.jasenon.lab.schedule.model.TimeRule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class TimeCheckService {

    public boolean pass(TimeRule rule) {
        if (rule == null) {
            return true;
        }
        ZoneId zone = ZoneId.of(rule.getTimezone() == null ? "Asia/Shanghai" : rule.getTimezone());
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDate today = now.toLocalDate();
        LocalTime time = now.toLocalTime();

        if (rule.getWeekdays() != null && !rule.getWeekdays().isEmpty()) {
            int d = now.getDayOfWeek().getValue();
            if (!rule.getWeekdays().contains(d)) {
                return false;
            }
        }
        if (rule.getWeekParity() != null) {
            int week = today.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
            if ("ODD".equalsIgnoreCase(rule.getWeekParity()) && week % 2 == 0) return false;
            if ("EVEN".equalsIgnoreCase(rule.getWeekParity()) && week % 2 == 1) return false;
        }
        if (rule.getDateRange() != null && rule.getDateRange().size() == 2) {
            LocalDate start = LocalDate.parse(rule.getDateRange().get(0));
            LocalDate end = LocalDate.parse(rule.getDateRange().get(1));
            if (today.isBefore(start) || today.isAfter(end)) {
                return false;
            }
        }
        if (rule.getBusinessTime() != null && !rule.getBusinessTime().isEmpty()) {
            boolean hit = false;
            for (String seg : rule.getBusinessTime()) {
                String[] arr = seg.split("-");
                if (arr.length != 2) continue;
                LocalTime s = LocalTime.parse(arr[0]);
                LocalTime e = LocalTime.parse(arr[1]);
                if (!time.isBefore(s) && !time.isAfter(e)) {
                    hit = true;
                    break;
                }
            }
            if (!hit) return false;
        }
        return true;
    }
}







