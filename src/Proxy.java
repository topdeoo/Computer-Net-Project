import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class Proxy { //类服务器

    private static final int PORT = 8080;

    public static void main( String[] args ) throws IOException {

        ServerSocket serverSocket = new ServerSocket(PORT);

        ExecutorService HandlerPool = Executors.newFixedThreadPool(100);

        while (true){

            try {
                Socket socket = serverSocket.accept();

                HandlerPool.execute(new ProxyHandler(socket));

            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

    }

}

class ProxyHandler implements Runnable{

    private final Socket client;

    private int port = 80; //http端口

    ProxyHandler( Socket socket){
        this.client = socket;
    }

    private @NotNull String getMsg( @NotNull BufferedReader reader) throws IOException {
        StringBuilder ret = new StringBuilder();
        do{
            char[] chars = new char[Utils.SIZE];
            reader.read(chars);
            ret.append(chars);
        } while (reader.ready());
        return ret.toString(); //读报文数据，同Server
    }

    @Contract(pure = true)
    private byte @NotNull [] getData( @NotNull String data,int length){ //去除报文内容中的null
        byte[] ret = new byte[length]; //开一个等同报文体长度的字节数组
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8); //将捕获的报文体内容重新转为字节格式
        System.arraycopy(bytes ,0 ,ret ,0 ,length); //将报文体内容复制到新开的数组中
        return ret;
    }

    @Override
    public void run(){

        try {

            String temp = getMsg(new BufferedReader(new InputStreamReader(client.getInputStream()))); //读取客户端请求报文
            RequestHeader requestHeader = Utils.requestParseString(temp); //解析报文
            String host = requestHeader.getHost(); //获取目的服务器
            int idx = host.indexOf(":");
            if(idx != -1) {
                port = Integer.parseInt(host.substring(idx + 1)); //截取(localhost:8081)目的端口号，若无则为80
                host = host.substring(0, idx);
                String[] parts = requestHeader.getUrl().split("/"); // http://localhost:8081/index.html
                requestHeader.setUrl(parts[parts.length - 1]); //截出所需url部分（即index.html）
            }
            Socket server = new Socket(host, port);
            server.getOutputStream().write(requestHeader.toString().getBytes(StandardCharsets.UTF_8));

            temp = getMsg(new BufferedReader(new InputStreamReader(server.getInputStream()))); //获取服务器响应报文
            ResponseHeader responseHeader = Utils.responseParseString(temp);
            OutputStream os = client.getOutputStream();
            os.write(responseHeader.toString().getBytes(StandardCharsets.UTF_8));

            byte[] responseBody = getData(responseHeader.getData(), responseHeader.getContent_length());
            os.write(responseBody);

            server.close();
            client.close();

        }
        catch (Exception e) {
            e.printStackTrace();
        }


    }
}



