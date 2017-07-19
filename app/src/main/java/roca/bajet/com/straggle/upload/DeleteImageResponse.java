package roca.bajet.com.straggle.upload;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Arnold on 7/17/2017.
 */

public class DeleteImageResponse {

    @SerializedName("data")
    @Expose
    public boolean data;

    @SerializedName("success")
    @Expose
    public Boolean success;
    @SerializedName("status")
    @Expose
    public Integer status;


}
