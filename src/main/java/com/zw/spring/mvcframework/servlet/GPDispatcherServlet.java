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
 * ������Ϊ�������
 */
public class GPDispatcherServlet extends HttpServlet{

    private static final long serialVersionUID = 1L;
    //��web.xml��param-name��ֵһ��
    private static final String LOCATION = "contextConfigLocation";
    //�������е�������Ϣ
    private Properties p = new Properties();
    //�������б�ɨ�赽���������
    private List<String> classNames = new ArrayList<String>();
    //����ioc�����������г�ʼ����bean
    private Map<String, Object> ioc = new HashMap<String,Object>();
    //�������е�url�ͷ�����ӳ���ϵ key��url
    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

    public GPDispatcherServlet() {
        super();
    }

    //��Servlet��������ʱ�������GPDispatcherServlet��init()������
    // ��init�����Ĳ����У����ǿ����õ��������ļ���·����
    // ���ܹ���ȡ�������ļ��е���Ϣ��
    // ǰ�������Ѿ�������Spring�������׶Σ���������ɳ�ʼ���׶εĴ��롣
    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.���������ļ�
        doLoadConfig(config.getInitParameter(LOCATION));
        //2.ɨ��������ص���
        doScanner(p.getProperty("scanPackage"));
        //3.��ʼ����Ҫ���ص������ioc������
        doInstance();
        //4.����ע��
        doAutowired();
        //5.����HandlerMapping
        initHandlerMapping();
        //6.�ȴ�����ƥ��url ��λ���� �������ִ��
        //����doGet����doPost����
        //��ʾ
        System.out.println("�к� spring ��ܳ�ʼ�����");
    }

    //1.���������ļ�
    private void doLoadConfig(String location) {
        InputStream fis = null;

        try {
            fis = this.getClass().getClassLoader().getResourceAsStream(location);
            //��ȡ�����ļ�
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

    //2.ɨ��������ص���
    private void doScanner(String packageName) {
        //�����еİ�·��ת��Ϊ�ļ�·��
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            //������ļ��У������ݹ�
            if (file.isDirectory()) {
                doScanner(packageName + "." + file.getName());
            } else {
                //�������б�ɨ�赽���������
                classNames.add(packageName + "." + file.getName().replace(".class", "").trim());
            }
        }
    }

    //3.��ʼ����Ҫ���ص������ioc������
    private void doInstance() {
        if (classNames.size() == 0) return;
        try {
            for (String className : classNames) {
                //����
                Class<?> clazz = Class.forName(className);
                //������а���controllerע�� ����ioc��
                if (clazz.isAnnotationPresent(GPController.class)) {
                    //getName ----��ʵ�����ơ� ---- com.se7en.test.Main
                    //getSimpleName ---- ���ײ����ơ� ---- Main
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    //�����ʼ������ioc������ȥ
                    ioc.put(beanName, clazz.newInstance());

                } else if (clazz.isAnnotationPresent(GPService.class)) {
                    //�������service
                    GPService service = clazz.getAnnotation(GPService.class);
                    //��ȡservcie��valueֵ
                    String beanName = service.value();
                    //����û��Լ�����������
                    if (!"".equals(beanName.trim())) {
                        ioc.put(beanName, clazz.newInstance());
                        //����ѭ��������һ��ѭ��
                        continue;
                    }
                    //����Լ�û������
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

    //4.����ע��
    private void doAutowired() {
        //���ioc�����ǿյĵ�������
        if (ioc.isEmpty()) return;
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //��ȡ��������������
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                //��û������ע��ע�ⲻ���ض���
                if (!field.isAnnotationPresent(GPAutowired.class)) continue;
                GPAutowired autowired = field.getAnnotation(GPAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)) {
                    //��ȡ����
                    beanName = field.getType().getName();
                }
                //�������� ���Է���˽������
                field.setAccessible(true);
                try {
                    //��iocnew�Ķ������@Autowiredע���������
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    //5.����HandlerMapping
    private void initHandlerMapping() {
        if (ioc.isEmpty()) return;
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //ioc�е���
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(GPController.class)) {
                continue;
            }
            String baseUrl = "";
            if (clazz.isAnnotationPresent(GPRequestMapping.class)) {
                GPRequestMapping requestMapping = clazz.getAnnotation(GPRequestMapping.class);
                baseUrl = requestMapping.value();
            }
            //��ȡmethod��url����
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                //���û�м�RequestMappingע���ֱ�Ӻ���
                if (!method.isAnnotationPresent(GPRequestMapping.class)) {
                    continue;
                }
                GPRequestMapping requestMapping = method.getAnnotation(GPRequestMapping.class);
                //�����м��� /// ���滻��һ�� /
                String url = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);
                System.out.println("·��" + url + "����" + method);
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
        //������
        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 Not Found!!");
            return;
        }
        //��ȡ����Ĳ���
        Map<String,String[]> params = req.getParameterMap();
        //��ȡ����
        Method method = this.handlerMapping.get(url);
        //��ȡ�����Ĳ����б�
        Class<?>[] parameterTypes = method.getParameterTypes();
        //��ȡ����Ĳ���
        Map<String,String[]> parameterMap = req.getParameterMap();
        //�������ֵ
        Object[] paramValues = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            //���ݲ������ƣ���ĳЩ����
            Class<?> parameterType = parameterTypes[i];
            if (parameterType== HttpServletRequest.class) {
                //�����Ѿ���ȷ ǽש����
                paramValues[i]=req;
                continue;
            }else if(parameterType== HttpServletResponse.class){
                paramValues[i]=resp;
            }else if(parameterType==String.class){
                //�����ǲ���
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]","")
                            .replaceAll(",\\&",",");
                    paramValues[i]=value;
                }
            }
        }
        try {
            String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
            //���÷���ִ�з���
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
