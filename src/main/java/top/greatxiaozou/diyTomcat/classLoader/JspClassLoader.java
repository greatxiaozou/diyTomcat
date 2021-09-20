package top.greatxiaozou.diyTomcat.classLoader;

import cn.hutool.core.util.StrUtil;
import top.greatxiaozou.Utils.Utils;
import top.greatxiaozou.diyTomcat.Context;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于加载Jsp的类加载器
 */
public class JspClassLoader extends URLClassLoader {

    //记录jsp文件和对应的加载器的map
    private static Map<String,JspClassLoader> map = new HashMap<>();


    //用相对路径作为key
    public static void invalidJspClassLoader(String uri,Context context){
        String key = context.getPath() + "/" + uri;
        map.remove(key);
    }

//    获取的JspClassLoader
    public static JspClassLoader getJspClassLoader(String uri,Context context){
        String key = context.getPath() + "/" + uri;
        JspClassLoader loader = map.get(key);
        if (loader == null){
            loader = new JspClassLoader(context);
        }
        map.put(key,loader);
        return loader;
    }

//    构造方法
    private JspClassLoader(Context context){
        super(new URL[]{},context.getWebappClassLoader());
        try{
            String subFolder;
            String path = context.getPath();
            //如果path为/，则用占位符，否则取最后的uri之前的文件夹定位
            if ("/".equals(path)){
                subFolder = "_";
            }else {
                subFolder = StrUtil.subAfter(path,"/",false);
            }

            //获取需要加载的文件
            File classFolder = new File(Utils.workFolder,subFolder);
            URL url = new URL("file:" + classFolder.getAbsolutePath() + "/");
            this.addURL(url);

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
