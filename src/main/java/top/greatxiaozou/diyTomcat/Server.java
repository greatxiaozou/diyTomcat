package top.greatxiaozou.diyTomcat;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;

import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tomcat服务器本身
 */
public class Server {
    private Service service;

    public Server() {
        this.service = new Service(this);
    }

    /**
     * 服务启动的方法
     */
    public void start(){
        TimeInterval timer = DateUtil.timer();
        logJVM();
        init();
        LogFactory.get().info("Server startup in {} ms",timer.intervalMs());
    }

    /**
     * 原本的bootstrap的main方法
     */
    private void init(){
        service.start();
    }

    //===============jvm日志测试=================//
    private static void logJVM() {
        Map<String,String> infos = new LinkedHashMap<>();
        infos.put("Server version", "Greatxiaozou DiyTomcat/1.0.1");
        infos.put("Server built", "2020-04-08 10:20:22");
        infos.put("Server number", "1.0.1");
        infos.put("OS Name\t", SystemUtil.get("os.name"));
        infos.put("OS Version", SystemUtil.get("os.version"));
        infos.put("Architecture", SystemUtil.get("os.arch"));
        infos.put("Java Home", SystemUtil.get("java.home"));
        infos.put("JVM Version", SystemUtil.get("java.runtime.version"));
        infos.put("JVM Vendor", SystemUtil.get("java.vm.specification.vendor"));
        Set<String> keys = infos.keySet();
        for (String key : keys) {
            LogFactory.get().info(key+":\t\t" + infos.get(key));
        }
    }

}
