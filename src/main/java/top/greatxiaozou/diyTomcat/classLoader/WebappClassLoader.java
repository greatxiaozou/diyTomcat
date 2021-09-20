package top.greatxiaozou.diyTomcat.classLoader;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * WebappClassLoader用于加载用户打包好的classes文件
 */
public class WebappClassLoader extends URLClassLoader {
    public WebappClassLoader(String docBase, ClassLoader classLoader){
        super(new URL[]{},classLoader);

        try{
            //获取WEB—INF的三个文件夹
            File webinfFolder = new File(docBase, "WEB-INF");
            File classFolder = new File(webinfFolder, "classes");
            File libFolder = new File(webinfFolder, "lib");

            //因为是目录所以要加“/”
            URL url = new URL("file:"+classFolder.getAbsolutePath()+"/");
            this.addURL(url);

            //获取所有在classes里的文件
            List<File> files = FileUtil.loopFiles(libFolder);
            for (File file : files) {
                url = new URL("file:"+file.getAbsolutePath());
                this.addURL(url);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void stop(){
        try {
            this.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
