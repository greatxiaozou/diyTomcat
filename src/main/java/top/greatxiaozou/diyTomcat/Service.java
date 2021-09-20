package top.greatxiaozou.diyTomcat;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import top.greatxiaozou.Utils.ServerXmlUtils;
import top.greatxiaozou.Utils.WebXmlUtils;

import java.util.List;

@Getter
@Setter
public class Service {
    private String name;
    private Engine engine;
    private Server server;
    private List<Connector> connectors;

    public Service(Server server){
        this.server = server;
        this.name = ServerXmlUtils.getServiceName();
        this.engine = new Engine(this);
        this.connectors = ServerXmlUtils.getConnectors(this);
    }

    //初始化的方法，启动Connector
    private void init(){
//        TimeInterval timeInterval = DateUtil.timer();
        for (Connector connector : connectors) {
            connector.init();
//            LogFactory.get().info("Initialization processd in {} ms",timeInterval.intervalMs());
            connector.start();
        }
    }

    //启动方法
    public void start(){
        init();
    }
}
