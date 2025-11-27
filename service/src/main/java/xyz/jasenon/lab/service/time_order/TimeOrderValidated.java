package xyz.jasenon.lab.service.time_order;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Jasenon_ce
 * @date 2025/7/19
 */
@Constraint(validatedBy = TimeOrderValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TimeOrderValidated {
    String message() default "时间顺序不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
