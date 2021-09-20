import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import sun.plugin2.util.SystemUtil;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 自定义ClassLoader的测试类
 */
public class CustomizedClassLoader extends ClassLoader {
    private File classFolder = new File(System.getProperty("user.dir"),"classes_4_test");

    //根据类的全限定名找到类的文件并将其定义为一个class对象
    @Override
    protected Class<?> findClass(String qualifiedName) throws ClassNotFoundException {
        byte[] data = loadClassData(qualifiedName);
        return defineClass(qualifiedName, data, 0, data.length);
    }

//    加载类文件的信息（以二进制的形式）
    private byte[] loadClassData(String fullQualifiedName) throws ClassNotFoundException {
        String filename = StrUtil.replace(fullQualifiedName, ".", "/") + ".class";
        File file = new File(classFolder,filename);
        if (!file.exists()){
            throw new ClassNotFoundException(filename);
        }
        return FileUtil.readBytes(file);
    }

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        CustomizedClassLoader myClassloader = new CustomizedClassLoader();
        Class<?> clazz = myClassloader.loadClass("cn.how2j.diytomcat.test.HOW2J");
        Object o = clazz.newInstance();
        Method hello = clazz.getMethod("hello");

        hello.invoke(o);

        System.out.println(clazz.getClassLoader());
    }
}
