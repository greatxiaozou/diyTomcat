import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;

import org.junit.BeforeClass;
import org.junit.Test;
import top.greatxiaozou.Utils.Utils;
import top.greatxiaozou.diyTomcat.MiniBrowser;

import javax.sound.midi.SoundbankResource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 * 自动的单元测试
 */
public class TestTomcat {

    //定义地址信息
    private static int port = 18080;
    private static String host = "127.0.0.1";


    @BeforeClass
    public static void beforeClass(){
        //测试开始前检查端口是否启动
        if (NetUtil.isUsableLocalPort(port)){
            System.out.println("端口未启动，请检查端口："+port);
        }else{
            System.out.println("端口启动正常，可以开始单元测试");
        }
    }

    //检查服务器的启动
    @Test
    public  void testHelloTomcat(){
        String string = getContentString("/");
        System.out.println(string);
    }

    //检查访问文件
    @Test
    public void testHtmlTomcat(){
        String string = getContentString("/a.html");
        System.out.println(string);
    }

    //检查耗时任务的模拟
    @Test
    public void testTimeConsumeTomcat(){
        CountDownLatch latch = new CountDownLatch(3);
        TimeInterval interval = DateUtil.timer();
        for (int i = 0; i < 3; i++) {

            //三个线程访问耗时任务的模拟界面
            new Thread(()->{
                String content = getContentString("/timeConsume.html");
                latch.countDown();
            }).start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //记录耗时
        long time = interval.intervalMs();

        System.out.println(time);
    }

    /**
     * 测试多应用开发
     */
    @Test
    public void textFolderaTomcat(){
        String string = getContentString("/a");
        System.out.println(string);
    }

    //测试xml的配置是否可行
    @Test
    public void textXMLTomcat(){
        String string = getContentString("/b/");
        System.out.println(string);
    }


    /**
     * 404测试
     */
    @Test
    public void test404(){
        String string = getContentString("/not_exist.html");
        System.out.println(string);
    }

    /**
     * 500测试
     */
    @Test
    public void test500(){
        String string = getContentString("/500.html");
        System.out.println(string);
    }

    /**
     * mimeType测试
     */
    @Test
    public void mimeTypeTest(){
        String string = getContentString("/a.txt");
        System.out.println(string);
    }

    @Test
    public void testPng(){
        byte[] contentBytes = getContentBytes("/logo.png");
        System.out.println(contentBytes.length);
    }

    @Test
    public void testPdf(){
        byte[] contentBytes = getContentBytes("/etf.pdf");
        System.out.println(contentBytes.length);
    }

    @Test
    public void testHelloServlet(){
        String contentString = getContentString("/j2ee/hello");
        System.out.println(contentString);
    }

    @Test
    public void testJavawebHelloServlet(){
        String str = getContentString("/javaweb/hello");
        System.out.println(str);
    }

    //测试参数的携带
    @Test
    public void testGetParam(){
        String uri = "/javaweb/param";
        String url = StrUtil.format("http://{}:{}{}",host,port,uri);
        HashMap<String,Object> params = new HashMap<>();
        params.put("name","zqfgxm");
        String contentString = MiniBrowser.getContentString(url, params, true);
        System.out.println(contentString);
    }

    //测试request的头部的参数
    @Test
    public void testRequestHeader(){
        String s = getContentString("/javaweb/header");
        System.out.println(s);
    }

    //测试cookie的设置
    @Test
    public void testCookies(){
        String s = getContentString("/javaweb/setCookie");
        System.out.println(s);
    }

    //测试cookie的获取
    @Test
    public void testgetCookie() throws IOException {
        String url = StrUtil.format("http://{}:{}{}", host,port,"/javaweb/getCookie");
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestProperty("Cookie","name=Gareen(cookie)");
        conn.connect();
        InputStream is = conn.getInputStream();
        String html = IoUtil.read(is, "utf-8");
        System.out.println(html);
    }

    //测试session的设置和获取
    @Test
    public void testSession() throws IOException {
        String sessionId = getContentString("/javaweb/setSession");
        if (sessionId != null){
            sessionId = sessionId.trim();
        }
        System.out.println(sessionId);
        String url = StrUtil.format("http://{}:{}{}",host,port,"/javaweb/getSession");
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        //设置sessionid到cookie中
        conn.setRequestProperty("Cookie","JSESSIONID="+sessionId);
        conn.connect();
        InputStream is = conn.getInputStream();
        String res = IoUtil.read(is, "utf-8");
        System.out.println(res);
    }

    //测试Gzip压缩的结果
    @Test
    public void testGzip(){
        byte[] gzipBytes = getContentBytes("/", true);
        byte[] bytes = ZipUtil.unGzip(gzipBytes);
        System.out.println("gzip : "+new String(gzipBytes));
        System.out.println("unGzip: "+new String(bytes));
    }

    //测试jsp欢迎文件
    @Test
    public void testJspWelcome(){
        String string = getContentString("/javaweb/");
        System.out.println(string);
    }

//    测试自写的jsp文件能否访问
    @Test
    public void testDiyJsp(){
        String string = getContentString("/javaweb/a.jsp");
        System.out.println(string);
    }

//    测试客户端跳转
    @Test
    public void testClientJump(){
        String string = getContentString("/javaweb/jump1");
        System.out.println(string);
    }

//    测试服务端跳转
    @Test
    public void testServerJump(){
        String contentString = getContentString("/javaweb/jump2");
        System.out.println(contentString);
    }

    @Test
    public void testWarHello(){
        String res = getContentString("/javaweb1/hello");
        System.out.println(res);
    }

    /**
     * 获取资源的方法
     * @param uri 传入路径参数
     */
    public static String getContentString(String uri,boolean gzip){
        //对url进行一个拼接
        String url = StrUtil.format("http://{}:{}{}",host,port,uri);
        //获取资源
        return MiniBrowser.getContentString(url,gzip);
    }

    public static String getContentString(String uri){
        return getContentString(uri,false);
    }

    public static byte[] getContentBytes(String uri,boolean gzip){
        String url = StrUtil.format("http://{}:{}{}",host,port,uri);
        return MiniBrowser.getContentBytes(url,gzip);
    }

    public static byte[] getContentBytes(String uri){
        return getContentBytes(uri,false);
    }

}
