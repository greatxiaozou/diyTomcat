import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * 通过URL来加载类
 * 加载Jar包里面的类
 */
public class CustomizedURLClassLoader extends URLClassLoader {

    public CustomizedURLClassLoader(URL[] urls) {
        super(urls);
    }

    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        URL url = new URL("file:D:/intellij/work_space/diyTomcat/jar_4_test/test.jar");

        //数组形式
        URL[] urls = new URL[]{url};

        CustomizedURLClassLoader loader = new CustomizedURLClassLoader(urls);
        Class<?> clazz = loader.loadClass("cn.how2j.diytomcat.test.HOW2J");

        Object instance = clazz.newInstance();
        Method hello = clazz.getMethod("hello");

        hello.invoke(instance);

        System.out.println(clazz.getClassLoader());
    }
}
