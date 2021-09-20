package top.greatxiaozou.diyTomcat;


import cn.hutool.core.util.ArrayUtil;

import javax.servlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 责任链模式的调用链
 */
public class ApplicationFilterChain implements FilterChain {
    private Filter[] filters;
    private Servlet servlet;
    int pos;

    public ApplicationFilterChain(List<Filter> filterList,Servlet servlet) {
        filters = ArrayUtil.toArray(filterList,Filter.class);
        this.servlet = servlet;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
        if (pos < filters.length){
            Filter filter = filters[pos];
            pos++;
            filter.doFilter(servletRequest,servletResponse,this);
        }else {
            //当调用链完成之后，调用service方法，类似于递归中的basecase
            servlet.service(servletRequest,servletResponse);
        }
    }
}
