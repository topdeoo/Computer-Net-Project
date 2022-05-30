import com.google.gson.Gson;
import jdk.jshell.execution.Util;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;


public class Utils {

    static final String SERVER_NAME = "java/jdk11.0";

    static final String INDEX_PAGE = "index.html";
    static final String EXIT = "shutdown";

    static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    static final String CONTENT_TYPE_FILE = "multipart/form-data";

    final static String CRLF = "\r\n";

    public static String beanToJSONString(Object obj){
        Gson gson = new Gson();
        return gson.toJson(obj);
    }


    public static String mdToHtml(String md){
        Parser parser = Parser.builder().build();
        Node document = parser.parse(md);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(document);
    }

    public static String queryCode(int code){
        return CodeStatus.valueOf("STATUS_CODE_"+code).getStatus();
    }

    public static String queryFileType(String url){
        String type = url.split("\\.")[1].toLowerCase();
        return FileType.valueOf(type).getType();
    }

    public enum MethodName{
        GET, POST, HEAD;
    }


}

enum CodeStatus{

    STATUS_CODE_200("OK", 200), STATUS_CODE_404("Not Found", 404),
    STATUS_CODE_403("Bad Request", 400), STATUS_CODE_501("Not Implemented", 501),
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


