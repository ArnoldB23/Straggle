package roca.bajet.com.straggle;


import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import roca.bajet.com.straggle.data.ContentProviderDbSchema;
import roca.bajet.com.straggle.data.ContentProviderDbSchema.ImageTextures;
import roca.bajet.com.straggle.data.ContentProviderOpenHelper;
import roca.bajet.com.straggle.dialog.LocationCheckDialog;
import roca.bajet.com.straggle.objects.ImageTexture;
import roca.bajet.com.straggle.upload.ApiUtils;
import roca.bajet.com.straggle.upload.DeleteImageResponse;
import roca.bajet.com.straggle.upload.ImgurService;
import roca.bajet.com.straggle.util.TextureHelper;


/**
 * A simple {@link Fragment} subclass.
 */
public class CameraFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        LoaderManager.LoaderCallbacks<Cursor>{

    private final String LOG_TAG = getClass().getSimpleName();

    private static final int ACCURACY_RADIUS_M = 7; //in meters
    private static final int TIME_INTERVAL_MS = 1000 * 60 * 2;
    private static final int LOCATION_REQUEST_INTERVAL_MS = 1000;
    private static final int LOCATION_REQUEST_FASTEST_INTERVAL_MS = 100;
    private static final float SEARCH_RADIUS_M = 145f; //in meters

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 12;
    public static final int CAMERA_PERMISSION_REQUEST_CODE = 13;
    public static final int WRITE_STORAGE_PERMISSION_REQUEST_CODE = 14;
    public static final int NEEDED_PERMISSION_REQUEST_CODE = 15;
    private static final int IMAGESEARCH_LOADER = 0;

    public boolean mRequestingLocationUpdates = true;
    public boolean mIsCameraPermissionGranted;
    public boolean mIsStoragePermissionGranted;
    public boolean mIsLocationPermissionGranted;
    public boolean mIsLocationEnabled;
    public boolean mIsTablet;

    private LocationCallback mLocationCallback;
    private LocationCheckDialog mLocationCheckDialog;
    private PendingResult<LocationSettingsResult> mLocationSettingsResultPendingResult;

    @BindView(R.id.gl_surfaceview) public GLSurfaceView mGLSurfaceView;
    //@BindView(R.id.debug_textview) public TextView mDebugTextView;
    @Nullable @BindView(R.id.take_picture_button) public ImageButton mTakePicButton;
    @Nullable @BindView (R.id.take_picture_frame_button) public RatioTabletFrameLayout mRatioTabletFrameLayout;
    @BindView(R.id.camera_framelayout) public FrameLayout mFrameLayout;

    private String debugText;
    public CameraRenderer mCameraRenderer;
    public Camera mCamera;
    public CameraPreview mCameraPreview;
    public GoogleApiClient mGoogleApiClient;
    public Context mContext;

    public LocationRequest mLocationRequest;
    public Location mCurrentLocation;
    public Handler mHandler;
    public float [] mOrientation;
    public float mCameraAzimuth;
    public int mOrientationDeg = 0;
    public HashSet<String> mITset;
    public ImgurService mImgurService;


    public static final String [] IMAGESEARCH_COLUMNS = {
            ImageTextures.COL_FILENAME,
            ImageTextures.COL_LAT,
            ImageTextures.COL_LON,
            ImageTextures.COL_ANGLE,
            ImageTextures.COL_DELETE_HASH,
            ImageTextures._ID
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

        mIsTablet = mRatioTabletFrameLayout != null && mRatioTabletFrameLayout.getVisibility() == View.VISIBLE;

        mImgurService = ApiUtils.getImgurService();
        mHandler = new Handler();
        mContext = getContext();

        mCameraRenderer = new CameraRenderer(getContext(), mGLSurfaceView);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        //mGLSurfaceView.setZOrderOnTop(true);
        mGLSurfaceView.setRenderer(mCameraRenderer);
        //mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mLocationCheckDialog = new LocationCheckDialog();

        mLocationCheckDialog.setOnLocationCheckDialogCallback(new LocationCheckDialog.LocationCheckDialogCallback() {
            @Override
            public void onPositiveButton() {

            }

            @Override
            public void onNegativeButton() {

            }

            @Override
            public void onCancelled() {

            }
        });

        Log.d(LOG_TAG, "onCreateView, ContentProviderOpenHelper.DEFAULT_USER_ID = " + ContentProviderOpenHelper.DEFAULT_USER_ID);

        mITset = new HashSet<>();
        mCameraRenderer.setOnOrientationCallback(new CameraRenderer.OrientationCallback() {
             @Override
             public void onOrientationChange(int orientation) {

                 if (orientation >= 0)
                 {
                     mOrientationDeg = orientation;
                 }

                 //mDebugTextView.setText("Vector Orientation to phone: " + mOrientationDeg);
             }

             @Override
             public void onAzimuthOrientationChange(double orientation) {
                 mCameraAzimuth = (float)orientation;
                 //mDebugTextView.setText("azimuth: " + orientation);
             }

         }
        );


        if (!mIsTablet)
        {
            mTakePicButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    takePicture();
                }
            });
        }
        else{
            mRatioTabletFrameLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    takePicture();
                }
            });
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

    private void takePicture()
    {
        if (mCamera == null || !mIsCameraPermissionGranted || !mIsStoragePermissionGranted || !mIsLocationEnabled || !mIsLocationPermissionGranted)
        {
            Log.d(LOG_TAG, "TakePicButton onclick, missing requirement(s)!");
            Log.d(LOG_TAG, mIsCameraPermissionGranted + " " + mIsStoragePermissionGranted + " " + mIsLocationEnabled + " " + mIsLocationPermissionGranted);
            return;
        }

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
                            im.mFilename = pictureFile.getName();
                            im.rotateAroundCamera(mCameraAzimuth);
                            mCameraRenderer.mImageTextures.add(im);
                        }
                    });

                    mITset.add(pictureFile.getName());


                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(pictureFile.getAbsolutePath(), options);
                    int aspect_ratio = TextureHelper.getBestAspectRatio(options);



                    ContentValues cv = new ContentValues();
                    cv.put(ImageTextures.COL_FILENAME, pictureFile.getName());
                    cv.put(ImageTextures.COL_LAT, currentLocation.getLatitude());
                    cv.put(ImageTextures.COL_LON, currentLocation.getLongitude());
                    cv.put(ImageTextures.COL_USER_ID, ContentProviderOpenHelper.DEFAULT_USER_ID);
                    cv.put(ImageTextures.COL_ANGLE, mCameraAzimuth);
                    cv.put(ImageTextures.COL_ASPECT_RATIO, aspect_ratio);
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
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {


        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        Log.d(LOG_TAG, "onResume");
        super.onResume();

        mCameraRenderer.startReadingSensor();

        if (!mIsTablet)
        {
            mTakePicButton.setVisibility(View.VISIBLE);
        }
        else {
            mRatioTabletFrameLayout.setVisibility(View.VISIBLE);
        }


        resolveAllNeededPermissions();

        if (mIsCameraPermissionGranted && mIsLocationPermissionGranted && mIsStoragePermissionGranted)
        {
            if (mCamera == null && checkCameraHardware(mContext)) {

                mCamera = getCameraInstance();

                if (mCamera ==  null)
                {
                    //Camera dialog
                    Toast.makeText(mContext, R.string.no_access_camera, Toast.LENGTH_LONG);
                    if (!mIsTablet) {
                        mTakePicButton.setVisibility(View.INVISIBLE);
                    }
                    else {
                        mRatioTabletFrameLayout.setVisibility(View.INVISIBLE);
                    }

                    return;
                }

                Log.d(LOG_TAG, "onResume, requirements met!");

                if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
                    startLocationUpdates();
                }

                setCameraDisplayOrientation(getActivity(), 0, mCamera);
                mCameraPreview = new CameraPreview(getContext(), mCamera);
                mFrameLayout.addView(mCameraPreview);


                if (!mIsTablet)
                {
                    mFrameLayout.removeView(mTakePicButton);
                    mFrameLayout.addView(mTakePicButton);
                }
                else {
                    mFrameLayout.removeView(mRatioTabletFrameLayout);
                    mFrameLayout.addView(mRatioTabletFrameLayout);
                }
            }
        }

    }

    @Override
    public void onPause() {
        Log.d(LOG_TAG, "onPause");

        if (mGoogleApiClient.isConnected())
        {
            stopLocationUpdates();
        }


        super.onPause();

        mCameraRenderer.stopReadingSensor();


        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;

        }

        mFrameLayout.removeView(mCameraPreview);

        if (!mIsTablet)
        {
            mFrameLayout.removeView(mTakePicButton);
        }else{
            mFrameLayout.removeView(mRatioTabletFrameLayout);
        }


        mCameraPreview = null;




    }

    @Override
    public void onStart() {
        Log.d(LOG_TAG, "onStart");
        if (mGoogleApiClient != null) {
            Log.d(LOG_TAG, "onStart, Google API Client connect");
            mGoogleApiClient.connect();
        }

        super.onStart();
    }

    @Override
    public void onStop() {
        Log.d(LOG_TAG, "onStop");

        if (mGoogleApiClient != null) {
            Log.d(LOG_TAG, "onStop, Google API Client disconnect");

            mGoogleApiClient.disconnect();
            mRequestingLocationUpdates = true;
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


    public void resolveAllNeededPermissions() {
        ArrayList<String> permissionList = new ArrayList<>();

        Log.d(LOG_TAG, "resolveNeededPermissions");
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_DENIED) {

            permissionList.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            mIsLocationPermissionGranted = true;
            Log.d(LOG_TAG, "resolveNeededPermissions, location permission granted!");
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {

            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            mIsStoragePermissionGranted = true;
            Log.d(LOG_TAG, "resolveNeededPermissions, storage permission granted!");
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {

            permissionList.add(Manifest.permission.CAMERA);
        } else {
            mIsCameraPermissionGranted = true;
            Log.d(LOG_TAG, "resolveNeededPermissions, camera permission granted!");
        }

        if (!mIsStoragePermissionGranted || !mIsLocationPermissionGranted || !mIsCameraPermissionGranted) {
            Log.d(LOG_TAG, "resolveNeededPermissions, permission(s) required");

            if (!permissionList.isEmpty())
            {
                ActivityCompat.requestPermissions(getActivity(),
                        permissionList.toArray(new String [permissionList.size()]),
                        NEEDED_PERMISSION_REQUEST_CODE);
            }



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
        boolean isSignificantlyNewer = timeDelta > TIME_INTERVAL_MS;
        boolean isSignificantlyOlder = timeDelta < -TIME_INTERVAL_MS;
        boolean isNewer = timeDelta > 0;

        // If it's been more than TIME_INTERVAL_MS since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than TIME_INTERVAL_MS older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > ACCURACY_RADIUS_M;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy


        /*
        if (isMoreAccurate && isNewer) {
            return true;
        }
        */


        if (isMoreAccurate) {
            return true;
        }
        else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }



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

        Log.d(LOG_TAG, "onConnected, Google API Client disconnect");
        createLocationRequest();

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
            mRequestingLocationUpdates = false;
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //long prevtime = System.currentTimeMillis();
    @Override
    public void onLocationChanged(Location location) {

        if ( isBetterLocation(location, mCurrentLocation))
        {

            mCurrentLocation = location;
            //mCurrentLocation.getTime();
            //long currentTime = System.currentTimeMillis();


            //prevtime = currentTime;

            mCameraRenderer.mNewCameraLocation = mCurrentLocation;

            //File imageFile = new File (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+ File.separator + "Straggle", filename);
            //ImageTexture im = new ImageTexture(imageFile.getAbsolutePath(), mCurrentLocation, mContext);
            getLoaderManager().restartLoader(IMAGESEARCH_LOADER, null, this);
        }

    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(LOCATION_REQUEST_INTERVAL_MS);
        mLocationRequest.setFastestInterval(LOCATION_REQUEST_FASTEST_INTERVAL_MS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {

        Log.d(LOG_TAG, "startLocationUpdates");

        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        mIsLocationPermissionGranted = true;

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d(LOG_TAG, "LocationCallback, onLocationResult...");

                super.onLocationResult(locationResult);
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                Log.d(LOG_TAG, "LocationCallback, onLocationAvailability...");

                LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
                mLocationSettingsResultPendingResult =
                        LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                                builder.build());

                mLocationSettingsResultPendingResult.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                    @Override
                    public void onResult(LocationSettingsResult result) {
                        final Status status = result.getStatus();
                        final LocationSettingsStates locSettingsStates = result.getLocationSettingsStates();

                        switch (status.getStatusCode()) {
                            case LocationSettingsStatusCodes.SUCCESS:
                                // All location settings are satisfied. The client can
                                // initialize location requests here.

                                mIsLocationEnabled = true;

                                if (mCamera != null && mIsCameraPermissionGranted && mIsStoragePermissionGranted && mIsLocationEnabled && mIsLocationPermissionGranted)
                                {
                                    Log.d(LOG_TAG, "onResult Location, SUCCESS and TakePicButton visible!");
                                    if(!mIsTablet)
                                    {
                                        mTakePicButton.setVisibility(View.VISIBLE);
                                    }
                                    else{
                                        mRatioTabletFrameLayout.setVisibility(View.VISIBLE);
                                    }

                                }

                                Log.d(LOG_TAG, "onResult Location, SUCCESS!");

                                break;
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                // Location settings are not satisfied, but this can be fixed
                                // by showing the user a dialog.


                                Log.d(LOG_TAG, "onResult Location, RESOLUTION_REQUIRED!");

                                if ( !locSettingsStates.isGpsUsable())
                                {
                                    mIsLocationEnabled = false;


                                    if(!mIsTablet)
                                    {
                                        mTakePicButton.setVisibility(View.INVISIBLE);
                                    }
                                    else{
                                        mRatioTabletFrameLayout.setVisibility(View.INVISIBLE);
                                    }

                                    Log.d(LOG_TAG, "onResult Location, RESOLUTION_REQUIRED && GPS not usable!");
                                    if (mLocationCheckDialog.getDialog() == null)
                                    {
                                        mLocationCheckDialog.show(getFragmentManager(), "LocationCheckDialog");
                                    }

                                }

                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                Log.d(LOG_TAG, "onResult Location, SETTINGS_CHANGE_UNAVAILABLE!");
                                // Location settings are not satisfied. However, we have no way
                                // to fix the settings so we won't show the dialog.

                                if(!mIsTablet)
                                {
                                    mTakePicButton.setVisibility(View.INVISIBLE);
                                }else{
                                    mRatioTabletFrameLayout.setVisibility(View.INVISIBLE);
                                }


                                mIsLocationEnabled = false;
                                break;
                        }
                    }
                });


                if (locationAvailability.isLocationAvailable()) {
                    Log.d(LOG_TAG, "LocationCallback, onLocationAvailability: location enabled!");
                    mIsLocationEnabled = true;

                    if (mCamera != null && mIsCameraPermissionGranted && mIsStoragePermissionGranted && mIsLocationEnabled && mIsLocationPermissionGranted)
                    {
                        Log.d(LOG_TAG, "LocationCallback, onLocationAvailability: location enabled and TakePicButton visible!");

                        if(!mIsTablet)
                        {
                            mTakePicButton.setVisibility(View.VISIBLE);
                        }else{
                            mRatioTabletFrameLayout.setVisibility(View.VISIBLE);
                        }

                    }

                } else {
                    Log.d(LOG_TAG, "LocationCallback, onLocationAvailability: location disabled!");

                }

                super.onLocationAvailability(locationAvailability);
            }
        };


        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mLocationCallback, mHandler.getLooper());
    }

    protected void stopLocationUpdates() {

        Log.d(LOG_TAG, "stopLocationUpdates");
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);

        if (mLocationCallback != null)
        {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, mLocationCallback);
        }
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
        Long id;
        float [] distance = new float [3];
        String deletehash;

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

                id = data.getLong(data.getColumnIndex(ImageTextures._ID));
                deletehash = data.getString(data.getColumnIndex(ImageTextures.COL_DELETE_HASH));
                lat = data.getDouble(data.getColumnIndex(ImageTextures.COL_LAT));
                lon = data.getDouble(data.getColumnIndex(ImageTextures.COL_LON));
                final float angle = (float)data.getDouble(data.getColumnIndex(ImageTextures.COL_ANGLE));

                Location.distanceBetween(currentLat, currentLon, lat, lon, distance);

                Log.d(LOG_TAG, "onLoadFinished: distance = " + distance[0]) ;

                if (distance[0] > SEARCH_RADIUS_M)
                {
                    Log.d(LOG_TAG, "onLoadFinished: distance > " + SEARCH_RADIUS_M) ;
                    break;
                }


                final String filename = data.getString(data.getColumnIndex(ImageTextures.COL_FILENAME));
                final File mediaFile = new File(mediaStorageDir.getPath() + File.separator + filename);

                if (!mediaFile.exists())
                {
                    if (mITset.contains(filename))
                    {
                        mITset.remove(filename);
                    }

                    Uri deleteIdUri = ContentProviderDbSchema.ImageTextures.buildImageTextureUriWithUserId(ContentProviderOpenHelper.DEFAULT_USER_ID);
                    String where = ContentProviderDbSchema.ImageTextures._ID + " = ?";
                    String selectionArgs [] = {String.valueOf(id)};
                    int deleted = mContext.getContentResolver().delete(deleteIdUri, where, selectionArgs);

                    Log.d(LOG_TAG, "Image missing in app directory, deleting its record in content provider, deleted: " + deleted);

                    if (deletehash != null)
                    {
                        mImgurService.deleteImage(BuildConfig.IMGUR_AUTHORIZATION, deletehash).enqueue(new Callback<DeleteImageResponse>() {
                            @Override
                            public void onResponse(Call<DeleteImageResponse> call, Response<DeleteImageResponse> response) {

                                if (response.isSuccessful())
                                {
                                    Log.d(LOG_TAG, "onResponse, Successful HTTP response");

                                }else{
                                    Log.d(LOG_TAG, "onResponse, Failed HTTP response code : "  + response.code());
                                }
                            }

                            @Override
                            public void onFailure(Call<DeleteImageResponse> call, Throwable t) {
                                Log.d(LOG_TAG, "onFailure, " + t.toString());
                            }
                        });
                    }


                }


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
