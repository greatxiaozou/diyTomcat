package top.greatxiaozou.Utils;

import cn.hutool.system.SystemUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 常用的工具方法和常量
 */
public class Utils {

    /**
     * 常见响应模板
    */
    //200
    public static final String RESPONSE_HEAD_200 = "HTTP/1.1 200 OK\r\n" + "Content-Type: {}{}\r\n\r\n";
    //404
    public static final String RESPONSE_HEAD_404 = "HTTP/1.1 404 Not Found\r\n" + "Content-Type: {}\r\n\r\n";
    public static final String TEXT_FORMAT_404 =  "<html><head><title>DIY Tomcat/1.0.1 - Error report</title><style>" +
            "<!--H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} " +
            "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} " +
            "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} " +
            "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} " +
            "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} " +
            "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}" +
            "A {color : black;}A.name {color : black;}HR {color : #525D76;}--></style> " +
            "</head><body><h1>HTTP Status 404 - {}</h1>" +
            "<HR size='1' noshade='noshade'><p><b>type</b> Status report</p><p><b>message</b> <u>{}</u></p><p><b>description</b> " +
            "<u>The requested resource is not available.</u></p><HR size='1' noshade='noshade'><h3>DiyTocmat 1.0.1</h3>" +
            "</body></html>";

    public static final String respone_head_200_gzip = "HTTP/1.1 200 OK\r\nContent-Type: {}{}\r\n" +
            "Content-Encoding:gzip" +
            "\r\n\r\n";
    //500
    public static final String RESPONSE_HEAD_500 = "HTTP/1.1 500 Internal Server Error\r\n" + "Content-Type: {}\r\n\r\n";
    public static final String TEXT_FORMAT_500 =  "<html><head><title>DIY Tomcat/1.0.1 - Error report</title><style>"
            + "<!--H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} "
            + "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} "
            + "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} "
            + "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} "
            + "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} "
            + "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}"
            + "A {color : black;}A.name {color : black;}HR {color : #525D76;}--></style> "
            + "</head><body><h1>HTTP Status 500 - An exception occurred processing {}</h1>"
            + "<HR size='1' noshade='noshade'><p><b>type</b> Exception report</p><p><b>message</b> <u>An exception occurred processing {}</u></p><p><b>description</b> "
            + "<u>The server encountered an internal error that prevented it from fulfilling this request.</u></p>"
            + "<p>Stacktrace:</p>" + "<pre>{}</pre>" + "<HR size='1' noshade='noshade'><h3>DiyTocmat 1.0.1</h3>"
            + "</body></html>";


//    302
    public static final String RESPONSE_HEAD_302 = "HTTP/1.1 302 Found\r\nLocation:{}\r\n\r\n";

    //目录文件
    public static final File webappsFolder = new File(SystemUtil.get("user.dir"),"webapps");
    public static final File rootFolder = new File(webappsFolder,"ROOT");

    //定位配置文件
    public static final File confFolder = new File(SystemUtil.get("user.dir"),"conf");
    public static final File serverXmlFile = new File(confFolder,"server.xml");

    //定位欢迎文件(web.xml文件)
    public static final File webXmlFile = new File(confFolder,"web.xml");

    //定位查找Web-Info文件夹以及web.xml文件的context文件，context.xml里写了在哪里找到配置Servlet的web.xml
    public static final File contextXmlFile = new File(confFolder,"context.xml");

    //转义成java文件的jsp的目录
    public static final String workFolder = SystemUtil.get("user.dir") + File.separator + "work";

    //Http返回代码常量
    public static final int CODE_200 = 200;
    public static final int CODE_302 = 302;
    public static final int CODE_404 = 404;
    public static final int CODE_500 = 500;


    //从输入流中读取数据
    public static byte[] readBytes(InputStream is,boolean fully) throws IOException{
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while(true){
            int read = is.read(buffer);
            //读到-1表示缓冲区为空，取消循环
            if (read == -1){
                break;
            }
            //讲buffe中的数据读到baos输出流中
            baos.write(buffer,0,read);
            if(!fully && read != buffer.length){
                break;
            }
        }
        //读取完毕后，换成byte数组
        byte[] res = baos.toByteArray();
        return res;
    }
}
