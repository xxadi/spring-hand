package com.zw.spring.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * Created by Super on 2020/4/9.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GPService {
    String value() default "";
}
