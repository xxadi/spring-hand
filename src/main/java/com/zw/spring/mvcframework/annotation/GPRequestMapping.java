package com.zw.spring.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * Created by Super on 2020/4/9.
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GPRequestMapping {
    String value() default "";
}
