package top.greatxiaozou.diyTomcat;

import cn.hutool.http.HttpUtil;
import top.greatxiaozou.Utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 用一个迷你的浏览器来模仿浏览器的HTTP请求
 * 可视为客户端
 */
public class MiniBrowser {
    public static void main(String[] args) {

        String url = "http://static.how2j.cn/diytomcat.html";
        String contentString = getContentString(url, false,null,true);
        System.out.println(contentString);
        System.out.println("====================分隔符=================");
        String string = getHttpString(url, false);
        System.out.println(string);

    }


    //=================获取http内容，去掉响应的头部====================//

    //获取字符的http响应内容，不带gzip
    public static String getContentString(String url){
        return getContentString(url,false,null,true);
    }

    public static String getContentString(String url,boolean gzip){
        return getContentString(url,gzip,null,true);
    }

    public static String getContentString(String url,Map<String,Object> params,boolean isGet){
        return getContentString(url,false,params,isGet);
    }

    //获取字符的http响应内容
    public static String getContentString(String url,boolean gzip,Map<String,Object> map,boolean isGet){
        byte[] contentBytes = getContentBytes(url, gzip,map,isGet);
        if(contentBytes == null){
            return null;
        }

        return new String(contentBytes).trim();

    }

    //获取二进制的内容，不带gzip
    public static byte[] getContentBytes(String url){
        return getContentBytes(url,false,null,true);
    }

    //获取二进制的内容
    public static byte[] getContentBytes(String url,boolean gzip,Map<String,Object> params,boolean isGet){
        byte[] resp = getHttpBytes(url, gzip,params,isGet);
        byte[] doubleReturn = "\r\n\r\n".getBytes();

        int pos = -1;

        //找到头部结束的位置，使用pos
        for (int i = 0; i < resp.length - doubleReturn.length; i++) {
            byte[] temp = Arrays.copyOfRange(resp, i, i + doubleReturn.length);

            if(Arrays.equals(temp,doubleReturn)){
                pos = i;
                break;
            }
        }

        if (-1 == pos){
            return null;
        }

        //获取响应体并返回
        pos += doubleReturn.length;

        return Arrays.copyOfRange(resp,pos,resp.length);
    }

    public static byte[]  getContentBytes(String url,boolean gzip){
        return getContentBytes(url, gzip, null, true);
    }


    //====================获取http响应，包括字符形式和二进制形式======================//
    //获取字符的http响应
    public static String getHttpString(String url){
        return getHttpString(url,false,null,true);
    }

    //获取字符的http响应
    public static String getHttpString(String url,boolean gzip){
        byte[] httpBytes = getHttpBytes(url, gzip,null,true);

        return new String(httpBytes).trim();
    }

    public static String getHttpString(String url,Map<String,Object> map,boolean isGet){
        byte[] bytes = getHttpBytes(url, false, map, isGet);
        return new String(bytes).trim();
    }

    public static String getHttpString(String url,boolean gizp,Map<String,Object> map,boolean isGet){
        byte[] httpBytes = getHttpBytes(url, gizp, map, isGet);
        return new String(httpBytes).trim();
    }



    //获取二进制的Http响应
    public static byte[] getHttpBytes(String url, boolean gzip, Map<String,Object> params,boolean isGet){
        String method = isGet?"GET":"POST";
        byte[] res = null;

        //使用socket连接来模拟获取url，并以字节形式返回
        try {
            URL u = new URL(url);
            Socket client = new Socket();

            //设置端口号
            int port = u.getPort();
            if(port == -1){
                port = 80;
            }

            //设置地址
            InetSocketAddress add = new InetSocketAddress(u.getHost(),port);

            //连接服务器
            client.connect(add,1000);

            //使用hashMap封装头部
            HashMap<String, String> httpHead = new HashMap<>();

            //设置主机地址以及其他参数
            httpHead.put("Host",u.getHost()+":"+port);
            httpHead.put("Accept","text/html");
            httpHead.put("Connection","close");
            httpHead.put("User-Agent","greatxiaozou Mini Browser / java 1.8");

            if (gzip){
                httpHead.put("Accept-Encoding","gzip");
            }

            //获取路径
            String path = u.getPath();
            if (path.length() == 0){
                path = "/";
            }
            if (params != null && isGet){
                String paramString = HttpUtil.toParams(params);
                path = path + "?" + paramString;
            }

            String firstLine = method +" " + path +" HTTP/1.1\r\n";

            //填充完整请求
            StringBuffer httpReq = new StringBuffer();

            httpReq.append(firstLine);
            for (String s : httpHead.keySet()) {
                httpReq.append(s+":"+httpHead.get(s)+"\r\n");
            }

            //如果不是get请求，则将参数放置于请求体里。
            if (params != null && !isGet){
                String paramsString = HttpUtil.toParams(params);
                httpReq.append("\r\n");
                httpReq.append(paramsString);
            }

            PrintWriter pWriter = new PrintWriter(client.getOutputStream(),true);
            pWriter.println(httpReq);

            //读取到字节数组
            res = Utils.readBytes(client.getInputStream(),true);

            //关闭资源
            client.close();
        } catch (IOException e) {
            e.printStackTrace();

            res = e.toString().getBytes();
        }
        return res;
    }
}
