package com.zw.spring.demo.service.impl;

import com.zw.spring.demo.service.IDemoService;
import com.zw.spring.mvcframework.annotation.GPService;

/**
 * Created by Super on 2020/4/9.
 */
@GPService
public class DemoService implements IDemoService{
    public String get(String name){
        return "Myname is " + name;
    }
}
