package roca.bajet.com.straggle;


import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.location.Location;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.util.HashSet;

import butterknife.BindView;
import butterknife.ButterKnife;
import roca.bajet.com.straggle.data.ContentProviderDbSchema.ImageTextures;
import roca.bajet.com.straggle.data.ContentProviderOpenHelper;
import roca.bajet.com.straggle.objects.ImageTexture;


/**
 * A simple {@link Fragment} subclass.
 */
public class CameraFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        LoaderManager.LoaderCallbacks<Cursor>{

    private final String LOG_TAG = getClass().getSimpleName();
    private static final int ACCURACY_RADIUS = 7; //in meters
    private static final int TIME_INTERVAL_MINUTES = 1000 * 60 * 2;
    public static final int PERMISSION_REQUEST_CODE = 123;
    private static final int LOCATION_REQUEST_INTERVAL = 10000;
    private static final int LOCATION_REQUEST_FASTEST_INTERVAL = 5000;
    private static final float SEARCH_RADIUS = 145f; //in meters
    private static final int IMAGESEARCH_LOADER = 0;

    @BindView(R.id.gl_surfaceview) public GLSurfaceView mGLSurfaceView;
    //@BindView(R.id.debug_textview) public TextView mDebugTextView;
    @BindView(R.id.take_picture_button) public ImageButton mTakePicButton;

    private String debugText;
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
    public float mCameraAzimuth;
    public int mOrientationDeg = 0;
    public HashSet<String> mITset;


    public static final String [] IMAGESEARCH_COLUMNS = {
            ImageTextures.COL_FILENAME,
            ImageTextures.COL_LAT,
            ImageTextures.COL_LON,
            ImageTextures.COL_ANGLE
    };


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
        //mGLSurfaceView.setZOrderOnTop(true);
        mGLSurfaceView.setRenderer(mCameraRenderer);
        //mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        Log.d(LOG_TAG, "onCreateView, ContentProviderOpenHelper.DEFAULT_USER_ID = " + ContentProviderOpenHelper.DEFAULT_USER_ID);

        mITset = new HashSet<>();
        mCameraRenderer.setOnOrientationCallback(new CameraRenderer.OrientationCallback() {
             @Override
             public void onOrientationChange(int orientation) {
                 /*
                 String x = String.format("%+3.2f", orientation[1]);
                 String y = String.format("%+3.2f", orientation[2]);
                 String z = String.format("%+3.2f", orientation[3]);
                 String w = String.format("%+3.2f", orientation[0]);


                 String txt = "X = " + x + ", Y = " +  y +  ", Z = " +  z + ", W = " +  w;;
                 mDebugTextView.setText(txt);
                 */

                 if (orientation >= 0)
                 {
                     mOrientationDeg = orientation;
                 }

                 //mDebugTextView.setText("Vector Orientation to phone: " + mOrientationDeg);
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

             @Override
             public void onAzimuthOrientationChange(double orientation) {
                 mCameraAzimuth = (float)orientation;
                 //mDebugTextView.setText("azimuth: " + orientation);
             }

             @Override
             public void onAccelerometerChange(float[] data) {
                 String x = String.format("%+3.0f", data[0]);
                 String y = String.format("%+3.0f", data[1]);
                 String z = String.format("%+3.0f", data[2]);

                 String txt = "X = " + x + ", Y = " +  y +  ", Z = " +  z;
                 //mDebugTextView.setText(txt);

             }
         }
        );


        if (checkCameraHardware(getContext())) {
            mCamera = getCameraInstance();
            mCameraPreview = new CameraPreview(getContext(), mCamera);
            setCameraDisplayOrientation(getActivity(), 0, mCamera);
            FrameLayout preview = (FrameLayout) rootView.findViewById(R.id.camera_preview);

            preview.addView(mCameraPreview);
            preview.removeView(mTakePicButton);
            preview.addView(mTakePicButton);



            mTakePicButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    /*

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

                                final Location currentLocation = new Location(mCurrentLocation);

                                mGLSurfaceView.queueEvent(new Runnable() {
                                    @Override
                                    public void run() {
                                        ImageTexture im = new ImageTexture(pictureFile.getAbsolutePath(), currentLocation, mContext);
                                        //im.rotateAroundCamera(-mCameraAzimuth[2]+180);
                                        im.mFilename = pictureFile.getName();
                                        im.rotateAroundCamera(mCameraAzimuth);
                                       mCameraRenderer.mImageTextures.add(im);
                                    }
                                });

                                mITset.add(pictureFile.getName());


                                ContentValues cv = new ContentValues();
                                cv.put(ImageTextures.COL_FILENAME, pictureFile.getName());
                                cv.put(ImageTextures.COL_LAT, currentLocation.getLatitude());
                                cv.put(ImageTextures.COL_LON, currentLocation.getLongitude());
                                cv.put(ImageTextures.COL_USER_ID, ContentProviderOpenHelper.DEFAULT_USER_ID);
                                cv.put(ImageTextures.COL_ANGLE, mCameraAzimuth);
                                Uri imageTexturesUri = ImageTextures.CONTENT_URI;
                                mContext.getContentResolver().insert(imageTexturesUri,cv);

                                //TextureHelper.setImageTextureLocation(mContext, pictureFile.getAbsolutePath(), currentLocation);


                            } catch (FileNotFoundException e) {
                                Log.d(LOG_TAG, "File not found: " + e.getMessage());
                            } catch (IOException e) {
                                Log.d(LOG_TAG, "Error accessing file: " + e.getMessage());
                            }


                            camera.startPreview();
                        }
                    });

                    */
                }
            });

            mCameraPreview.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {

                    /*
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

                    int deleteCount = mContext.getContentResolver().delete(ImageTextures.CONTENT_URI,null,null);
                    Log.d(LOG_TAG, "Contentresolver delete:  " + deleteCount);

                    mGLSurfaceView.queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            mCameraRenderer.mImageTextures.clear();
                        }
                    });

                    mITset.clear();

                    */
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
    public void onActivityCreated(Bundle savedInstanceState) {


        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {

        mCameraRenderer.startReadingSensor();

        if (mCamera == null && checkCameraHardware(mContext)) {
            mCamera = getCameraInstance();
            setCameraDisplayOrientation(getActivity(), 0, mCamera);
            mCameraPreview.mCamera = mCamera;
        }

        super.onResume();
    }

    @Override
    public void onPause() {

        if (mGoogleApiClient.isConnected())
        {
            stopLocationUpdates();
        }


        super.onPause();

        mCameraRenderer.stopReadingSensor();
        mIsLocationGranted = false;

        Log.d(LOG_TAG, "onPause");
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }


    }

    @Override
    public void onStart() {

        if (mGoogleApiClient != null) {
            Log.d(LOG_TAG, "onStart, Google API Client connect");
            mGoogleApiClient.connect();
        }

        super.onStart();
    }

    @Override
    public void onStop() {


        if (mGoogleApiClient != null) {
            Log.d(LOG_TAG, "onStop, Google API Client disconnect");

            mGoogleApiClient.disconnect();
        }


        super.onStop();
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
    public static boolean checkCameraHardware(Context context) {
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
    public static boolean isBetterLocation(Location location, Location currentBestLocation) {
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


        if (isMoreAccurate && isNewer) {
            return true;
        }


        /*
        if (isMoreAccurate) {
            return true;
        }
        else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        */


        return false;
    }

    /** Checks whether two providers are the same */
    public static boolean isSameProvider(String provider1, String provider2) {
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

    long prevtime = System.currentTimeMillis();
    @Override
    public void onLocationChanged(Location location) {

        if ( isBetterLocation(location, mCurrentLocation))
        {

            mCurrentLocation = location;
            mCurrentLocation.getTime();
            long currentTime = System.currentTimeMillis();

            /*
            mDebugTextView.setText("new mCurrentLocation: " + String.format("%3.7f",mCurrentLocation.getLatitude())
                    + ", " + String.format("%3.7f",mCurrentLocation.getLongitude())
                    + "\nAccuracy: " + String.format("%3.7f",mCurrentLocation.getAccuracy())
                    + "\nTime elapsed: " +  String.valueOf((double)(currentTime - prevtime)/1000) );
            */
            prevtime = currentTime;

            mCameraRenderer.mNewCameraLocation.add(mCurrentLocation);

            //File imageFile = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+ File.separator + "Straggle", filename);
            //ImageTexture im = new ImageTexture(imageFile.getAbsolutePath(), mCurrentLocation, mContext);
            getLoaderManager().restartLoader(IMAGESEARCH_LOADER, null, this);
        }

        //mCurrentLocation = location;

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

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
    public Loader onCreateLoader(int id, Bundle args) {

        CursorLoader cursorLoader = null;

        if (id == IMAGESEARCH_LOADER)
        {
            Location currentLocation = new Location(mCurrentLocation);


            double currentLat = currentLocation.getLatitude();
            double currentLon = currentLocation.getLongitude();

            /*
            ((<lat> - LAT_COLUMN) * (<lat> - LAT_COLUMN) +
            (<lng> - LNG_COLUMN) * (<lng> - LNG_COLUMN) * <fudge>)
             */

            double fudge = Math.pow(Math.cos(Math.toRadians(currentLat)),2);
            Uri imageUri = ImageTextures.buildImageTextureUriWithUserId(ContentProviderOpenHelper.DEFAULT_USER_ID);
            String orderby = "( (" + currentLat + " - " + ImageTextures.COL_LAT + ") * (" + currentLat + " - " + ImageTextures.COL_LAT + ") + (" +
                    currentLon + " - " + ImageTextures.COL_LON + ") * (" + currentLon + " - " + ImageTextures.COL_LON + ") * " + fudge + " ) " +
                    " ASC ";
            //Location.distanceBetween(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude(),);

            Location wLoc = CameraRenderer.calculateDestinationLocation(currentLocation, 270, 1113.2f);
            Location eLoc = CameraRenderer.calculateDestinationLocation(currentLocation, 90, 1113.2f);

            double nLat = (currentLat + 0.01);//0.01 in degrees decimal is 1.1132 km Lat
            double sLat = (currentLat - 0.01);

            String selection = ImageTextures.COL_LON + " > " + wLoc.getLongitude() + " AND " +  ImageTextures.COL_LON + " < " + eLoc.getLongitude() +
                    " AND " +ImageTextures.COL_LAT + " > " + sLat + " AND " + ImageTextures.COL_LAT + " < " + nLat;


            Log.d(LOG_TAG, "onCreateLoader, selection: " + selection);
            Log.d(LOG_TAG, "onCreateLoader, orderby: " + orderby);

            cursorLoader = new CursorLoader(getActivity(),
                    imageUri,
                    IMAGESEARCH_COLUMNS,
                    selection,
                    null,
                    orderby);
        }

        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {


        final Location currentLocation = new Location(mCurrentLocation);

        double currentLat = currentLocation.getLatitude();
        double currentLon = currentLocation.getLongitude();
        double lat;
        double lon;
        float [] distance = new float [3];


        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Straggle");

        /*
        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg")
        */

        if (loader.getId() == IMAGESEARCH_LOADER)
        {
            Log.d(LOG_TAG, "onLoadFinished: count = " + data.getCount()) ;

            while(data.moveToNext())
            {


                lat = data.getDouble(data.getColumnIndex(ImageTextures.COL_LAT));
                lon = data.getDouble(data.getColumnIndex(ImageTextures.COL_LON));
                final float angle = (float)data.getDouble(data.getColumnIndex(ImageTextures.COL_ANGLE));

                Location.distanceBetween(currentLat, currentLon, lat, lon, distance);

                Log.d(LOG_TAG, "onLoadFinished: distance = " + distance[0]) ;

                if (distance[0] > SEARCH_RADIUS)
                {
                    Log.d(LOG_TAG, "onLoadFinished: distance > " + SEARCH_RADIUS) ;
                    break;
                }


                final String filename = data.getString(data.getColumnIndex(ImageTextures.COL_FILENAME));
                final File mediaFile = new File(mediaStorageDir.getPath() + File.separator + filename);


                if (!mITset.contains(filename))
                {
                    Log.d(LOG_TAG, "onLoadFinished: Adding to mItset, " + filename) ;

                    mGLSurfaceView.queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            ImageTexture im = new ImageTexture(mediaFile.getAbsolutePath(), currentLocation, mContext);
                            im.rotateAroundCamera(angle);
                            im.mFilename = filename;
                            mCameraRenderer.mImageTextures.add(im);
                        }
                    });

                    mITset.add(filename);
                }

            }


        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }
}
