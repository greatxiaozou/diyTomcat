package top.greatxiaozou.diyTomcat.config;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 需要作为参数用于filter的初始化的类
 *
 */
public class StandardFilterConfig implements FilterConfig {
//    应用，初始参数和过滤器
    private ServletContext servletContext;
    private Map<String,String> initParameters;
    private String filterName;


    public StandardFilterConfig(ServletContext servletContext, Map<String, String> initParameters, String filterName) {
        this.servletContext = servletContext;
        this.initParameters = initParameters;
        this.filterName = filterName;
        if (initParameters == null){
            this.initParameters = new HashMap<>();
        }
    }

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getInitParameter(String s) {
        return initParameters.get(s);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }
}
