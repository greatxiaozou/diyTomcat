package top.greatxiaozou.diyTomcat;



import top.greatxiaozou.diyTomcat.base.BaseServletContext;

import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;

/**
 * 继承了Base类
 *
 */
public class ApplicationContext extends BaseServletContext {

//    用于存放属性的参数表
    private HashMap<String,Object> attributesMap;
//    context应用
    private Context context;


    public ApplicationContext(Context context) {
        attributesMap = new HashMap<>();
        this.context = context;
    }

    //    移除参数
    public void removeAttribute(String name){
        attributesMap.remove(name);
    }

//    获取参数
    public Object getAttribute(String name){
        return attributesMap.get(name);
    }

//    设置参数
    public void setAttribute(String name,Object value){
        attributesMap.put(name,value);
    }

//    获取参数name集合
    public Enumeration<String> getAttributeNames(){
        Set<String> set = attributesMap.keySet();
        return Collections.enumeration(set);
    }

//    获取真实路径
    @Override
    public String getRealPath(String path){
        return new File(context.getDocBase(),path).getAbsolutePath();
    }
}
