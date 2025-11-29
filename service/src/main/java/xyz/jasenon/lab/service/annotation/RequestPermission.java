package xyz.jasenon.lab.service.annotation;

import xyz.jasenon.lab.service.constants.Permissions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Jasenon_ce
 * @date 2025/11/29
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestPermission {

    Permissions[] allowed() default {};

}
