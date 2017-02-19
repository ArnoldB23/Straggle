package roca.bajet.com.straggle.objects;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;

import roca.bajet.com.straggle.programs.TextureShaderProgram;
import roca.bajet.com.straggle.util.TextureHelper;

import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;

/**
 * Created by Arnold on 2/8/2017.
 */

public class ImageTexture {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * VertexArray.BYTES_PER_FLOAT;
    public int mTextureId;
    private float [] mModelMatrix = new float[16];
    public float [] cameraTranslateMatrix = new float[16];
    public float mCameraRotationAngle;
    public Float[] mTranslationValuesXYZ = new Float [3];
    public Float[] mRotationValuesXYZ = new Float [3];
    public float opacity;

    private static float[] VERTEX_DATA;
    private VertexArray vertexArray;


    public ImageTexture(int resId, Context c) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(c.getResources(), resId, options);
        int aratioNum = TextureHelper.getBestAspectRatio(options);
        Log.d("ImageTexture", "Aspect ratio num : " + aratioNum);

        VERTEX_DATA = TextureHelper.getTextureVertexSet(aratioNum);
        vertexArray = new VertexArray(VERTEX_DATA);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        ((Activity) c).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        Bitmap bitmap = TextureHelper.decodeSampledBitmapFromResource(c.getResources(), resId, displaymetrics.widthPixels, displaymetrics.heightPixels);
        mTextureId = TextureHelper.loadTexture(c, bitmap);

        mTranslationValuesXYZ = new Float [] {0f, -1.9f, -2f};
        mRotationValuesXYZ = new Float [] {0f, 0f, 0f};
        mCameraRotationAngle = 155f;
        opacity = 0.8f;


        setIdentityM(cameraTranslateMatrix, 0);
        translateM(cameraTranslateMatrix, 0, 0, 0, -2);
    }

    public void bindData(TextureShaderProgram textureProgram) {
        vertexArray.setVertexAttribPointer(
                0,
                textureProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE);

        vertexArray.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT,
                textureProgram.getTextureCoordinatesAttributeLocation(),
                TEXTURE_COORDINATES_COMPONENT_COUNT,
                STRIDE);

    }

    public void draw() {
        glDrawArrays(GL_TRIANGLE_FAN, 0, 6);
    }


    public float [] calculateModelMatrix()
    {
        setIdentityM(mModelMatrix, 0);

        rotateM(mModelMatrix, 0, mCameraRotationAngle, 0, 1, 0); //rotate around camera
        multiplyMM(mModelMatrix, 0,  mModelMatrix, 0, cameraTranslateMatrix, 0);//translate camera z position
        translateM(mModelMatrix, 0, mTranslationValuesXYZ[0], mTranslationValuesXYZ[1], mTranslationValuesXYZ[2]);//translate object with respect to own axis

        rotateM(mModelMatrix, 0, mRotationValuesXYZ[0], 1, 0, 0);   //rotate around own x axis
        rotateM(mModelMatrix, 0, mRotationValuesXYZ[1], 0, 1, 0); //rotate around own y axis
        rotateM(mModelMatrix, 0, mRotationValuesXYZ[2], 0, 0, 1);   //rotate around own z axis

        return mModelMatrix;
    }

    public void moveFromToCamera(float distance)
    {
        mTranslationValuesXYZ[2] = distance;
    }

    public void rotateAroundCamera(float angle)
    {
        mCameraRotationAngle = angle;

    }

    //Order of values: x,y,z
    public void translateImageTexture(Float [] values) {

        for(int i = 0; i < mTranslationValuesXYZ.length; i++)
        {
            if (values[i] != null)
            {
                mTranslationValuesXYZ[i] = values[i];
            }
        }
    }

    //Rotate around own x,y,z axis
    //Order of values: x,y,z
    public void rotateImageTexture(Float [] values) {

        for(int i = 0; i < mRotationValuesXYZ.length; i++)
        {
            if (values[i] != null)
            {
                mRotationValuesXYZ[i] = values[i];
            }
        }
    }
}
