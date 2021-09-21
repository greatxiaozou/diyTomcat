package top.greatxiaozou.diyTomcat;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.greatxiaozou.Utils.ThreadUtils;
import top.greatxiaozou.Utils.Utils;
import top.greatxiaozou.Utils.WebXmlUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


/**
 *  表示多端口启动的类
 *  同时处理http多连接
 */
@Getter
@Setter
@NoArgsConstructor
public class Connector implements Runnable {

    //端口号
    private int port;

    //父节点，服务层
    private Service service;

    //压缩相关的属性
    private String compression;
    private int compressionMinSize;
    private String noCompressionUserAgents;
    private String compressableMimeType;






    //带service的构造
    public Connector(Service service) {
        this.service = service;
    }

    //启动日志
    public void init(){
        LogFactory.get().info("initializing ProtocolHandler {http-bio-{}}",port);
    }

    public void start(){
        LogFactory.get().info("Starting ProtocolHandler {http-bio-{}}",port);
        new Thread(this).start();
    }

//    任务或者线程的run方法，执行的真正流程在此处
    @Override
    public void run() {
        try{
            ServerSocket serverSocket = new ServerSocket(port);

            while(true){
                Socket socket = serverSocket.accept();

                ThreadUtils.run(()->{
                    try{

                        //使用request对象来获取请求
                        Request request = new Request(socket,this);
                        //                String req = request.getRequestString();
                        System.out.println("接受到请求数据："+request.getRequestString());
                        System.out.println("请求的uri为："+request.getUri());
                        //返回数据
                        Response response = new Response();

                        //将连接的处理交给httpProcessor来处理
                        HttpProcessor httpProcessor = new HttpProcessor();
                        httpProcessor.executor(socket,request,response);

                    }catch (IOException e) {
                        LogFactory.get().error(e);
                        e.printStackTrace();
                    }

////                关闭资源
//                out.flush();
//                socket.close(); 移动至handle200方法中关闭
                });
            }
        }catch (IOException e){
            LogFactory.get().error(e);
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Connector{" +
                "port=" + port +
                ", service=" + service +
                ", compression='" + compression + '\'' +
                ", compressionMinSize=" + compressionMinSize +
                ", noCompressionUserAgents='" + noCompressionUserAgents + '\'' +
                ", compressableMimeType='" + compressableMimeType + '\'' +
                '}';
    }
}
