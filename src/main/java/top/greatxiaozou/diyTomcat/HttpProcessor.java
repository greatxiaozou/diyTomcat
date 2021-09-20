package top.greatxiaozou.diyTomcat;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.log.LogFactory;
import top.greatxiaozou.Utils.SessionManager;
import top.greatxiaozou.Utils.Utils;
import top.greatxiaozou.diyTomcat.servlets.DefaultServlet;
import top.greatxiaozou.diyTomcat.servlets.InvokerServlet;
import top.greatxiaozou.diyTomcat.servlets.JspServlet;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 处理HTTP连接的一个类
 */
public class HttpProcessor {
    /**
     * 处理http的socket连接的方法
     * @param s：接受到的Socket连接
     * @param request：http请求内容
     * @param response http响应内容
     */
    public void executor(Socket socket,Request request,Response response){
        try {
            String uri = request.getUri();
            if (uri == null){
                return;
            }
            prepareSession(request,response);

            Context context = request.getContext();
            String className = context.getServletClassName(uri);


            HttpServlet workServlet;
            //当className不为空，则让InvokerServlet去处理http请求
            //如果className为空，即未在web.xml中配置相关映射，则用DefaultServlet来处理静态资源的访问
            if (className != null){
                workServlet = InvokerServlet.getINSTANCE();
            }else if (uri.endsWith(".jsp")){
                workServlet = JspServlet.getINSTANCE();
            }else{
                workServlet = DefaultServlet.getINSTANCE();
            }

//            过滤器的调用链,将servlet传入chains，让他在过滤完之后在调用
            List<Filter> filters = context.getMatchedFilter(request.getRequestURI());
            ApplicationFilterChain filterChain = new ApplicationFilterChain(filters, workServlet);
            filterChain.doFilter(request,response);
//            是否已经请求转发了,防止已经关闭的socket重复使用
            if (request.isForwarded()){
                return;
            }

            if (response.getStatus() == 200){
                handle200(socket, request,response);
            }
            if (response.getStatus() == 302){
                handle302(socket,response);
            }
            if (response.getStatus() == 404){
                handle404(socket,uri);
            }
        }catch (Exception e){
            LogFactory.get().error(e);
            handle500(socket,e);
        }finally {
            try {
                if (!socket.isClosed()){
                    socket.close();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


//     解析session,将Session从sessionManager中取出并放到request中
    public void prepareSession(Request request,Response response){
        String jsessionId = request.getJsessionIdFromCookie();
        HttpSession session = SessionManager.getSession(jsessionId,request,response);
        request.setSession(session);
    }


    //========对200响应的处理,将response变成字节数组的形式并返回==============================
    private static void handle200(Socket socket,Request request,Response response) throws IOException {

        //分别获取响应头和响应体
        String contentType = response.getContentType();
        String cookieHeader = response.getCookiesHeader();
        byte[] body = response.getBody();

        boolean gzip = isGzip(request, body, contentType);


        String headText = gzip?Utils.respone_head_200_gzip:Utils.RESPONSE_HEAD_200;
        headText = StrUtil.format(headText,contentType,cookieHeader);

        //如果可以，压缩
        if (gzip){
            body = ZipUtil.gzip(body);
        }
        //转换成二进制形式并进行拼接
        byte[] head = headText.getBytes();

        byte[] respBytes = new byte[head.length+body.length];
        ArrayUtil.copy(head,0,respBytes,0,head.length);
        ArrayUtil.copy(body,0,respBytes,head.length,body.length);

        //写入响应
        socket.getOutputStream().write(respBytes,0,respBytes.length);

        //关闭资源
        //关闭做到start的finally里
        // socket.close();
    }

    //处理404
    protected void handle404(Socket s,String uri) throws IOException {
        OutputStream os = s.getOutputStream();
//        拼接回复
        String text = StrUtil.format(Utils.TEXT_FORMAT_404, uri, uri);
        text = Utils.RESPONSE_HEAD_404 + text;

        byte[] res = text.getBytes(StandardCharsets.UTF_8);

        //写入到socket回复
        os.write(res);

    }

    //处理500错误
    protected void handle500(Socket s,Exception e){
        try {
            OutputStream os = s.getOutputStream();
            //将错误调用栈打印出来
            StackTraceElement[] ses = e.getStackTrace();
            StringBuffer sb = new StringBuffer();

            sb.append(e.toString());
            sb.append("\r\n");
            for (StackTraceElement se : ses) {
                sb.append("\t");
                sb.append(se.toString());
                sb.append("\r\n");
            }

            //错误的信息
            String message = e.getMessage();
            if (message != null && message.length() > 20){
                message = message.substring(0,19);
            }

            //将错误信息、错误、和错误调用栈返回给前端
            String text = StrUtil.format(Utils.TEXT_FORMAT_500, message, e.toString(), sb.toString());
            text = Utils.RESPONSE_HEAD_500 + text;

            os.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    //处理302跳转
    private void handle302(Socket socket,Response response) throws IOException {
        OutputStream os = socket.getOutputStream();
        String redirectPath = response.getRedirectPath();
        String header = StrUtil.format(Utils.RESPONSE_HEAD_302,redirectPath);
        byte[] bytes = header.getBytes();
        System.out.println(header);
        os.write(bytes);
    }

//    判断是否需要压缩
    private static boolean isGzip(Request request,byte[] body,String mimeType){
        String acceptEcd = request.getHeader("Accept-Encoding");
        if (!StrUtil.containsAny(acceptEcd,"gzip")){
            return false;
        }
        Connector conn = request.getConnector();
        if (mimeType.contains(";")){
            mimeType = StrUtil.subBefore(mimeType,";",false);
        }
        if (!"on".equals(conn.getCompression())){
            return false;
        }
        if (conn.getCompressionMinSize() > body.length){
            return false;
        }

        //获取用户
        String userAgents = conn.getNoCompressionUserAgents();
        String[] eachUserAgents = userAgents.split(",");
        for (String eachUserAgent : eachUserAgents) {
            eachUserAgent = eachUserAgent.trim();
            String userAgent = request.getHeader("User-Agent");
            //如果gzip里的用户包含req里的useragent
            if (StrUtil.containsAny(userAgent,eachUserAgent)){
                return false;
            }
        }

//      判断类型是否是支持的类型
        String mimeTypes = conn.getCompressableMimeType();
        String[] eachMimeTypes = mimeTypes.split(",");
        for (String eachMimeType : eachMimeTypes) {

            if (mimeType.equals(eachMimeType)){
                return true;
            }
        }
        return false;
    }
}
