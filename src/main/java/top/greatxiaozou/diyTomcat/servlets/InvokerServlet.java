package top.greatxiaozou.diyTomcat.servlets;

import cn.hutool.core.util.ReflectUtil;
import top.greatxiaozou.Utils.Utils;
import top.greatxiaozou.diyTomcat.Context;
import top.greatxiaozou.diyTomcat.Request;
import top.greatxiaozou.diyTomcat.Response;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用于处理Servlet的InvokerServlet
 * 单例的
 */
public class InvokerServlet extends HttpServlet {
    private InvokerServlet(){}
    private static volatile InvokerServlet INSTANCE;

    public static InvokerServlet getINSTANCE(){
        if (INSTANCE == null){
            synchronized (InvokerServlet.class){
                if (INSTANCE == null){
                    INSTANCE = new InvokerServlet();
                }
            }
        }
        return INSTANCE;
    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        //强转为自己编写的Request和Response
        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;

        String uri = request.getUri();
        Context context = request.getContext();
        String className = context.getServletClassName(uri);

        //使用反射创建相应的Servlet并进行处理
        try {
            Class servletClass = context.getWebappClassLoader().loadClass(className);
            System.out.println("ServletClass:"+servletClass);
            System.out.println("classLoader:"+servletClass.getClassLoader());
            Object instance = context.getServlet(servletClass);
            ReflectUtil.invoke(instance,"service",request,response);
//        设置响应状态码,如果重定向路径不为空，则设置重定向
            if (null != response.getRedirectPath()){
                response.setStatus(Utils.CODE_302);
            }else {
                response.setStatus(Utils.CODE_200);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
