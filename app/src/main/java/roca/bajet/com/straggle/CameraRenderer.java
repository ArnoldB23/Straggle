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

    public int mCameraOrientation;

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
        void onAzimuthOrientationChange(double orientation);
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
            double ItAngle;

            if (it == null)
            {
                return;
            }

            double distance = mCameraLocation.distanceTo(it.mLocation);




            if (distance > mCameraLocation.getAccuracy() /0.68f)
            {
                ItAngle = mCameraLocation.bearingTo(it.mLocation);
                it.rotateAroundCamera((float) -ItAngle);
            }

            String debugStr = "Camera: " +  String.format("%5.7f",mCameraLocation.getLatitude()) + ", " + String.format("%5.7f",mCameraLocation.getLongitude())
                    + "\nAccuracy: " + String.format("%5.7f",mCameraLocation.getAccuracy()) + ", Z: " +  String.format("%3.1f",orientation[0]);
                    //+ "\nImage: " + String.format("%5.7f",it.mLocation.getLatitude()) + ", " + String.format("%5.7f",it.mLocation.getLongitude())
                    //+ "\nDistance: " + String.format("%5.5f", distance) + ", ItAngle: " + String.format("%3.7f", ItAngle);
            if (mOrientationCallback != null)
            {
                mOrientationCallback.onDebugString(debugStr);
            }




            it.moveFromToCamera((float)-distance * 0.79f);

            float[] modelMatrix = it.calculateModelMatrix();
            multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
            mTextureShaderProgram.useProgram();
            mTextureShaderProgram.setUniforms(modelViewProjectionMatrix, it.mTextureId, it.opacity);//every object sets its own mvpMatrix, texture, and alpha values
            it.bindData(mTextureShaderProgram);
            it.draw();
        }
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
            //orientation[2] = (float)Math.toDegrees(2*Math.asin(sensorEvent.values[2]));//azimuth z

            float a = sensorEvent.values[3];
            float b = sensorEvent.values[0];
            float c = sensorEvent.values[1];
            float d = sensorEvent.values[2];
            float [] quaternion = new float [4];
            SensorManager.getQuaternionFromVector(quaternion, sensorEvent.values);

            double azimuth = Math.atan2((double) (2*(a*d+b*c)),Math.pow(a,2)+Math.pow(b,2)-Math.pow(c,2)-Math.pow(d,2));

            float [] baseVector = new float [] {0,0,0,-1}; //w,x,y,z
            float [] northVector = new float [] {0,0,1,0};

            float [] h = new float [] {sensorEvent.values[3],sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2]};
            float [] hprime = new float [] {h[0], -h[1], -h[2], -h[3]};

            //quaternion_mult(quaternion_mult(q,r),q_conj)



            float [] rotationVector = quatmultiply(quatmultiply(h,baseVector),hprime);


            float [] normRotationVector = normalizeVector(rotationVector);
            normRotationVector[3] = 0;

            float angle = getAngleBetweenVectors(northVector, normRotationVector);
            angle *= Math.signum(rotationVector[1]);
            /*
            rotationVector[0] = (float)(2*Math.acos(sensorEvent.values[3])); //w
            rotationVector[1] = (float)(sensorEvent.values[0]/(Math.sin(rotationVector[0]/2)));
            rotationVector[2] = (float)(sensorEvent.values[1]/(Math.sin(rotationVector[0]/2)));
            rotationVector[3] = (float)(sensorEvent.values[2]/(Math.sin(rotationVector[0]/2)));
            rotationVector[0] = (float)Math.toDegrees(rotationVector[0]);
            */
            //Matrix.multiplyMV(rotationVector, 0, rotationMatrix, 0, baseVector, 0);




            if (mOrientationCallback != null)
            {
                azimuth = (int) ( Math.toDegrees( angle ) + 360 ) % 360;
                //azimuth = Math.toDegrees(angle);
                mOrientationCallback.onAzimuthOrientationChange(azimuth);
                mOrientationCallback.onOrientationChange(rotationVector);
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

    public float getAngleBetweenVectors(float [] u, float []v)
    {
        return (float)Math.acos((u[0]*v[0]+u[1]*v[1]+u[2]*v[2]+u[3]*v[3])/(getVectorMag(u)*getVectorMag(v)));
    }

    public float [] normalizeVector(float [] vector)
    {
        float mag = getVectorMag(vector);
        return new float [] {vector[0]/mag, vector[1]/mag, vector[2]/mag, vector[3]/mag };
    }

    public float getVectorMag(float [] vector)
    {
        return (float) Math.sqrt( Math.pow(vector[0],2)+Math.pow(vector[1],2)+Math.pow(vector[2],2)+Math.pow(vector[3],2) );
    }

    public float [] quatmultiply(float [] q, float []r)
    {
        float [] n = new float [4];
        n[0] = r[0]*q[0]-r[1]*q[1]-r[2]*q[2]-r[3]*q[3];
        n[1] = r[0]*q[1]+r[1]*q[0]-r[2]*q[3]+r[3]*q[2];
        n[2] = r[0]*q[2]+r[1]*q[3]+r[2]*q[0]-r[3]*q[1];
        n[3] = r[0]*q[3]-r[1]*q[2]+r[2]*q[1]+r[3]*q[0];

        return n;
    }
}
