import com.google.gson.Gson;

/**
 * 工具类
 * <p>
 * 实现的工具如下：
 * 1. {@link #beanToJSONString}
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

}
