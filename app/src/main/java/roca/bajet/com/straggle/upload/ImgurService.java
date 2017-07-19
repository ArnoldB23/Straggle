package roca.bajet.com.straggle.upload;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by Arnold on 7/14/2017.
 */

public interface ImgurService {


    @Multipart
    @POST("/3/image")
    Call<PostImageResponse> postImage(
            @Header("Authorization") String auth,
            @Part("image") RequestBody image
    );

    @Multipart
    @POST("/3/image")
    Call<PostImageResponse> postImage(
            @Header("Authorization") String auth,
            @Query("title") String title,
            @Query("description") String description,
            @Query("album") String albumId,
            @Query("account_url") String username,
            @Part("image") RequestBody image
    );


    @DELETE("/3/image/{deletehash}")
    Call<DeleteImageResponse> deleteImage(
            @Header("Authorization") String auth,
            @Path("deletehash") String deletehash
    );
}
