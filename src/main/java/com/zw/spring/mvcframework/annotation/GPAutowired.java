package com.zw.spring.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * Created by Super on 2020/4/9.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GPAutowired {
    String value() default "";
}
