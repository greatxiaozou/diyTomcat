package top.greatxiaozou.diyTomcat.exception;

/**
 * 定义Servlet配置出现重复的异常
 */
public class WebConfigDuplicatedException extends Exception {
    public WebConfigDuplicatedException(String message) {
        super(message);
    }
}
