import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Utils {


    public static final String CRLF = "\r\n";

    public static final String EXIT = "shutdown";

    private static final Pattern HOST = Pattern.compile("Host.*");

    private static final Pattern CONTENT_LENGTH = Pattern.compile("Content-Length.*");

    private static final Pattern CONTENT_TYPE = Pattern.compile("Content-Type.*");

    public static String beanToJSONString(Object bean){
        Gson gson = new Gson();
        return gson.toJson(bean);
    }

    public static @NotNull RequestHeader requestParseString( @NotNull String temp){

        assert temp.contains(CRLF);

        RequestHeader requestHeader = new RequestHeader();

        String firstLine = temp.substring(0, temp.indexOf(CRLF));
        String[] parts = firstLine.split(" ");

        assert parts.length == 3;

        requestHeader.setMethod(parts[0]);
        requestHeader.setUrl(parts[1]);
        requestHeader.setVersion(parts[2]);

        parts = temp.split(CRLF);

        int split = -1;
        for(int i = 0; i < parts.length; i++){
            if(parts[i].equals("")){
                split = i;
                break;
            }
            int idx = parts[i].indexOf(":");
            if(idx == -1)
                continue;
            if(Utils.HOST.matcher(parts[i]).matches())
                requestHeader.setHost(parts[i].substring(idx + 2));

            else if(Utils.CONTENT_LENGTH.matcher(parts[i]).matches())
                requestHeader.setContent_length(Integer.parseInt(parts[i].substring(idx + 2)));

            else if(Utils.CONTENT_TYPE.matcher(parts[i]).matches())
                requestHeader.setContent_type(parts[i].substring(idx + 2));
            else {
                String K = parts[i].substring(0 ,idx);
                String V = "";
                if (idx + 1 < parts[i].length())
                    V = parts[i].substring(idx + 1);
                requestHeader.putHeadMap(K ,V);
            }
        }
        if(split > -1)
            for(int i = split; i < parts.length; i++){
                requestHeader.setData(parts[i]);
            }

        return requestHeader;
    }

    public static @NotNull ResponseHeader responseParseString( @NotNull String temp){
        ResponseHeader responseHeader = new ResponseHeader();

        return responseHeader;
    }

    public static byte @NotNull [] NIOReadFile( String url ) throws IOException {
        RandomAccessFile access = new RandomAccessFile(url, "r");
        FileChannel channel = access.getChannel();
        ByteBuffer temp = ByteBuffer.allocate((int) access.length());
        channel.read(temp);
        temp.flip();
        byte[] ret = new byte[temp.remaining()];
        temp.get(ret, 0, ret.length);
        return ret;
    }

    public static void NIOWriteFile( String filename ,String data ) throws IOException {
        RandomAccessFile access = new RandomAccessFile(filename, "rw");

        FileChannel channel = access.getChannel();
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(data);

        channel.write(byteBuffer);
    }

    public static String queryCode(int code){
        return CodeStatus.valueOf("STATUS_CODE_"+code).getStatus();
    }

}

enum CodeStatus{

    STATUS_CODE_200("OK", 200), STATUS_CODE_404("Not Found", 404),
    STATUS_CODE_501("Not Implemented", 501),STATUS_CODE_500 ("Internal Server Error",500);

    private int code;
    private String status;

    private CodeStatus(String status, int code){
        this.status = status;
        this.code = code;
    }

    public String getStatus(){
        return status;
    }
}
