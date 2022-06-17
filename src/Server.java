import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int PORT = 8081;


    public static void main( String[] args ) throws IOException {
        ExecutorService ThreadPool = Executors.newFixedThreadPool(200);
        ServerSocket serverSocket = new ServerSocket(PORT);
        while (true){
            Socket socket = serverSocket.accept();
            System.out.println("Success");
            ThreadPool.execute(new Handler(socket));
        }
    }

}

class Handler implements Runnable{

    private final Socket socket;

    private static RequestHeader requestHeader;

    private static ResponseHeader responseHeader;

    Handler(Socket socket){
        this.socket = socket;
    }

    private static void handle501( Socket socket ){
        try{
            handleError(socket, 501);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    static void handle500( Socket socket ) {
        try{
            handleError(socket, 500);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    static void handle404( Socket socket ) {
        try{
            handleError(socket, 404);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void handleError( @NotNull Socket socket ,int code ) throws IOException {

        String filename = "web/error/" + code + ".html";
        byte[] responseBody = Utils.NIOReadFile(filename);
        Utils.writeResponse(responseHeader, code, responseBody.length, filename);
        socket.getOutputStream().write(responseHeader.toString().getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().write(responseBody);

    }

    private static void handle200( Socket socket ) throws IOException {

        String method = requestHeader.getMethod();

        switch (method){
            case "GET":
                String url = requestHeader.getUrl();

                if(url.equals(Utils.EXIT))
                    System.exit(-1);

                byte[] responseBody = Utils.NIOReadFile(url);

                Utils.writeResponse(responseHeader, 200, responseBody.length, url);
                socket.getOutputStream().write(responseHeader.toString().getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().write(responseBody);
                break;
            case "HEAD":
                Utils.writeResponse(responseHeader, 200);
                socket.getOutputStream().write(responseHeader.toString().getBytes(StandardCharsets.UTF_8));
                break;
            case "POST":
                Utils.writeResponse(responseHeader, 200);
                responseHeader.setContent_type("");
                responseHeader.setContent_length(0);
                String data = requestHeader.getData();
                Utils.NIOWriteFile("db/data.txt", data);
                socket.getOutputStream().write(responseHeader.toString().getBytes(StandardCharsets.UTF_8));
                break;
            default:
                handle501(socket);
        }

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

    @Override
    public void run() {
        try{

            InputStream is = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String temp = getMsg(reader);

            requestHeader = Utils.requestParseString(temp);
            responseHeader = new ResponseHeader(requestHeader);

            try {
                Handler.handle200(socket);
            }
            catch (FileNotFoundException e){
                Handler.handle404(socket);
            }
            catch (Exception e){
                Handler.handle500(socket);
            }
            finally {
                socket.close();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
