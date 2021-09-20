package top.greatxiaozou.diyTomcat.classLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * tomcat的公共类加载器
 * 用于加载tomcat的部分文件
 */
public class CommonClassLoader extends URLClassLoader {
    public CommonClassLoader() {
        super(new URL[]{});

        try {
            //项目根目录
            File workingFolder = new File(System.getProperty("user.dir"));
            //项目的lib目录
            File libFolder = new File(workingFolder, "lib");

            File[] jarFiles = libFolder.listFiles();

            //遍历项目目录并将jar包load进类加载器的urls里
            for (File file : jarFiles) {
                if (file.getName().endsWith(".jar")){
                    URL url = new URL("file:" + file.getAbsolutePath());
                    this.addURL(url);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
