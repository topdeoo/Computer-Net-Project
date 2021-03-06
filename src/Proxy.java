import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Proxy {

    private static final int PORT = 8080;

    public static void main( String[] args ) {

        ExecutorService threadPool = Executors.newScheduledThreadPool(200);

        try(ServerSocketChannel server = ServerSocketChannel.open()){

            server.bind(new InetSocketAddress(PORT));
            server.configureBlocking(false);
            Selector master = Selector.open();
            server.register(master, SelectionKey.OP_ACCEPT);
            while (Server.flag.get() == 1) {
                master.select();
                Iterator<SelectionKey> iterator = master.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        SocketChannel client = server.accept();
                        threadPool.execute(new ProxyHandler(client));
                    } else
                        key.cancel();
                }
            }

        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}

class ProxyHandler implements Runnable{

    private final SocketChannel client;

    String host;

    int port = 80;

    RequestHeader requestHeader;

    ResponseHeader responseHeader;

    ProxyHandler(SocketChannel client){

        this.client = client;
    }

    @Override
    public void run() {

        try {

            client.configureBlocking(false);
            Selector clientSelector = Selector.open();
            client.register(clientSelector, SelectionKey.OP_READ);
            int over = 1; //????????????????????????????????????
            while (over == 1) {
                clientSelector.select();
                Iterator<SelectionKey> clientIt = clientSelector.selectedKeys().iterator();
                while (clientIt.hasNext()) {
                    SelectionKey clientKey = clientIt.next();
                    clientIt.remove();

                    if (clientKey.isReadable()) { //?????????????????????????????????

                        SocketChannel clientChannel = (SocketChannel) clientKey.channel(); //?????????????????????????????????
                        ByteBuffer content = ByteBuffer.allocate(Utils.SIZE);
                        clientChannel.read(content);
                        content.flip();
                        requestHeader = Utils.requestParseByteBuffer(content); //??????????????????

                        host = requestHeader.getHost(); //??????????????????????????????url
                        int idx = host.indexOf(":");
                        if (idx != -1) {
                            port = Integer.parseInt(host.substring(idx + 1));
                            host = host.substring(0 ,idx);
                            String[] parts = requestHeader.getUrl().split("/");
                            requestHeader.setUrl(parts[ parts.length - 1 ]);
                        }

                        SocketChannel server = SocketChannel.open(); //????????????????????????
                        server.connect(new InetSocketAddress(host, port));
                        server.configureBlocking(false);

                        Selector serverSelector = Selector.open();
                        server.register(serverSelector , SelectionKey.OP_WRITE); //??????selector???????????????????????????
                        int flag = 1; //????????????????????????????????????
                        while (flag == 1) {

                            serverSelector.select();
                            Iterator<SelectionKey> serverIt = serverSelector.selectedKeys().iterator();

                            while (serverIt.hasNext()) {

                                SelectionKey serverKey = serverIt.next();
                                serverIt.remove();
                                if (serverKey.isWritable()) { //???????????????????????????????????????
                                    SocketChannel serverChannel = (SocketChannel) serverKey.channel();
                                    ByteBuffer trans = ByteBuffer.wrap(
                                            requestHeader.trans().getBytes(StandardCharsets.UTF_8)
                                    );
                                    serverChannel.write(trans);
                                    serverKey.interestOps(SelectionKey.OP_READ); //????????????
                                }
                                else if (serverKey.isReadable()) { //?????????????????????????????????
                                    SocketChannel serverChannel = (SocketChannel) serverKey.channel();
                                    ByteBuffer response = ByteBuffer.allocate(Utils.SIZE);
                                    serverChannel.read(response);
                                    response.flip();
                                    responseHeader = Utils.responseParseByteBuffer(response);
                                    serverChannel.close();
                                    flag = 0;
                                }

                            }
                        }

                        clientKey.interestOps(SelectionKey.OP_WRITE); //?????????????????????????????????????????????
                    } else if (clientKey.isWritable()) {
                        SocketChannel clientChannel = (SocketChannel) clientKey.channel();
                        ByteBuffer response = ByteBuffer.wrap(responseHeader.trans().getBytes(StandardCharsets.UTF_8));
                        clientChannel.write(response);
                        over = 0;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
