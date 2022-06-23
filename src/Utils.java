import com.google.gson.Gson;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
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

    public static final String EXIT = "web/request//shutdown";
    public static final int SIZE = 1024;

    private static final Pattern HOST = Pattern.compile("Host.*");

    private static final Pattern CONTENT_LENGTH = Pattern.compile("Content-Length.*");

    private static final Pattern CONTENT_TYPE = Pattern.compile("Content-Type.*");

    public static String mdToHtml(String md){
        Parser parser = Parser.builder().build();
        Node document = parser.parse(md);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(document);
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

        int split = 0;
        for(int i = 0; i < parts.length; i++){
            if(parts[i].equals("")){
                split += 2;
                break;
            }

            split += (2 + parts[i].length());
            int idx = parts[i].indexOf(":");
            if(idx == -1)
                continue;
            if(Utils.HOST.matcher(parts[i]).matches())
                requestHeader.setHost(parts[i].substring(idx + 2));

            else if(Utils.CONTENT_LENGTH.matcher(parts[i]).matches())
                requestHeader.setContent_length(Integer.parseInt(parts[i].substring(idx + 2)));

            else if(Utils.CONTENT_TYPE.matcher(parts[i]).matches())
                requestHeader.setContent_type(parts[i].substring(idx + 2));

            String K = parts[i].substring(0 ,idx);
            String V = "";
            if (idx + 1 < parts[i].length())
                V = parts[i].substring(idx + 1);
            requestHeader.putHeadMap(K ,V);

        }

        requestHeader.setData(temp.substring(split));

        return requestHeader;
    }

    public static @NotNull ResponseHeader responseParseString( @NotNull String temp ) {
        assert temp.contains(Utils.CRLF);

        ResponseHeader responseHeader = new ResponseHeader();

        String firstLine = temp.substring(0, temp.indexOf(Utils.CRLF));
        String[] parts = firstLine.split(" ");

        assert parts.length == 3;

        responseHeader.setVersion(parts[0]);
        if(parts[1].equals("0"))
            responseHeader.setCode(500);
        else
            responseHeader.setCode(Integer.parseInt(parts[1])); //赋状态码

        int split = 0;
        parts = temp.split(Utils.CRLF);

        for(int i = 0;i < parts.length;i++){
            if(parts[i].equals("")){
                split += 2;
                break;
            }
            split += (2 + parts[i].length());
            int idx = parts[i].indexOf(":");
            if(idx == -1)
                continue;

            if(Utils.CONTENT_LENGTH.matcher(parts[i]).matches())
                responseHeader.setContent_length(Integer.parseInt(parts[i].substring(idx + 2)));

            else if(Utils.CONTENT_TYPE.matcher(parts[i]).matches())
                responseHeader.setContent_type(parts[i].substring(idx + 2));

            String K = parts[i].substring(0 ,idx);
            String V = "";
            if (idx + 1 < parts[i].length())
                V = parts[i].substring(idx + 1);
            responseHeader.putHeadMap(K ,V);
        }

        responseHeader.setData(temp.substring(split));

        return responseHeader;
    }

    public static void writeResponse( @NotNull ResponseHeader header,int code,int length,String url){
        header.setCode(code);
        header.setContent_length(length);
        header.setContent_type(Utils.queryFileType(url));
    }

    public static void writeResponse( @NotNull ResponseHeader header,int code){
        header.setCode(code);
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

    public static void NIOWriteFile( String filename , @NotNull String data, int length ) throws IOException {
        RandomAccessFile access = new RandomAccessFile(filename, "rw");

        FileChannel channel = access.getChannel();

        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[length];
        System.arraycopy(dataBytes ,0 ,content ,0 ,length);

        ByteBuffer byteBuffer = ByteBuffer.wrap(content);

        channel.write(byteBuffer);
    }

    public static String queryCode(int code){

        return CodeStatus.valueOf("STATUS_CODE_"+code).getStatus();
    }

    public static String queryFileType( @NotNull String url){
        String type = url.split("\\.")[1].toLowerCase();
        return FileType.valueOf(type).getType();
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

enum FileType{

    html("text/html"), txt("text/plain"), json("application/json"), md("text/markdown");

    private String type;

    FileType(String type){
        this.type = type;
    }

    public String getType(){
        return type;
    }

}