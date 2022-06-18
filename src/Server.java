import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private static final int PORT = 8081;

    public static AtomicInteger flag = new AtomicInteger(1);

    public static void main(String[] args) {

        try(ServerSocketChannel server = ServerSocketChannel.open()) {
            Thread.currentThread().setName("master");
            server.bind(new InetSocketAddress(PORT));
            server.configureBlocking(false);

            Selector master = Selector.open();
            server.register(master, SelectionKey.OP_ACCEPT);

            Handler[] handlers = new Handler[4];
            for (int i = 0 ; i< handlers.length; i++)
                handlers[i] = new Handler(String.valueOf(i));

            AtomicInteger idx = new AtomicInteger();


            while(true) {

                master.select();

                Iterator<SelectionKey> iter = master.selectedKeys().iterator();
                while (iter.hasNext()) {

                    SelectionKey key = iter.next();
                    iter.remove();

                    if(key.isAcceptable()) {
                        SocketChannel channel = server.accept();
                        channel.configureBlocking(false);
                        handlers[idx.getAndIncrement() % handlers.length].register(channel);
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
        queue.add(()->{
            try {
                sc.register(this.selector,SelectionKey.OP_READ,null);//boss
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        });

        selector.wakeup();
    }


    @Override
    public void run() {

        while(true){
            try {

                selector.select();
                Runnable task = queue.poll();

                if(task!=null)
                    task.run();

                Iterator<SelectionKey> iter = this.selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();
                    if (key.isReadable()) {
                        Method method = new Method();
                        method.processRequest(key);
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                    else if(key.isWritable()){
                        Method method = new Method();
                        method.processResponse(key);
                    }
                    else
                        key.cancel();
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}