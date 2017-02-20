package roca.bajet.com.straggle;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import roca.bajet.com.straggle.objects.ImageTexture;
import roca.bajet.com.straggle.programs.TextureShaderProgram;

import static android.content.Context.SENSOR_SERVICE;
import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_SRC_ALPHA;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.perspectiveM;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.translateM;

/**
 * Created by Arnold on 2/8/2017.
 */

public class CameraRenderer implements GLSurfaceView.Renderer, SensorEventListener {

    public final static String LOG_TAG = "CameraRenderer";
    private final float[] modelViewProjectionMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] rotationMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] modelTranslateRotateMatrix = new float[16];
    private final float[] cameraTranslateMatrix = new float[16];
    private final float[] modelSensorMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private Float[] modelValues = new Float [6];
    private Float[] viewValues = new Float [9];

    public float[] orientation = new float[3];

    public ImageTexture mImageTexture;
    private TextureShaderProgram mTextureShaderProgram;
    public ArrayList<ImageTexture> mImageTextures;

    private Context mContext;
    private SensorManager mSensorManager;
    private Sensor mRotationVectorSensor;
    private final int SENSORTYPE = Sensor.TYPE_ROTATION_VECTOR;
    private OrientationCallback mOrientationCallback;
    private GLSurfaceView mGLSurfaceView;


    public interface OrientationCallback
    {
        void onOrientationChange(float [] orientation);
    }

    public void setOnOrientationCallback(OrientationCallback callback)
    {

        mOrientationCallback = callback;
    }

    public CameraRenderer (Context c, GLSurfaceView gl)
    {
        mContext = c;
        mGLSurfaceView = gl;
        mSensorManager =(SensorManager)mContext.getSystemService(SENSOR_SERVICE);

        mRotationVectorSensor = mSensorManager.getDefaultSensor(SENSORTYPE);

        if (mRotationVectorSensor == null)
        {
            Log.d("CameraRenderer", "mRotationVectorSensor is null");
        }

        setIdentityM(modelSensorMatrix, 0);
        rotateM(modelSensorMatrix, 0, 90, 1, 0, 0);

        mImageTextures = new ArrayList<>();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        Log.d(LOG_TAG, "onSurfaceCreated begin");

        glClearColor(0f, 0f, 0f, 0f);
        glEnable(GL_CULL_FACE);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        mTextureShaderProgram = new TextureShaderProgram(mContext);

        //mImageTexture = new ImageTexture(R.drawable.frame1_1, mContext);
        mImageTextures.add(new ImageTexture(R.drawable.frame1_1, mContext));
        mImageTextures.add(new ImageTexture(R.drawable.frame2_3, mContext));
        mImageTextures.add(new ImageTexture(R.drawable.frame3_4, mContext));
        mImageTextures.add(new ImageTexture(R.drawable.frame3_5, mContext));
        mImageTextures.add(new ImageTexture(R.drawable.frame4_5, mContext));
        mImageTextures.add(new ImageTexture(R.drawable.frame9_16, mContext));

        float rotate = 0f;
        for(ImageTexture it : mImageTextures) {

            it.mCameraRotationAngle = (rotate * 60 + it.mCameraRotationAngle) - 180;
            rotate++;

            Random r = new Random();
            it.moveFromToCamera(-r.nextFloat()*2 + it.mTranslationValuesXYZ[2]);
        }


        Log.d(LOG_TAG, "onSurfaceCreated end");
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        Log.d(LOG_TAG, "onSurfaceChanged begin");

        // Set the OpenGL viewport to fill the entire surface.
        glViewport(0, 0, width, height);

        perspectiveM(projectionMatrix, 0, 65, (float) width/height, 0f, 1000f);


        setIdentityM(cameraTranslateMatrix, 0);
        translateM(cameraTranslateMatrix, 0, 0, 0, -2);

        Log.d(LOG_TAG, "onSurfaceChanged end");
    }

    @Override
    public void onDrawFrame(GL10 gl10) {

        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT);


        /*
        setLookAtM(viewMatrix, 0, viewValues[0], viewValues[1], viewValues[2]
                , viewValues[3], viewValues[4], viewValues[5]
                , viewValues[6], viewValues[7], viewValues[8]);
                */

        multiplyMM(viewMatrix, 0, rotationMatrix, 0, modelSensorMatrix, 0);
        // Multiply the view and projection matrices together.
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        for(ImageTexture it : mImageTextures) {

            float[] modelMatrix = it.calculateModelMatrix();
            multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
            mTextureShaderProgram.useProgram();
            mTextureShaderProgram.setUniforms(modelViewProjectionMatrix, it.mTextureId, it.opacity);//every object sets its own mvpMatrix, texture, and alpha values
            it.bindData(mTextureShaderProgram);
            it.draw();
        }
    }


    public void stopReadingSensor()
    {
        mSensorManager.unregisterListener(this);
    }

    public void startReadingSensor()
    {
        // 10 ms updates.
        mSensorManager.registerListener(this, mRotationVectorSensor, 10000);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == SENSORTYPE) {
            // convert the rotation-vector to a 4x4 matrix. the matrix
            // is interpreted by Open GL as the inverse of the
            // rotation-vector, which is what we want.

            SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values);


            //SensorManager.getOrientation(rotationMatrix,orientation);
            //orientation[0] = (float)Math.toDegrees(orientation[0]);//azimuth z
            //orientation[1] = (float)Math.toDegrees(orientation[1]);//pitch x
            //orientation[2] = (float)Math.toDegrees(orientation[2]);//roll y

            if (mOrientationCallback != null)
            {
                mOrientationCallback.onOrientationChange(orientation);
            }


            String x = String.format("%+3.2f", sensorEvent.values[0]);
            String y = String.format("%+3.2f", sensorEvent.values[1]);
            String z = String.format("%+3.2f", sensorEvent.values[2]);
            String w = String.format("%+3.2f", sensorEvent.values[3]);
            String txt = "X = " + x + ", Y = " +  y +  ", Z = " +  z;

            //mGLSurfaceView.requestRender();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
