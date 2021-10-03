package top.greatxiaozou.diyTomcat;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import lombok.Data;
import top.greatxiaozou.Utils.Utils;
import top.greatxiaozou.diyTomcat.base.BaseRequest;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 封装的http请求对象
 * 重点是字符串的解析和socket
 */
@Data
public class Request extends BaseRequest {
    private Socket socket;
    private String uri;
    private String requestString;
//    private Service service;
    private Connector connector;
    private Context context;
    private String method;
    private HttpSession session;

    //NIO形式的返回
    private SelectionKey key;

    //cookies
    private Cookie[] cookies;

    //查询字符串和参数map
    private String queryString;
    private Map<String,String[]> parameterMap;

//    请求头信息
    private HashMap<String,String> headerMap;

// 服务端跳转信息
    private boolean forwarded;

//    参数map，使得request支持带参数跳转
    private Map<String,Object> attributesMap;

//    构造方法
    public Request(Socket socket,Connector connector) throws IOException {
        this(connector);
        this.socket = socket;
//      BIO，即Socket的形式返回、
        parseHttpRequest();
        init();

    }
//   传入key时的构造方法
    public Request(SelectionKey key,Connector connector) throws IOException {
        this(connector);
        this.key = key;
        SocketChannel channel = (SocketChannel) key.channel();
        socket = channel.socket();
//      NIO的形式构造Request
        parseHttpRequestNio();
        init();
    }

    public Request(Connector connector){
        this.connector = connector;
        this.parameterMap = new HashMap<>();
        this.attributesMap = new HashMap<>();

        //请求头参数的map
        this.headerMap = new HashMap<>();
    }

    private void init() {
        //调用http的解析方法
        if (StrUtil.isEmpty(requestString)){
            return;
        }

        //解析
        parseUri();
        parseContext();
        parseMethod();
        //对uri进行一个修正
        if (!"/".equals(context.getPath())){
            uri = StrUtil.removePrefix(uri,context.getPath());
            if (StrUtil.isEmpty(uri)){
                uri = "/";
            }
        }
        parseParameter();
        parseHeader();
        parseCookie();
    }

    //从cookie中获取SessionId的方法
    public String getJsessionIdFromCookie(){
        if (cookies == null){
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("JSESSIONID".equals(cookie.getName())){
                return cookie.getValue();
            }
        }
        return null;
    }

//    是否需要转发
    public boolean isForwarded(){
        return this.forwarded;
    }

    //=========================解析的方法=========================//
    //解析请求
    public void parseHttpRequest() throws IOException {
        InputStream is = this.socket.getInputStream();
        byte[] readBytes = Utils.readBytes(is,false);
        requestString = new String(readBytes, StandardCharsets.UTF_8).trim();
    }

    //使用NIO解析请求
    public void parseHttpRequestNio() throws IOException {
        SocketChannel channel = (SocketChannel) this.key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int read = channel.read(buffer);
        if (read > 0){
            buffer.flip();
            requestString = new String(buffer.array(),0,read);
//            请求传入，在另一端可以获取到请求的内容
            key.attach(requestString);
        }else {
            channel.close();
        }
        buffer.clear();
    }

    //解析uri
    public void parseUri(){
        String temp;
        temp = StrUtil.subBetween(requestString," "," ");

        if(!StrUtil.contains(temp, '?')){
            uri = temp;
            return;
        }

        uri = StrUtil.subBefore(temp,'?',false);
    }

    //解析Context
    public void parseContext(){
        Engine engine = connector.getService().getEngine();
        this.context = engine.getDefaultHost().getContext(uri);
        if (context != null){
            return;
        }

        String path = StrUtil.subBetween(uri, "/", "/");
        if (null == path){
            path = "/";
        }else {
            path = "/" + path;
        }
        context = engine.getDefaultHost().getContext(path);
        //context为空时，使用默认的/目录为默认context
        if (context == null){
            context = engine.getDefaultHost().getContext("/");
        }
    }

    //解析参数
    public void parseParameter(){
        String method = getMethod();
        //处理get方法的参数
        if (method.equals("GET")){
            String url = StrUtil.subBetween(requestString," "," ");
            if (StrUtil.contains(url, '?')){
                queryString = StrUtil.subAfter(url,"?",false);
            }
        }

        //处理post方法的参数
        if (method.equals("POST")){
            queryString = StrUtil.subAfter(requestString,"\r\n\r\n",false);
        }
        if (queryString == null){
            return;
        }
        queryString = URLUtil.decode(queryString);
        String[] paramValues = queryString.split("&");
        for (String param : paramValues) {
            String[] nameValues = param.split("=");
            String name = nameValues[0];
            String value = nameValues[1];

            //将已有的参数数组提取出来，然后加入，如果没有，则新增
            String[] values = parameterMap.get(name);
            if (values == null){
                parameterMap.put(name,new String[]{value});
            }else {
                values = ArrayUtil.append(values,value);
                parameterMap.put(name,values);
            }
        }
    }

    //解析请求的方法
    public void parseMethod(){
        this.method = StrUtil.subBefore(requestString," ",false);
    }

    //解析request的头部参数
    public void parseHeader(){
        //使用IO工具，将头部数据读取到lines中
        StringReader stringReader = new StringReader(requestString);
        List<String> lines = new ArrayList<>();

        IoUtil.readLines(stringReader,lines);

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (0 == line.length()){
                break;
            }
            String[] segs = line.split(":");
            String headerName = segs[0].toLowerCase();
            String headerValue = segs[1];

            //将解析好的数据放到headermap中
            headerMap.put(headerName,headerValue);
        }
    }

    //解析cookie
    public void parseCookie(){
        List<Cookie> cookieList = new ArrayList<>();
        String cookies = headerMap.get("cookie");
        if (cookies != null){
            String[] pairs = StrUtil.split(cookies, ";");
            for (String pair : pairs) {
                if (StrUtil.isBlank(pair))
                    continue;
                String[] seg = StrUtil.split(pair, "=");
                String name = seg[0].trim();
                String value = seg[1].trim();

                Cookie cookie = new Cookie(name, value);
                cookieList.add(cookie);

            }
        }
        //将列表转换成数组
        this.cookies = ArrayUtil.toArray(cookieList,Cookie.class);
    }


//    返回请求转发器
    public RequestDispatcher getRequestDispatcher(String uri){
        return new ApplicationRequestDispatcher(uri);
    }
//=============================重写的相关方法===========================//


    @Override
    public Object getAttribute(String s) {
        return attributesMap.get(s);
    }

    @Override
    public void setAttribute(String s, Object o) {
        attributesMap.put(s,o);
    }

    @Override
    public void removeAttribute(String s) {
        attributesMap.remove(s);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributesMap.keySet());
    }

    @Override
    public Cookie[] getCookies(){
        return this.cookies;
    }

    @Override
    public String getHeader(String s) {
        if (s == null){
            return null;
        }
        s = s.toLowerCase();
        return this.headerMap.get(s);
    }


    @Override
    public int getIntHeader(String s) {
        String value = headerMap.get(s);
        return Convert.toInt(value,0);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headerMap.keySet());
    }

    //根据参数名获取参数
    @Override
    public String getParameter(String name){
        String[] ress = parameterMap.get(name);
        if (ress != null && ress.length != 0){
            return ress[0];
        }
        return null;
    }


    @Override
    public String toString() {
        return "Request{}";
    }

    //获取参数map
    @Override
    public Map<String, String[]> getParameterMap(){
        return parameterMap;
    }

    //获取参数值的集合
    @Override
    public String[] getParameterValues(String s) {
        return parameterMap.get(s);
    }

    //获取参数名的集合
    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

    @Override
    public String getMethod(){
        return this.method;
    }

    @Override
    public ServletContext getServletContext() {
        return context.getServletContext();
    }

    @Override
    public String getRealPath(String s) {
        return getServletContext().getRealPath(s);
    }

    public String getLocalAddr(){
        return socket.getLocalAddress().getHostAddress();
    }

    public String getLocalName(){
        return socket.getLocalAddress().getHostName();
    }

    public int getLocalPort(){
        return socket.getLocalPort();
    }

    public String getProtocol(){
        return "HTTP:1.1";
    }

    public String getRemoteAddr(){
        InetSocketAddress address = ((InetSocketAddress) socket.getRemoteSocketAddress());
        String temp = address.getAddress().toString();
        return StrUtil.subAfter(temp,"/",false);
    }

    public String getRemoteHost(){
        InetSocketAddress address = ((InetSocketAddress) socket.getRemoteSocketAddress());
        return address.getHostName();
    }

    public int getRemotePort(){
        return socket.getPort();
    }

    public String getScheme(){
        return "http";
    }

    public int getServerPort(){
        return getLocalPort();
    }

    public String gerContextPath(){
        String result = this.context.getPath();
        if ("/".equals(result)){
            return "";
        }
        return result;
    }

    public String getRequestURI(){
        return uri;
    }

    public StringBuffer getRequestURL(){
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        if (port < 0) {
            port = 80; // Work around java.net.URL bug
        }
        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if ((scheme.equals("http") && (port != 80)) || (scheme.equals("https") && (port != 443))) {
            url.append(':');
            url.append(port);
        }
        url.append(getRequestURI());
        return url;
    }

    public String getServletPath(){
        return uri;
    }

}
