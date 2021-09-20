package top.greatxiaozou.diyTomcat;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import lombok.Builder;
import lombok.Data;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 响应的对象，内含响应头响应体等
 */
@Data
public class Response extends BaseResponse {

    private StringWriter stringWriter;

//    写入工具
    private PrintWriter writer;

    //类型
    private String contentType;

    //回复内容二进制
    private byte[] body;

    //返回的状态码
    private int status;

//    cookies
    private List<Cookie> cookies;

//    客户端跳转路径
    private String redirectPath;


    public Response() {
        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
        contentType = "text/html";
        this.cookies = new ArrayList<>();
    }

    public void addCookie(Cookie cookie){
        cookies.add(cookie);
    }


    //将cookies解析成字符串的形式的方法
    public String getCookiesHeader(){
        if (cookies==null){
            return "";
        }
        String pattern = "EEE, d MMM yyyy HH:mm:ss 'GMT'";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern,Locale.ENGLISH);
        StringBuffer sb = new StringBuffer();
        for (Cookie cookie : getCookies()) {
            sb.append("\r\n");
            sb.append("Set-Cookie");
            System.out.println(cookie.getName()+"="+cookie.getValue()+";");
            sb.append(cookie.getName() + "=" +cookie.getValue()+";");
            if (-1 != cookie.getMaxAge()){
                sb.append("Expires=");
                Date now = new Date();
                Date expire = DateUtil.offset(now, DateField.MINUTE,cookie.getMaxAge());
                sb.append(sdf.format(expire));
                sb.append("; ");
            }
            if (null != cookie.getPath()){
                sb.append("Path=" + cookie.getPath());
            }
        }
        return sb.toString();
    }

    public void setBody(byte[] body){
        this.body = body;
    }

//    设置客户端跳转路径
    public void sendRedirect(String redirect){
        this.redirectPath = redirect;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    //获取内容格式
    public String getContentType(){
        return this.contentType;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }

    //获取writer
    public PrintWriter getWriter(){
        return this.writer;
    }


    //获取响应内容,二进制形式
    public byte[] getBody(){
        if (body == null){
            String content = stringWriter.toString();
            body = content.getBytes(StandardCharsets.UTF_8);
        }
        return body;
    }

    @Override
    public int getStatus(){
        return status;
    }

    @Override
    public void setStatus(int status){
        this.status = status;
    }
}
