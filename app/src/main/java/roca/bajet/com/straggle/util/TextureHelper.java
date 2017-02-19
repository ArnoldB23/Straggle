package roca.bajet.com.straggle.util;

/**
 * Created by Arnold on 2/8/2017.
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LINEAR_MIPMAP_LINEAR;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGenerateMipmap;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLUtils.texImage2D;

public class TextureHelper {
    private static final String TAG = "TextureHelper";

    public static final int ARATIO_1X1 = 11;
    public static final int ARATIO_2X3 = 23;
    public static final int ARATIO_3X5 = 35;
    public static final int ARATIO_3X4 = 34;
    public static final int ARATIO_4X5 = 45;
    public static final int ARATIO_5X7 = 57;
    public static final int ARATIO_9X16 = 916;



    public static int getBestAspectRatio(BitmapFactory.Options options)
    {
        int width = options.outWidth;
        int height = options.outHeight;
        float aspectRatio = (float)width/height;

        float bestFitError = 1000;
        int bestFitAspectRatio = 0;

        if (Math.abs(aspectRatio - 1f) < bestFitError)
        {
            bestFitError = Math.abs(aspectRatio - 1f);
            bestFitAspectRatio = ARATIO_1X1;
        }

        if (Math.abs(aspectRatio - 2/3f) < bestFitError)
        {
            bestFitError = Math.abs(aspectRatio - 2/3f);
            bestFitAspectRatio = ARATIO_2X3;
        }

        if (Math.abs(aspectRatio - 3/5f) < bestFitError)
        {
            bestFitError = Math.abs(aspectRatio - 3/5f);
            bestFitAspectRatio = ARATIO_3X5;
        }

        if (Math.abs(aspectRatio - 3/4f) < bestFitError)
        {
            bestFitError = Math.abs(aspectRatio - 3/4f);
            bestFitAspectRatio = ARATIO_3X4;
        }

        if (Math.abs(aspectRatio - 4/5f) < bestFitError)
        {
            bestFitError = Math.abs(aspectRatio - 4/5f);
            bestFitAspectRatio = ARATIO_4X5;
        }

        if (Math.abs(aspectRatio - 5/7f) < bestFitError)
        {
            bestFitError = Math.abs(aspectRatio - 5/7f);
            bestFitAspectRatio = ARATIO_5X7;
        }

        if (Math.abs(aspectRatio - 9/16f) < bestFitError)
        {
            bestFitError = Math.abs(aspectRatio - 9/16f);
            bestFitAspectRatio = ARATIO_9X16;
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


    public static Bitmap scaleBitmap(int resId, Context context)
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), resId, options);
        int aspect_ratio = getBestAspectRatio(options);




        switch(aspect_ratio)
        {
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

    public static float [] getTextureVertexSet(int aspect_ratio)
    {
        switch(aspect_ratio)
        {
            case ARATIO_1X1:
                return new float [] {
                        // Order of coordinates: X, Y, S, T
                        0f,    0f,      0.5f, 0.5f,
                        -1.5f, -1.5f,   0f, 1f,
                        1.5f, -1.5f,    1f, 1f,
                        1.5f,  1.5f,    1f, 0f,
                        -1.5f,  1.5f,   0f, 0f,
                        -1.5f, -1.5f,    0f, 1f
                };

            case ARATIO_2X3:
                return new float [] {
                        // Order of coordinates: X, Y, S, T
                        0f,    0f,      0.5f, 0.5f,
                        -1f, -1.5f,   0f, 1f,
                        1f, -1.5f,    1f, 1f,
                        1f,  1.5f,    1f, 0f,
                        -1f,  1.5f,   0f, 0f,
                        -1f, -1.5f,    0f, 1f

                };

            case ARATIO_3X5:
                return new float [] {
                        // Order of coordinates: X, Y, S, T
                        0f,    0f,      0.5f, 0.5f,
                        -1.5f, -2.5f,   0f, 1f,
                        1.5f, -2.5f,    1f, 1f,
                        1.5f,  2.5f,    1f, 0f,
                        -1.5f,  2.5f,   0f, 0f,
                        -1.5f, -2.5f,    0f, 1f

                };

            case ARATIO_3X4:
                return new float [] {
                        // Order of coordinates: X, Y, S, T
                        0f,    0f,      0.5f, 0.5f,
                        -1.5f, -2f,   0f, 1f,
                        1.5f, -2f,    1f, 1f,
                        1.5f,  2f,    1f, 0f,
                        -1.5f,  2f,   0f, 0f,
                        -1.5f, -2f,    0f, 1f

                };

            case ARATIO_4X5:
                return new float [] {
                        // Order of coordinates: X, Y, S, T
                        0f,    0f,      0.5f, 0.5f,
                        -2f, -2.5f,   0f, 1f,
                        2f, -2.5f,    1f, 1f,
                        2f,  2.5f,    1f, 0f,
                        -2f,  2.5f,   0f, 0f,
                        -2f, -2.5f,    0f, 1f

                };

            case ARATIO_5X7:
                return new float [] {
                        // Order of coordinates: X, Y, S, T
                        0f,    0f,      0.5f, 0.5f,
                        -2.5f, -3.5f,   0f, 1f,
                        2.5f, -3.5f,    1f, 1f,
                        2.5f,  3.5f,    1f, 0f,
                        -2.5f,  3.5f,   0f, 0f,
                        -2.5f, -3.5f,    0f, 1f

                };

            case ARATIO_9X16:
                return new float [] {
                        // Order of coordinates: X, Y, S, T
                        0f,    0f,      0.5f, 0.5f,
                        -1.125f, -2f,   0f, 1f,
                        1.125f, -2f,    1f, 1f,
                        1.125f,  2f,    1f, 0f,
                        -1.125f,  2f,   0f, 0f,
                        -1.125f, -2f,    0f, 1f

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

            Log.w(TAG, "Could not generate a new OpenGL texture object.");


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
}