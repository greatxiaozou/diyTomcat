package top.greatxiaozou.diyTomcat;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import lombok.Data;
import top.greatxiaozou.Utils.ServerXmlUtils;
import top.greatxiaozou.Utils.Utils;
import top.greatxiaozou.diyTomcat.watcher.WarFileWatcher;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

/**
 * Tomcat内置的Host对象
 */
@Data
public class Host {
    private String name;
    private HashMap<String,Context> contextMap;
    private Engine engine;

    //构造方法
    public Host(String name, Engine engine) {
        this.name = name;
        this.contextMap = new HashMap<>();
        this.engine = engine;
//        扫描context工程文件
        scanContextOnWebappsFolder();
        scanContextXml();

//        扫描war文件
        scanWarOnWebAppsFolder();

//        动态war部署的监视器
        new WarFileWatcher(this).start();
    }


//    获取应用对象

    public Context getContext(String path){
        return contextMap.get(path);
    }
    /**
     * 通过xml配置的方式加载应用
     */
    private void scanContextXml(){
        List<Context> contexts = ServerXmlUtils.getContexts(this);

        for (Context context : contexts) {
            contextMap.put(context.getPath(),context);
        }
    }

    /**
     * 通过扫描文件夹的形式
     */
    private void scanContextOnWebappsFolder(){
        //拿到webapps下面的所以文件和文件夹
        File[] files = Utils.webappsFolder.listFiles();

        if (files == null){
            return;
        }

//        如果是文件夹，即是一个应用，进行加载
        for (File file : files) {
            if (!file.isDirectory()){
                continue;
            }
            loadContext(file);
        }
    }



    //=========热加载，重新部署======================//
    public void reload(Context context){
        //热部署日志
        LogFactory.get().info("Reloading context with name [{}] has started,",context.getPath());
        String path = context.getPath();
        String docBase = context.getDocBase();
        boolean reloadable = context.isReloadable();

        context.stop();
        contextMap.remove(path);
        //重新获取context
        Context newContext = new Context(path,docBase,this,reloadable);
        contextMap.put(newContext.getPath(),newContext);
        LogFactory.get().info("Reload context with name [{}] has finished,",newContext.getPath());
    }

    //================加载目录的方法==================//
    private void loadContext(File folder){
//        获取路径和path
        String path = folder.getName();
        if(path.equals("ROOT")){
            path = "/";
        }else{
            path = "/" + path;
        }

//       制造context对象
        String docBase = folder.getAbsolutePath();
        Context context = new Context(path,docBase,this,true);
//        建立对象和context的映射
        contextMap.put(path,context);
    }

    //==================war加载=====================//
//   将一个war文件加载为一个context
    public void loadWar(File warFile){
        String fileName = warFile.getName();
        String folderName = StrUtil.subBefore(fileName, ".", true);

//        看context是否存在
        Context context = getContext("/" + folderName);
//        存在则返回
        if (context != null){
            return;
        }

//        看是否有对应的文件夹
        File folder = new File(Utils.webappsFolder, folderName);
        if (folder.exists()){
            return;
        }

//        移动war文件
        File tempWarFile = FileUtil.file(Utils.webappsFolder, folderName, fileName);
        File contextFolder = tempWarFile.getParentFile();
        contextFolder.mkdir();
        FileUtil.copyFile(warFile,tempWarFile);
//        解压
        String command = "jar xvf " + fileName;
        System.out.println(command);

//        执行解压指令
        Process process = RuntimeUtil.exec(null, contextFolder, command);
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        删除临时文件
        tempWarFile.delete();
//        创建新的context
        loadContext(contextFolder);
    }

    private void scanWarOnWebAppsFolder(){
        File folder = FileUtil.file(Utils.webappsFolder);
        File[] files = folder.listFiles();
        for (File file : files) {
            if (!file.getName().toLowerCase().endsWith(".war")){
                continue;
            }
            loadWar(file);
        }
    }
}
