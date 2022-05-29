import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 题目一的服务器类
 * <p>
 * 实现的功能如下：<br/>
 * 1. 简单的GET功能<br/>
 * 2. 简单的POST功能<br/>
 *
 * 回复报文有 4 种状态码<br/>
 * 1. 200 OK<br/>
 * 2. 404 Not Found<br/>
 * 3. 500 Internal Server Error<br/>
 * 4. 501 Not Implemented<br/>
 *
 * 来表示可能存在的 4 中状态结果
 *
 *
 * @version 1.0.2
 * @author 郑勤
 * @since jdk11.0.6
 *
 */

public class Server {


    /* 通信端口 */
    private static final int PORT = 8081;

    /* 模拟服务器数据库 */
    public static Hashtable<String, String> sqlData = new Hashtable<>();

    /* 服务器的运行状态 */
    public static AtomicInteger flag = new AtomicInteger(1);

    /* 线程池 */
    private static ExecutorService ThreadPool = Executors.newFixedThreadPool(200);


    public static void main( String[] args ) throws IOException {

        try(ServerSocketChannel server = ServerSocketChannel.open()) {

            Thread.currentThread().setName("Master");

            server.bind(new InetSocketAddress(PORT));
            server.configureBlocking(false);

            Selector master = Selector.open();
            server.register(master, SelectionKey.OP_ACCEPT);

            while (true){

                master.select();
                Set<SelectionKey> selectionKeySet = master.selectedKeys();
                Iterator<SelectionKey> selectionKeyIt = selectionKeySet.iterator();

                while (selectionKeyIt.hasNext()){

                    SelectionKey key = selectionKeyIt.next();
                    selectionKeyIt.remove();

                    if(key.isAcceptable()){

                        try {

                            ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                            SocketChannel client = server.accept();
                            client.configureBlocking(false);

                            ThreadPool.execute(new ServerHandler(client));

                            if (flag.get() == 0)
                                System.exit(0);
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            key.cancel();
                        }
                    }
                    else {
                        key.cancel();
                    }
                }

            }

        }
        catch (IOException e){
            e.printStackTrace();
        }

    }
}

class ServerHandler implements Runnable{

    private Selector selector;

    private final Method method;

    private final SocketChannel socketChannel;

    private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

    private static final int SIZE = 4096;

    ServerHandler(SocketChannel client) throws IOException {
        this.socketChannel = client;
        method  = new Method();
        selector = Selector.open();
    }


    void register(SocketChannel sc) throws IOException{
        queue.add(()->{
           try{
               sc.register(this.selector, SelectionKey.OP_READ, null);
           } catch (ClosedChannelException e) {
               e.printStackTrace();
           }
        });
        selector.wakeup();
    }



    @Override
    public void run() {

        try{
            register(socketChannel);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        while (true){

            try{

                selector.select();
                Runnable task = queue.poll();

                if(task != null)
                    task.run();

                Iterator<SelectionKey> it = this.selector.selectedKeys().iterator();
                while (it.hasNext()){

                    SelectionKey key = it.next();
                    it.remove();

                    if(key.isReadable()){

                        method.processRequest(key);

                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                    else if(key.isWritable()){

                        method.processResponse(key);

                    }
                    else {
                        key.cancel();
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
