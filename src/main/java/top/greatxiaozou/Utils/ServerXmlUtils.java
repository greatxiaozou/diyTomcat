package top.greatxiaozou.Utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import top.greatxiaozou.diyTomcat.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析XML的工具
 */
public class ServerXmlUtils {

    private static String xml = FileUtil.readUtf8String(Utils.serverXmlFile);
//    根据server.xml文件解析出Context对象
    public static List<Context> getContexts(Host host){
        List<Context> res = new ArrayList<>();



        //使用jsoup的parse解析xml文件
        Document document = Jsoup.parse(xml);

        Elements es = document.select("Context");
        for (Element e : es) {
            String path = e.attr("path");
            String docBase = e.attr("docBase");
            boolean reloadable = Convert.toBool(e.attr("reloadable"));
            Context context = new Context(path, docBase,host,reloadable);
            res.add(context);
        }
        return res;
    }

//    根据根据server.xml获取Host的name属性
    public static String getServiceName(){
        Document document = Jsoup.parse(xml);
        Element host = document.select("Service").first();
        return host.attr("name");
    }

//     获取默认的host
    public static String getEngineDefaultHost(){
        Document document = Jsoup.parse(xml);

        Element engine = document.select("Engine").first();

        return engine.attr("defaultHost");
    }

//获取Host对象
    public static List<Host> getHosts(Engine engine){
        List<Host> res = new ArrayList<>();
        Document document = Jsoup.parse(xml);
        for (Element e : document.select("Host")) {
            String name = e.attr("name");
            Host host = new Host(name, engine);
            res.add(host);
        }

        return res;

    }

//    获取connector对象
    public static List<Connector> getConnectors(Service service){
        List<Connector> result = new ArrayList<>();
        Document d = Jsoup.parse(xml);
        Elements es = d.select("Connector");
        for (Element e : es) {
            //一个一个读取
            int port = Convert.toInt(e.attr("port"));
            String compression = e.attr("compression");
            int compressionMinSize = Convert.toInt(e.attr("compressionMinSize"),0);
            String noCompressionUserAgent = e.attr("noCompressionUserAgents");
            String compressableMimeType = e.attr("compressableMimeType");
            Connector c = new Connector(service);

//            一个一个注入
            c.setPort(port);
            c.setCompressableMimeType(compressableMimeType);
            c.setCompression(compression);
            c.setCompressionMinSize(compressionMinSize);
            c.setNoCompressionUserAgents(noCompressionUserAgent);

            result.add(c);
        }
        return result;
    }
}
