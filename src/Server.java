import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

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

                        default:
                            break;
                    }

                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private static void response( @NotNull OutputStream outputStream, int status, String type, String status_code)
            throws IOException {

        outputStream.write(("HTTP/1.1 "+status+" "+status_code+"\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write("Server: Java/jdk11.0\r\n".getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Type: "+type+"\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Date:"+ new Date()+"\r\n").getBytes(StandardCharsets.UTF_8));

    }

    private static void getMethod( Socket socket,@NotNull String url) throws IOException {


        Pattern urlPattern = Pattern.compile("(^[a-zA-Z0-9]*)\\.([a-zA-Z]*)");
        if(!urlPattern.matcher(url).matches())
            url = url.split("/")[3];

        if(url.equals(INDEX_PAGE)) {
            OutputStream outputStream = socket.getOutputStream();

            response(outputStream, 200, CONTENT_TYPE_HTML, STATUS_CODE_200);

            FileInputStream fileInputStream = new FileInputStream("webpage/" + url);

            outputStream.write(("Content-Length: " + fileInputStream.available() + "\r\n\r\n").
                    getBytes(StandardCharsets.UTF_8));

            byte[] data = new byte[1024];
            int eof = 0;
            while ((eof = fileInputStream.read(data)) != -1)
                outputStream.write(data, 0, eof);

            socket.close();
            fileInputStream.close();
        }
        else if(url.equals(EXIT)){
            flag.set(0);
            socket.close();
        }
        else {
            OutputStream outputStream = socket.getOutputStream();

            response(outputStream, 404, CONTENT_TYPE_HTML, STATUS_CODE_404);

            socket.close();
        }
    }

    private static void headMethod( @NotNull Socket socket,@NotNull String url) throws IOException {

        OutputStream outputStream = socket.getOutputStream();
        if(url.equals(INDEX_PAGE))
            response(outputStream, 200, CONTENT_TYPE_HTML, STATUS_CODE_200);
        else
            response(outputStream, 404, CONTENT_TYPE_TEXT, STATUS_CODE_404);

    }

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
            response(socket.getOutputStream(), 500, CONTENT_TYPE_FORM, STATUS_CODE_500);
            return;
        }

        String total_data = new String(buff);

        String[] t = type_charset.split(";");
        String type = t[0];         //ignore charset
        switch (type){
            case CONTENT_TYPE_FORM:

                String[] data = total_data.split("&");
                HashMap<String, String> hashMap = new HashMap<>();
                for (String data_i : data) {
                    String[] key_value = data_i.split("=");
                    hashMap.put(key_value[0] ,key_value[1]);
                }
                // 输出成功代表POST成功
                for(String key: hashMap.keySet())
                    System.out.println(key+"=>"+hashMap.get(key));

                response(socket.getOutputStream(), 200, CONTENT_TYPE_FORM, STATUS_CODE_200);

                break;
            case CONTENT_TYPE_FILE:
                // 可以提交文件，但是这个你也没法查看文件啊。。。
                System.out.println("POST SUCCESS");

                response(socket.getOutputStream(), 200, CONTENT_TYPE_FILE, STATUS_CODE_200);

                break;
            case CONTENT_TYPE_JSON:

                JsonObject jsonObject = JsonParser.parseString(total_data).getAsJsonObject();
                System.out.println(jsonObject);

                response(socket.getOutputStream(), 200, CONTENT_TYPE_JSON, STATUS_CODE_200);

                break;
            case CONTENT_TYPE_TEXT:

                System.out.println(total_data);
                response(socket.getOutputStream(), 200, CONTENT_TYPE_TEXT, STATUS_CODE_200);

                break;
            default:
                response(socket.getOutputStream(), 501, CONTENT_TYPE_TEXT, STATUS_CODE_501);
                break;
        }

        socket.close();
    }

}
