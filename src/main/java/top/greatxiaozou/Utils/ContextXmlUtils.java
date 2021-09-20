package top.greatxiaozou.Utils;

import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * 获取Servlet配置地址的工具类
 */
public class ContextXmlUtils {
    //获取能过获取web配置的配置文件位置的方法
    public static String getWatchedResource(){
        try {
            String context = FileUtil.readUtf8String(Utils.contextXmlFile);
            Document document = Jsoup.parse(context);
            Element e = document.select("WatchedResource").first();
            return e.text();
        }catch (Exception e){
            e.printStackTrace();
            return "WEB-INF/web.xml";
        }
    }
}
