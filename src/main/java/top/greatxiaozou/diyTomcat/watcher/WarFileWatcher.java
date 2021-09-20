package top.greatxiaozou.diyTomcat.watcher;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import top.greatxiaozou.Utils.Utils;
import top.greatxiaozou.diyTomcat.Host;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

import static cn.hutool.core.io.watch.WatchMonitor.ENTRY_CREATE;

/**
 * 进行动态war部署使用的观察类
 */
public class WarFileWatcher {
    private WatchMonitor monitor;

    public WarFileWatcher(Host host){
        this.monitor = WatchUtil.createAll(Utils.webappsFolder, 1, new Watcher() {
            private synchronized void dealWith(WatchEvent<?> event,Path currentPath){
                String fileName = event.context().toString();
                if (fileName.toLowerCase().endsWith(".war") && ENTRY_CREATE.equals(event.kind())){
                    File warFile = FileUtil.file(Utils.webappsFolder, fileName);
                    host.loadWar(warFile);
                }
            }

            @Override
            public void onCreate(WatchEvent<?> event, Path currentPath) {
                dealWith(event,currentPath);
            }

            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                dealWith(event,currentPath);
            }

            @Override
            public void onDelete(WatchEvent<?> event, Path currentPath) {
                dealWith(event,currentPath);
            }

            @Override
            public void onOverflow(WatchEvent<?> event, Path currentPath) {
                dealWith(event,currentPath);
            }
        });
    }

    public void start(){
        monitor.start();
    }

    public void stop(){
        monitor.interrupt();
    }
}
