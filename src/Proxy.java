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

public class Proxy {

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

    private int port = 80;

    ProxyHandler( Socket socket){
        this.client = socket;
    }

    private @NotNull String getMsg( @NotNull BufferedReader reader) throws IOException {
        StringBuilder ret = new StringBuilder();
        char[] chars = new char[Utils.SIZE];
        do{
            reader.read(chars);
            ret.append(chars);
            Arrays.fill(chars, '\0');
        } while (reader.ready());
        return ret.toString();
    }

    @Contract(pure = true)
    private byte @NotNull [] getData( @NotNull String data,int length){
        byte[] ret = new byte[length];
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(bytes ,0 ,ret ,0 ,length);
        return ret;
    }

    @Override
    public void run(){

        try {

            String temp = getMsg(new BufferedReader(new InputStreamReader(client.getInputStream())));
            RequestHeader requestHeader = Utils.requestParseString(temp);
            String host = requestHeader.getHost();
            int idx = host.indexOf(":");
            if(idx != -1) {
                port = Integer.parseInt(host.substring(idx + 1));
                host = host.substring(0, idx);
            }
            Socket server = new Socket(host, port);
            String[] parts = requestHeader.getUrl().split("/");
            requestHeader.setUrl("/" + parts[parts.length - 1]);
            server.getOutputStream().write(requestHeader.toString().getBytes(StandardCharsets.UTF_8));

            temp = getMsg(new BufferedReader(new InputStreamReader(server.getInputStream())));
            ResponseHeader responseHeader = Utils.responseParseString(temp);
            OutputStream os = client.getOutputStream();
            os.write(responseHeader.toString().getBytes(StandardCharsets.UTF_8));

            byte[] data = getData(responseHeader.getData(), responseHeader.getContent_length());
            os.write(data);

            server.close();
            client.close();

        }
        catch (Exception e) {
            e.printStackTrace();
        }


    }
}



