import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * 题目一的服务器类
 * <p>
 * 实现的功能如下：<br/>
 * 1. 简单的GET功能{@link #getMethod}<br/>
 * 2. 简单的POST功能{@link #postMethod}<br/>
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
    private static Hashtable<String, String> sqlData = new Hashtable<>();

    /* 服务器的运行状态 */
    static AtomicInteger flag = new AtomicInteger(1);


    public static void main( String[] args ) throws IOException {

        ServerSocket serverSocket = new ServerSocket(PORT);

        while (flag.get() == 1){

            Socket socket = serverSocket.accept();
            System.out.println("Success");

            new Thread(()->{
                try{
                    InputStream inputStream = socket.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(inputStream)
                    );
                    String[] head = bufferedReader.readLine().split(" ");

                    switch (head[0]){
                        case "GET":

                            getMethod(socket, head[1].substring(1));
                            break;

                        case "HEAD":

                            headMethod(socket, head[1].substring(1));
                            break;

                        case "POST":

                            postMethod(socket,bufferedReader);
                            break;

                        case "PUT":

                            putMethod(socket, bufferedReader);
                            break;

                        case "DELETE":
                            break;
                        case "CONNECT":
                            break;
                        case "OPTIONS":
                            break;
                        case "TRACE":
                            break;
                        default:
                            response(socket.getOutputStream(), 501,
                                    Utils.CONTENT_TYPE_TEXT, Utils.STATUS_CODE_501);
                            break;
                    }

                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }).start();
        }
    }


    /**
     * 对HTTP的请求报做出回复，写回复报给请求对象
     *
     * @param outputStream socket的输出流{@link java.io.OutputStream}
     * @param status 回复报的状态码
     * @param type 回复报的文档类型
     * @param status_code 状态码所对应的说明
     */

    private static void response( @NotNull OutputStream outputStream, int status, String type, String status_code)
            throws IOException {

        outputStream.write(("HTTP/1.1 "+status+" "+status_code+"\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write("Server: Java/jdk11.0\r\n".getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Type: "+type+"\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write("Transfer-Encoding: UTF-8\r\n".getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Date:"+ new Date()+"\r\n\r\n").getBytes(StandardCharsets.UTF_8));

    }

    /**
     * HTTP中的GET方法，实现的功能有：<br/>
     * 1. 浏览器中输入在浏览器中输入localhost:8081/index.html能显示自己的学号信息;<br/>
     * 2. 在浏览器中输入localhost:8081下其他无效路径，浏览器显示404 not found;<br/>
     * 3. 在浏览器中输入localhost:8081/shutdown能使服务器关闭;<br/>
     *
     * @param socket 当前连接的socket{@link java.net.Socket}
     * @param url 请求网页的URL
     */

    private static void getMethod( Socket socket ,@NotNull String url ) throws IOException {

        if(url.equals(Utils.INDEX_PAGE)) {
            OutputStream outputStream = socket.getOutputStream();

            response(outputStream, 200, Utils.CONTENT_TYPE_HTML, Utils.STATUS_CODE_200);

            FileInputStream fileInputStream = new FileInputStream("webpage/" + url);
            byte[] data = new byte[1024];
            int eof;
            while ((eof = fileInputStream.read(data)) != -1)
                outputStream.write(data, 0, eof);
            fileInputStream.close();
            socket.close();
        }
        else if(url.equals(Utils.EXIT)){
            flag.set(0);
            socket.close();
        }
        else {
            OutputStream outputStream = socket.getOutputStream();

            response(outputStream, 404, Utils.CONTENT_TYPE_HTML, Utils.STATUS_CODE_404);

            /* outputStream.write(NOT_FOUND, 0, NOT_FOUND.length);*/
            socket.close();
        }
    }

    /**
     * HTTP中的HEAD方法，与GET方法类似，但只返回回复报文头，不返回具体内容
     *
     * @param socket 当前连接的socket{@link java.net.Socket}
     * @param url 请求网页的URL
     */

    private static void headMethod( @NotNull Socket socket ,@NotNull String url ) throws IOException {

        OutputStream outputStream = socket.getOutputStream();
        if(url.equals(Utils.INDEX_PAGE))
            response(outputStream, 200, Utils.CONTENT_TYPE_HTML, Utils.STATUS_CODE_200);
        else
            response(outputStream, 404, Utils.CONTENT_TYPE_TEXT, Utils.STATUS_CODE_404);

    }

    /**
     * HTTP中的POST方法，实现的功能有：<br/>
     * 1. HTML原生表单的数据提交<br/>
     * 2. JSON的数据提交<br/>
     * 3. TEXT的数据提交<br/>
     * 由于没有数据库，使用POST方法提交的数据没有用处，只能打印出来，表示POST方法是可行的
     *
     * @param socket 当前连接的socket{@link java.net.Socket}
     * @param bufferedReader 用于读取当前缓冲区中的内容{@link java.io.BufferedReader}
     */

    private static void postMethod( Socket socket ,@NotNull BufferedReader bufferedReader )
            throws IOException {

        int isSuccess = 1;
        String type_charset = null;
        int length = 0;
        String temp;

        Pattern patternType = Pattern.compile("Content-Type.*");
        Pattern patternLength = Pattern.compile("Content-Length.*");
        while (!(temp = bufferedReader.readLine()).equals("")) {
            if(patternType.matcher(temp).matches()){
                type_charset = temp.split(":")[1].substring(1);
            }
            else if(patternLength.matcher(temp).matches()){
                length = Integer.parseInt(temp.split(":")[1].substring(1));
            }
        }

        assert type_charset != null;


        char[] buff = new char[length];

        if(bufferedReader.read(buff, 0, length) < 0)
            isSuccess = 0;

        if(isSuccess == 0){
            response(socket.getOutputStream(), 500, Utils.CONTENT_TYPE_FORM, Utils.STATUS_CODE_500);
            return;
        }

        String total_data = new String(buff);

        String[] t = type_charset.split(";");
        String type = t[0];         //ignore charset
        switch (type){
            case Utils.CONTENT_TYPE_FORM:

                String[] data = total_data.split("&");

                for (String data_i : data) {
                    String[] key_value = data_i.split("=");
                    if (sqlData.containsKey(key_value[0]))
                        sqlData.replace(key_value[0], key_value[1]);
                    else
                        sqlData.put(key_value[0], key_value[1]);
                }

                OutputStream outputStream = socket.getOutputStream();
                response(outputStream, 200, Utils.CONTENT_TYPE_FORM, Utils.STATUS_CODE_200);


                break;
            case Utils.CONTENT_TYPE_FILE:
                // 可以提交文件，但是这个你也没法查看文件啊。。。
                System.out.println("POST SUCCESS");

                response(socket.getOutputStream(), 200, Utils.CONTENT_TYPE_FILE, Utils.STATUS_CODE_200);

                break;
            case Utils.CONTENT_TYPE_JSON:

                JsonObject jsonObject = JsonParser.parseString(total_data).getAsJsonObject();
                System.out.println(jsonObject);

                response(socket.getOutputStream(), 200, Utils.CONTENT_TYPE_JSON, Utils.STATUS_CODE_200);

                break;
            case Utils.CONTENT_TYPE_TEXT:

                System.out.println(total_data);
                response(socket.getOutputStream(), 200, Utils.CONTENT_TYPE_TEXT, Utils.STATUS_CODE_200);

                break;
            default:
                response(socket.getOutputStream(), 501, Utils.CONTENT_TYPE_TEXT, Utils.STATUS_CODE_501);
                break;
        }

        socket.close();
    }


    /**
     * HTTP中的PUT方法，形式上与POST类似，但是PUT方法不会产生副作用<br/>
     * 这里实现的PUT方法只能够传输Markdown文件，将其转化为HTML文档并返回给客户端<br/>
     * 可以当做实现了一个文件转化(Markdown2Html)
     *
     * @param socket 当前连接的socket{@link java.net.Socket}
     * @param bufferedReader 用于读取当前缓冲区中的内容{@link java.io.BufferedReader}
     */
    private static void putMethod( Socket socket ,@NotNull BufferedReader bufferedReader )
            throws IOException {

        int length = 0;
        int isSuccess = 1;
        String temp;
        Pattern patternLength = Pattern.compile("Content-Length.*");
        while (!(temp = bufferedReader.readLine()).equals("")) {
            if(patternLength.matcher(temp).matches()){
                length = Integer.parseInt(temp.split(":")[1].substring(1));
            }
        }

        char[] buff = new char[length];
        if(bufferedReader.read(buff, 0, length) < 0)
            isSuccess = 0;

        if(isSuccess == 0){
            response(socket.getOutputStream(), 500, Utils.CONTENT_TYPE_FORM, Utils.STATUS_CODE_500);
            return;
        }

        OutputStream outputStream = socket.getOutputStream();
        String HtmlContent =  Utils.mdToHtml(new String(buff));
        response(outputStream, 200, Utils.CONTENT_TYPE_HTML, Utils.STATUS_CODE_200);
        outputStream.write(HtmlContent.getBytes(StandardCharsets.UTF_8));

        socket.close();
    }

}
