import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Hashtable;


public class RequestHeader {

    protected String method;

    protected String url;

    protected String version;

    protected String content_type;

    protected int content_length;

    private String Host;

    protected Hashtable<String, String> headMap = new Hashtable<>();

    protected StringBuilder data = new StringBuilder();

    public RequestHeader(){}

    public RequestHeader( @NotNull RequestHeader header){
        this.method = header.method;
        this.url = header.url;
        this.version = header.version;
        this.content_length = header.content_length;
        this.content_type = header.content_type;
        this.headMap = new Hashtable<>(header.headMap);
        this.data = new StringBuilder(header.data.toString());
    }


    public String getMethod(){return method;}

    public void setMethod(String method){this.method = method;}

    public String getUrl(){return url;}

    public void setUrl(String url){this.url = url;}

    public String getVersion(){return version;}

    public void setVersion( String version ){this.version = version;}

    public void putHeadMap( String K,String V){headMap.put(K, V);}

    public String getHeadMap( String K){return headMap.get(K);}

    public String getData(){
        return data.toString();
    }

    public void setData(String data){
        this.data.append(data);
    }

    public void setContent_type(String type){
        content_type = type;
    }

    public void setContent_length(int length){
        content_length = length;
    }

    public String getContent_type(){
        return content_type;
    }

    public int getContent_length(){
        return content_length;
    }

    public String getHost(){
        return Host;
    }

    public void setHost(String Host){
        this.Host = Host;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%s %s %s\r\n", getMethod(), getUrl(), getVersion()));
        for(String K : headMap.keySet())
            sb.append(String.format("%s:%s\r\n", K, headMap.get(K)));
        sb.append("\r\n");
        sb.append(data.toString());

        return sb.toString();
    }

}

class ResponseHeader extends RequestHeader {

    private int code;

    private String code_meaning;

    private String server = "java/jdk17.0";

    ResponseHeader(){}

    ResponseHeader(RequestHeader header){
        super(header);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();

        ret.append(String.format("%s %d %s\r\n", getVersion(), code, code_meaning));
        ret.append(String.format("Server: %s\r\n", getServer()));
        ret.append(String.format("Content-Type: %s\r\n", getContent_type()));
        ret.append(String.format("Content-Length: %d\r\n", getContent_length()));
        ret.append("Date:").append(new Date()).append("\r\n\r\n");

        return ret.toString();
    }

    public String getServer() {
        return server;
    }


    public void setCode( int code ) {
        this.code = code;
        this.code_meaning = Utils.queryCode(code);
    }

}