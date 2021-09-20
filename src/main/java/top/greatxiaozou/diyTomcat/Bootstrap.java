package top.greatxiaozou.diyTomcat;


import top.greatxiaozou.diyTomcat.classLoader.CommonClassLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 整个项目的启动类，根类
 */
public class Bootstrap {
    public static void main(String[] args) throws IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        //传统的启动方法
//        Server server = new Server();
//        server.start();

        //使用类加载器启动
        CommonClassLoader loader = new CommonClassLoader();
        Thread.currentThread().setContextClassLoader(loader);

        String serverClassName = "top.greatxiaozou.diyTomcat.Server";
        Class<?> serverClass = loader.loadClass(serverClassName);

        Object o = serverClass.newInstance();
        Method start = serverClass.getMethod("start");

        start.invoke(o);

        System.out.println(serverClass.getClassLoader());

//        不能关闭，否则后续无法使用啦
//        loader.close();
    }
}
