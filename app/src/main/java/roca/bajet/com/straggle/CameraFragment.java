package roca.bajet.com.straggle;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import roca.bajet.com.straggle.objects.ImageTexture;
import roca.bajet.com.straggle.util.TextureHelper;

import static android.content.Context.SENSOR_SERVICE;


/**
 * A simple {@link Fragment} subclass.
 */
public class CameraFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, SensorEventListener {

    private final String LOG_TAG = getClass().getSimpleName();
    private static final int ACCURACY_RADIUS = 7; //in meters
    private static final int TIME_INTERVAL_MINUTES = 1000 * 60 * 2;
    public static final int PERMISSION_REQUEST_CODE = 123;
    private static final int ACCELEROMETER_RATE = 1000000;
    private static final int LOCATION_REQUEST_INTERVAL = 10000;
    private static final int LOCATION_REQUEST_FASTEST_INTERVAL = 5000;

    @BindView(R.id.gl_surfaceview) public GLSurfaceView mGLSurfaceView;
    @BindView(R.id.debug_textview) public TextView mDebugTextView;
    @BindView(R.id.take_picture_button) public ImageButton mTakePickButton;

    public CameraRenderer mCameraRenderer;
    public Camera mCamera;
    public CameraPreview mCameraPreview;
    public GoogleApiClient mGoogleApiClient;
    public Context mContext;
    public boolean mIsLocationGranted = false;
    public LocationRequest mLocationRequest;
    public Location mCurrentLocation;
    public Handler mHandler;
    public float [] mOrientation;
    public int mOrientationDeg = 0;

    
    public SensorManager mSensorManager;
    public Sensor mAccelerometer;



    public CameraFragment() {
        // Required empty public constructor
    }

    public static CameraFragment newInstance() {
        CameraFragment fragment = new CameraFragment();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_camera, container, false);

        ButterKnife.bind(this, rootView);

        mHandler = new Handler();
        mContext = getContext();

        mCameraRenderer = new CameraRenderer(getContext(), mGLSurfaceView);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mGLSurfaceView.setZOrderOnTop(true);
        mGLSurfaceView.setRenderer(mCameraRenderer);
        //mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mSensorManager =(SensorManager)mContext.getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mCameraRenderer.setOnOrientationCallback(new CameraRenderer.OrientationCallback() {
             @Override
             public void onOrientationChange(float[] orientation) {
                 mOrientation = orientation;

                 String x = String.format("%+3.2f", orientation[0]);
                 String y = String.format("%+3.2f", orientation[1]);
                 String z = String.format("%+3.2f", orientation[2]);

                 String txt = "X = " + x + ", Y = " +  y +  ", Z = " +  z;
                 //mDebugTextView.setText(txt);
             }
             @Override
             public void onDebugString(String str) {

                 final String fstr = str;
                 mHandler.post(new Runnable() {
                     @Override
                     public void run() {

                         //mDebugTextView.setText(fstr);

                         //Log.d(LOG_TAG, "Debug " + fstr);
                     }
                 });
             }
            }
        );


        if (checkCameraHardware(getContext())) {
            mCamera = getCameraInstance();
            mCameraPreview = new CameraPreview(getContext(), mCamera);
            setCameraDisplayOrientation(getActivity(), 0, mCamera);
            FrameLayout preview = (FrameLayout) rootView.findViewById(R.id.camera_preview);

            preview.addView(mCameraPreview);
            preview.removeView(mTakePickButton);
            preview.addView(mTakePickButton);

            mTakePickButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    mCamera.takePicture(null, null, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] bytes, Camera camera) {
                            final File pictureFile = TextureHelper.getOutputMediaFile(TextureHelper.MEDIA_TYPE_IMAGE);

                            if (pictureFile == null)
                            {
                                Log.d(LOG_TAG, "Error creating media file, check storage permissions... ");
                                return;
                            }

                            try {
                                FileOutputStream fos = new FileOutputStream(pictureFile);

                                DisplayMetrics displaymetrics = new DisplayMetrics();
                                Display dp = getActivity().getWindowManager().getDefaultDisplay();
                                dp.getMetrics(displaymetrics);

                                //float [] adjustedOrientation = adjustAccelOrientation(dp.getRotation(), mOrientation);


                                Bitmap scaledBitmap = TextureHelper.decodeSampledBitmapFromBytes(bytes, displaymetrics.widthPixels, displaymetrics.heightPixels);

                                //mDebugTextView.setText("Orientation rotation: " + mOrientationDeg);

                                Bitmap rotateBitmap = rotate(scaledBitmap, mOrientationDeg);

                                rotateBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);


                                //fos.write(bytes);
                                fos.close();

                                scaledBitmap.recycle();
                                rotateBitmap.recycle();


                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    Intent mediaScanIntent = new Intent(
                                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                    Uri contentUri = Uri.fromFile(pictureFile);
                                    mediaScanIntent.setData(contentUri);
                                    getActivity().sendBroadcast(mediaScanIntent);
                                } else {
                                    getActivity().sendBroadcast(new Intent(
                                            Intent.ACTION_MEDIA_MOUNTED,
                                            Uri.parse("file://"
                                                    + Environment.getExternalStorageDirectory())));
                                }

                                mGLSurfaceView.queueEvent(new Runnable() {
                                    @Override
                                    public void run() {
                                       mCameraRenderer.mImageTextures.add(new ImageTexture(pictureFile.getAbsolutePath(), mCurrentLocation, mContext));
                                    }
                                });


                                TextureHelper.setImageTextureLocation(mContext, pictureFile.getAbsolutePath(), mCurrentLocation);


                            } catch (FileNotFoundException e) {
                                Log.d(LOG_TAG, "File not found: " + e.getMessage());
                            } catch (IOException e) {
                                Log.d(LOG_TAG, "Error accessing file: " + e.getMessage());
                            }


                            camera.startPreview();
                        }
                    });
                }
            });

            mCameraPreview.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {

                    File directory = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES), "Straggle");
                    File[] files = directory.listFiles();
                    for(int i = 0; files != null && i < files.length; i++)
                    {
                        if(files[i].delete())
                        {
                            Log.d(LOG_TAG, "Successfully deleted: " + files[i].getAbsolutePath());
                        }
                        else{
                            Log.d(LOG_TAG, "Failed to delete: " + files[i].getAbsolutePath());
                        }
                    }

                    return false;
                }
            });

            float horizontalViewAngle = mCamera.getParameters().getHorizontalViewAngle();
            float verticalViewAngle = mCamera.getParameters().getVerticalViewAngle();

            Log.d(LOG_TAG, "Viewing Angles H = " + horizontalViewAngle + ", V = " + verticalViewAngle);

        }

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        return rootView;
    }

    @Override
    public void onResume() {
        mGoogleApiClient.connect();
        super.onResume();
        mCameraRenderer.startReadingSensor();
        mSensorManager.registerListener(this, mAccelerometer, ACCELEROMETER_RATE);

        if (mCamera == null) {
            mCamera = getCameraInstance();
            setCameraDisplayOrientation(getActivity(), 0, mCamera);
            mCameraPreview.mCamera = mCamera;
        }

    }

    @Override
    public void onPause() {
        stopLocationUpdates();
        mGoogleApiClient.disconnect();

        super.onPause();

        mCameraRenderer.stopReadingSensor();
        mSensorManager.unregisterListener(this);
        mIsLocationGranted = false;

        Log.d(LOG_TAG, "onPause");
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }


    }
    public static float[] adjustAccelOrientation(int displayRotation, float[] eventValues)
    {
        float[] adjustedValues = new float[3];

        final int axisSwap[][] = {
                {  1,  -1,  0,  1  },     // ROTATION_0
                {-1,  -1,  1,  0  },     // ROTATION_90
                {-1,    1,  0,  1  },     // ROTATION_180
                {  1,    1,  1,  0  }  }; // ROTATION_270

        final int[] as = axisSwap[displayRotation];
        adjustedValues[0]  =  (float)as[0] * eventValues[ as[2] ];
        adjustedValues[1]  =  (float)as[1] * eventValues[ as[3] ];
        adjustedValues[2]  =  eventValues[2];

        return adjustedValues;
    }



    public int getRotationFromAccel()
    {
        float x = Math.abs(mOrientation[0]);
        float y = Math.abs(mOrientation[1]);
        float z = Math.abs(mOrientation[2]);

        //Landscape
        if (x/(y+z) > 1f)
        {
            if (mOrientation[0] > 0)
            {
                return 0;
            }
            else{
                return 180;
            }
        }
        //Portrait
        else if (y/(x+z) > 1f)
        {
            if (mOrientation[1] > 0)
            {
                return 90;
            }
            else{
                //return 270;
                return 90; //Treat 270 degree rotation as upside down portrait
            }
        }

        return -1;
    }


    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }


    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    public void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        //mDebugTextView.setText("Current Rotation: " + degrees);

        camera.setDisplayOrientation(result);
    }


    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TIME_INTERVAL_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TIME_INTERVAL_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > ACCURACY_RADIUS;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        /*
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);

            return;
        }

        mIsLocationGranted = true;
        */

        //Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        createLocationRequest();

        startLocationUpdates();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        if ( isBetterLocation(location, mCurrentLocation))
        {
            mCurrentLocation = location;
        }

        //mCurrentLocation = location;

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        mCameraRenderer.mCameraLocation = mCurrentLocation;


        //Log.d(LOG_TAG, "onLocationChanged: " + String.format("%3.6f", latitude) + ", " + String.format("%3.6f", longitude));
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(LOCATION_REQUEST_INTERVAL);
        mLocationRequest.setFastestInterval(LOCATION_REQUEST_FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //mOrientationDeg;

            float x = Math.abs(sensorEvent.values[0]);
            float y = Math.abs(sensorEvent.values[1]);
            float z = Math.abs(sensorEvent.values[2]);

            /*
            mDebugTextView.setText("Accelerometer: " + String.format("%2.2f",sensorEvent.values[0])
                    + ", " + String.format("%2.2f",sensorEvent.values[1])
                    + ", " + String.format("%2.2f",sensorEvent.values[2])
                    + "\nx/y+z ratio: " + String.format("%4.2f",x/(y+z))
                    + "\ny/x+z ratio: " + String.format("%4.2f",y/(x+z)));
            */

            mOrientation = sensorEvent.values;
            //mDebugTextView.setText("Accelerometer: x:y" + String.format("%2.2f",x/y));



            /*
            DisplayMetrics displaymetrics = new DisplayMetrics();
            Display dp = getActivity().getWindowManager().getDefaultDisplay();
            dp.getMetrics(displaymetrics);


            float [] adjustedOrientation = adjustAccelOrientation(dp.getRotation(), mOrientation);
            mDebugTextView.setText("Accelerometer: " + String.format("%2.2f",adjustedOrientation[0])
                    + ", " + String.format("%3.2f",adjustedOrientation[1])
                    + ", " + String.format("%3.2f",adjustedOrientation[2]) );

            */

            int rotation = getRotationFromAccel();
            if (rotation >= 0)
            {
                mOrientationDeg = rotation;
            }
            mDebugTextView.setText("Rotation: " + mOrientationDeg);

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
