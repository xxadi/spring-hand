package com.zw.spring.demo.mvc.action;

import com.zw.spring.demo.service.IDemoService;
import com.zw.spring.mvcframework.annotation.GPAutowired;
import com.zw.spring.mvcframework.annotation.GPController;
import com.zw.spring.mvcframework.annotation.GPRequestMapping;
import com.zw.spring.mvcframework.annotation.GPRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Super on 2020/4/9.
 */
@GPController
@GPRequestMapping("/demo")
public class DemoAction {
    @GPAutowired
    private IDemoService demoService;

    @GPRequestMapping("/query.json")
    public void query(HttpServletRequest req, HttpServletResponse resp, @GPRequestParam("name") String name) {
        String result = demoService.get(name);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @GPRequestMapping("/add/json")
    public void add(HttpServletRequest req, HttpServletResponse resp, @GPRequestParam("a") Integer a, @GPRequestParam("b") Integer b) {
        try {
            resp.getWriter().write(a + "+" + b + "=" + (a + b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void remove(HttpServletRequest req,HttpServletResponse resp,@GPRequestParam("id") Integer id){

    }
}
