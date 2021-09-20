package top.greatxiaozou.diyTomcat.session;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class StandardSession implements HttpSession {
    private Map<String,Object> attributesMap;

    //id，创建时间。最后有效时间。context，
    private String id;
    private long creationTime;
    private long lastAccessedTime;
    private ServletContext servletContext;
    private int maxInactiveInterval;


    public StandardSession(String id, ServletContext servletContext) {
        this.attributesMap = new HashMap<>();
        this.id = id;
        this.servletContext = servletContext;
        this.creationTime = System.currentTimeMillis();
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void setMaxInactiveInterval(int i) {
        this.maxInactiveInterval = i;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return null;
    }

    @Override
    public Object getAttribute(String s) {
        return attributesMap.get(s);
    }

    @Override
    public Object getValue(String s) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributesMap.keySet());
    }

    @Override
    public String[] getValueNames() {
        return null;
    }

    @Override
    public void setAttribute(String s, Object o) {
        attributesMap.put(s,o);
    }

    @Override
    public void putValue(String s, Object o) {

    }

    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    @Override
    public void removeAttribute(String s) {
        attributesMap.remove(s);
    }

    @Override
    public void removeValue(String s) {

    }

    @Override
    public void invalidate() {
        attributesMap.clear();
    }

    @Override
    public boolean isNew() {
        return creationTime==lastAccessedTime;
    }
}
