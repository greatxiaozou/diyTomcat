package top.greatxiaozou.Utils;

import com.sun.org.apache.bcel.internal.generic.Select;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

/**
 * 使用NIO的工具类
 */
public class NIOUtil {
    /**
     *
     * @param port 需要绑定的端口号
     * @return 多路复用器selector实例
     */
    public static Selector getSelector(int port){
        try {
//            获取多路复用器
            Selector selector = Selector.open();
//            获取 服务端的channel并设置为非阻塞运行
            ServerSocketChannel serverSocket = ServerSocketChannel.open();
            serverSocket.configureBlocking(false);

//            获取底层的Socket并绑定端口号
            ServerSocket socket = serverSocket.socket();
            socket.bind(new InetSocketAddress(port));

//            注册并绑定监听事件
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            return selector;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Selector getSelector() throws IOException {
        return Selector.open();
    }
}
