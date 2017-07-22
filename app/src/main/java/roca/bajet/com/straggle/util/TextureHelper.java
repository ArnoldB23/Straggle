package roca.bajet.com.straggle.util;

/**
 * Created by Arnold on 2/8/2017.
 */

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import roca.bajet.com.straggle.data.ContentProviderDbSchema;
import roca.bajet.com.straggle.data.ContentProviderOpenHelper;

import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LINEAR_MIPMAP_LINEAR;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGenerateMipmap;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLUtils.getEGLErrorString;
import static android.opengl.GLUtils.texImage2D;

public class TextureHelper {
    private static final String TAG = "TextureHelper";

    public static final int ARATIO_1X1 = 1011;
    public static final int ARATIO_2X3 = 1022;
    public static final int ARATIO_3X5 = 1035;
    public static final int ARATIO_3X4 = 1034;
    public static final int ARATIO_4X5 = 1045;
    public static final int ARATIO_5X7 = 1057;
    public static final int ARATIO_9X16 = 1916;

    public static final int ARATIO_3X2 = 2032;
    public static final int ARATIO_5X3 = 2053;
    public static final int ARATIO_4X3 = 2043;
    public static final int ARATIO_5X4 = 2054;
    public static final int ARATIO_7X5 = 2075;
    public static final int ARATIO_16X9 = 2169;
    private static final String IMAGETEXTURE_LOC = "IMAGETEXTURE_LOC";


    public static int getBestAspectRatio(BitmapFactory.Options options) {
        int width = options.outWidth;
        int height = options.outHeight;
        float aspectRatio = (float) width / height;

        float bestFitError = 1000;
        int bestFitAspectRatio = 0;

        if (Math.abs(aspectRatio - 1f) < bestFitError) {
            bestFitError = Math.abs(aspectRatio - 1f);
            bestFitAspectRatio = ARATIO_1X1;
        }

        if (Math.abs(aspectRatio - 2 / 3f) < bestFitError) {
            bestFitError = Math.abs(aspectRatio - 2 / 3f);
            bestFitAspectRatio = ARATIO_2X3;
        }

        if (Math.abs(aspectRatio - 3 / 5f) < bestFitError) {
            bestFitError = Math.abs(aspectRatio - 3 / 5f);
            bestFitAspectRatio = ARATIO_3X5;
        }

        if (Math.abs(aspectRatio - 3 / 4f) < bestFitError) {
            bestFitError = Math.abs(aspectRatio - 3 / 4f);
            bestFitAspectRatio = ARATIO_3X4;
        }

        if (Math.abs(aspectRatio - 4 / 5f) < bestFitError) {
            bestFitError = Math.abs(aspectRatio - 4 / 5f);
            bestFitAspectRatio = ARATIO_4X5;
        }

        if (Math.abs(aspectRatio - 5 / 7f) < bestFitError) {
            bestFitError = Math.abs(aspectRatio - 5 / 7f);
            bestFitAspectRatio = ARATIO_5X7;
        }

        if (Math.abs(aspectRatio - 9 / 16f) < bestFitError) {
            bestFitError = Math.abs(aspectRatio - 9 / 16f);
            bestFitAspectRatio = ARATIO_9X16;
        }


        //landscape ratios
        if (Math.abs(aspectRatio - 3 / 2f) < bestFitError) {
            bestFitError = Math.abs(aspectRatio - 3 / 2f);
            bestFitAspectRatio = ARATIO_3X2;
        }

        if (Math.abs(aspectRatio - 5 / 3f) < bestFitError) {
            bestFitError = Math.abs(aspectRatio - 5 / 3f);
            bestFitAspectRatio = ARATIO_5X3;
        }

        if (Math.abs(aspectRatio - 4 / 3f) < bestFitError) {
            bestFitError = Math.abs(aspectRatio - 4 / 3f);
            bestFitAspectRatio = ARATIO_4X3;
        }

        if (Math.abs(aspectRatio - 5 / 4f) < bestFitError) {
            bestFitError = Math.abs(aspectRatio - 5 / 4f);
            bestFitAspectRatio = ARATIO_5X4;
        }

        if (Math.abs(aspectRatio - 7 / 5f) < bestFitError) {
            bestFitError = Math.abs(aspectRatio - 7 / 5f);
            bestFitAspectRatio = ARATIO_7X5;
        }

        if (Math.abs(aspectRatio - 16 / 9f) < bestFitError) {
            bestFitError = Math.abs(aspectRatio - 16 / 9f);
            bestFitAspectRatio = ARATIO_16X9;
        }

        return bestFitAspectRatio;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static Bitmap decodeSampledBitmapFromFile(String fileStr,
                                                     int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileStr, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(fileStr, options);
    }

    public static Bitmap decodeSampledBitmapFromBytes(byte[] data,
                                                      int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    public static Bitmap getCircleClip(Bitmap bitmap) {
        Bitmap output;

        if (bitmap.getWidth() > bitmap.getHeight()) {
            output = Bitmap.createBitmap(bitmap.getHeight(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        } else {
            output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getWidth(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        float r = 0;

        if (bitmap.getWidth() > bitmap.getHeight()) {
            r = bitmap.getHeight() / 2;
        } else {
            r = bitmap.getWidth() / 2;
        }

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(r, r, r, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    public static Bitmap getSquareClip(Bitmap bitmap) {
        Bitmap output;

        if (bitmap.getWidth() > bitmap.getHeight()) {
            output = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getHeight(), bitmap.getHeight());
        } else {
            output = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getWidth());
        }

        return output;
    }

    public static Bitmap getLayeredBitmap(Bitmap[] bps, Context c) {
        Bitmap output;

        /*
        ArrayList<BitmapDrawable> bitmapDrawables = new ArrayList<>(3);

        for (int i = 0; i < bps.length; i++)
        {
            BitmapDrawable bitmapDrawable = new BitmapDrawable(c.getResources(), bps[i]);
            bitmapDrawable.setGravity(Gravity.CENTER);
            int a = i *10;
            bitmapDrawable.setBounds(a, a, a + bps[i].getWidth(), a + bps[i].getHeight());
            bitmapDrawables.add(bitmapDrawable);

        }


        LayerDrawable layerDrawable = new LayerDrawable( bitmapDrawables.toArray(new Drawable[bitmapDrawables.size()]));

        output = drawableToBitmap(layerDrawable);
        */
        int width = bps[0].getWidth();
        int height = bps[0].getHeight();
        output = Bitmap.createBitmap(width + bps.length * 10, height + bps.length * 10, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Rect entireRect = new Rect(0, 0, bps[0].getWidth(), bps[0].getHeight());

        for (int i = 0; i < bps.length; i++) {
            int a = i * 10;
            Rect offsetRect = new Rect(a, a, bps[i].getWidth() + a, bps[i].getHeight() + a);
            canvas.drawBitmap(bps[i], entireRect, offsetRect, null);
        }

        return output;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }


    public static Bitmap scaleBitmap(int resId, Context context) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), resId, options);
        int aspect_ratio = getBestAspectRatio(options);


        switch (aspect_ratio) {
            case ARATIO_1X1:

                break;
            case ARATIO_2X3:
                break;
            case ARATIO_3X5:
                break;
            case ARATIO_3X4:
                break;
            case ARATIO_4X5:
                break;
            case ARATIO_5X7:
                break;
            case ARATIO_9X16:
                break;
        }

        return null;
    }

    public static float[] getTextureVertexSet(int aspect_ratio) {
        switch (aspect_ratio) {
            case ARATIO_1X1:
                return new float[]{
                        // Order of coordinates: X, Y, S, T
                        0f, 0f, 0.5f, 0.5f,
                        -1.5f, -1.5f, 0f, 1f,
                        1.5f, -1.5f, 1f, 1f,
                        1.5f, 1.5f, 1f, 0f,
                        -1.5f, 1.5f, 0f, 0f,
                        -1.5f, -1.5f, 0f, 1f
                };

            case ARATIO_2X3:
                return new float[]{
                        // Order of coordinates: X, Y, S, T
                        0f, 0f, 0.5f, 0.5f,
                        -1f, -1.5f, 0f, 1f,
                        1f, -1.5f, 1f, 1f,
                        1f, 1.5f, 1f, 0f,
                        -1f, 1.5f, 0f, 0f,
                        -1f, -1.5f, 0f, 1f

                };

            case ARATIO_3X5:
                return new float[]{
                        // Order of coordinates: X, Y, S, T
                        0f, 0f, 0.5f, 0.5f,
                        -1.5f, -2.5f, 0f, 1f,
                        1.5f, -2.5f, 1f, 1f,
                        1.5f, 2.5f, 1f, 0f,
                        -1.5f, 2.5f, 0f, 0f,
                        -1.5f, -2.5f, 0f, 1f

                };

            case ARATIO_3X4:
                return new float[]{
                        // Order of coordinates: X, Y, S, T
                        0f, 0f, 0.5f, 0.5f,
                        -1.5f, -2f, 0f, 1f,
                        1.5f, -2f, 1f, 1f,
                        1.5f, 2f, 1f, 0f,
                        -1.5f, 2f, 0f, 0f,
                        -1.5f, -2f, 0f, 1f

                };

            case ARATIO_4X5:
                return new float[]{
                        // Order of coordinates: X, Y, S, T
                        0f, 0f, 0.5f, 0.5f,
                        -2f, -2.5f, 0f, 1f,
                        2f, -2.5f, 1f, 1f,
                        2f, 2.5f, 1f, 0f,
                        -2f, 2.5f, 0f, 0f,
                        -2f, -2.5f, 0f, 1f

                };

            case ARATIO_5X7:
                return new float[]{
                        // Order of coordinates: X, Y, S, T
                        0f, 0f, 0.5f, 0.5f,
                        -2.5f, -3.5f, 0f, 1f,
                        2.5f, -3.5f, 1f, 1f,
                        2.5f, 3.5f, 1f, 0f,
                        -2.5f, 3.5f, 0f, 0f,
                        -2.5f, -3.5f, 0f, 1f

                };

            case ARATIO_9X16:
                return new float[]{
                        // Order of coordinates: X, Y, S, T
                        0f, 0f, 0.5f, 0.5f,
                        -1.125f, -2f, 0f, 1f,
                        1.125f, -2f, 1f, 1f,
                        1.125f, 2f, 1f, 0f,
                        -1.125f, 2f, 0f, 0f,
                        -1.125f, -2f, 0f, 1f

                };

            case ARATIO_3X2:
                return new float[]{
                        // Order of coordinates: X, Y, S, T
                        0f, 0f, 0.5f, 0.5f,
                        -1.5f, -1f, 0f, 1f,
                        1.5f, -1f, 1f, 1f,
                        1.5f, 1f, 1f, 0f,
                        -1.5f, 1f, 0f, 0f,
                        -1.5f, -1f, 0f, 1f

                };

            case ARATIO_5X3:
                return new float[]{
                        // Order of coordinates: X, Y, S, T
                        0f, 0f, 0.5f, 0.5f,
                        -2.5f, -1.5f, 0f, 1f,
                        2.5f, -1.5f, 1f, 1f,
                        2.5f, 1.5f, 1f, 0f,
                        -2.5f, 1.5f, 0f, 0f,
                        -2.5f, -1.5f, 0f, 1f

                };

            case ARATIO_4X3:
                return new float[]{
                        // Order of coordinates: X, Y, S, T
                        0f, 0f, 0.5f, 0.5f,
                        -2f, -1.5f, 0f, 1f,
                        2f, -1.5f, 1f, 1f,
                        2f, 1.5f, 1f, 0f,
                        -2f, 1.5f, 0f, 0f,
                        -2f, -1.5f, 0f, 1f

                };

            case ARATIO_5X4:
                return new float[]{
                        // Order of coordinates: X, Y, S, T
                        0f, 0f, 0.5f, 0.5f,
                        -2.5f, -2f, 0f, 1f,
                        2.5f, -2f, 1f, 1f,
                        2.5f, 2f, 1f, 0f,
                        -2.5f, 2f, 0f, 0f,
                        -2.5f, -2f, 0f, 1f

                };

            case ARATIO_7X5:
                return new float[]{
                        // Order of coordinates: X, Y, S, T
                        0f, 0f, 0.5f, 0.5f,
                        -3.5f, -2.5f, 0f, 1f,
                        3.5f, -2.5f, 1f, 1f,
                        3.5f, 2.5f, 1f, 0f,
                        -3.5f, 2.5f, 0f, 0f,
                        -3.5f, -2.5f, 0f, 1f

                };

            case ARATIO_16X9:
                return new float[]{
                        // Order of coordinates: X, Y, S, T
                        0f, 0f, 0.5f, 0.5f,
                        -2f, -1.125f, 0f, 1f,
                        2f, -1.125f, 1f, 1f,
                        2f, 1.125f, 1f, 0f,
                        -2f, 1.125f, 0f, 0f,
                        -2f, -1.125f, 0f, 1f

                };

        }

        return null;
    }

    /**
     * Loads a texture from a resource ID, returning the OpenGL ID for that
     * texture. Returns 0 if the load failed.
     *
     * @param context
     * @param resourceId
     * @return
     */
    public static int loadTexture(Context context, int resourceId) {
        final int[] textureObjectIds = new int[1];
        glGenTextures(1, textureObjectIds, 0);

        if (textureObjectIds[0] == 0) {

            Log.w(TAG, "Could not generate a new OpenGL texture object.");


            return 0;
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;


        // Read in the resource
        final Bitmap bitmap = BitmapFactory.decodeResource(
                context.getResources(), resourceId, options);

        if (bitmap == null) {

            Log.w(TAG, "Resource ID " + resourceId + " could not be decoded.");


            glDeleteTextures(1, textureObjectIds, 0);

            return 0;
        }

        // Bind to the texture in OpenGL
        glBindTexture(GL_TEXTURE_2D, textureObjectIds[0]);

        // Set filtering: a default must be set, or the texture will be
        // black.
        glTexParameteri(GL_TEXTURE_2D,
                GL_TEXTURE_MIN_FILTER,
                GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D,
                GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        // Load the bitmap into the bound texture.
        texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);


        // Note: Following code may cause an error to be reported in the
        // ADB log as follows: E/IMGSRV(20095): :0: HardwareMipGen:
        // Failed to generate texture mipmap levels (error=3)
        // No OpenGL error will be encountered (glGetError() will return
        // 0). If this happens, just squash the source image to be
        // square. It will look the same because of texture coordinates,
        // and mipmap generation will work.

        glGenerateMipmap(GL_TEXTURE_2D);

        // Recycle the bitmap, since its data has been loaded into
        // OpenGL.
        bitmap.recycle();

        // Unbind from the texture.
        glBindTexture(GL_TEXTURE_2D, 0);

        return textureObjectIds[0];
    }

    public static int loadTexture(Context context, Bitmap bitmap) {
        final int[] textureObjectIds = new int[1];
        glGenTextures(1, textureObjectIds, 0);

        if (textureObjectIds[0] == 0) {

            Log.w(TAG, "Could not generate a new OpenGL texture object. Error: " + getEGLErrorString(glGetError()));


            return 0;
        }

        if (bitmap == null) {

            Log.w(TAG, "Bitmap null");


            glDeleteTextures(1, textureObjectIds, 0);

            return 0;
        }

        // Bind to the texture in OpenGL
        glBindTexture(GL_TEXTURE_2D, textureObjectIds[0]);

        // Set filtering: a default must be set, or the texture will be
        // black.
        glTexParameteri(GL_TEXTURE_2D,
                GL_TEXTURE_MIN_FILTER,
                GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D,
                GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        // Load the bitmap into the bound texture.
        texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);


        // Note: Following code may cause an error to be reported in the
        // ADB log as follows: E/IMGSRV(20095): :0: HardwareMipGen:
        // Failed to generate texture mipmap levels (error=3)
        // No OpenGL error will be encountered (glGetError() will return
        // 0). If this happens, just squash the source image to be
        // square. It will look the same because of texture coordinates,
        // and mipmap generation will work.

        glGenerateMipmap(GL_TEXTURE_2D);

        // Recycle the bitmap, since its data has been loaded into
        // OpenGL.
        bitmap.recycle();

        // Unbind from the texture.
        glBindTexture(GL_TEXTURE_2D, 0);

        return textureObjectIds[0];
    }

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /**
     * Create a file Uri for saving an image or video
     */
    public static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    public static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Straggle");

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }


        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        String generatedString = randomstring();


        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + "_" + generatedString + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + "_" + generatedString + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


    public static String randomstring(int lo, int hi) {
        int n = rand(lo, hi);
        byte b[] = new byte[n];
        for (int i = 0; i < n; i++)
            b[i] = (byte) rand('a', 'z');
        return new String(b, 0);
    }

    private static int rand(int lo, int hi) {
        java.util.Random rn = new java.util.Random();
        int n = hi - lo + 1;
        int i = rn.nextInt(n);
        if (i < 0)
            i = -i;
        return lo + i;
    }

    //http://www.codecodex.com/wiki/Generate_a_random_password_or_random_string#Java
    public static String randomstring() {
        return randomstring(5, 25);
    }


    public static void saveContentProviderDataState(final Context c) {
        new Thread(new Runnable() {
            @Override
            public void run() {


                File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "Straggle");

                File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "contentprovider_db_list" + ".txt");


                try {
                    if (mediaFile.exists()) {
                        mediaFile.delete();
                    }

                    PrintWriter pWriter = new PrintWriter(mediaFile);


                    Cursor cursor = c.getContentResolver().query(ContentProviderDbSchema.ImageTextures.CONTENT_URI, null, null, null, null);

                    while (cursor.moveToNext()) {

                        String imageFileName = cursor.getString(cursor.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_FILENAME));
                        String lon = String.valueOf(cursor.getDouble(cursor.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_LON)));
                        String lat = String.valueOf(cursor.getDouble(cursor.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_LAT)));
                        String angle = String.valueOf(cursor.getDouble(cursor.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_ANGLE)));
                        String aspectRatio = String.valueOf(cursor.getInt(cursor.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_ASPECT_RATIO)));
                        String line = imageFileName + "," + lon + "," + lat + "," + angle + "," + aspectRatio;

                        //Log.d("saveContentProvider", line);

                        pWriter.println(line);

                        if (pWriter.checkError()) {
                            Log.d("saveContentProvider", " Error writing line!");
                        }
                    }

                    cursor.close();
                    pWriter.close();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        Intent mediaScanIntent = new Intent(
                                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri contentUri = Uri.fromFile(mediaFile);
                        mediaScanIntent.setData(contentUri);
                        c.sendBroadcast(mediaScanIntent);
                    } else {
                        c.sendBroadcast(new Intent(
                                Intent.ACTION_MEDIA_MOUNTED,
                                Uri.parse("file://"
                                        + Environment.getExternalStorageDirectory())));
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }


            }
        }).start();

    }

    public static void restoreContentProviderDataState(final Context c) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                c.getContentResolver().delete(ContentProviderDbSchema.ImageTextures.CONTENT_URI, null, null);

                File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "Straggle");

                File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "contentprovider_db_list" + ".txt");

                try {

                    String currentLine;

                    BufferedReader br = new BufferedReader(new FileReader(mediaFile));

                    while ((currentLine = br.readLine()) != null) {

                        //Log.d("restoreContentProvider", currentLine);

                        String[] tokens = currentLine.split(",");

                        Log.d("restoreContentProvider", tokens[0] + " " + tokens[1] + " " + tokens[2] + " " + tokens[3] + " " + tokens[4]);

                        ContentValues cv = new ContentValues();
                        cv.put(ContentProviderDbSchema.ImageTextures.COL_FILENAME, tokens[0]);
                        cv.put(ContentProviderDbSchema.ImageTextures.COL_LON, tokens[1]);
                        cv.put(ContentProviderDbSchema.ImageTextures.COL_LAT, tokens[2]);
                        cv.put(ContentProviderDbSchema.ImageTextures.COL_ANGLE, tokens[3]);
                        cv.put(ContentProviderDbSchema.ImageTextures.COL_ASPECT_RATIO, tokens[4]);
                        cv.put(ContentProviderDbSchema.ImageTextures.COL_USER_ID, ContentProviderOpenHelper.DEFAULT_USER_ID);

                        c.getContentResolver().insert(ContentProviderDbSchema.ImageTextures.CONTENT_URI, cv);
                    }

                } catch (FileNotFoundException e) {

                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }).start();

    }

    public static void setImageTextureLocation(Context c, String fileStr, Location loc) {

        HashSet<String> hashSet = new HashSet<>(2);
        hashSet.add(String.valueOf(loc.getLatitude()));
        hashSet.add(String.valueOf(loc.getLongitude()));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        prefs.edit().putStringSet(fileStr, hashSet).apply();
    }

    public static Location getImageTextureLocation(Context c, String fileStr) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        HashSet<String> dataSet = (HashSet<String>) prefs.getStringSet(fileStr, null);

        Location loc = new Location(LocationManager.GPS_PROVIDER);


        Iterator itr = dataSet.iterator();
        while (itr.hasNext()) {
            loc.setLatitude(Double.valueOf((String) itr.next()));
            loc.setLongitude(Double.valueOf((String) itr.next()));
        }

        return loc;
    }


    public static boolean deleteDirectory(File path) {

        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }
}