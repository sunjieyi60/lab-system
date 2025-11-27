package xyz.jasenon.lab.service.time_order;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/7/19
 */
public class TimeOrderValidator implements ConstraintValidator<TimeOrderValidated, Object> {
    /**
     * 验证给定的对象是否满足时间顺序要求。
     *
     * @param object 要验证的对象
     * @param constraintValidatorContext 验证上下文
     * @return 如果对象满足时间顺序要求，则返回true；否则返回false
     */
    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {

        Class<?> clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();
        List<TimeOrder$> timeOrderList = new ArrayList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(TimeOrder.class)){
                TimeOrder timeOrder = field.getAnnotation(TimeOrder.class);
                try {
                    Object timeObj = field.get(object);
                    int order = timeOrder.order();
                    // 支持多种时间类型
                    if (timeObj instanceof LocalDateTime ||
                            timeObj instanceof LocalTime ||
                            timeObj instanceof LocalDate ||
                            timeObj instanceof Date) {
                        timeOrderList.add(new TimeOrder$(timeObj, order));
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        timeOrderList.sort((o1, o2) -> o1.order - o2.order);

        boolean flag = true;

        for(int i =0; i < timeOrderList.size() - 1; i++){
            flag = timeOrderList.get(i).isBefore(timeOrderList.get(i+1));
            if(!flag){
                throw TimeOrderException.deadTimeBeforeStartTime();
            }
//            if (timeOrderList.get(i).isBeforeNow()){
//                throw TimeOrderException.timeHasPassed();
//            }
        }

        return flag;
    }

    @AllArgsConstructor
    class TimeOrder$ {
        Object time;  // 改为Object以支持多种类型
        int order;

        // 添加比较方法
        public boolean isBefore(TimeOrder$ other) {
            if (time instanceof LocalDateTime) {
                return ((LocalDateTime) time).isBefore((LocalDateTime) other.time);
            } else if (time instanceof LocalTime) {
                return ((LocalTime) time).isBefore((LocalTime) other.time);
            } else if (time instanceof LocalDate) {
                return ((LocalDate) time).isBefore((LocalDate) other.time);
            } else if (time instanceof Date){
                return ((Date) time).before((Date) other.time);
            }
            return false;
        }

        public boolean isBeforeNow() {
            if (time instanceof LocalDateTime) {
                return ((LocalDateTime) time).isBefore(LocalDateTime.now());
            } else if (time instanceof LocalTime) {
                return ((LocalTime) time).isBefore(LocalTime.now());
            } else if (time instanceof LocalDate) {
                return ((LocalDate) time).isBefore(LocalDate.now());
            } else if (time instanceof Date){
                return ((Date) time).before(new Date());
            }
            return false;
        }
    }
}
