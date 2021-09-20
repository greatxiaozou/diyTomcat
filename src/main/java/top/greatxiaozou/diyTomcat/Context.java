package top.greatxiaozou.diyTomcat;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import com.sun.media.sound.SoftChorus;
import com.sun.xml.internal.fastinfoset.tools.FI_DOM_Or_XML_DOM_SAX_SAXEvent;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.jasper.JspC;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import top.greatxiaozou.Utils.ContextXmlUtils;
import top.greatxiaozou.Utils.Utils;
import top.greatxiaozou.diyTomcat.classLoader.CommonClassLoader;
import top.greatxiaozou.diyTomcat.classLoader.WebappClassLoader;
import top.greatxiaozou.diyTomcat.config.StandardFilterConfig;
import top.greatxiaozou.diyTomcat.config.StandardServletConfig;
import top.greatxiaozou.diyTomcat.exception.WebConfigDuplicatedException;
import top.greatxiaozou.diyTomcat.session.StandardSession;
import top.greatxiaozou.diyTomcat.watcher.ContextFileChangeWatcher;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.swing.text.DefaultStyledDocument;
import java.io.File;
import java.sql.Struct;
import java.util.*;

/**
 * 代表一个应用的对象
 */
@Getter
@Setter
public class Context {
    //访问的路径
    private String path;

    // 系统中路径
    private String docBase;

    //webxml文件
    private File contextWebXmlFile;

    //是否允许热加载
    private boolean reloadable;

    //对应的host
    private Host host;

//    观察者对象（生命周期相关）
    private List<ServletContextListener> listeners;
    

    /**
     *四个map存放从web.xml中读取的servlet-url映射
     */
    private Map<String,String> url_servletClassName;
    private Map<String,String> url_servletName;
    private Map<String,String> servletName_className;
    private Map<String,String> className_servletName;

    /**
     * 5个map用于读取web.xml中的过滤器的加载和初始化
     */

    private Map<String,List<String>> url_filterClassName;
    private Map<String,List<String>> url_filterNames;
    private Map<String,String> filterName_className;
    private Map<String,String> className_filterName;
    private Map<String,Map<String,String>>filterClassName_initParams; ;

//  类名和servlet的map
    private HashMap<Class<?>, HttpServlet> servletPool;

//    用于获取参数的map
    private HashMap<String , Map<String,String>> servletName_attributeMapMap;


    //每个应用都要用tomcat自定义的classLoader
    private WebappClassLoader webappClassLoader;

    //热部署的监听器具
    private ContextFileChangeWatcher watcher;

    //ServletContext
    private ServletContext servletContext;

    //需要自启动的servlet的列表
    private List<String> loadOnStartupServletClassNames;

//    过滤器类名到对象的池
    private Map<String, Filter> filterPool;


    //初始化，构造方法
    public Context(String path, String docBase,Host host,boolean reloadable) {

        this.path = path;
        this.docBase = docBase;
        this.reloadable = reloadable;
        this.host = host;
        this.contextWebXmlFile = new File(docBase, ContextXmlUtils.getWatchedResource());
        this.servletContext = new ApplicationContext(this);
        this.servletPool = new HashMap<>();
        this.servletName_attributeMapMap = new HashMap<>();
        loadOnStartupServletClassNames = new ArrayList<>();
        this.filterPool = new HashMap<>();
        this.listeners = new ArrayList<>();

        //从web.xml里获取的servlet-url映射容器,用于处理servlet映射
        this.className_servletName = new HashMap<>();
        this.url_servletClassName = new HashMap<>();
        this.url_servletName = new HashMap<>();
        this.servletName_className = new HashMap<>();

//        初始化过滤器需要的容器
        this.filterClassName_initParams = new HashMap<>();
        this.filterName_className = new HashMap<>();
        this.url_filterClassName = new HashMap<>();
        this.url_filterNames = new HashMap<>();
        this.className_filterName = new HashMap<>();

        //获取在commonClassLoader里设置的CommonClassLoader
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        this.webappClassLoader = new WebappClassLoader(docBase, classLoader);


        deploy();
    }

    //对象在构建时的日志通知
    private void deploy(){
        TimeInterval timer = DateUtil.timer();
        //日志通知
        LogFactory.get().info("Deploying web.xml application directory {}",this.docBase);
        loadListeners();
        init();
        //如果可以热加载，则开启观察者线程
        if (reloadable){
            watcher = new ContextFileChangeWatcher(this);
            watcher.start();
        }

        JspC c = new JspC();
//        为了让getDefaultFactory这个代码能有返回值，需要在这里将JspContext初始化
        new JspRuntimeContext(servletContext, c);
        LogFactory.get().info("Deployment of web.xml application {} has finished in {} ms",this.docBase,timer.intervalMs());
    }

    //初始化
    private void init(){
        if (!contextWebXmlFile.exists()){
            return;
        }

        try{
            checkDuplicated();
        }catch (WebConfigDuplicatedException e){
            e.printStackTrace();
            return;
        }

        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document document = Jsoup.parse(xml);
        //解析文档到map里
        parseServletMapping(document);
        //解析参数
        parseServletInitParames(document);
        //解析自启动的servlet
        parseLoadOnStartup(document);
        //处理自启动的servlet
        handleLoadOnStartup();
//        解析过滤器映射和参数
        parseFilterMapping(document);
        parseFilterParams(document);
//        初始化filterPool
        initFilter();
        fireEvent("init");
    }

    public void stop(){
        //停止类加载器
        this.webappClassLoader.stop();
//        停止观察者线程
        this.watcher.stop();

//        销毁所有Servlet
        destroyServlets();
        fireEvent("destroy");
    }

    public void reload(){
        host.reload(this);
    }





    //从文件中提取，并且填充到map里
    private void parseServletMapping(Document d){
        //url_servletName
        Elements urlServletNameElements = d.select("servlet-mapping url-pattern");
        for (Element element : urlServletNameElements) {
            String urlPattern = element.text();
            String servletMapping = element.parent().select("servlet-name").first().text();
            url_servletName.put(urlPattern,servletMapping);
        }

        //servletName_className  + className_servletName
        Elements elements = d.select("servlet servlet-name");
        for (Element element : elements) {
            String servletName = element.text();
            String className = element.parent().select("servlet-class").first().text();
            servletName_className.put(servletName,className);
            className_servletName.put(className,servletName);
        }

        //url-servletClassName
        Set<String> urls = url_servletName.keySet();
        for (String url : urls) {
            String name = url_servletName.get(url);
            String className = servletName_className.get(name);
            url_servletClassName.put(url,className);
        }
    }

    //解析servlet里的参数
    public void parseServletInitParames(Document d){
        Elements servletClassNames = d.select("Servlet-class");
        for (Element servletClassName : servletClassNames) {
            String name = servletClassName.text();
            Elements params = servletClassName.parent().select("init-param");

            if (params.isEmpty()){
                continue;
            }
            Map<String,String> initParams = new HashMap<>();

            for (Element param : params) {
                String paramName = param.select("param-name").get(0).text();
                String paramValue = param.select("param-value").get(0).text();
//                作为值的参数map
                initParams.put(paramName,paramValue);
            }
            //与servlet的name一起放入map
            this.servletName_attributeMapMap.put(name,initParams);
        }
        System.out.println("servlet的参数map初始化完成");
    }

    //解析需要自启动的servlet类
    public void parseLoadOnStartup(Document d){
        Elements elements = d.select("load-on-startup");
        for (Element element : elements) {
            //获取servlet的名称
            String name = element.parent().select("servlet-class").text();
            loadOnStartupServletClassNames.add(name);
        }
    }

//    解析过滤器的url和名称
    public void parseFilterMapping(Document d){
        //解析url映射和filter的名称
        Elements elements = d.select("filter-mapping url-pattern");
        for (Element e : elements) {
            String urlPattern = e.text();
            String filterName = e.parent().select("filter-name").first().text();

            List<String> filterNames = url_filterNames.get(urlPattern);
            if (filterNames == null){
                filterNames = new ArrayList<>();
                url_filterNames.put(urlPattern,filterNames);
            }
            filterNames.add(filterName);
        }

//      解析名称和类名
        elements = d.select("filter filter-name");
        for (Element e : elements) {
            String filterName = e.text();
            String className = e.parent().select("filter-class").first().text();
            filterName_className.put(filterName,className);
            className_filterName.put(className,filterName);

        }

//        解析出由url到filter的类名的映射
        for (String s : url_filterNames.keySet()) {
            List<String> filterNames = url_filterNames.get(s);
            //为null则填充空值
            if (filterNames == null){
                filterNames = new ArrayList<>();
                url_filterNames.put(s,filterNames);
            }
            for (String filterName : filterNames) {
                String clazzName = filterName_className.get(filterName);
                List<String> clazzNames = url_filterClassName.get(s);
//                为null则填充空值
                if (null == clazzNames){
                    clazzNames = new ArrayList<>();
                    url_filterClassName.put(s,clazzNames);
                }
                clazzNames.add(clazzName);
            }

        }
    }

//    解析过滤器的参数并放到map里
    private void parseFilterParams(Document d){
        Elements elements = d.select("filter-class");
        for (Element e : elements) {
            String filterClazzName = e.text();
            Elements initElement = e.parent().select("init-param");
//             如果解析出来的参数为空，即无参数，则跳过该过滤器
            if (initElement.isEmpty()) {
                continue;
            }

            Map<String,String> initParams = new HashMap<>();
            for (Element e2 : initElement) {
                String name = e2.select("param-name").get(0).text();
                String value = e2.select("param-value").get(0).text();
                initParams.put(name,value);
            }

            //填充过滤器类名和参数的map
            filterClassName_initParams.put(filterClazzName,initParams);
        }
    }

//    初始化过滤器
    private void initFilter() {
        Set<String> clazzNames = className_filterName.keySet();
        try{
            for (String clazzName : clazzNames) {
                Class<?> clazz = this.getWebappClassLoader().loadClass(clazzName);
                Map<String, String> initParams = filterClassName_initParams.get(clazzName);
                String filterName = className_filterName.get(clazzName);

                StandardFilterConfig filterConfig = new StandardFilterConfig(servletContext, initParams, filterName);

                //填充filter池子
                Filter filter = filterPool.get(clazzName);
                if (filter == null){
                    filter = (Filter) ReflectUtil.newInstance(clazz);
                    filter.init(filterConfig);
                    filterPool.put(clazzName,filter);
                }
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }

    }

//    filter的匹配模式
    private boolean match(String pattern, String uri){
        //完全匹配
        if (StrUtil.equals(pattern,uri)){
            return true;
        }
        // /*匹配模式
        if (StrUtil.equals(pattern,"/*")){
            return true;
        }
        // 后缀名/*.jsp
        if (StrUtil.startWith(pattern,"/*.")){
            String patternExtName = StrUtil.subAfter(pattern, ".", false);
            String uriExtName = StrUtil.subAfter(uri,".",false);
            if (StrUtil.equals(patternExtName,uriExtName)){
                return true;
            }
        }
        //其他模式先不管
        return false;
    }

//    获取匹配的过滤器集合
    public List<Filter> getMatchedFilter(String uri){
        List<Filter> filters = new ArrayList<>();
        Set<String> patterns = url_filterClassName.keySet();
        Set<String> matchedPatterns = new HashSet<>();
//        如果匹配则加入到列表中
        for (String pattern : patterns) {
            if (match(pattern,uri)){
                matchedPatterns.add(pattern);
            }
        }

//        根据匹配列表中的uri，获取对应的类名列表
        Set<String> matchedFilterClassNames = new HashSet<>();
        for (String pattern : matchedPatterns) {
            List<String> filterClassName = url_filterClassName.get(pattern);
            matchedFilterClassNames.addAll(filterClassName);
        }
//         根据类名列表，从过滤器池子中获取过滤器实例
        for (String className : matchedFilterClassNames) {
            Filter filter = filterPool.get(className);
            filters.add(filter);
        }

        return filters;

    }

    //对需要自启动的类进行自启动
    public void handleLoadOnStartup(){
        for (String name : loadOnStartupServletClassNames) {
            try {
                Class<?> clazz = webappClassLoader.loadClass(name);
                getServlet(clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //对servlet配置是否重复进行检查
    private void checkDuplicated() throws WebConfigDuplicatedException {
        String xml = FileUtil.readUtf8String(Utils.contextXmlFile);
        Document document = Jsoup.parse(xml);
        checkDuplicated(document,"servlet-mapping url-pattern","servlet-url 重复，请保持唯一性：{}");
        checkDuplicated(document,"servlet servlet-class","servlet类名重复，请保持唯一性：{}");
        checkDuplicated(document,"servlet servlet-name","servlet名称重复，请保持唯一性：{}");

    }

    //检查的源方法
    private void checkDuplicated(Document document,String mapping,String desc) throws WebConfigDuplicatedException {
        Elements elements = document.select(mapping);
        List<String> contents = new ArrayList<>();
        for (Element element : elements) {
            contents.add(element.text());
        }

        Collections.sort(contents);

        for (int i = 0; i < contents.size() - 1; i++) {
            String pre = contents.get(i);
            String next = contents.get(i + 1);
            if (pre.equals(next)){
                throw new WebConfigDuplicatedException(StrUtil.format(desc,pre));
            }
        }
    }

    //通过uri获取servlet的类名方法
    public String getServletClassName(String uri){
        if (url_servletClassName.containsKey(uri)){
            return url_servletClassName.get(uri);
        }
        return null;
    }

    public boolean isReloadable(){
        return this.reloadable;
    }

//    通过类名获取servlet
    public synchronized HttpServlet getServlet(Class<?> clazz) throws IllegalAccessException, InstantiationException, ServletException {
        HttpServlet servlet = servletPool.get(clazz);
        if (servlet == null){
            servlet = (HttpServlet) clazz.newInstance();
            //获取并传参
            ServletContext context = this.getServletContext();
            String className = clazz.getName();
            String servletName = className_servletName.get(className);
            Map<String, String> map = servletName_attributeMapMap.get(className);
            StandardServletConfig config = new StandardServletConfig(context, servletName, map);
            servlet.init(config);
            servletPool.put(clazz,servlet);
        }
        return servlet;
    }

//     销毁该Context里的所有servlet
    public void destroyServlets(){
        Collection<HttpServlet> values = servletPool.values();
        for (HttpServlet value : values) {
            value.destroy();
        }
    }

//    添加监听器
    private void addListener(ServletContextListener listener){
        this.listeners.add(listener);
    }

    private void loadListeners(){
        try {
            if (!contextWebXmlFile.exists()){
                return;
            }
            String xml = FileUtil.readUtf8String(contextWebXmlFile);
            Document d = Jsoup.parse(xml);

            Elements elements = d.select("listener listener-class");

            for (Element element : elements) {
                String listenerClassName = element.text();

                Class<?> clazz = this.getWebappClassLoader().loadClass(listenerClassName);
                ServletContextListener listener = (ServletContextListener) clazz.newInstance();
                addListener(listener);

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void fireEvent(String type){
        ServletContextEvent event = new ServletContextEvent(servletContext);
        for (ServletContextListener listener : listeners) {
            if ("init".equals(type)){
                listener.contextInitialized(event);
            }
            if ("destroy".equals(type)){
                listener.contextDestroyed(event);
            }
        }
    }
}
