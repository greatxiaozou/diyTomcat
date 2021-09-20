package top.greatxiaozou.diyTomcat;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import top.greatxiaozou.Utils.ServerXmlUtils;

import java.util.List;

/**
 * Tomcat内部的引擎类
 */
@Getter
@Setter
public class Engine {
    private String defaultHost;
    private List<Host> hosts;
    private Service service;

    public Engine(Service service) {
        this.service = service;
        this.defaultHost = ServerXmlUtils.getEngineDefaultHost();
        this.hosts = ServerXmlUtils.getHosts(this);
        checkDefault();
    }

    //检查默认host
    private void checkDefault(){
        if (getDefaultHost() == null){
            throw new RuntimeException("未找到默认的Host，请检查相关文件");
        }
    }

    //获取默认host
    public Host getDefaultHost(){
        for (Host host : hosts) {
            if (host.getName().equals(defaultHost)){
                return host;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Engine{" +
                "defaultHost='" + defaultHost + '\'' +
                ", hosts=" + hosts +
                ", service=" + service +
                '}';
    }
}
