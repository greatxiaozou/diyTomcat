package top.greatxiaozou.Utils;

import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import top.greatxiaozou.diyTomcat.Connector;
import top.greatxiaozou.diyTomcat.Context;
import top.greatxiaozou.diyTomcat.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * webxml的解析和使用工具
 */
public class WebXmlUtils {
    //mime-type 映射表
    public static HashMap<String,String> mimeTypeMapping = new HashMap<>();
    public static String webXml = FileUtil.readUtf8String(Utils.webXmlFile);
    public static String serverXml = FileUtil.readUtf8String(Utils.serverXmlFile);

    public static String getWelcomeFile(Context context){
        //解析web.xml文件
        Document d = Jsoup.parse(webXml);
        Elements elements = d.select("welcome-file");

        for (Element element : elements) {
            String fileName = element.text();
            File f = new File(context.getDocBase(),fileName);
            if (f.exists()){
                return f.getName();
            }
        }
        //不存在时默认返回index.html
        return "index.html";
    }

    /**
     * 获取mimeType
     */

    public static String getMimeTypeByName(String name){
        //使用dcl进行线程安全的初始化
        if (mimeTypeMapping.isEmpty()){
            synchronized (WebXmlUtils.class){
                if (mimeTypeMapping.isEmpty()){
                    initMimeMap();
                }
            }
        }
        String mimeType = mimeTypeMapping.get(name);
        if (mimeType == null){
            //未出现对应类型则默认为html
            return "text/html";
        }
        return mimeType;
    }

    //将所有的mime-type映射加载到内存
    private static void initMimeMap(){
        //获取映射
        Document document = Jsoup.parse(webXml);
        Elements elements = document.select("mime-mapping");
        //遍历加载到内存
        for (Element element : elements) {
            String typeName = element.select("extension").first().text();
            String mimeName = element.select("mime-type").first().text();
            mimeTypeMapping.put(typeName,mimeName);
        }
    }

}
