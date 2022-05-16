import com.google.gson.Gson;
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

    static final String INDEX_PAGE = "index.html";
    static final String EXIT = "shutdown";

    static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    static final String CONTENT_TYPE_FILE = "multipart/form-data";
    static final String CONTENT_TYPE_JSON = "application/json";
    static final String CONTENT_TYPE_HTML = "text/html";
    static final String CONTENT_TYPE_TEXT = "text/plain";
    static final String CONTENT_TYPE_MD = "text/markdown";


    static final String STATUS_CODE_500 = "Internal Server Error";
    static final String STATUS_CODE_200 = "OK";
    static final String STATUS_CODE_404 = "Not Found";
    static final String STATUS_CODE_501 = "Not Implemented";
    private static final String STATUS_CODE_201 = "Created";
    private static final String STATUS_CODE_204 = "No Content";

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

}
