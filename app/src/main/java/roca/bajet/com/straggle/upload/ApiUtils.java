package roca.bajet.com.straggle.upload;

/**
 * Created by Arnold on 7/17/2017.
 */

public class ApiUtils {

    public static final String IMGUR_URL = "https://api.imgur.com/";

    public static ImgurService getImgurService() {
        return RetrofitClient.getClient(IMGUR_URL).create(ImgurService.class);
    }


}
