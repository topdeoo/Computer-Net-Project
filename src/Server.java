import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static final int PORT = 8081;
    private static final String INDEX_PAGE = "index.html";
    private static final String EXIT = "shutdown";
    private static final byte[] NOT_FOUND = "404 not found".getBytes(StandardCharsets.UTF_8);
    public static void main( String[] args ) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        AtomicInteger flag = new AtomicInteger(1);
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
                            String pageName = head[1].substring(1);
                            if(pageName.equals(INDEX_PAGE)) {
                                OutputStream outputStream = socket.getOutputStream();
                                outputStream.write("HTTP/1.1 200 OK\r\n".getBytes(StandardCharsets.UTF_8));
                                outputStream.write("Server: Java/jdk11.0\r\n".getBytes(StandardCharsets.UTF_8));
                                outputStream.write("Content-Type: text/html\r\n".getBytes(StandardCharsets.UTF_8));
                                outputStream.write("Transfer-Encoding: UTF-8\r\n".getBytes(StandardCharsets.UTF_8));
                                outputStream.write(("Date:"+ new Date()+"\r\n\r\n").getBytes(StandardCharsets.UTF_8));

                                FileInputStream fileInputStream = new FileInputStream("webpage/" + pageName);
                                byte[] data = new byte[1024];
                                int eof = 0;
                                while ((eof = fileInputStream.read(data)) != -1)
                                    outputStream.write(data, 0, eof);
                                fileInputStream.close();
                                socket.close();
                            }
                            else if(pageName.equals(EXIT)){
                                flag.set(0);
                                socket.close();
                            }
                            else {
                                OutputStream outputStream = socket.getOutputStream();
                                outputStream.write("HTTP/1.1 404 NOT Found\r\n".getBytes(StandardCharsets.UTF_8));
                                outputStream.write("Server: Java/jdk11.0\r\n".getBytes(StandardCharsets.UTF_8));
                                outputStream.write("Content-Type: text/html\r\n".getBytes(StandardCharsets.UTF_8));
                                outputStream.write("Transfer-Encoding: UTF-8\r\n".getBytes(StandardCharsets.UTF_8));
                                outputStream.write(("Date:"+ new Date()+"\r\n\r\n").getBytes(StandardCharsets.UTF_8));

                                outputStream.write(NOT_FOUND, 0, NOT_FOUND.length);
                                socket.close();
                            }
                            break;
                        case "HEAD":
                            OutputStream outputStream = socket.getOutputStream();
                            if(head[1].substring(1).equals(INDEX_PAGE))
                                outputStream.write("HTTP/1.1 200 OK\r\n".getBytes(StandardCharsets.UTF_8));
                            else
                                outputStream.write("HTTP/1.1 404 Not Found\r\n".getBytes(StandardCharsets.UTF_8));
                            outputStream.write("Server: Java/jdk11.0\r\n".getBytes(StandardCharsets.UTF_8));
                            outputStream.write("Content-Type: text/html\r\n".getBytes(StandardCharsets.UTF_8));
                            outputStream.write("Transfer-Encoding: UTF-8\r\n".getBytes(StandardCharsets.UTF_8));
                            outputStream.write(("Date:"+ new Date()+"\r\n\r\n").getBytes(StandardCharsets.UTF_8));

                            break;
                        case "POST":

                            break;
                        case "PUT":
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
                            break;
                    }

                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
