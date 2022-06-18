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

        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(Utils.SIZE);
        channel.read(byteBuffer);

        byteBuffer.flip();
        String temp = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        try{
            RequestHeader requestHeader = Utils.requestParseString(temp);
            key.attach(Optional.of(requestHeader));
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    void processResponse( @NotNull SelectionKey key ) throws IOException {

        SocketChannel channel = (SocketChannel) key.channel();
        Optional<RequestHeader> op = (Optional<RequestHeader>) key.attachment();

        if(op.isEmpty()){
            handle400(channel);
            channel.close();
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
