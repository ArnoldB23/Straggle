package roca.bajet.com.straggle;

import android.animation.Animator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;

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
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import roca.bajet.com.straggle.Map.ImageBubbleIcon;
import roca.bajet.com.straggle.Map.ImageBubbleIconRenderer;
import roca.bajet.com.straggle.Map.LocationCheckDialog;
import roca.bajet.com.straggle.Map.OverviewClusterManager;
import roca.bajet.com.straggle.data.ContentProviderDbSchema;
import roca.bajet.com.straggle.data.ContentProviderOpenHelper;
import roca.bajet.com.straggle.util.TextureHelper;

import static roca.bajet.com.straggle.CameraRenderer.getAngleBetweenVectors;
import static roca.bajet.com.straggle.CameraRenderer.normalizeVector;
import static roca.bajet.com.straggle.CameraRenderer.quatmultiply;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnCameraMoveCanceledListener,
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        SensorEventListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        OverviewClusterManager.onClusterManagerCallback,
        ClusterManager.OnClusterClickListener<ImageBubbleIcon>,
        ClusterManager.OnClusterInfoWindowClickListener<ImageBubbleIcon>,
        ClusterManager.OnClusterItemClickListener<ImageBubbleIcon>,
        ClusterManager.OnClusterItemInfoWindowClickListener<ImageBubbleIcon> {

    private static final String KEY_CAMERA_POSITION = "key_camera_position";
    private static final String KEY_LOCATION = "key_location";
    private static final String KEY_LOCATION_MARKER_VISIBLE = "key_location_marker_visible";
    private static final String KEY_LOCATION_MARKER_ROTATION = "key_location_marker_rotation";
    private static final String KEY_TOGGLE_UPDATE = "key_toggle_update";
    private static final String KEY_REQUESTING_LOCATION_UPDATES = "key_requesting_location_updates";
    private static final String KEY_LOCATION_PERMISSION_GRANTED = "key_location_permission_granted";
    private static final String KEY_LOCATION_SETTINGS_ENTERED = "key_location_settings_entered";
    private static final String KEY_IS_ROTATION_VECTOR = "key_is_rotation_vector";

    private View mCircularRevealView;
    private MapFragment mMapFragment;
    private LinearSnapHelper mLinearSnapHelper;
    private SlidingUpPanelLayout mSlidingUpLayout;
    private RecyclerView mRecyclerView;
    private ImageCursorRecyclerViewAdapter mRecyclerViewAdapter;
    private GoogleMap mGoogleMap;
    private float mCameraRadius;
    private SensorManager mSensorManager;
    private Sensor mRotationVectorSensor;
    private final int ROTATIONSENSORTYPE = Sensor.TYPE_ROTATION_VECTOR;

    private float mAzimuth;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation = null;
    private Location mPreviousCurrentLocation;
    private GestureDetector mGestureDetector;
    private MotionEvent mTapMotionEvent;
    private Marker mCurrentLocationMarker;
    private CameraPosition mInitialCameraPosition = null;
    public HashMap<String, ImageBubbleIcon> mITmap;
    private OverviewClusterManager<ImageBubbleIcon> mClusterManager;
    private static final int LOCATION_REQUEST_INTERVAL = 10000;
    private static final int LOCATION_REQUEST_FASTEST_INTERVAL = 5000;
    private boolean isConfigurationChange;

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 36;
    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int IMAGESEARCH_LOADER = 0;
    private static final String[] IMAGESEARCH_COLUMNS = {
            ContentProviderDbSchema.ImageTextures._ID,
            ContentProviderDbSchema.ImageTextures.COL_FILENAME,
            ContentProviderDbSchema.ImageTextures.COL_LAT,
            ContentProviderDbSchema.ImageTextures.COL_LON
    };

    private LocationCheckDialog mLocationCheckDialog;
    private LocationCallback mLocationCallback;
    private boolean toggleUpdate;
    private boolean mRequestingLocationUpdates = true;
    private boolean mLocationPermissionGranted = true;
    private boolean mLocationSettingsEntered = false;
    private boolean mIsRotationVectorEnabled = false;

    public Handler mHandler;
    PendingResult<LocationSettingsResult> mLocationSettingsResultPendingResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        mHandler = new Handler();


        mCircularRevealView = findViewById(R.id.circular_reveal_view);

        String sel = ContentProviderDbSchema.Users.COL_USERNAME + " = 'DEFAULT_USER'";
        Cursor c = getContentResolver().query(ContentProviderDbSchema.Users.CONTENT_URI, null, sel, null, null);
        if (c.moveToFirst()) {
            ContentProviderOpenHelper.DEFAULT_USER_ID = c.getInt(c.getColumnIndex(ContentProviderDbSchema.Users._ID));
            Log.d(LOG_TAG, "onCreate, DEFAULT_USER_ID = " + ContentProviderOpenHelper.DEFAULT_USER_ID);
        }

        mSlidingUpLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_up_layout);
        mSlidingUpLayout.setAnchorPoint(1);

        mSlidingUpLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Log.d(LOG_TAG, "onCreate, ontouch...");
                return false;
            }
        });

        mRecyclerViewAdapter = new ImageCursorRecyclerViewAdapter(getApplicationContext(), null);

        mRecyclerView = (RecyclerView) findViewById(R.id.images_recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));

        mRecyclerView.setAdapter(mRecyclerViewAdapter);
        mLinearSnapHelper = new LinearSnapHelper();
        mLinearSnapHelper.attachToRecyclerView(mRecyclerView);


        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);

        mMapFragment.getMapAsync(this);

        if (mMapFragment.getView() == null)
        {
            Log.d(LOG_TAG, "onCreate, map fragment view is null!");
        }else{
            Log.d(LOG_TAG, "onCreate, map fragment view exists!");
        }


        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mRotationVectorSensor = mSensorManager.getDefaultSensor(ROTATIONSENSORTYPE);

        mITmap = new HashMap<>();

        mLocationCheckDialog = new LocationCheckDialog();
        mLocationCheckDialog.setOnLocationCheckDialogCallback(new LocationCheckDialog.LocationCheckDialogCallback() {
            @Override
            public void onPositiveButton() {
                mLocationSettingsEntered = true;
            }

            @Override
            public void onNegativeButton() {
                mLocationSettingsEntered = false;
            }

            @Override
            public void onCancelled() {
                mLocationSettingsEntered = false;
            }
        });

        if (savedInstanceState != null) {

            mInitialCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);

            isConfigurationChange = true;

            mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);

        }

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    public boolean dispatchTouchEvent (MotionEvent ev)
    {
        Log.d(LOG_TAG, "dispatchTouchEvent");

        mGestureDetector.onTouchEvent(ev);

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");


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
            mRequestingLocationUpdates = true;
        }

        mLocationSettingsEntered = false;
        mLocationPermissionGranted = true;


        super.onStop();
    }

    @Override
    public void onResume() {

        Log.d(LOG_TAG, "onResume, register RotationVector sensor");

        mIsRotationVectorEnabled = mSensorManager.registerListener(this, mRotationVectorSensor, 10000);

        mCircularRevealView.setVisibility(View.INVISIBLE);
        if (!mIsRotationVectorEnabled) {
            Log.d(LOG_TAG, "onResume, UNABLE to register RotationVector sensor");
        }

        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }


        super.onResume();

    }

    @Override
    public void onPause() {


        Log.d(LOG_TAG, "onPause, unregister RotationVector sensor");

        mSensorManager.unregisterListener(this);

        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }

        super.onPause();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(LOG_TAG, "onMapReady...");
        mGoogleMap = googleMap;

        //mGoogleMap.setOnCameraIdleListener(this);
        mGoogleMap.setOnCameraMoveStartedListener(this);
        mGoogleMap.setOnCameraMoveListener(this);
        mGoogleMap.setOnCameraMoveCanceledListener(this);



        mClusterManager = new OverviewClusterManager<>(this, mGoogleMap);
        mClusterManager.setOnClusterManagerCallback(this);
        mClusterManager.setRenderer(new ImageBubbleIconRenderer(getApplicationContext(), mGoogleMap, mClusterManager));
        mClusterManager.setOnClusterClickListener(this);

        mGoogleMap.setOnCameraIdleListener(mClusterManager);
        mGoogleMap.setOnMarkerClickListener(mClusterManager);
        mGoogleMap.setOnMyLocationButtonClickListener(this);




        mCurrentLocationMarker = mGoogleMap.addMarker(new MarkerOptions()
                .anchor(0.5f, 0.5f)
                .position(new LatLng(0, 0))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.north_direction_48))
                .alpha(0.6f)
                .visible(false)
                .flat(true));

        if (isConfigurationChange == false) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

                if (mCurrentLocation != null)
                {
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), 16f);
                    mGoogleMap.moveCamera(cameraUpdate);
                }
            }

        }
        else{
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (mCurrentLocation != null) {
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), 16f);
                    mGoogleMap.moveCamera(cameraUpdate);
                }
            }
        }

        mGestureDetector = new GestureDetector(getApplicationContext(), new GestureDetector.SimpleOnGestureListener()
        {
            @Override
            public boolean onSingleTapUp (MotionEvent ev)
            {
                Log.d(LOG_TAG, "onSingleTapUp...");
                mTapMotionEvent = ev;
                return false;
            }
        });

        //updateLocationUI();

    }

    private void updateLocationUI() {
        if (mGoogleMap == null) {
            return;
        }

    /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            Log.d(LOG_TAG, "updateLocationUI, location permission granted!");

            mGoogleMap.setMyLocationEnabled(true);
            mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            Log.d(LOG_TAG, "updateLocationUI, location permission NOT granted!");

            mGoogleMap.setMyLocationEnabled(false);
            mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
            mCurrentLocation = null;

            mCurrentLocationMarker.setVisible(false);

            if (mLocationPermissionGranted)
            {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_FINE_LOCATION);
            }

        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        Log.d(LOG_TAG, "onRequestPermissionsResult...");


        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the mLocationSettingsResultPendingResult arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    mLocationPermissionGranted = true;

                    Log.d(LOG_TAG, "onRequestPermissionsResult, location permission granted!");
                    updateLocationUI();

                } else {
                    Log.d(LOG_TAG, "onRequestPermissionsResult, location permission NOT granted!");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    mLocationPermissionGranted = false;
                    mCurrentLocation = null;
                    mCurrentLocationMarker.setVisible(false);
                }


            }

        }
    }

    @Override
    public void onCameraIdle() {

        Log.d(LOG_TAG, "onCameraIdle...");
        float zoom = mGoogleMap.getCameraPosition().zoom;
        double d = 87601515 * Math.pow(0.50269686, zoom);

        mCameraRadius = (float) d / 2;

        getSupportLoaderManager().restartLoader(IMAGESEARCH_LOADER, null, MainActivity.this);

    }

    @Override
    public void onCameraMoveCanceled() {

    }

    @Override
    public void onCameraMove() {

    }

    @Override
    public void onCameraMoveStarted(int i) {

    }

    @Override
    public void onMapClick(LatLng latLng) {


    }


    @Override
    public boolean onMyLocationButtonClick() {

        //Intent i = new Intent(this, CameraActivity.class);
        Log.d(LOG_TAG, "onMyLocationButtonClick...");

        return false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Log.d(LOG_TAG, "onConnected...");




        createLocationRequest();

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
            mRequestingLocationUpdates = false;
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        /*
        private static final String KEY_LOCATION_MARKER_VISIBLE =  "key_location_marker_visible";
        private static final String KEY_LOCATION_MARKER_ROTATION = "key_location_marker_rotation";
        private static final String KEY_TOGGLE_UPDATE = "key_toggle_update";
        private static final String KEY_REQUESTING_LOCATION_UPDATES = "key_requesting_location_updates";
        private static final String KEY_LOCATION_PERMISSION_GRANTED = "key_location_permission_granted";
        private static final String KEY_LOCATION_SETTINGS_ENTERED = "key_location_settings_entered";
        private static final String KEY_IS_ROTATION_VECTOR = "key_is_rotation_vector";
        */

        if (mGoogleMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mGoogleMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mCurrentLocation);
            //outState.putBoolean(KEY_LOCATION_MARKER_VISIBLE, mCurrentLocationMarker.isVisible());
            //outState.putFloat(KEY_LOCATION_MARKER_ROTATION, mCurrentLocationMarker.getRotation());


            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "onConnectionSuspended...");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "onConnectionFailed...");
    }

    @Override
    public void onLocationChanged(Location location) {


        if (mCurrentLocation == null && mGoogleMap != null) {

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16f);
            mGoogleMap.moveCamera(cameraUpdate);

        }

        mPreviousCurrentLocation = mCurrentLocation;


        mCurrentLocation = location;


        LatLng latlng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        Log.d(LOG_TAG, "onLocationChanged, " + mCurrentLocation.getLatitude() + ", " + mCurrentLocation.getLongitude());

        mCurrentLocationMarker.setPosition(latlng);
        //mCurrentLocationMarker.setVisible(true);

    }


    protected void createLocationRequest() {
        Log.d(LOG_TAG, "createLocationRequest...");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(LOCATION_REQUEST_INTERVAL);
        mLocationRequest.setFastestInterval(LOCATION_REQUEST_FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        Log.d(LOG_TAG, "startLocationUpdates...");
        updateLocationUI();

        if (!mLocationPermissionGranted)
        {
            Log.d(LOG_TAG, "startLocationUpdates, Location permission NOT granted!");

            return;
        }

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

                                Log.d(LOG_TAG, "onResult Location, SUCCESS!");

                                break;
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                // Location settings are not satisfied, but this can be fixed
                                // by showing the user a dialog.


                                Log.d(LOG_TAG, "onResult Location, RESOLUTION_REQUIRED!");

                                if ( !locSettingsStates.isGpsUsable())
                                {

                                    mCurrentLocationMarker.setVisible(false);

                                    if (!mLocationSettingsEntered && mLocationCheckDialog.getDialog() == null)
                                    {
                                        mLocationCheckDialog.show(getSupportFragmentManager(), "LocationCheckDialog");
                                    }

                                    /*
                                    try {
                                        // Show the dialog by calling startResolutionForResult(),
                                        // and check the mLocationSettingsResultPendingResult in onActivityResult().

                                        status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);


                                    } catch (IntentSender.SendIntentException e) {
                                        // Ignore the error.
                                    }

                                    */
                                }

                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                Log.d(LOG_TAG, "onResult Location, SETTINGS_CHANGE_UNAVAILABLE!");
                                // Location settings are not satisfied. However, we have no way
                                // to fix the settings so we won't show the dialog.
                                mCurrentLocationMarker.setVisible(false);
                                break;
                        }
                    }
                });


                if (locationAvailability.isLocationAvailable()) {
                    Log.d(LOG_TAG, "LocationCallback, onLocationAvailability: location enabled!");
                    mCurrentLocationMarker.setVisible(true);

                } else {
                    Log.d(LOG_TAG, "LocationCallback, onLocationAvailability: location disabled!");

                }

                super.onLocationAvailability(locationAvailability);
            }
        };

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Log.d(LOG_TAG, "startLocationUpdates, requested Location Updates!");

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mLocationCallback, mHandler.getLooper());

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            return;
        }

        Log.d(LOG_TAG, "startLocationUpdates, UNABLE to request Location Updates!");


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(LOG_TAG, "onActivityResult...");


    }




    protected void stopLocationUpdates() {
        Log.d(LOG_TAG, "stopLocationUpdates...");
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);

        if (mLocationCallback != null)
        {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, mLocationCallback);
        }


    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == ROTATIONSENSORTYPE) {
            // convert the rotation-vector to a 4x4 matrix. the matrix
            // is interpreted by Open GL as the inverse of the
            // rotation-vector, which is what we want.

            /*
            float [] rotationMatrix = new float [16];

            SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values);


            float [] rotateOrientation = new float[3];
            SensorManager.getOrientation(rotationMatrix,rotateOrientation);


            mAzimuth = (float)( Math.toDegrees( rotateOrientation[0] ) + 360 ) % 360;
            */


            //mSlidingUpPanel.setText("Azimuth: " + mAzimuth);



            float [] baseAzimuthVector = new float [] {0,0,1,-1}; //w,x,y,z
            float [] northVector = new float [] {0,0,1,0};
            float [] h = new float [] {sensorEvent.values[3],sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2]};
            float [] hprime = new float [] {h[0], -h[1], -h[2], -h[3]};

            float [] rotationAzimuthVector = quatmultiply(quatmultiply(h,baseAzimuthVector),hprime);
            float [] normRotationVector = normalizeVector(rotationAzimuthVector);
            normRotationVector[3] = 0;

            float angle = getAngleBetweenVectors(northVector, normRotationVector);
            angle *= Math.signum(rotationAzimuthVector[1]);
            mAzimuth = (float) ((Math.toDegrees( angle ) + 360 ) % 360);


            if (mCurrentLocation != null && mGoogleMap != null && mIsRotationVectorEnabled)
            {
                mCurrentLocationMarker.setRotation(mAzimuth);
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = null;

        if (id == IMAGESEARCH_LOADER)
        {


            double currentLat = mGoogleMap.getCameraPosition().target.latitude;
            double currentLon = mGoogleMap.getCameraPosition().target.longitude;

            Location currentLocation = new Location(LocationManager.GPS_PROVIDER);
            currentLocation.setLongitude(currentLon);
            currentLocation.setLatitude(currentLat);

            /*
            ((<lat> - LAT_COLUMN) * (<lat> - LAT_COLUMN) +
            (<lng> - LNG_COLUMN) * (<lng> - LNG_COLUMN) * <fudge>)
             */

            double fudge = Math.pow(Math.cos(Math.toRadians(currentLat)),2);
            Uri imageUri = ContentProviderDbSchema.ImageTextures.buildImageTextureUriWithUserId(ContentProviderOpenHelper.DEFAULT_USER_ID);
            String orderby = "( (" + currentLat + " - " + ContentProviderDbSchema.ImageTextures.COL_LAT + ") * (" + currentLat + " - " + ContentProviderDbSchema.ImageTextures.COL_LAT + ") + (" +
                    currentLon + " - " + ContentProviderDbSchema.ImageTextures.COL_LON + ") * (" + currentLon + " - " + ContentProviderDbSchema.ImageTextures.COL_LON + ") * " + fudge + " ) " +
                    " ASC ";
            //Location.distanceBetween(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude(),);


            Location wLoc = CameraRenderer.calculateDestinationLocation(currentLocation, 270, mCameraRadius);
            Location eLoc = CameraRenderer.calculateDestinationLocation(currentLocation, 90, mCameraRadius);

            double decDegrees = (mCameraRadius * 0.01f)/1113.2f;

            double nLat = (currentLat + decDegrees);
            double sLat = (currentLat - decDegrees);

            if ( wLoc.getLongitude() > 0 && eLoc.getLongitude() < 0)
            {

            }

            else {

            }

            String selection = ContentProviderDbSchema.ImageTextures.COL_LON + " > " + wLoc.getLongitude() + " AND " +  ContentProviderDbSchema.ImageTextures.COL_LON + " < " + eLoc.getLongitude() +
                    " AND " + ContentProviderDbSchema.ImageTextures.COL_LAT + " > " + sLat + " AND " + ContentProviderDbSchema.ImageTextures.COL_LAT + " < " + nLat;

            Log.d(LOG_TAG, "onCreateLoader, selection = " + selection);
            Log.d(LOG_TAG, "onCreateLoader, p1 = " + String.format("%3.7f",wLoc.getLatitude()) + ", " + String.format("%3.7f",wLoc.getLongitude()) + " p2 = " + String.format("%3.7f",eLoc.getLatitude()) + ", " + String.format("%3.7f",eLoc.getLongitude()) );

            cursorLoader = new CursorLoader(this,
                    imageUri,
                    IMAGESEARCH_COLUMNS,
                    selection,
                    null,
                    orderby);
        }

        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        Log.d(LOG_TAG, "onLoadFinished...");

        double currentLat = mGoogleMap.getCameraPosition().target.latitude;
        double currentLon = mGoogleMap.getCameraPosition().target.longitude;
        String filename;
        double lat;
        double lon;
        float [] distance = new float [3];

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Straggle");
        mRecyclerViewAdapter.swapCursor(data);

        if (loader.getId() == IMAGESEARCH_LOADER)
        {



            Log.d(LOG_TAG, "onLoadFinished: number of items in cursor = " + data.getCount());


            toggleUpdate = !toggleUpdate;

            while(data.moveToNext())
            {
                //Log.d(LOG_TAG, "")
                lat = data.getDouble(data.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_LAT));
                lon = data.getDouble(data.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_LON));

                Location.distanceBetween(currentLat, currentLon, lat, lon, distance);



                if (distance[0] > mCameraRadius)
                {
                    Log.d(LOG_TAG, "onLoadFinished: distance > cameraRadius : " + mCameraRadius) ;
                    break;
                }



                Log.d(LOG_TAG, "onLoadFinished: distance = " + distance[0]) ;


                filename = data.getString(data.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_FILENAME));
                final File mediaFile = new File(mediaStorageDir.getPath() + File.separator + filename);




                if (!mITmap.containsKey(filename))
                {
                    Bitmap bp = TextureHelper.decodeSampledBitmapFromFile(mediaFile.getAbsolutePath(), 48, 48);

                    LatLng latlng = new LatLng(lat, lon);

                    /*
                    Marker newImageIcon = mGoogleMap.addMarker(new MarkerOptions()
                    .position(latlng)
                    .icon(BitmapDescriptorFactory.fromBitmap(TextureHelper.getCircleClip(bp)))
                    .flat(true)
                    .anchor(0.5f, 0.5f));

                    */

                    ImageBubbleIcon newImageIcon = new ImageBubbleIcon(latlng,TextureHelper.getSquareClip(bp));

                    mClusterManager.addItem(newImageIcon);

                    newImageIcon.mToggleUpdate = toggleUpdate;

                    mITmap.put(filename, newImageIcon);
                }
                else {
                    //mITmap.get(filename).setTag(toggleUpdate);
                    mITmap.get(filename).mToggleUpdate = toggleUpdate;
                }
            }



            ArrayList<String> deleteList = new ArrayList<>();

            //Remove unneeded markers
            for (String f : mITmap.keySet()) {

                //Marker m = mITmap.get(f);
                ImageBubbleIcon m = mITmap.get(f);

                if (m.mToggleUpdate != toggleUpdate)
                {
                    deleteList.add(f);
                    //mITmap.remove(f);
                    mClusterManager.removeItem(m);


                }
            }

            for (String f : deleteList)
            {
                mITmap.remove(f);
            }

            mClusterManager.cluster();



        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(LOG_TAG, "onLoaderReset...") ;
        mRecyclerViewAdapter.swapCursor(null);
    }

    @Override
    public boolean onClusterClick(Cluster<ImageBubbleIcon> cluster) {
        // Create the builder to collect all essential cluster items for the bounds.
        LatLngBounds.Builder builder = LatLngBounds.builder();
        for (ClusterItem item : cluster.getItems()) {
            builder.include(item.getPosition());
        }
        // Get the LatLngBounds
        final LatLngBounds bounds = builder.build();

        // Animate camera to the bounds
        try {
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void onClusterInfoWindowClick(Cluster<ImageBubbleIcon> cluster) {

    }

    @Override
    public boolean onClusterItemClick(ImageBubbleIcon imageBubbleIcon) {
        return false;
    }

    @Override
    public void onClusterItemInfoWindowClick(ImageBubbleIcon imageBubbleIcon) {

    }

    @Override
    public void onCameraIdle(float cameraRadius)
    {



        mCameraRadius = cameraRadius;

        Log.d(LOG_TAG, "onCameraIdle, mCameraRadius : " + mCameraRadius);

        getSupportLoaderManager().restartLoader(IMAGESEARCH_LOADER, null, MainActivity.this);

        //mClusterManager.cluster();
    }

    @Override
    public void onMarkerClickListener(Marker marker) {

        if ( mCurrentLocationMarker != null && marker.getId().equalsIgnoreCase(mCurrentLocationMarker.getId()) && mTapMotionEvent != null )
        {
            Log.d(LOG_TAG, "CurrentLocationMarker x = " + mTapMotionEvent.getX() + ", y = " + mTapMotionEvent.getY());



            float finalRadius = (float) Math.hypot(mTapMotionEvent.getX(), mTapMotionEvent.getY());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                Animator anim = ViewAnimationUtils.createCircularReveal(mCircularRevealView, (int)mTapMotionEvent.getX(), (int)mTapMotionEvent.getY()-152, 0, finalRadius);
                mCircularRevealView.setVisibility(View.VISIBLE);
                mCircularRevealView.bringToFront();

                anim.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {

                        Log.d(LOG_TAG, "onAnimationEnd...");


                        Intent i = new Intent(MainActivity.this, CameraActivity.class);
                        startActivity(i);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                });

                anim.start();
            }



        }

    }

}
