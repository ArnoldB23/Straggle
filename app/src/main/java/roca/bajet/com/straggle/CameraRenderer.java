package roca.bajet.com.straggle;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.opengl.GLSurfaceView;
import android.util.Log;

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
    private Float[] modelValues = new Float[6];
    private Float[] viewValues = new Float[9];

    public float mAzimuth;

    public float[] orientation = new float[3];


    private Location mCurrentCameraLocation;
    public Location mNewCameraLocation;
    private Location mTargetCameraLocation;

    private TextureShaderProgram mTextureShaderProgram;
    public ArrayList<ImageTexture> mImageTextures;

    private Context mContext;
    private SensorManager mSensorManager;
    private Sensor mRotationVectorSensor;

    private final int ROTATIONSENSORTYPE = Sensor.TYPE_ROTATION_VECTOR;
    private final int ACCELEROMETERTYPE = Sensor.TYPE_LINEAR_ACCELERATION;
    private OrientationCallback mOrientationCallback;
    private GLSurfaceView mGLSurfaceView;


    public interface OrientationCallback {
        void onOrientationChange(int orientation);

        void onAzimuthOrientationChange(double orientation);
    }

    public void setOnOrientationCallback(OrientationCallback callback) {

        mOrientationCallback = callback;
    }

    public CameraRenderer(Context c, GLSurfaceView gl) {
        mContext = c;
        mGLSurfaceView = gl;
        mSensorManager = (SensorManager) mContext.getSystemService(SENSOR_SERVICE);

        mRotationVectorSensor = mSensorManager.getDefaultSensor(ROTATIONSENSORTYPE);

        if (mRotationVectorSensor == null) {
            Log.d("CameraRenderer", "mAccelerometer is null");
        }

        setIdentityM(modelSensorMatrix, 0);
        rotateM(modelSensorMatrix, 0, 90, 1, 0, 0);

        mImageTextures = new ArrayList<>();


        //mCurrentCameraLocation = new Location(LocationManager.GPS_PROVIDER);
        //setCoordinates(mCurrentCameraLocation, 37.389332, -121.876170);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        Log.d(LOG_TAG, "onSurfaceCreated begin");

        glClearColor(0f, 0f, 0f, 0f);
        glEnable(GL_CULL_FACE);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        mTextureShaderProgram = new TextureShaderProgram(mContext);


        Log.d(LOG_TAG, "onSurfaceCreated end");
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        Log.d(LOG_TAG, "onSurfaceChanged begin");

        // Set the OpenGL viewport to fill the entire surface.
        glViewport(0, 0, width, height);

        perspectiveM(projectionMatrix, 0, 40.3f, (float) width / height, 0f, 10000f);

        setIdentityM(cameraTranslateMatrix, 0);
        translateM(cameraTranslateMatrix, 0, 0, 0, -2);

        Log.d(LOG_TAG, "onSurfaceChanged end");
    }

    private final int TOTALCAMERASTEPS = 60;
    private int mCameraStep = 0;

    public static Location calculateDestinationLocation(Location initLoc, float bearing, float distance) {

        double R = 6371e3f; //Radius of the Earth
        double delta = distance / R;

        bearing = (float) Math.toRadians(bearing);


        double lat1 = (float) Math.toRadians(initLoc.getLatitude());
        double lon1 = (float) Math.toRadians(initLoc.getLongitude());

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(delta) + Math.cos(lat1) * Math.sin(delta) * Math.cos(bearing));

        double lon2 = lon1 + Math.atan2(Math.sin(bearing) * Math.sin(delta) * Math.cos(lat1),
                Math.cos(delta) - Math.sin(lat1) * Math.sin(lat2));

        Location destinationLoc = new Location(LocationManager.GPS_PROVIDER);
        setCoordinates(destinationLoc, Math.toDegrees(lat2), Math.toDegrees(lon2));

        return destinationLoc;

    }

    @Override
    public void onDrawFrame(GL10 gl10) {

        //Log.d(LOG_TAG, "onDrawFrame");

        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT);

        Location drawCameraLocation;

        //Don't draw until camera location is known
        if (mCurrentCameraLocation == null && mNewCameraLocation == null) {
            //Log.d(LOG_TAG, "onDraw 1");
            return;
        } else if (mCurrentCameraLocation == null && mNewCameraLocation != null) {
            //Log.d(LOG_TAG, "onDraw 2");
            mCurrentCameraLocation = mNewCameraLocation;
            drawCameraLocation = mCurrentCameraLocation;
        } else if (mCurrentCameraLocation != null && mNewCameraLocation != null && mTargetCameraLocation == null) {
            //Log.d(LOG_TAG, "onDraw 3");

            float distance = mNewCameraLocation.distanceTo(mCurrentCameraLocation);

            ///Increment step to target location when new camera location
            // is greater than 1.5 meters from current location
            if (distance > 1.5f) {
                mTargetCameraLocation = mNewCameraLocation;
            }

            drawCameraLocation = mCurrentCameraLocation;
        }

        //Provide increment location (if necessary) for smooth changes in camera positions
        else if (mCurrentCameraLocation != null && mTargetCameraLocation != null) {
            //Log.d(LOG_TAG, "onDraw 4");
            float cameraDistance = mCurrentCameraLocation.distanceTo(mTargetCameraLocation);


            float x = (float) mCameraStep / TOTALCAMERASTEPS;
            //float y = (float)Math.pow(x,3)*(x*(6*x-15)+10);
            //float y = (float)Math.pow(x,2)*(3-2*x); //smoothstep
            float y = x;

            float incDistance = y * cameraDistance;
            float bearing = mCurrentCameraLocation.bearingTo(mTargetCameraLocation);

            bearing = (bearing + 360) % 360;

            drawCameraLocation = calculateDestinationLocation(mCurrentCameraLocation, bearing, incDistance);
            drawCameraLocation.setAccuracy(mTargetCameraLocation.getAccuracy());

            mCameraStep++;

            if (mCameraStep == TOTALCAMERASTEPS) {
                //mCurrentCameraLocation = mTargetCameraLocation;
                mCurrentCameraLocation = drawCameraLocation;
                mTargetCameraLocation = null;
                mCameraStep = 0;
            }
        } else {//Never should come to this
            Log.d(LOG_TAG, "onDraw 5");

            drawCameraLocation = mCurrentCameraLocation;
        }

        /*
        setLookAtM(viewMatrix, 0, viewValues[0], viewValues[1], viewValues[2]
                , viewValues[3], viewValues[4], viewValues[5]
                , viewValues[6], viewValues[7], viewValues[8]);
                */

        multiplyMM(viewMatrix, 0, rotationMatrix, 0, modelSensorMatrix, 0);
        // Multiply the view and projection matrices together.
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);


        //Process each image texture with drawCameraLocation
        for (ImageTexture it : mImageTextures) {


            if (it == null) {
                return;
            }

            double ItAngle;
            double distance = drawCameraLocation.distanceTo(it.mLocation);



            /*
            if (distance > (drawCameraLocation.getAccuracy() /0.68f))
            {
                ItAngle = (drawCameraLocation.bearingTo(it.mLocation) + 360) % 360;
                it.rotateAroundCamera((float) ItAngle );

                Log.d(LOG_TAG, "onDrawFrame: distance = " + distance + " > " + (drawCameraLocation.getAccuracy() /0.68f)  +  ", ItAngle = " +ItAngle  );
            }
            //Within accuracy radius
            else {
                distance = 4;
            }

            */

            ItAngle = (drawCameraLocation.bearingTo(it.mLocation) + 360) % 360;


            if (distance != 0 || ItAngle != 0.0f) {
                it.rotateAroundCamera((float) ItAngle);
                it.moveFromToCamera((float) -distance * 0.79f);
            }


            Log.d(LOG_TAG, "onDrawFrame: distance = " + distance + " > " + (drawCameraLocation.getAccuracy() / 0.68f) + ", ItAngle = " + ItAngle);
            Log.d(LOG_TAG, "onDrawFrame: drawCameraLocation = " + drawCameraLocation.getLatitude() + ", " + drawCameraLocation.getLongitude()
                    + "   " + it.mLocation.getLatitude() + ", " + it.mLocation.getLongitude());


            String debugStr = "Camera: " + String.format("%5.6f", drawCameraLocation.getLatitude()) + ", " + String.format("%5.6f", drawCameraLocation.getLongitude())
                    + "\nAccuracy: " + String.format("%5.7f", drawCameraLocation.getAccuracy()) + ", Z: " + String.format("%3.1f", mAzimuth)
                    //+ "\nImage: " + String.format("%5.7f",it.mLocation.getLatitude()) + ", " + String.format("%5.7f",it.mLocation.getLongitude())
                    + "\nDistance: " + String.format("%5.5f", distance) + ", ItAngle: " + String.format("%3.7f", it.mCameraRotationAngle);


            float[] modelMatrix = it.calculateModelMatrix();
            multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
            mTextureShaderProgram.useProgram();
            mTextureShaderProgram.setUniforms(modelViewProjectionMatrix, it.mTextureId, it.opacity);//every object sets its own mvpMatrix, texture, and alpha values
            it.bindData(mTextureShaderProgram);
            it.draw();
        }
    }


    public static void setCoordinates(Location loc, double lat, double lon) {
        loc.setLatitude(lat);
        loc.setLongitude(lon);
    }


    public void stopReadingSensor() {
        mSensorManager.unregisterListener(this);


    }

    public void startReadingSensor() {
        // 10 ms updates.
        mSensorManager.registerListener(this, mRotationVectorSensor, 10000);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == ROTATIONSENSORTYPE) {
            // convert the rotation-vector to a 4x4 matrix. the matrix
            // is interpreted by Open GL as the inverse of the
            // rotation-vector, which is what we want.

            SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values);


            float[] rotateOrientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, rotateOrientation);


            float[] quaternion = new float[4];
            SensorManager.getQuaternionFromVector(quaternion, sensorEvent.values);


            float[] baseAzimuthVector = new float[]{0, 0, 0, -1}; //w,x,y,z
            float[] northVector = new float[]{0, 0, 1, 0};
            float[] h = new float[]{sensorEvent.values[3], sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]};
            float[] hprime = new float[]{h[0], -h[1], -h[2], -h[3]};

            float[] rotationAzimuthVector = quatmultiply(quatmultiply(h, baseAzimuthVector), hprime);
            float[] normRotationVector = normalizeVector(rotationAzimuthVector);
            normRotationVector[3] = 0;

            float angle = getAngleBetweenVectors(northVector, normRotationVector);
            angle *= Math.signum(rotationAzimuthVector[1]);


            float[] baseYCameraOrientationVector = new float[]{0, 0, 1, 0}; //w,x,y,z
            float[] rotationYCameraOrientationVector = quatmultiply(quatmultiply(h, baseYCameraOrientationVector), hprime);
            float[] baseXCameraOrientationVector = new float[]{0, 1, 0, 0}; //w,x,y,z
            float[] rotationXCameraOrientationVector = quatmultiply(quatmultiply(h, baseXCameraOrientationVector), hprime);


            if (mOrientationCallback != null) {
                mAzimuth = (float) ((Math.toDegrees(angle) + 360) % 360);
                //azimuth = Math.toDegrees(angle);
                mOrientationCallback.onAzimuthOrientationChange(mAzimuth);
                mOrientationCallback.onOrientationChange(getOrientationFromVectors(rotationXCameraOrientationVector[3], rotationYCameraOrientationVector[3]));
            }

            //mGLSurfaceView.requestRender();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public static int getOrientationFromVectors(float xZVector, float yZVector) {
        //landscape
        if (Math.abs(xZVector) > 0.75f) {
            if (xZVector > 0) {
                return 0;
            } else {
                return 180;
            }
        }
        //portrait
        else if (Math.abs(yZVector) > 0.75f) {
            if (yZVector > 0) {
                return 90;
            } else {
                //return 270;
                return 90; //Treat 270 degree rotation as upside down portrait
            }
        }


        return -1;
    }


    public static float getAngleBetweenVectors(float[] u, float[] v) {
        return (float) Math.acos((u[0] * v[0] + u[1] * v[1] + u[2] * v[2] + u[3] * v[3]) / (getVectorMag(u) * getVectorMag(v)));
    }

    public static float[] normalizeVector(float[] vector) {
        float mag = getVectorMag(vector);
        return new float[]{vector[0] / mag, vector[1] / mag, vector[2] / mag, vector[3] / mag};
    }

    public static float getVectorMag(float[] vector) {
        return (float) Math.sqrt(Math.pow(vector[0], 2) + Math.pow(vector[1], 2) + Math.pow(vector[2], 2) + Math.pow(vector[3], 2));
    }

    public static float[] quatmultiply(float[] q, float[] r) {
        float[] n = new float[4];
        n[0] = r[0] * q[0] - r[1] * q[1] - r[2] * q[2] - r[3] * q[3];
        n[1] = r[0] * q[1] + r[1] * q[0] - r[2] * q[3] + r[3] * q[2];
        n[2] = r[0] * q[2] + r[1] * q[3] + r[2] * q[0] - r[3] * q[1];
        n[3] = r[0] * q[3] - r[1] * q[2] + r[2] * q[1] + r[3] * q[0];

        return n;
    }
}
