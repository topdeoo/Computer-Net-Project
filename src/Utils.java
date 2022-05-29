import com.google.gson.Gson;
import jdk.jshell.execution.Util;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * 工具类
 * <p>
 *
 * 实现的工具如下：<br/>
 * 1. {@link #beanToJSONString}<br/>
 * 2. 保存各种常量值<br/>
 *
 *
 * @see #beanToJSONString(Object)
 *
 * @author 郑勤
 * @version 1.0.0
 * @since jdk11.0.6
 *
 */

public class Utils {

    static final String SERVER_NAME = "java/jdk11.0";

    static final String INDEX_PAGE = "index.html";
    static final String EXIT = "shutdown";

    static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    static final String CONTENT_TYPE_FILE = "multipart/form-data";

    final static String CRLF = "\r\n";

    /**
     * 将一个Object类对象转化为Json式的字符串
     *
     * @param obj 待转换的对象
     * @return JSON型字符串
     */

    public static String beanToJSONString(Object obj){
        Gson gson = new Gson();
        return gson.toJson(obj);
    }

    /**
     * 将Markdown转换为Html文档
     *
     * @param md 待转换的Markdown文档
     * @return HTML文档
     */

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


