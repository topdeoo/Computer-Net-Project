import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Hashtable;

public class Headers {

    private String method;

    private String url;

    private String version;

    private Hashtable<String, String> headMap = new Hashtable<>();

    private Hashtable<String, String> data = new Hashtable<>();

    public Headers(){}

    public String getMethod(){return method;}

    public void setMethod(String method){this.method = method;}

    public String getUrl(){return url;}

    public void setUrl(String url){this.url = "webpage/" + url;}

    public String getVersion(){return version;}

    public void setVersion( String version ){this.version = version;}

    public void putHeadMap( String K,String V){headMap.put(K, V);}

    public String getHeadMap( String K){return headMap.get(K);}

    public String getData(String K) {
        return data.get(K);
    }

    public String getData(){
        return data.toString();
    }

    public void putData(String K, String V) {
        data.put(K, V);
    }

    @Override
    public String toString() {
        return "Headers{" +
                "method='" + method + '\'' +
                ", url='" + url + '\'' +
                ", version='" + version + '\'' +
                ", headerMap=" + headMap + '\'' +
                ", data=" + data +
                '}';
    }

    public static @NotNull Headers parseHeader( String temp){
        assert temp != null;

        assert temp.contains(Utils.CRLF);

        Headers headers = new Headers();

        String firstLine = temp.substring(0, temp.indexOf(Utils.CRLF));
        String[] parts = firstLine.split(" ");

        assert parts.length == 3;

        headers.setMethod(parts[0]);
        headers.setUrl(parts[1]);
        headers.setVersion(parts[2]);

        parts = temp.split(Utils.CRLF);
        for(String part: parts){
            int idx = part.indexOf(":");
            if(idx == -1)
                continue;
            String K = part.substring(0, idx);
            String V = "";
            if(idx + 1 < part.length())
                V = part.substring(idx + 1);
            headers.putHeadMap(K, V);
        }

        if(headers.getHeadMap("Content-Length") != null){
            String[] dataStr = parts[ parts.length - 1].split("&");
            for(String data_i : dataStr){
                int idx = data_i.indexOf("=");
                String K = data_i.substring(0, idx);
                String V = data_i.substring(idx + 1);
                headers.putData(K, V);
            }

        }

        return headers;
    }

}

class ResponseHeaders extends Headers{

    private int code;

    private String code_meaning;

    private String content_type;

    private int content_length;

    private String server;

    public ResponseHeaders(int code){
        this.code = code;
        this.server = Utils.SERVER_NAME;
        this.code_meaning = Utils.queryCode(code);
    }

    public void setCode(int code){
        this.code = code;
    }

    public int getCode() {
        return code;
    }


    public String getCode_meaning() {
        return code_meaning;
    }

    public void setCode_meaning( String code_meaning ) {
        this.code_meaning = code_meaning;
    }


    public String getServer() {
        return server;
    }

    public void setServer( String server ) {
        this.server = server;
    }


    public int getContent_length() {
        return content_length;
    }

    public void setContent_length( int content_length ) {
        this.content_length = content_length;
    }

    public String getContent_type() {
        return content_type;
    }

    public void setContent_type( String content_type ) {
        this.content_type = content_type;
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
}
