package top.greatxiaozou.diyTomcat.servlets;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import top.greatxiaozou.Utils.JspUtil;
import top.greatxiaozou.Utils.Utils;
import top.greatxiaozou.Utils.WebXmlUtils;
import top.greatxiaozou.diyTomcat.BaseRequest;
import top.greatxiaozou.diyTomcat.Context;
import top.greatxiaozou.diyTomcat.Request;
import top.greatxiaozou.diyTomcat.Response;
import top.greatxiaozou.diyTomcat.classLoader.JspClassLoader;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * 处理JSP的servlet
 * 同样是单例的
 */
public class JspServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static JspServlet instance = new JspServlet();

    public static JspServlet getINSTANCE(){
        return instance;
    }
    //构造私有化，
    private JspServlet(){}

    //处理jsp的具体过程
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){

        try{
//            将http请求和响应转化成自己写的请求与响应
            Request request = (Request) httpServletRequest;
            Response response = (Response) httpServletResponse;

            String uri = request.getUri();

//            如果访问/，则访问欢迎文件
            if ("/".equals(uri)){
                uri = WebXmlUtils.getWelcomeFile(request.getContext());
            }
            String fileName = StrUtil.removePrefix(uri, "/");
            File file = FileUtil.file(request.getRealPath(fileName));

            File jspFile = file;
            if (jspFile.exists()){
                Context context = request.getContext();
                String path = context.getPath();
                String subFolder;
                if ("/".equals(path)){
                    subFolder = "_";
                }else{
                    subFolder = StrUtil.subAfter(path,"/",false);
                }
                String servletClassPath = JspUtil.getServletClassPath(uri, subFolder);
                File jspServletClassFile = new File(servletClassPath);
                //如果jsp对应的class文件不存在，则编译生成class文件
                if (!jspServletClassFile.exists()){
                    JspUtil.compileJsp(context,jspFile);

                }
//                如果编译生成的文件版本低于jsp文件的版本，则重新编译
                else if (jspFile.lastModified() > jspServletClassFile.lastModified()){
                    //当发现JSP更新后，重新编译，并且让之前的加载器脱钩
                    JspUtil.compileJsp(context,jspFile);
                    JspClassLoader.invalidJspClassLoader(uri,context);
                }
                //设置contentType
                String extName = FileUtil.extName(file);
                String mimeType = WebXmlUtils.getMimeTypeByName(extName);
                response.setContentType(mimeType);
//                获取JSP对应的Servlet并进行处理
                JspClassLoader jspClassLoader = JspClassLoader.getJspClassLoader(uri,context);
                String jspServletClassName = JspUtil.getJspServletClassName(uri, subFolder);
                Class<?> clazz = jspClassLoader.loadClass(jspServletClassName);
                HttpServlet servlet = context.getServlet(clazz);
                servlet.service(request,response);
//                处理成功则设置状态码，如果重定向路径不为空，则设置状态码302,否则设置200
                if (null != response.getRedirectPath()){
                    response.setStatus(Utils.CODE_302);
                }else {
                    response.setStatus(Utils.CODE_200);
                }
            }else{
                response.setStatus(Utils.CODE_404);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
