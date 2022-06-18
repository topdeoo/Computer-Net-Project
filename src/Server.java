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
        ExecutorService ThreadPool = Executors.newFixedThreadPool(200); //开辟一个固定大小的线程池
        ServerSocket serverSocket = new ServerSocket(PORT);
        while (true){
            Socket socket = serverSocket.accept(); //监听固定端口是否有客户端连接
            System.out.println("Success"); //检测到连接则print成功
            ThreadPool.execute(new Handler(socket)); //使用线程池调用一个线程处理连接
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

    private static void handle500( Socket socket ) {
        try{
            handleError(socket, 500);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void handle404( Socket socket ) {
        try{
            handleError(socket, 404);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void handleError( @NotNull Socket socket ,int code ) throws IOException {

        String filename = "web/error/" + code + ".html"; //确定响应页面
        byte[] responseBody = Utils.NIOReadFile(filename); //获取响应报文数据
        Utils.writeResponse(responseHeader, code, responseBody.length, filename); //生成响应报文头
        socket.getOutputStream().write(responseHeader.toString().getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().write(responseBody); //回复响应报文头及数据

    }

    private static void handle200( Socket socket ) throws IOException {

        String method = requestHeader.getMethod(); //获取请求方法

        switch (method){
            case "GET":
                String url = requestHeader.getUrl(); //获取请求的url

                if(url.equals(Utils.EXIT)) //if shutdown
                    System.exit(-1); //程序退出，关闭服务器

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
                String data = requestHeader.getData(); //获取post的内容
                Utils.NIOWriteFile("db/data.txt", data, requestHeader.getContent_length()); //将data写入数据库db（伪）
                socket.getOutputStream().write(responseHeader.toString().getBytes(StandardCharsets.UTF_8));
                break;
            case "PUT":
                Utils.writeResponse(responseHeader, 200);
                responseHeader.setContent_type(Utils.queryFileType(".html"));
                responseBody = Utils.mdToHtml(responseHeader.getData()).getBytes(StandardCharsets.UTF_8);
                //实现将md文件转换成html（读取请求报文体内容，转换成html并转换成字节数组）
                requestHeader.setContent_length(responseBody.length); //获取字节数组长度
                socket.getOutputStream().write(responseHeader.toString().getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().write(responseBody);
                break;
            default:
                handle501(socket); //未实现
        }

    }

    private @NotNull String getMsg( @NotNull BufferedReader br) throws IOException {
        StringBuilder ret = new StringBuilder(); //创建一个可变的字符序列
        char[] chars = new char[Utils.SIZE];
        do{
            br.read(chars);
            ret.append(chars);
            Arrays.fill(chars, '\0'); //clear chars
        } while (br.ready());
        return ret.toString();
    }

    @Override
    public void run() {
        try{

            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String temp = getMsg(br); //将报文转为字符串

            requestHeader = Utils.requestParseString(temp); //解析报文
            responseHeader = new ResponseHeader(requestHeader); //创建报文头

            try {
                Handler.handle200(socket); //请求成功
            }
            catch (FileNotFoundException e){
                Handler.handle404(socket); //not found
            }
            catch (Exception e){
                Handler.handle500(socket); //服务器错误
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
