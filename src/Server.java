import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private static final int PORT = 8081;

    public static AtomicInteger flag = new AtomicInteger(1); //标记是否为shutdown

    public static void main(String[] args) {

        try(ServerSocketChannel server = ServerSocketChannel.open()) { //打开服务器的套接字通道
            Thread.currentThread().setName("master"); //启动一个主线程
            server.bind(new InetSocketAddress(PORT)); //监听固定端口
            server.configureBlocking(false); //将该通道设置为非阻塞（不设置则仍为BIO）

            Selector master = Selector.open(); //创建一个Selector
            server.register(master, SelectionKey.OP_ACCEPT);
            //将当前这个 server 注册到 Selector 下面, 后面的OP_ACCEPT表示这个server只管接受连接，其他什么都不做

            Handler[] handlers = new Handler[4];
            for (int i = 0 ; i< handlers.length; i++)
                handlers[i] = new Handler(String.valueOf(i));

            AtomicInteger idx = new AtomicInteger(); //线程安全


            while(true) {

                master.select();

                Iterator<SelectionKey> iter = master.selectedKeys().iterator();
                while (iter.hasNext()) { //遍历Selector中监听到的事件

                    SelectionKey key = iter.next();
                    iter.remove();

                    if(key.isAcceptable()) {
                        //事件：有客户端已建立连接（类似的事件还有 isReadable, isWriteable, isConnected）
                        SocketChannel channel = server.accept(); //获取建立的通道
                        channel.configureBlocking(false);
                        handlers[idx.getAndIncrement() % handlers.length].register(channel);
                        //循环选择线程，将通道注册到其下
                    }
                    else {
                        key.cancel();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

class Handler implements Runnable{

    private Thread thread;
    private Selector selector;
    private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

    public Handler( String name) throws IOException {
        thread = new Thread(this, name);
        thread.start();
        selector = Selector.open();
    }


    public void register(SocketChannel sc) throws IOException {
        queue.add(()->{ //将注册通道加入队列维护
            try {
                sc.register(this.selector,SelectionKey.OP_READ,null); //注册感兴趣的事件
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        });

        selector.wakeup(); //唤醒阻塞的select()方法
    }


    @Override
    public void run() {

        while(true){
            try {

                selector.select();
                Runnable task = queue.poll(); //取出队首并删除结点

                if(task!=null)
                    task.run(); //调用register run方法以注册通道

                Iterator<SelectionKey> iter = this.selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();
                    if (key.isReadable()) { //若该事件可读，即收到请求报文
                        Method method = new Method(); //实例化method方法
                        method.processRequest(key); //解析报文并将其附着到该通道所关联的key的attchment中
                        key.interestOps(SelectionKey.OP_WRITE); //将该事件改为可写
                    }
                    else if(key.isWritable()){ //若该事件可写，即需回复报文
                        Method method = new Method();
                        method.processResponse(key); //写响应报文
                    }
                    else
                        key.cancel(); //忽略该事件
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}