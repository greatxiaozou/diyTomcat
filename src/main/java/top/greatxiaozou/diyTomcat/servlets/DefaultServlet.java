package top.greatxiaozou.diyTomcat.servlets;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import top.greatxiaozou.Utils.Utils;
import top.greatxiaozou.Utils.WebXmlUtils;
import top.greatxiaozou.diyTomcat.Context;
import top.greatxiaozou.diyTomcat.Request;
import top.greatxiaozou.diyTomcat.Response;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * 处理静态资源的servlet
 * 同样是单例的
 */
public class DefaultServlet extends HttpServlet {
    private static volatile DefaultServlet INSTANCE;

    private DefaultServlet(){}

    //单例的获取实例的方法
    public static DefaultServlet getINSTANCE(){
        if (INSTANCE == null){
            synchronized (DefaultServlet.class){
                if (INSTANCE == null){
                    INSTANCE = new DefaultServlet();
                }
            }
        }
        return INSTANCE;
    }

    //处理静态资源
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        //请求和响应强转
        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;

        Context context = request.getContext();
        String uri = request.getUri();
//          硬编码
        if (uri.equals("/500.html")){
            throw new RuntimeException("Test 500 page!");
        }
        else {
            //如果uri匹配的是"/"，则访问欢迎文件，即修改uri为欢迎文件
            if (uri.equals("/")){
                uri = WebXmlUtils.getWelcomeFile(request.getContext());
            }

            //如果访问的是jsp文件，则交由jspServlet处理
            if (uri.endsWith(".jsp")){
                JspServlet.getINSTANCE().service(request,response);
                return;
            }
            //取uri中/后面的路径
            String fileName = StrUtil.removePrefix(uri,"/");
            File file = new File(request.getRealPath(fileName));


            if (file.exists()){
                //获取文件后缀名
                String extName = FileUtil.extName(file);
                String mimeType = WebXmlUtils.getMimeTypeByName(extName);
                //在响应中设置mimetype
                response.setContentType(mimeType);

//              读入二进制文件
                byte[] body = FileUtil.readBytes(file);
                response.setBody(body);

                //睡眠测试
                if (file.getName().equalsIgnoreCase("timeConsume.html")){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                response.setStatus(Utils.CODE_200);
            }else{
                response.setStatus(Utils.CODE_404);

            }
        }
    }
}
