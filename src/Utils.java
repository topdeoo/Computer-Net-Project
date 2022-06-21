import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;

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

    private static final Pattern HOST = Pattern.compile("Host.*"); //匹配关心内容的正则

    private static final Pattern CONTENT_LENGTH = Pattern.compile("Content-Length.*");

    private static final Pattern CONTENT_TYPE = Pattern.compile("Content-Type.*");

    public static String mdToHtml(String md){ //调包
        Parser parser = Parser.builder().build();
        Node document = parser.parse(md);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(document);
    }

    public static @NotNull RequestHeader requestParseString( @NotNull String temp){

        assert temp.contains(CRLF);

        RequestHeader requestHeader = new RequestHeader();

        String firstLine = temp.substring(0, temp.indexOf(CRLF)); //提取出请求行
        String[] parts = firstLine.split(" ");

        assert parts.length == 3;

        requestHeader.setMethod(parts[0]);
        requestHeader.setUrl(parts[1]);
        requestHeader.setVersion(parts[2]); //分隔出请求方法，请求url，版本号

        parts = temp.split(CRLF); //分隔请求报文的每一行

        int split = 0; //为标志报文体开始位置
        for(int i = 0; i < parts.length; i++){ //分隔每一请求报文首部行
            if(parts[i].equals("")){ //为空则读至报文头和报文体的分界
                split += 2; //跳过\r\n
                break;
            }
            split += (2 + parts[i].length()); //增加\r\n与首部行长度
            int idx = parts[i].indexOf(":");
            if(idx == -1)
                continue; //去除第一行
            if(Utils.HOST.matcher(parts[i]).matches())
                requestHeader.setHost(parts[i].substring(idx + 2)); //匹配首部名为host的值

            else if(Utils.CONTENT_LENGTH.matcher(parts[i]).matches())
                requestHeader.setContent_length(Integer.parseInt(parts[i].substring(idx + 2)));

            else if(Utils.CONTENT_TYPE.matcher(parts[i]).matches())
                requestHeader.setContent_type(parts[i].substring(idx + 2));
            else {
                String K = parts[i].substring(0 ,idx); //key 为首部名
                String V = "";
                if (idx + 1 < parts[i].length())
                    V = parts[i].substring(idx + 1); //value为首部值
                requestHeader.putHeadMap(K ,V);
            }
        }
        requestHeader.setData(temp.substring(split)); //读取请求报文体内容

        return requestHeader;
    }

    public static void writeResponse( @NotNull ResponseHeader header,int code,int length,String url){
        header.setCode(code); //设置状态码及含义
        header.setContent_length(length); //设置长度
        header.setContent_type(Utils.queryFileType(url)); //查询并设置类型
    }

    public static void writeResponse( @NotNull ResponseHeader header,int code){

        header.setCode(code);
    }

    public static byte @NotNull [] NIOReadFile( String url ) throws IOException {
        RandomAccessFile access = new RandomAccessFile(url, "r"); //随机读文件
        FileChannel channel = access.getChannel(); //打开文件通道
        ByteBuffer temp = ByteBuffer.allocate((int) access.length()); //申请缓冲区空间
        channel.read(temp); //数据从通道写入缓冲区
        temp.flip(); //反转读写模式，position变为0
        byte[] ret = new byte[temp.remaining()];
        temp.get(ret, 0, ret.length); //将缓冲区内数据写入ret数组
        return ret;
    }

    public static void NIOWriteFile( String filename ,String data , int length ) throws IOException {
        RandomAccessFile access = new RandomAccessFile(filename, "rw");

        FileChannel channel = access.getChannel();

        byte[] content = new byte[length];
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(dataBytes, 0, content, 0,  length); //将data中数据复制到定长的字节数组中，避免末尾出现null
        ByteBuffer byteBuffer = ByteBuffer.wrap(content); //创建缓冲区

        channel.write(byteBuffer);
    }

    public static String queryCode(int code){
        return CodeStatus.valueOf("STATUS_CODE_"+code).getStatus(); //查询状态码对应含义
    }

    public static String queryFileType( @NotNull String url){
        String type = url.split("\\.")[1].toLowerCase(); //获得类型字符串
        return FileType.valueOf(type).getType(); //返回对应类型
    }

}

enum CodeStatus{

    STATUS_CODE_200("OK", 200),
    STATUS_CODE_404("Not Found", 404),
    STATUS_CODE_501("Not Implemented", 501),
    STATUS_CODE_500 ("Internal Server Error",500);

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

    private FileType(String type){
        this.type = type;
    }

    public String getType(){
        return type;
    }

}
