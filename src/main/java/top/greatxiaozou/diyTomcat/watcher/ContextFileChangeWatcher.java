package top.greatxiaozou.diyTomcat.watcher;

import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.log.LogFactory;
import top.greatxiaozou.diyTomcat.Context;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * 监听项目文件class和jar、xml等是否改变的监视器
 * 用于实现热加载
 */
public class ContextFileChangeWatcher {

    //监听器
    private WatchMonitor monitor;

    //是否停止监听
    private boolean stop = false;

    public ContextFileChangeWatcher(Context context) {
        this.monitor = WatchUtil.createAll(context.getDocBase(),Integer.MAX_VALUE, new Watcher() {
            private void dealWith(WatchEvent<?> event){
                synchronized (ContextFileChangeWatcher.class){
                    String fileName = event.context().toString();
                    if (stop){
                        return;
                    }
                    if (fileName.endsWith(".jar") || fileName.endsWith(".class") || fileName.endsWith(".xml")){
                        stop = true;
                    }
                    LogFactory.get().info(this+"检测到重要文件变化：{}",fileName);
                    context.reload();
                }
            }

            @Override
            public void onCreate(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
            }

            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
            }

            @Override
            public void onDelete(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
            }

            @Override
            public void onOverflow(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
            }
        });
        this.monitor.setDaemon(true);
    }

    public void start(){
        monitor.start();
    }

    public void stop(){
        monitor.close();
    }
}
