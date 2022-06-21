import org.jetbrains.annotations.NotNull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Method {

    private final NIOFileHandler nioFileHandler;

    RequestHeader requestHeader;

    ResponseHeader responseHeader;

    Method(){
        nioFileHandler = new NIOFileHandler();
    }

    void processRequest( @NotNull SelectionKey key) throws IOException {

        SocketChannel channel = (SocketChannel) key.channel(); //获取传输报文的通道
        ByteBuffer byteBuffer = ByteBuffer.allocate(Utils.SIZE); //申请一个固定大小的缓冲区
        channel.read(byteBuffer); //将报文写至缓冲区

        byteBuffer.flip(); //翻转读写方式
        String temp = StandardCharsets.UTF_8.decode(byteBuffer).toString(); //将报文格式转变为string
        try{
            RequestHeader requestHeader = Utils.requestParseString(temp);
            key.attach(Optional.of(requestHeader));
            //设置key的attachment字段，而一个key和一个channel绑定，即可后续从通道中获取该请求报文
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    void processResponse( @NotNull SelectionKey key ) throws IOException {

        SocketChannel channel = (SocketChannel) key.channel();
        Optional<RequestHeader> op = (Optional<RequestHeader>) key.attachment(); //请求报文

        if(op.isEmpty()){
            handle400(channel); //错误请求
            channel.close(); //关闭通道
            return;
        }

        requestHeader = op.get();
        responseHeader = new ResponseHeader(requestHeader);

        try{
            handle200(channel, requestHeader.getUrl());
        }
        catch (FileNotFoundException e){
            handle404(channel);
        }
        catch (Exception e){
            handle500(channel);
        }
        finally {
            channel.close();
        }

    }

    private void handle400( SocketChannel channel ) {
        try{
            handleError(channel, 400);
        }
        catch (Exception e){
            handle500(channel);
        }
    }

    private void handle404( SocketChannel channel ) {
        try{
            handleError(channel, 404);
        }
        catch (Exception e){
            handle500(channel);
        }

    }

    private void handle500( SocketChannel channel ) {
        try{
            handleError(channel, 500);
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    private void handle501( SocketChannel channel ){
        try{
            handleError(channel, 501);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void handle200( @NotNull SocketChannel channel ,String url ) throws IOException {
        responseHeader.setCode(200);
        String method = requestHeader.getMethod();
        if(method.equals(Utils.MethodName.GET.toString())){

            if(url.equals(Utils.EXIT)){
                Server.flag.set(0);
                System.exit(-1);
            }

            ByteBuffer responseBody = nioFileHandler.read("web/request/" + url);
            responseHeader.setContent_length(responseBody.capacity());
            responseHeader.setContent_type(Utils.queryFileType(url));
            ByteBuffer responseHead = ByteBuffer.wrap(responseHeader.toString().getBytes(StandardCharsets.UTF_8));
            //将string格式转变为butebuffer
            channel.write(new ByteBuffer[]{responseHead, responseBody});
        }
        else if(method.equals(Utils.MethodName.HEAD.toString())){
            ByteBuffer responseHead = ByteBuffer.wrap(responseHeader.toString().getBytes(StandardCharsets.UTF_8));
            channel.write(responseHead);
        }
        else if(method.equals(Utils.MethodName.POST.toString())){
            String data = requestHeader.getData();
            responseHeader.setContent_length(0);
            responseHeader.setContent_type("");
            nioFileHandler.write("db/data.txt", data);
            ByteBuffer responseHead = ByteBuffer.wrap(responseHeader.toString().getBytes(StandardCharsets.UTF_8));
            channel.write(responseHead);
        }
        else if(method.equals(Utils.MethodName.PUT.toString())){
            String data = Utils.mdToHtml(requestHeader.getData());
            ByteBuffer responseBody = StandardCharsets.UTF_8.encode(data);
            responseHeader.setContent_type(Utils.queryFileType(".html"));
            responseHeader.setContent_length(responseBody.limit());
            ByteBuffer responseHead = ByteBuffer.wrap(responseHeader.toString().getBytes(StandardCharsets.UTF_8));
            channel.write(new ByteBuffer[]{responseHead, responseBody});
        }
        else {
            handle501(channel);
        }

    }

    private void handleError( @NotNull SocketChannel channel ,int code ) throws IOException {
        responseHeader.setCode(code);

        String filename = "web/error/" + code + ".html";
        ByteBuffer responseBody = nioFileHandler.read(filename);
        responseHeader.setContent_length(responseBody.capacity());
        responseHeader.setContent_type(Utils.queryFileType(filename));
        requestHeader.setVersion(this.requestHeader.getVersion());
        ByteBuffer responseHead = ByteBuffer.wrap(responseHeader.toString().getBytes(StandardCharsets.UTF_8));

        channel.write(new ByteBuffer[]{responseHead, responseBody});
    }

}

class NIOFileHandler{

    NIOFileHandler(){}


    ByteBuffer read(String filename) throws IOException{

        RandomAccessFile access = new RandomAccessFile(filename, "r");

        FileChannel channel = access.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate((int)channel.size());

        channel.read(byteBuffer);
        byteBuffer.flip();

        return byteBuffer;
    }

    void write(String filename, String data) throws IOException{

        RandomAccessFile access = new RandomAccessFile(filename, "rw");

        FileChannel channel = access.getChannel();
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(data);

        channel.write(byteBuffer);

    }

}
