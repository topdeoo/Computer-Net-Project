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

    private final static int SIZE = 4096;

    Headers headers;

    Method(){
        nioFileHandler = new NIOFileHandler();
    }

    void processRequest( @NotNull SelectionKey key) throws IOException {

        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(SIZE);
        channel.read(byteBuffer);

        byteBuffer.flip();
        String temp = StandardCharsets.UTF_8.decode(byteBuffer).toString();
        try{
            Headers headers = Headers.parseHeader(temp);
            key.attach(Optional.of(headers));
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    void processResponse( @NotNull SelectionKey key ) throws IOException {

        SocketChannel channel = (SocketChannel) key.channel();
        Optional<Headers> op = (Optional<Headers>) key.attachment();

        if(op.isEmpty()){
            handle400(channel);
            channel.close();
            return;
        }

        headers = op.get();

        try{
            handle200(channel, headers.getUrl());
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

    private void handle200( @NotNull SocketChannel channel ,String url ) throws IOException {
        ResponseHeaders headers = new ResponseHeaders(200);

        String method = this.headers.getMethod();
        if(method.equals(Utils.MethodName.GET.toString())){
            ByteBuffer context = nioFileHandler.read(url);
            headers.setContent_length(context.capacity());
            headers.setContent_type(Utils.queryFileType(url));
            headers.setVersion(this.headers.getVersion());
            ByteBuffer responseHead = ByteBuffer.wrap(headers.toString().getBytes(StandardCharsets.UTF_8));

            channel.write(new ByteBuffer[]{responseHead, context});
        }
        else if(method.equals(Utils.MethodName.HEAD.toString())){
            headers.setVersion(this.headers.getVersion());
            ByteBuffer responseHead = ByteBuffer.wrap(headers.toString().getBytes(StandardCharsets.UTF_8));
            channel.write(responseHead);
        }
        else if(method.equals(Utils.MethodName.POST.toString())){
            String data = this.headers.getData();
            nioFileHandler.write("webpage/data.txt", data);
            headers.setVersion(this.headers.getVersion());
            ByteBuffer responseHead = ByteBuffer.wrap(headers.toString().getBytes(StandardCharsets.UTF_8));
            channel.write(responseHead);
        }


    }

    private void handleError( @NotNull SocketChannel channel ,int code ) throws IOException {
        ResponseHeaders headers = new ResponseHeaders(code);
        ByteBuffer responseHead = ByteBuffer.wrap(headers.toString().getBytes(StandardCharsets.UTF_8));

        channel.write(responseHead);
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
