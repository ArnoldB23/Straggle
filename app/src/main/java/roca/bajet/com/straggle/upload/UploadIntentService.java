package roca.bajet.com.straggle.upload;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Response;
import roca.bajet.com.straggle.BuildConfig;
import roca.bajet.com.straggle.data.ContentProviderDbSchema;

/**
 * Created by Arnold on 7/19/2017.
 */

public class UploadIntentService extends IntentService {

    private static final String LOG_TAG = "UploadIntentService";
    public static final String DELETEIMAGE = "DELETEIMAGE";
    public static final String DELETEIMAGEANDRECORD = "DELETEIMAGEANDRECORD";
    public static final String DELETEIMAGEANDRECORD_ID = "DELETEIMAGEANDRECORD_ID";
    public static final String POSTIMAGE = "POSTIMAGE";
    public static final String POSTIMAGE_ID = "POSTIMAGE_ID";

    private ImgurService mImgurService;
    private int mDefaultUser;
    private File mediaStorageDir;
    private Handler mUIHandler;
    private UploadNotificationHelper mNotificationHelper;
    private Context mContext;


    public UploadIntentService() {
        super(UploadIntentService.class.getName());
        Log.d(LOG_TAG, "UploadIntentService");


    }

    public UploadIntentService(String name) {
        super(name);

        Log.d(LOG_TAG, "UploadIntentService");


    }



    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG, "onHandleIntent");

        mImgurService = ApiUtils.getImgurService();
        mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Straggle");

        mContext = this;
        mNotificationHelper = new UploadNotificationHelper(mContext);
        mUIHandler = new Handler(Looper.getMainLooper());

        String sel = ContentProviderDbSchema.Users.COL_USERNAME + " = 'DEFAULT_USER'";
        Cursor c = getContentResolver().query(ContentProviderDbSchema.Users.CONTENT_URI, null, sel, null, null);
        if (c.moveToFirst()) {
            mDefaultUser = c.getInt(c.getColumnIndex(ContentProviderDbSchema.Users._ID));
            Log.d(LOG_TAG, "DEFAULT_USER_ID: " + mDefaultUser);
        }
        c.close();


        if (intent.getStringExtra("tag").equals(POSTIMAGE))
        {
            final Long id = intent.getLongExtra(POSTIMAGE_ID, -1);
            MediaType MEDIA_TYPE = MediaType.parse("image/jpeg");
            String filename = intent.getStringExtra(POSTIMAGE);
            File mediaFile = new File(mediaStorageDir.getPath() + File.separator + filename);
            RequestBody requestBody = RequestBody.create(MEDIA_TYPE, mediaFile);
            Response<PostImageResponse> response = null;

            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    mNotificationHelper.createUploadingNotification();
                }
            });

            try {
                response = mImgurService.postImage(BuildConfig.IMGUR_AUTHORIZATION, requestBody).execute();


                if (response.isSuccessful())
                {
                    ContentValues cv = new ContentValues();
                    cv.put(ContentProviderDbSchema.ImageTextures.COL_URL, response.body().data.link);
                    cv.put(ContentProviderDbSchema.ImageTextures.COL_DELETE_HASH, response.body().data.deletehash);

                    Uri updateIdUri = ContentProviderDbSchema.ImageTextures.buildImageTextureUriWithUserId(mDefaultUser);
                    String where = ContentProviderDbSchema.ImageTextures._ID + " = ?";
                    String selectionArgs [] = {String.valueOf(id)};
                    int updated = getContentResolver().update(updateIdUri, cv, where, selectionArgs);
                    final String link = response.body().data.link;
                    Log.d(LOG_TAG, "postImage onResponse, updated : " + updated);

                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mNotificationHelper.createUploadedNotification(link);
                        }
                    });

                    Log.d(LOG_TAG, "postImage execute, Successful HTTP response");
                }else{
                    Log.d(LOG_TAG, "postImage execute, Failed HTTP response, code: " + response.code() + ", " + response.body().data.error);
                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mNotificationHelper.createFailedUploadNotification();
                        }
                    });
                }

            } catch (IOException e) {
                Log.d(LOG_TAG, "postImage execute, exception: " + e.toString());
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mNotificationHelper.createFailedUploadNotification();
                    }
                });
            }

        }

        else if(intent.getStringExtra("tag").equals(DELETEIMAGE))
        {
            String deletehash = intent.getStringExtra(DELETEIMAGE);
            Response<DeleteImageResponse> response = null;
            try {
                response = mImgurService.deleteImage(BuildConfig.IMGUR_AUTHORIZATION, deletehash).execute();

                if (response.isSuccessful())
                {
                    Log.d(LOG_TAG, "onResponse, Successful HTTP response");

                }else{
                    Log.d(LOG_TAG, "onResponse, Failed HTTP response code : "  + response.code());
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
            }

        }

        else if(intent.getStringExtra("tag").equals(DELETEIMAGEANDRECORD))
        {
            String deletehash = intent.getStringExtra(DELETEIMAGEANDRECORD);
            final Long id = intent.getLongExtra(DELETEIMAGEANDRECORD_ID, -1);
            Response<DeleteImageResponse> response = null;
            try {
                response = mImgurService.deleteImage(BuildConfig.IMGUR_AUTHORIZATION, deletehash).execute();

                if (response.isSuccessful())
                {
                    ContentValues cv = new ContentValues();
                    cv.putNull(ContentProviderDbSchema.ImageTextures.COL_URL);
                    cv.putNull(ContentProviderDbSchema.ImageTextures.COL_DELETE_HASH);

                    Uri updateIdUri = ContentProviderDbSchema.ImageTextures.buildImageTextureUriWithUserId(mDefaultUser);
                    String where = ContentProviderDbSchema.ImageTextures._ID + " = ?";
                    String selectionArgs [] = {String.valueOf(id)};
                    int updated = getContentResolver().update(updateIdUri, cv, where, selectionArgs);


                    Log.d(LOG_TAG, "onResponse, Successful HTTP response, updated : " + updated);

                }else{
                    Log.d(LOG_TAG, "onResponse, Failed HTTP response code : "  + response.code());
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
            }


        }

    }
}
