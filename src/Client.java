import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Client {

    private static @NotNull String getMsg( @NotNull BufferedReader reader) throws IOException {
        StringBuilder ret = new StringBuilder();
        char[] chars = new char[Utils.SIZE];
        do{
            reader.read(chars);
            ret.append(chars);
            Arrays.fill(chars, '\0');
        } while (reader.ready());
        return ret.toString();
    }

    public static void main( String[] args ) {
        try (Socket socket = new Socket("localhost" ,8080)) {
            RequestHeader header = new RequestHeader();
            header.setMethod("GET");
            header.setUrl("http://localhost:8081/index.html");
            header.setVersion("HTTP/1.1");
            header.setContent_type("text/html");
            header.setHost("localhost:8081");
            socket.getOutputStream().write(header.toString().getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg = getMsg(br);
            FileOutputStream os = new FileOutputStream("test.txt");
            os.write(msg.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
