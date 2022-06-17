import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Proxy {

    private static final int PORT = 8080;

    public static void main( String[] args ) throws IOException {

        ServerSocket serverSocket = new ServerSocket(PORT);

        ExecutorService HandlerPool = Executors.newFixedThreadPool(100);

        while (true){

            try {
                Socket socket = serverSocket.accept();

                HandlerPool.execute(new Handler(socket));

            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

    }

}

class Handler implements Runnable{

    private final Socket clientSocket;

    Handler(Socket socket){
        this.clientSocket = socket;
    }

    @Override
    public void run(){

        try {

            String temp = new String(clientSocket.getInputStream().readAllBytes());
            RequestHeader requestHeader = Utils.requestParseString(temp);
            requestHeader.setPort(clientSocket.getPort());

            Socket server = new Socket(requestHeader.getHost(), requestHeader.getPort());
            server.getOutputStream().write(requestHeader.toString().getBytes(StandardCharsets.UTF_8));

            temp = new String(server.getInputStream().readAllBytes());
            ResponseHeader responseHeader = Utils.responseParseString(temp);

            clientSocket.getOutputStream().write(responseHeader.toString().getBytes(StandardCharsets.UTF_8));

            server.close();
            clientSocket.close();

        }
        catch (Exception e) {
            e.printStackTrace();
        }


    }
}



