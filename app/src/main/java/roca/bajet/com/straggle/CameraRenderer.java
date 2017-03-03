package roca.bajet.com.straggle;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

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

    public float mHorizontalViewAngle;
    public Location mCameraLocation;

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
        void onDebugString(String str);
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
            Log.d("CameraRenderer", "mAccelerometer is null");
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

        /*
        Location location = new Location(LocationManager.GPS_PROVIDER);

        //Garage
        setCoordinates(location, 37.390980, -121.877402);

        //mImageTexture = new ImageTexture(R.drawable.frame1_1, mContext);
        mImageTextures.add(new ImageTexture(R.drawable.measure, location, mContext));
        */

        File directory = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Straggle");
        File[] files = directory.listFiles();

        if(files != null)
        {
            Log.d("Files", "Size: "+ files.length);
            for (int i = 0; i < files.length; i++)
            {
                Log.d("Files", "FileName: " + files[i].getName());
                //mImageTextures.add(new ImageTexture(files[i].getAbsolutePath(), TextureHelper.getImageTextureLocation(mContext,files[i].getAbsolutePath()),mContext ));

            }
        }


        /*
        //Parent's bedroom
        location = new Location(LocationManager.GPS_PROVIDER);
        setCoordinates(location, 37.390817, -121.877254);
        mImageTextures.add(new ImageTexture(R.drawable.frame2_3, location, mContext));

        //Kitchen
        location = new Location(LocationManager.GPS_PROVIDER);
        setCoordinates(location, 37.390880, -121.8771781);
        mImageTextures.add(new ImageTexture(R.drawable.frame3_4, location, mContext));


        //My room
        location = new Location(LocationManager.GPS_PROVIDER);
        setCoordinates(location, 37.390839, -121.877212);
        mImageTextures.add(new ImageTexture(R.drawable.frame3_5, location, mContext));
        //mImageTextures.add(new ImageTexture(R.drawable.frame4_5, mContext));
        //mImageTextures.add(new ImageTexture(R.drawable.frame9_16, mContext));
        */
        /*
        float rotate = 0f;
        for(ImageTexture it : mImageTextures) {

            it.mCameraRotationAngle = (rotate * 60 + it.mCameraRotationAngle) - 180;
            rotate++;

            Random r = new Random();
            it.moveFromToCamera(-r.nextFloat()*6 + it.mTranslationValuesXYZ[2]);
        }
        */


        Log.d(LOG_TAG, "onSurfaceCreated end");
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        Log.d(LOG_TAG, "onSurfaceChanged begin");

        // Set the OpenGL viewport to fill the entire surface.
        glViewport(0, 0, width, height);

        perspectiveM(projectionMatrix, 0, 40.3f, (float) width/height, 0f, 10000f);

        setIdentityM(cameraTranslateMatrix, 0);
        translateM(cameraTranslateMatrix, 0, 0, 0, -2);

        Log.d(LOG_TAG, "onSurfaceChanged end");
    }

    @Override
    public void onDrawFrame(GL10 gl10) {

        //Log.d(LOG_TAG, "onDrawFrame");

        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT);

        if(mCameraLocation == null)
        {
            return;
        }

        /*
        setLookAtM(viewMatrix, 0, viewValues[0], viewValues[1], viewValues[2]
                , viewValues[3], viewValues[4], viewValues[5]
                , viewValues[6], viewValues[7], viewValues[8]);
                */

        multiplyMM(viewMatrix, 0, rotationMatrix, 0, modelSensorMatrix, 0);
        // Multiply the view and projection matrices together.
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);



        for(ImageTexture it : mImageTextures) {

            //double distance = 3;
            //double ItAngle = 301.5f;

            if (it == null)
            {
                return;
            }

            double distance = mCameraLocation.distanceTo(it.mLocation);
            double ItAngle = mCameraLocation.bearingTo(it.mLocation);


            String debugStr = "Camera: " +  String.format("%5.7f",mCameraLocation.getLatitude()) + ", " + String.format("%5.7f",mCameraLocation.getLongitude())
                    + "\nAccuracy: " + String.format("%5.7f",mCameraLocation.getAccuracy()) + ", Z: " +  String.format("%3.1f",orientation[0]);
                    //+ "\nImage: " + String.format("%5.7f",it.mLocation.getLatitude()) + ", " + String.format("%5.7f",it.mLocation.getLongitude())
                    //+ "\nDistance: " + String.format("%5.5f", distance) + ", ItAngle: " + String.format("%3.7f", ItAngle);
            if (mOrientationCallback != null)
            {
                mOrientationCallback.onDebugString(debugStr);
            }

            it.rotateAroundCamera((float) -ItAngle);


            it.moveFromToCamera((float)-distance * 0.79f);

            float[] modelMatrix = it.calculateModelMatrix();
            multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
            mTextureShaderProgram.useProgram();
            mTextureShaderProgram.setUniforms(modelViewProjectionMatrix, it.mTextureId, it.opacity);//every object sets its own mvpMatrix, texture, and alpha values
            it.bindData(mTextureShaderProgram);
            it.draw();
        }
    }

    public double calculateBearing(Location loc1, Location loc2)
    {
        //ATAN2(COS(lat1)*SIN(lat2)-SIN(lat1)*COS(lat2)*COS(lon2-lon1), SIN(lon2-lon1)*COS(lat2))
        //y = Math.sin(λ2-λ1) * Math.cos(φ2);\
        //x = Math.cos(φ1)*Math.sin(φ2) - Math.sin(φ1)*Math.cos(φ2)*Math.cos(λ2-λ1);
        //brng = Math.atan2(y, x).toDegrees();
        double loc1Lon = Math.toRadians(loc1.getLongitude());
        double loc1Lat = Math.toRadians(loc1.getLatitude());

        double loc2Lon = Math.toRadians(loc2.getLongitude());
        double loc2Lat = Math.toRadians(loc2.getLatitude());
        double deltaLat = loc2Lat - loc1Lat;

        double y = Math.sin(deltaLat) * Math.cos(loc2Lon);
        double x = Math.cos(loc1Lon) * Math.sin(loc2Lon) - Math.sin(loc1Lon)*Math.cos(loc2Lon)*Math.cos(deltaLat);

        return (Math.toDegrees(Math.atan2(y,x))+360)%360;


    }

    public void setCoordinates(Location loc, double lat, double lon)
    {
        loc.setLatitude(lat);
        loc.setLongitude(lon);
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

            float [] rotateOrientation = new float[3];

            SensorManager.getOrientation(rotationMatrix,rotateOrientation);
            /*
            orientation[0] = (float)Math.toDegrees(2*Math.asin(sensorEvent.values[0]));//pitch x
            orientation[1] = (float)Math.toDegrees(2*Math.asin(sensorEvent.values[1]));//roll y
            orientation[2] = (float)Math.toDegrees(2*Math.asin(sensorEvent.values[2]));//azimuth z
            */
            orientation[0] = (float)Math.toDegrees(2*Math.asin(sensorEvent.values[0]));//pitch x
            orientation[1] = (float)Math.toDegrees(2*Math.asin(sensorEvent.values[1]));//roll y
            orientation[2] = (float)Math.toDegrees(2*Math.asin(sensorEvent.values[2]));//azimuth z

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
