package roca.bajet.com.straggle.upload;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Arnold on 7/13/2017.
 */


public class PostImageResponse {

    @SerializedName("data")
    @Expose
    public Data data;
    @SerializedName("success")
    @Expose
    public Boolean success;
    @SerializedName("status")
    @Expose
    public Integer status;

    public static class Data {

        @SerializedName("id")
        @Expose
        public String id;
        @SerializedName("title")
        @Expose
        public String title;
        @SerializedName("description")
        @Expose
        public Object description;
        @SerializedName("datetime")
        @Expose
        public Integer datetime;
        @SerializedName("type")
        @Expose
        public String type;
        @SerializedName("animated")
        @Expose
        public Boolean animated;
        @SerializedName("width")
        @Expose
        public Integer width;
        @SerializedName("height")
        @Expose
        public Integer height;
        @SerializedName("size")
        @Expose
        public Integer size;
        @SerializedName("views")
        @Expose
        public Integer views;
        @SerializedName("bandwidth")
        @Expose
        public Integer bandwidth;
        @SerializedName("vote")
        @Expose
        public Object vote;
        @SerializedName("favorite")
        @Expose
        public Boolean favorite;
        @SerializedName("nsfw")
        @Expose
        public Object nsfw;
        @SerializedName("section")
        @Expose
        public Object section;
        @SerializedName("account_url")
        @Expose
        public Object accountUrl;
        @SerializedName("account_id")
        @Expose
        public Integer accountId;
        @SerializedName("is_ad")
        @Expose
        public Boolean isAd;
        @SerializedName("in_most_viral")
        @Expose
        public Boolean inMostViral;
        @SerializedName("tags")
        @Expose
        public List<Object> tags = null;
        @SerializedName("ad_type")
        @Expose
        public Integer adType;
        @SerializedName("ad_url")
        @Expose
        public String adUrl;
        @SerializedName("in_gallery")
        @Expose
        public Boolean inGallery;
        @SerializedName("deletehash")
        @Expose
        public String deletehash;
        @SerializedName("name")
        @Expose
        public String name;
        @SerializedName("link")
        @Expose
        public String link;

        @SerializedName("error")
        @Expose
        public String error;
        @SerializedName("request")
        @Expose
        public String request;
        @SerializedName("method")
        @Expose
        public String method;

    }

}

