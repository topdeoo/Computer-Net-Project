import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;

public class Method {

    private final NIOFileHandler nioFileHandler;

    private ArrayList<String> requestHead = new ArrayList<>();

    private final static int SIZE = 4096;

    Method(){
        nioFileHandler = new NIOFileHandler();
    }


    void processResponse( @NotNull SelectionKey key ) throws IOException {

        SocketChannel channel = (SocketChannel) key.channel();
        Optional<Headers> op = (Optional<Headers>) key.attachment();

        if(!op.isPresent()){
            handle400(channel);
            channel.close();
            return;
        }

        Headers headers = op.get();

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

        ByteBuffer context = nioFileHandler.read(url);
        headers.setContent_length(context.capacity());
        headers.setContent_type(Utils.queryFileType(url));
        headers.setVersion("HTTP/1.1");
        ByteBuffer responseHead = ByteBuffer.wrap(headers.toString().getBytes(StandardCharsets.UTF_8));

        channel.write(new ByteBuffer[]{responseHead, context});

    }

    private void handleError( @NotNull SocketChannel channel ,int code ) throws IOException {
        ResponseHeaders headers = new ResponseHeaders(code);
        ByteBuffer responseHead = ByteBuffer.wrap(headers.toString().getBytes(StandardCharsets.UTF_8));

        channel.write(responseHead);
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

}

class NIOFileHandler{

    NIOFileHandler(){}

    NIOFileHandler(String filename) throws IOException{
    }


    ByteBuffer read(String filename) throws IOException{

        RandomAccessFile access = new RandomAccessFile(filename, "r");

        FileChannel channel = access.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate((int)channel.size());

        channel.read(byteBuffer);
        byteBuffer.flip();

        return byteBuffer;
    }

}
