import com.google.gson.Gson;

public class Utils {

    public static String beanToJSONString(Object bean){
        Gson gson = new Gson();
        return gson.toJson(bean);
    }

}
