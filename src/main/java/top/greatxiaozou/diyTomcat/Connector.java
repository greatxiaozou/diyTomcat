package top.greatxiaozou.diyTomcat;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import lombok.*;
import top.greatxiaozou.Utils.NIOUtil;
import top.greatxiaozou.Utils.ThreadUtils;
import top.greatxiaozou.Utils.Utils;
import top.greatxiaozou.Utils.WebXmlUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Set;


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

//    管理连接的多路复用器
    private Selector selector;






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
    @SneakyThrows
    @Override
    public void run() {
//        bioRun();
        nioRun();
    }


    //BIO的形式管理链接
    private void bioRun(){
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

//    NIO的形式管理连接和数据
    public void nioRun() throws IOException {
        selector = NIOUtil.getSelector(port);
        assert selector != null;

        while (true){
            selector.select(3000);

            Set<SelectionKey> keys = selector.selectedKeys();
            for (SelectionKey key : keys) {
                //处理key并移除
                processKey(key);
                keys.remove(key);
            }
        }



    }
//    对连接进行处理
    private void processKey(SelectionKey key){
        try{
//        判断key的类别并进行处理
            if (key.isAcceptable()){
                acceptKey(key);
            }else if (key.isReadable()){
                readKey(key);
            }else if (key.isWritable()){
                writeKey(key);
            }
        }catch (IOException e){
            LogFactory.get().error(e);
            e.printStackTrace();
        }
    }

//    对可接受的key进行处理
    private void acceptKey(SelectionKey key) throws IOException {


//        获取channel
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
//        设置非阻塞
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
//            连接之后设置为可读
        socketChannel.register(selector,SelectionKey.OP_READ);

    }

//    对可读的key进行处理
    private void readKey(SelectionKey key) throws IOException{
        //从key中解析出请求
        Request request = new Request(key, this);
        System.out.println("请求内容为："+request.getRequestString());
        System.out.println("请求uri为："+request.getUri());

//        获取response
        Response response = new Response();
//        使用HttpProcessor来处理响应
        HttpProcessor httpProcessor = new HttpProcessor();
        httpProcessor.executor(key,request,response);

    }

//    对可写的key进行处理
    private void writeKey(SelectionKey key) throws IOException{
//        SocketChannel socketChannel = (SocketChannel) key.channel();
//        socketChannel.close();
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
