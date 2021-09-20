package top.greatxiaozou.Utils;

import sun.nio.ch.ThreadPool;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程工具
 */
public class ThreadUtils {
    //创建一个线程池
    public static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(20,100,
            60, TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(20));

    //运行任务
    public static void run(Runnable runnable){
        threadPool.execute(runnable);
    }
}
