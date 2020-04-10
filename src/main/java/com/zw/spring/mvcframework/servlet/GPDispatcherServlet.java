package com.zw.spring.mvcframework.servlet;

import com.zw.spring.mvcframework.annotation.GPAutowired;
import com.zw.spring.mvcframework.annotation.GPController;
import com.zw.spring.mvcframework.annotation.GPRequestMapping;
import com.zw.spring.mvcframework.annotation.GPService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * Created by Super on 2020/4/9.
 * 此类作为启动入口
 */
public class GPDispatcherServlet extends HttpServlet{

    private static final long serialVersionUID = 1L;
    //跟web.xml中param-name的值一样
    private static final String LOCATION = "contextConfigLocation";
    //保存所有的配置信息
    private Properties p = new Properties();
    //保存所有被扫描到的相关类名
    private List<String> classNames = new ArrayList<String>();
    //核心ioc容器保存所有初始化的bean
    private Map<String, Object> ioc = new HashMap<String,Object>();
    //保存所有的url和方法的映射关系 key是url
    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

    public GPDispatcherServlet() {
        super();
    }

    //当Servlet容器启动时，会调用GPDispatcherServlet的init()方法，
    // 从init方法的参数中，我们可以拿到主配置文件的路径，
    // 从能够读取到配置文件中的信息。
    // 前面我们已经介绍了Spring的三个阶段，现在来完成初始化阶段的代码。
    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.加载配置文件
        doLoadConfig(config.getInitParameter(LOCATION));
        //2.扫描所有相关的类
        doScanner(p.getProperty("scanPackage"));
        //3.初始化需要加载的类存入ioc容器中
        doInstance();
        //4.依赖注入
        doAutowired();
        //5.构造HandlerMapping
        initHandlerMapping();
        //6.等待请求，匹配url 定位方法 反射调用执行
        //调用doGet或者doPost方法
        //提示
        System.out.println("中恒 spring 框架初始化完成");
    }

    //1.加载配置文件
    private void doLoadConfig(String location) {
        InputStream fis = null;

        try {
            fis = this.getClass().getClassLoader().getResourceAsStream(location);
            //读取配置文件
            p.load(fis);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //2.扫描所有相关的类
    private void doScanner(String packageName) {
        //将所有的包路径转换为文件路径
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            //如果是文件夹，继续递归
            if (file.isDirectory()) {
                doScanner(packageName + "." + file.getName());
            } else {
                //保存所有被扫描到的相关类名
                classNames.add(packageName + "." + file.getName().replace(".class", "").trim());
            }
        }
    }

    //3.初始化需要加载的类存入ioc容器中
    private void doInstance() {
        if (classNames.size() == 0) return;
        try {
            for (String className : classNames) {
                //反射
                Class<?> clazz = Class.forName(className);
                //如果类中包含controller注解 存入ioc中
                if (clazz.isAnnotationPresent(GPController.class)) {
                    //getName ----“实体名称” ---- com.se7en.test.Main
                    //getSimpleName ---- “底层类简称” ---- Main
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    //将类初始化放入ioc容器中去
                    ioc.put(beanName, clazz.newInstance());

                } else if (clazz.isAnnotationPresent(GPService.class)) {
                    //如果包含service
                    GPService service = clazz.getAnnotation(GPService.class);
                    //获取servcie的value值
                    String beanName = service.value();
                    //如果用户自己设置了名字
                    if (!"".equals(beanName.trim())) {
                        ioc.put(beanName, clazz.newInstance());
                        //调出循环进行下一次循环
                        continue;
                    }
                    //如果自己没有设置
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces) {
                        ioc.put(i.getName(), clazz.newInstance());
                    }
                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //4.依赖注入
    private void doAutowired() {
        //如果ioc容器是空的调出方法
        if (ioc.isEmpty()) return;
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //获取对象中所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                //若没有依赖注入注解不加载对象
                if (!field.isAnnotationPresent(GPAutowired.class)) continue;
                GPAutowired autowired = field.getAnnotation(GPAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)) {
                    //获取类名
                    beanName = field.getType().getName();
                }
                //暴力访问 可以访问私有属性
                field.setAccessible(true);
                try {
                    //把iocnew的对象给了@Autowired注入的属性了
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    //5.构造HandlerMapping
    private void initHandlerMapping() {
        if (ioc.isEmpty()) return;
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //ioc中的类
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(GPController.class)) {
                continue;
            }
            String baseUrl = "";
            if (clazz.isAnnotationPresent(GPRequestMapping.class)) {
                GPRequestMapping requestMapping = clazz.getAnnotation(GPRequestMapping.class);
                baseUrl = requestMapping.value();
            }
            //获取method的url配置
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                //如果没有加RequestMapping注解的直接忽略
                if (!method.isAnnotationPresent(GPRequestMapping.class)) {
                    continue;
                }
                GPRequestMapping requestMapping = method.getAnnotation(GPRequestMapping.class);
                //不管有几个 /// 都替换成一个 /
                String url = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);
                System.out.println("路径" + url + "方法" + method);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req,resp);
        }catch (Exception e){
            resp.getWriter().write("500 Exception,Details:\r\n"+ Arrays.toString(e.getStackTrace()).
                    replaceAll("\\[|\\]","")
                    .replaceAll(",\\&",","));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            doDispatch(req,resp);
        }catch (Exception e){
            resp.getWriter().write("500 Exception,Details:\r\n"+ Arrays.toString(e.getStackTrace()).
                    replaceAll("\\[|\\]","")
                    .replaceAll(",\\&",","));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (this.handlerMapping.isEmpty()) {
            return;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url=url.replace(contextPath,"").replaceAll("/+","/");
        //不包含
        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 Not Found!!");
            return;
        }
        //获取请求的参数
        Map<String,String[]> params = req.getParameterMap();
        //获取方法
        Method method = this.handlerMapping.get(url);
        //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        //获取请求的参数
        Map<String,String[]> parameterMap = req.getParameterMap();
        //保存参数值
        Object[] paramValues = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            //根据参数名称，做某些处理
            Class<?> parameterType = parameterTypes[i];
            if (parameterType== HttpServletRequest.class) {
                //参数已经明确 墙砖类型
                paramValues[i]=req;
                continue;
            }else if(parameterType== HttpServletResponse.class){
                paramValues[i]=resp;
            }else if(parameterType==String.class){
                //这里是参数
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]","")
                            .replaceAll(",\\&",",");
                    paramValues[i]=value;
                }
            }
        }
        try {
            String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
            //利用反射执行方法
            method.invoke(this.ioc.get(beanName),paramValues);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private String lowerFirstCase(String string) {
        char[] chars = string.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }
}
