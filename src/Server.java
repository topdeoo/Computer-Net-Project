import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {


    private static final int PORT = 8081;

    private static final String INDEX_PAGE = "index.html";

    private static final String EXIT = "shutdown";

    private static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    private static final String CONTENT_TYPE_FILE = "multipart/form-data";

    private static final String CONTENT_TYPE_JSON = "application/json";

    private static final String CONTENT_TYPE_HTML = "text/html";

    private static final String CONTENT_TYPE_TEXT = "text/plain";

    private static final String STATUS_CODE_500 = "Internal Server Error";

    private static final String STATUS_CODE_200 = "OK";

    private static final String STATUS_CODE_404 = "Not Found";

    private static final String STATUS_CODE_501 = "Not Implemented";

    static AtomicInteger flag = new AtomicInteger(1);


    public static void main( String[] args ) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        while (flag.get() == 1){
            Socket socket = serverSocket.accept();
            System.out.println("Success");
            new Thread(()->{
                try{

                    InputStream is = socket.getInputStream();
                    String temp = new String(is.readAllBytes());

                    RequestHeader requestHeader = Utils.requestParseString(temp);

                    try {
                        handle200(socket, requestHeader);
                    }
                    catch (FileNotFoundException e){
                        handle404(socket);
                    }
                    catch (Exception e){
                        handle500(socket);
                    }
                    finally {
                        socket.close();
                    }
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private static void handle501( Socket socket ){
        try{
            handleError(socket, 501);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void handle500( Socket socket ) {
        try{
            handleError(socket, 500);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void handle404(Socket socket) {
        try{
            handleError(socket, 404);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void handleError( @NotNull Socket socket ,int code ) throws IOException {

        String filename = "web/error/" + code + ".html";
        ResponseHeader responseHeader = new ResponseHeader(code);
        byte[] responseBody = Utils.NIOReadFile(filename);

        socket.getOutputStream().write(responseHeader.toString().getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().write(responseBody);

    }

    private static void handle200( Socket socket ,@NotNull RequestHeader requestHeader ) throws IOException {

        String method = requestHeader.getMethod();
        ResponseHeader responseHeader;

        switch (method){
            case "GET":
                String url = requestHeader.getUrl();

                if(url.equals(Utils.EXIT))
                    Server.flag.set(0);

                byte[] responseBody = Utils.NIOReadFile(url);

                responseHeader = new ResponseHeader(requestHeader);
                socket.getOutputStream().write(responseHeader.toString().getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().write(responseBody);
                break;
            case "HEAD":
                responseHeader = new ResponseHeader(requestHeader);
                socket.getOutputStream().write(responseHeader.toString().getBytes(StandardCharsets.UTF_8));
                break;
            case "POST":
                responseHeader = new ResponseHeader(requestHeader);
                String data = requestHeader.getData();
                Utils.NIOWriteFile("db/data.txt", data);
                socket.getOutputStream().write(responseHeader.toString().getBytes(StandardCharsets.UTF_8));
                break;
            default:
                handle501(socket);
        }

    }



}
