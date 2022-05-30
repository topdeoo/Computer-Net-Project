import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    private final Pattern HOST = Pattern.compile("Host.*");

    private final Pattern CONTENT_LENGTH = Pattern.compile("Content-Length.*");

    private String Host;

    private int content_length;

    Handler(Socket socket){
        this.clientSocket = socket;
    }

    @Override
    public void run(){

        try {

            InputStream isClient = clientSocket.getInputStream();
            BufferedReader brClient = new BufferedReader(new InputStreamReader(isClient));

            ArrayList<String> contents = new ArrayList<>();

            String temp;
            while(!(temp = brClient.readLine()).equals("")){

                contents.add(temp);
                if(HOST.matcher(temp).matches())
                    Host = temp.split(":")[1].substring(1);
                else if(CONTENT_LENGTH.matcher(temp).matches())
                    content_length = Integer.parseInt(temp.split(":")[1].substring(1));

            }

            System.out.println(contents.get(0));

            Socket toServer = new Socket(Host, 8081);
            OutputStream osServer = toServer.getOutputStream();

            for(String s: contents)
                osServer.write((s+"\r\n").getBytes(StandardCharsets.UTF_8));
            osServer.write("\r\n".getBytes(StandardCharsets.UTF_8));
            if(content_length > 0){
                char[] post = new char[content_length];
                assert brClient.read(post, 0, content_length) > 0;
                osServer.write(new String(post).getBytes(StandardCharsets.UTF_8));
            }

            contents.clear();

            InputStream isServer = toServer.getInputStream();
            BufferedReader brServer = new BufferedReader(new InputStreamReader(isServer));

            while (!(temp = brServer.readLine()).equals("")) {
                contents.add(temp);
                if(CONTENT_LENGTH.matcher(temp).matches())
                    content_length = Integer.parseInt(temp.split(":")[1].substring(1));
            }

            OutputStream osClient = clientSocket.getOutputStream();
            for(String s: contents)
                osClient.write((s+"\r\n").getBytes(StandardCharsets.UTF_8));
            osClient.write("\r\n".getBytes(StandardCharsets.UTF_8));

            if(content_length > 0){
                byte[] post = new byte[content_length];
                if(isServer.read(post, 0, content_length) >= 0)
                    osClient.write(post);

            }

            clientSocket.close();
            toServer.close();

        }
        catch (IOException e) {
            e.printStackTrace();
        }


    }
}



