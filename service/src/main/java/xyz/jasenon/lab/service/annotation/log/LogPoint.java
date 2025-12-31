package xyz.jasenon.lab.service.annotation.log;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogPoint {

    String title() default "未命名";

    String content() default "未定义";

    String sqEl() default "";

    Class<?> clazz() default Void.class;

}
