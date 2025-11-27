package xyz.jasenon.lab.service.time_order;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * @author Jasenon_ce
 * @date 2025/8/24
 */
@Getter
public class TimeOrderException extends RuntimeException {

    private final Integer code;

    public TimeOrderException(Integer code,String message) {
        super(message);
        this.code = code;
    }

    public TimeOrderException(HttpStatus httpStatus, String message){
        super(message);
        this.code = httpStatus.value();
    }

    public static TimeOrderException deadTimeBeforeStartTime(){
        return new TimeOrderException(HttpStatus.CONFLICT,"截止时间早于起始时间");
    }

    public static TimeOrderException timeHasPassed(){
        return new TimeOrderException(HttpStatus.FORBIDDEN,"时间已过");
    }
}
