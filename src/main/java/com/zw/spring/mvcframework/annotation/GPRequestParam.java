package com.zw.spring.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * Created by Super on 2020/4/9.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GPRequestParam {
    String value() default "";
}
