package top.greatxiaozou.Utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import sun.util.resources.cldr.ses.CurrencyNames_ses;
import top.greatxiaozou.diyTomcat.Request;
import top.greatxiaozou.diyTomcat.Response;
import top.greatxiaozou.diyTomcat.session.StandardSession;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

/**
 * 用于管理Session
 */
public class SessionManager {
    private static Map<String, StandardSession> sessionMap = new HashMap<>();
    private static int defaultTimeout = getTimeout();
    static {
        //开启session的过期时间检查线程
        startSessionOutdateCheckThread();
    }

    //由SessionManager获取session的方法
    public static HttpSession getSession(String id, Request request, Response response){
        if (id == null){
            return newSession(request,response);
        }
        StandardSession currentSession = sessionMap.get(id);
        if (currentSession == null){
            return newSession(request,response);
        }
        //将session最近的调用时间获取
        currentSession.setLastAccessedTime(System.currentTimeMillis());
        createCookieBySession(currentSession,request,response);
        return currentSession;
    }

//    将SessionId存到cookie中
    private static void createCookieBySession(HttpSession session,Request request,Response response){
        Cookie cookie = new Cookie("JSESSIONID",session.getId());
        cookie.setMaxAge(session.getMaxInactiveInterval());
        cookie.setPath(request.getContext().getPath());
        response.addCookie(cookie);
    }

    //通过request创建session
    private static HttpSession newSession(Request request,Response response){
        ServletContext servletContext = request.getServletContext();
        String sid = generateSessionId();
        StandardSession session = new StandardSession(sid, servletContext);
        session.setLastAccessedTime(defaultTimeout);
        sessionMap.put(sid,session);
        createCookieBySession(session,request,response);
        return session;
    }

    //从web文件中获取session的过期时间，默认为30分钟
    private static int getTimeout(){
        int defaultResult = 30;
        try {
            Document document = Jsoup.parse(Utils.webXmlFile, "utf-8");
            Elements es = document.select("session-config session-timeout");
            if (es.isEmpty()){
                return defaultResult;
            }
            return Convert.toInt(es.get(0).text());
        }catch (IOException e){
            e.printStackTrace();
            return defaultResult;
        }
    }

    //检查session是否过期
    private static void checkOutDateSession(){
        Set<String> sessionIds = sessionMap.keySet();
        List<String> outdateSessionIds = new ArrayList<>();
        //取出所有的session，判断是否过期，过期则将id放到list中
        for (String sessionId : sessionIds) {
            StandardSession session = sessionMap.get(sessionId);
            long interval = System.currentTimeMillis()-session.getCreationTime();
            if (interval > session.getMaxInactiveInterval() * 1000){
                outdateSessionIds.add(sessionId);
            }
        }

        //利用过期的id的list，对过期的session进行清除
        for (String outdateSessionId : outdateSessionIds) {
            sessionMap.remove(outdateSessionId);
        }
    }

    //开启检查线程
    private static void startSessionOutdateCheckThread(){
        new Thread(()->{
            while (true){
                checkOutDateSession();
                //30s检查一次
                ThreadUtil.sleep(30*1000);
            }
        }).start();
    }

    //生成sessionId的方法
    private static synchronized String generateSessionId(){
        String res = null;
        byte[] bytes = RandomUtil.randomBytes(16);
        res = new String(bytes);
        //使用md5加密
        res = SecureUtil.md5(res);
        res = res.toUpperCase();
        return res;
    }
}
