package roca.bajet.com.straggle;

import android.Manifest;
import android.animation.Animator;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
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
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import roca.bajet.com.straggle.data.ContentProviderDbSchema;
import roca.bajet.com.straggle.data.ContentProviderOpenHelper;
import roca.bajet.com.straggle.dialog.LocationCheckDialog;
import roca.bajet.com.straggle.map.ImageBubbleIcon;
import roca.bajet.com.straggle.map.ImageBubbleIconRenderer;
import roca.bajet.com.straggle.map.OverviewClusterManager;
import roca.bajet.com.straggle.upload.UploadIntentService;
import roca.bajet.com.straggle.util.TextureHelper;

import static roca.bajet.com.straggle.CameraRenderer.getAngleBetweenVectors;
import static roca.bajet.com.straggle.CameraRenderer.normalizeVector;
import static roca.bajet.com.straggle.CameraRenderer.quatmultiply;

public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnCameraMoveCanceledListener,
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
    private static final String KEY_IMAGE_FILENAME = "key_image_filename";
    private static final String KEY_PANEL_STATE = "key_panel_state";
    private static final String KEY_ADD_MARKER = "key_add_marker";
    public static final String EXTRA_IMAGE_LOCATION = "extra_image_location";
    public static final String EXTRA_IMAGE_FILENAME = "extra_image_filename";

    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private final static int CAMERA_ACTIVITY_REQUEST_CODE = 2;

    private View mCircularRevealView;
    private MapFragment mMapFragment;
    private LinearSnapHelper mLinearSnapHelper;
    private SlidingUpPanelLayout mSlidingUpLayout;
    private Toolbar mToolbar;
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
    private String mWidgetItemFilename;
    private Location mCurrentLocation = null;
    private Location mWidgetCameraLocation;
    private GestureDetector mGestureDetector;
    private MotionEvent mTapMotionEvent;
    private Marker mCurrentLocationMarker;
    private Marker mAddNewPhotoMarker;
    private CameraPosition mInitialCameraPosition = null;
    public HashMap<String, ImageBubbleIcon> mITmap;

    private OverviewClusterManager<ImageBubbleIcon> mClusterManager;
    private static final int LOCATION_REQUEST_INTERVAL = 1000;
    private static final int LOCATION_REQUEST_FASTEST_INTERVAL = 500;
    private boolean isConfigurationChange;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 36;
    private static final int READ_STORAGE_PERMISSION_REQUEST_CODE = 47;
    private static final int NEEDED_PERMISSION_REQUEST_CODE = 99;

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int IMAGESEARCH_LOADER = 0;
    private static final String[] IMAGESEARCH_COLUMNS = {
            ContentProviderDbSchema.ImageTextures._ID,
            ContentProviderDbSchema.ImageTextures.COL_FILENAME,
            ContentProviderDbSchema.ImageTextures.COL_LAT,
            ContentProviderDbSchema.ImageTextures.COL_LON,
            ContentProviderDbSchema.ImageTextures.COL_ASPECT_RATIO,
            ContentProviderDbSchema.ImageTextures.COL_URL,
            ContentProviderDbSchema.ImageTextures.COL_DELETE_HASH
    };

    private LocationCheckDialog mLocationCheckDialog;
    private LocationCallback mLocationCallback;
    private boolean toggleUpdate;
    private boolean mRequestingLocationUpdates = true;
    private boolean mLocationPermissionGranted = true;
    private boolean mLocationSettingsEntered = false;
    private boolean mIsRotationVectorEnabled = false;
    private boolean mIsStoragePermissionGranted = false;
    private boolean mDoneRequestingAllPermissions = false;


    private ActionMode mAddMenuActionMode;
    private CameraPosition mCurrentCameraPosition;
    private String mRestoreCurrentFilename;
    private SlidingUpPanelLayout.PanelState mRestorePanelState;
    private SlidingUpPanelLayout.PanelState mPanelState;
    private LatLng mRestoreAddMarkerLatLng;


    private Intent mIntent;
    private String mAction;
    private String mType;
    private ActionBar mActionBar;
    private Intent mUploadServiceIntent;


    public Handler mHandler;
    private PendingResult<LocationSettingsResult> mLocationSettingsResultPendingResult;

    private ActionMode.Callback mAddMenuActionModeCallback = new ActionMode.Callback() {

        // Called when the mAction mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.main_add_new_menu, menu);


            return true;
        }

        // Called each time the mAction mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_ok:

                    if (Intent.ACTION_SEND.equals(mAction) && mType != null) {

                        if (mType.startsWith("image/")) {
                            handleSendImage(mIntent); // Handle single image being sent
                        }
                    } else if (Intent.ACTION_SEND_MULTIPLE.equals(mAction) && mType != null) {
                        if (mType.startsWith("image/")) {
                            handleSendMultipleImages(mIntent); // Handle multiple images being sent
                        }
                    }

                    mode.finish(); // Action picked, so close the CAB
                    return true;

                case R.id.home:
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the mAction mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAddMenuActionMode = null;


            mAddNewPhotoMarker.remove();
            mAddNewPhotoMarker = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        mHandler = new Handler();
        mUploadServiceIntent = new Intent(this, UploadIntentService.class);

        mCircularRevealView = findViewById(R.id.circular_reveal_view);

        String sel = ContentProviderDbSchema.Users.COL_USERNAME + " = 'DEFAULT_USER'";
        Cursor c = getContentResolver().query(ContentProviderDbSchema.Users.CONTENT_URI, null, sel, null, null);
        if (c.moveToFirst()) {
            ContentProviderOpenHelper.DEFAULT_USER_ID = c.getInt(c.getColumnIndex(ContentProviderDbSchema.Users._ID));
            Log.d(LOG_TAG, "onCreate, DEFAULT_USER_ID = " + ContentProviderOpenHelper.DEFAULT_USER_ID);
        }

        mSlidingUpLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_up_layout);
        //mSlidingUpLayout.setAnchorPoint(1);

        mSlidingUpLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                mPanelState = newState;
            }
        });


        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);
        mActionBar = getSupportActionBar();

        mRecyclerViewAdapter = new ImageCursorRecyclerViewAdapter(this, null);

        mRecyclerView = (RecyclerView) findViewById(R.id.images_recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));

        mRecyclerView.setAdapter(mRecyclerViewAdapter);
        mLinearSnapHelper = new LinearSnapHelper();

        mLinearSnapHelper.attachToRecyclerView(mRecyclerView);


        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);


        mMapFragment.getMapAsync(this);


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
            Log.d(LOG_TAG, "onCreate, savedInstanceState != null");

            mCurrentCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);

            isConfigurationChange = true;

            mRecyclerViewAdapter.onRestoreInstanceState(savedInstanceState);

            mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);

            mRestoreCurrentFilename = savedInstanceState.getString(KEY_IMAGE_FILENAME);

            mRestorePanelState = (SlidingUpPanelLayout.PanelState) savedInstanceState.getSerializable(KEY_PANEL_STATE);
            Log.d(LOG_TAG, "onCreate, savedInstanceState != null, panelState = " + mRestorePanelState);
            if (mRestorePanelState != null && mRestorePanelState != SlidingUpPanelLayout.PanelState.DRAGGING) {
                mSlidingUpLayout.setPanelState(mRestorePanelState);
            }

            mRestoreAddMarkerLatLng = savedInstanceState.getParcelable(KEY_ADD_MARKER);


        }

        mIntent = getIntent();
        mAction = mIntent.getAction();
        mType = mIntent.getType();


        //WidgetLocationService.createPeriodicLocationTask(getApplicationContext());

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .enableAutoManage(this, this)
                    .build();
        }


        //TextureHelper.restoreContentProviderDataState(getApplicationContext());
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.d(LOG_TAG, "onNewIntent");

        mIntent = intent;
        mAction = intent.getAction();
        mType = intent.getType();

        if ((Intent.ACTION_SEND.equals(mAction) || Intent.ACTION_SEND_MULTIPLE.equals(mAction))
                && mAddMenuActionMode == null) {

            Log.d(LOG_TAG, "onNewIntent. Add menu action mode");

            mRecyclerViewAdapter.closeContextActionMenu();
            mAddMenuActionMode = startSupportActionMode(mAddMenuActionModeCallback);

            mCurrentCameraPosition = mGoogleMap.getCameraPosition();
            mAddNewPhotoMarker = mGoogleMap.addMarker(new MarkerOptions()
                    .anchor(0.5f, 1f)
                    .zIndex(1.0f)
                    .snippet(getString(R.string.add_new_photo_marker_title))
                    .position(new LatLng(mCurrentCameraPosition.target.latitude, mCurrentCameraPosition.target.longitude))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin_center_48))
                    .draggable(true)
                    .visible(true)
                    .flat(true));


            if (Intent.ACTION_SEND.equals(mAction)) {
                mAddMenuActionMode.setTitle(R.string.menu_add_photo_title);
            } else {
                mAddMenuActionMode.setTitle(R.string.menu_add_photos_title);
            }

        } else if (intent.hasExtra(EXTRA_IMAGE_FILENAME) && intent.hasExtra(EXTRA_IMAGE_LOCATION)) {


            mWidgetCameraLocation = intent.getExtras().getParcelable(EXTRA_IMAGE_LOCATION);
            mWidgetItemFilename = intent.getExtras().getString(EXTRA_IMAGE_FILENAME);

            Log.d(LOG_TAG, "onNewIntent, imageLocation " + String.format("%3.7f", mWidgetCameraLocation.getLatitude()) + ", " + String.format("%3.7f", mWidgetCameraLocation.getLongitude()));
            Log.d(LOG_TAG, "onNewIntent, filename: " + mWidgetItemFilename);

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(mWidgetCameraLocation.getLatitude(), mWidgetCameraLocation.getLongitude()), 16f);
            mGoogleMap.moveCamera(cameraUpdate);


            mSlidingUpLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
        }
    }


    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the mAction bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:

                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                                    .build(this);

                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                } catch (GooglePlayServicesRepairableException e) {

                    Log.d(LOG_TAG, "action_search selected error " + e.toString());
                } catch (GooglePlayServicesNotAvailableException e) {

                    Log.d(LOG_TAG, "action_search selected error " + e.toString());
                }


                return true;


            default:
                // If we got here, the user's mAction was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(ev);
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onDestroy() {
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
        Log.d(LOG_TAG, "onStop");

        if (mGoogleApiClient != null) {
            Log.d(LOG_TAG, "onStop, Google API Client disconnect");


            mGoogleApiClient.disconnect();
            mRequestingLocationUpdates = true;
        }

        mLocationSettingsEntered = false;
        mLocationPermissionGranted = true;
        mIsStoragePermissionGranted = false;

        isConfigurationChange = false;

        mDoneRequestingAllPermissions = false;

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

        //TextureHelper.saveContentProviderDataState(getApplicationContext());
        mSensorManager.unregisterListener(this);
        mWidgetCameraLocation = null;


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
        mGoogleMap.setOnMapClickListener(this);


        mClusterManager = new OverviewClusterManager<>(this, mGoogleMap);
        mClusterManager.setOnClusterManagerCallback(this);
        mClusterManager.setRenderer(new ImageBubbleIconRenderer(getApplicationContext(), mGoogleMap, mClusterManager));
        mClusterManager.setOnClusterClickListener(this);
        mClusterManager.setOnClusterItemClickListener(this);

        mGoogleMap.setOnCameraIdleListener(mClusterManager);
        mGoogleMap.setOnMarkerClickListener(mClusterManager);
        mGoogleMap.setOnMyLocationButtonClickListener(this);


        mCurrentLocationMarker = mGoogleMap.addMarker(new MarkerOptions()
                .anchor(0.5f, 0.5f)
                .position(new LatLng(0, 0))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.north_direction_48))
                .alpha(0.6f)
                .snippet(getString(R.string.current_location_marker_title))
                .visible(false)
                .flat(true));


        mGoogleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {

            }
        });


        if (isConfigurationChange) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (mCurrentLocation != null) {

                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(mCurrentCameraPosition.target.latitude, mCurrentCameraPosition.target.longitude), 16f);
                    mGoogleMap.moveCamera(cameraUpdate);


                }
            }


        } else if (!isConfigurationChange && mIntent.hasExtra(EXTRA_IMAGE_FILENAME) && mIntent.hasExtra(EXTRA_IMAGE_LOCATION)) {


            mWidgetCameraLocation = mIntent.getExtras().getParcelable(EXTRA_IMAGE_LOCATION);
            mWidgetItemFilename = mIntent.getExtras().getString(EXTRA_IMAGE_FILENAME);

            Log.d(LOG_TAG, "onMapReady, imageLocation " + String.format("%3.7f", mWidgetCameraLocation.getLatitude()) + ", " + String.format("%3.7f", mWidgetCameraLocation.getLongitude()));
            Log.d(LOG_TAG, "onMapReady, filename: " + mWidgetItemFilename);

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(mWidgetCameraLocation.getLatitude(), mWidgetCameraLocation.getLongitude()), 16f);
            mGoogleMap.moveCamera(cameraUpdate);

            mSlidingUpLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);


        } else if (!isConfigurationChange && mIntent.getExtras() == null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);


                if (mCurrentLocation != null) {
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), 16f);
                    mGoogleMap.moveCamera(cameraUpdate);
                }

            }
        }


        mGestureDetector = new GestureDetector(getApplicationContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent ev) {
                mTapMotionEvent = ev;
                return super.onSingleTapUp(ev);
            }

            @Override
            public void onLongPress(MotionEvent ev) {

                super.onLongPress(ev);
            }


        });

        if ((Intent.ACTION_SEND.equals(mAction) || Intent.ACTION_SEND_MULTIPLE.equals(mAction))
                && mAddMenuActionMode == null && !isConfigurationChange) {

            Log.d(LOG_TAG, "onMapReady. add menu action mode");

            mRecyclerViewAdapter.closeContextActionMenu();
            mAddMenuActionMode = startSupportActionMode(mAddMenuActionModeCallback);

            mCurrentCameraPosition = mGoogleMap.getCameraPosition();
            mAddNewPhotoMarker = mGoogleMap.addMarker(new MarkerOptions()
                    .anchor(0.5f, 1f)
                    .zIndex(1.0f)
                    .visible(true)
                    .snippet(getString(R.string.add_new_photo_marker_title))
                    .position(new LatLng(mCurrentCameraPosition.target.latitude, mCurrentCameraPosition.target.longitude))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin_center_48))
                    .draggable(true)
                    .flat(true));


            if (Intent.ACTION_SEND.equals(mAction)) {
                mAddMenuActionMode.setTitle(R.string.menu_add_photo_title);
            } else {
                mAddMenuActionMode.setTitle(R.string.menu_add_photos_title);
            }

        } else if (mRestoreAddMarkerLatLng != null && isConfigurationChange) {
            Log.d(LOG_TAG, "onMapReady. Add menu action mode and Configuration Change");

            mRecyclerViewAdapter.closeContextActionMenu();
            mAddMenuActionMode = startSupportActionMode(mAddMenuActionModeCallback);

            mAddNewPhotoMarker = mGoogleMap.addMarker(new MarkerOptions()
                    .anchor(0.5f, 1f)
                    .zIndex(1.0f)
                    .visible(true)
                    .snippet(getString(R.string.add_new_photo_marker_title))
                    .position(new LatLng(mRestoreAddMarkerLatLng.latitude, mRestoreAddMarkerLatLng.longitude))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin_center_48))
                    .draggable(true)
                    .flat(true));


            if (Intent.ACTION_SEND.equals(mAction)) {
                mAddMenuActionMode.setTitle(R.string.menu_add_photo_title);
            } else {
                mAddMenuActionMode.setTitle(R.string.menu_add_photos_title);
            }

        }


    }


    private void updateLocationUI() {
        if (mGoogleMap == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this,
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
            mLocationPermissionGranted = false;
        }


    }


    public void resolveAllNeededPermissions() {
        ArrayList<String> permissionList = new ArrayList<>();

        Log.d(LOG_TAG, "resolveNeededPermissions");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_DENIED) {

            permissionList.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            mLocationPermissionGranted = true;
            Log.d(LOG_TAG, "resolveNeededPermissions, location permission granted!");
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {

            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            mIsStoragePermissionGranted = true;
            Log.d(LOG_TAG, "resolveNeededPermissions, storage permission granted!");
        }

        if (!mIsStoragePermissionGranted || !mLocationPermissionGranted) {
            Log.d(LOG_TAG, "resolveNeededPermissions, permission(s) required");

            if (!mDoneRequestingAllPermissions && !permissionList.isEmpty()) {


                ActivityCompat.requestPermissions(this,
                        permissionList.toArray(new String[permissionList.size()]),
                        NEEDED_PERMISSION_REQUEST_CODE);

                mDoneRequestingAllPermissions = true;
            }

        }


    }

    public boolean resolveStoragePermission() {

        Log.d(LOG_TAG, "resolveStoragePermission");

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "resolveStoragePermission, storage permission granted!");
            mIsStoragePermissionGranted = true;
            return true;
        } else {

            Log.d(LOG_TAG, "resolveStoragePermission, storage permission NOT granted!");


            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_STORAGE_PERMISSION_REQUEST_CODE);


            return false;
        }

    }

    void handleSendImage(Intent intent) {
        final Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        final double lat = mAddNewPhotoMarker.getPosition().latitude;
        final double lon = mAddNewPhotoMarker.getPosition().longitude;

        if (imageUri != null) {
            // Update UI to reflect image being shared

            new Thread(new Runnable() {

                @Override
                public void run() {
                    //File file = new File(Uri.parse(imageUri));

                    File newPictureFile = TextureHelper.getOutputMediaFile(TextureHelper.MEDIA_TYPE_IMAGE);

                    final int chunkSize = 1024;  // We'll read in one kB at a time
                    byte[] imageData = new byte[chunkSize];

                    InputStream in = null;
                    OutputStream out = null;

                    try {
                        in = getContentResolver().openInputStream(imageUri);
                        out = new FileOutputStream(newPictureFile);  // I'm assuming you already have the File object for where you're writing to

                        int bytesRead;
                        while ((bytesRead = in.read(imageData)) > 0) {
                            out.write(Arrays.copyOfRange(imageData, 0, Math.max(0, bytesRead)));
                        }

                        if (in != null) {
                            in.close();
                        }

                        if (out != null) {
                            out.close();
                        }


                        Random randomAngle = new Random();
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(newPictureFile.getAbsolutePath(), options);
                        int aspect_ratio = TextureHelper.getBestAspectRatio(options);


                        ContentValues cv = new ContentValues();
                        cv.put(ContentProviderDbSchema.ImageTextures.COL_FILENAME, newPictureFile.getName());
                        cv.put(ContentProviderDbSchema.ImageTextures.COL_LAT, lat);
                        cv.put(ContentProviderDbSchema.ImageTextures.COL_LON, lon);
                        cv.put(ContentProviderDbSchema.ImageTextures.COL_USER_ID, ContentProviderOpenHelper.DEFAULT_USER_ID);
                        cv.put(ContentProviderDbSchema.ImageTextures.COL_ANGLE, randomAngle.nextInt(360));
                        cv.put(ContentProviderDbSchema.ImageTextures.COL_ASPECT_RATIO, aspect_ratio);
                        Uri imageTexturesUri = ContentProviderDbSchema.ImageTextures.CONTENT_URI;
                        getContentResolver().insert(imageTexturesUri, cv);

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                getSupportLoaderManager().restartLoader(IMAGESEARCH_LOADER, null, MainActivity.this);
                            }
                        });

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            Intent mediaScanIntent = new Intent(
                                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            Uri contentUri = Uri.fromFile(newPictureFile);
                            mediaScanIntent.setData(contentUri);
                            sendBroadcast(mediaScanIntent);
                        } else {
                            sendBroadcast(new Intent(
                                    Intent.ACTION_MEDIA_MOUNTED,
                                    Uri.parse("file://"
                                            + Environment.getExternalStorageDirectory())));
                        }

                    } catch (Exception ex) {
                        Log.e("handleSendImage", ex.toString());
                        if (newPictureFile != null & newPictureFile.exists()) {
                            newPictureFile.delete();
                        }
                    }

                }
            }).start();


            Log.d(LOG_TAG, "handleSendImage, uri: " + imageUri);
        }
    }

    void handleSendMultipleImages(Intent intent) {
        final ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        final double lat = mAddNewPhotoMarker.getPosition().latitude;
        final double lon = mAddNewPhotoMarker.getPosition().longitude;
        if (imageUris != null) {
            // Update UI to reflect multiple images being shared


            new Thread(new Runnable() {

                @Override
                public void run() {
                    //File file = new File(Uri.parse(imageUri));

                    int angleInc = 360 / imageUris.size();
                    int currentAngle = 0;

                    for (Uri imageUri : imageUris) {

                        File newPictureFile = TextureHelper.getOutputMediaFile(TextureHelper.MEDIA_TYPE_IMAGE);

                        final int chunkSize = 1024;  // We'll read in one kB at a time
                        byte[] imageData = new byte[chunkSize];

                        InputStream in = null;
                        OutputStream out = null;

                        try {
                            in = getContentResolver().openInputStream(imageUri);
                            out = new FileOutputStream(newPictureFile);  // I'm assuming you already have the File object for where you're writing to

                            int bytesRead;
                            while ((bytesRead = in.read(imageData)) > 0) {
                                out.write(Arrays.copyOfRange(imageData, 0, Math.max(0, bytesRead)));
                            }

                            if (in != null) {
                                in.close();
                            }

                            if (out != null) {
                                out.close();
                            }


                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile(newPictureFile.getAbsolutePath(), options);
                            int aspect_ratio = TextureHelper.getBestAspectRatio(options);


                            ContentValues cv = new ContentValues();
                            cv.put(ContentProviderDbSchema.ImageTextures.COL_FILENAME, newPictureFile.getName());
                            cv.put(ContentProviderDbSchema.ImageTextures.COL_LAT, lat);
                            cv.put(ContentProviderDbSchema.ImageTextures.COL_LON, lon);
                            cv.put(ContentProviderDbSchema.ImageTextures.COL_USER_ID, ContentProviderOpenHelper.DEFAULT_USER_ID);
                            cv.put(ContentProviderDbSchema.ImageTextures.COL_ANGLE, currentAngle);
                            cv.put(ContentProviderDbSchema.ImageTextures.COL_ASPECT_RATIO, aspect_ratio);
                            Uri imageTexturesUri = ContentProviderDbSchema.ImageTextures.CONTENT_URI;
                            getContentResolver().insert(imageTexturesUri, cv);

                            currentAngle += angleInc;

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                Intent mediaScanIntent = new Intent(
                                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                Uri contentUri = Uri.fromFile(newPictureFile);
                                mediaScanIntent.setData(contentUri);
                                sendBroadcast(mediaScanIntent);
                            } else {
                                sendBroadcast(new Intent(
                                        Intent.ACTION_MEDIA_MOUNTED,
                                        Uri.parse("file://"
                                                + Environment.getExternalStorageDirectory())));
                            }

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    getSupportLoaderManager().restartLoader(IMAGESEARCH_LOADER, null, MainActivity.this);
                                }
                            });


                        } catch (Exception ex) {
                            Log.e("handleSendImage", ex.toString());
                            if (newPictureFile != null & newPictureFile.exists()) {
                                newPictureFile.delete();
                            }
                        }

                        Log.d(LOG_TAG, "handleSendMultipleImages, uri: " + imageUri);
                    }

                }
            }).start();


        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        Log.d(LOG_TAG, "onRequestPermissionsResult...");


        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                // If request is cancelled, the mLocationSettingsResultPendingResult arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    mLocationPermissionGranted = true;

                    Log.d(LOG_TAG, "onRequestPermissionsResult, location permission granted!");
                    updateLocationUI();

                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Log.d(LOG_TAG, "onRequestPermissionsResult, location permission NOT granted!");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    mLocationPermissionGranted = false;
                    mCurrentLocation = null;
                    mCurrentLocationMarker.setVisible(false);
                }

                break;


            case READ_STORAGE_PERMISSION_REQUEST_CODE:

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(LOG_TAG, "onRequestPermissionsResult, mIsStoragePermissionGranted granted!");
                    mIsStoragePermissionGranted = true;
                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Log.d(LOG_TAG, "onRequestPermissionsResult, mIsStoragePermissionGranted NOT granted!");
                    mIsStoragePermissionGranted = false;
                }
                break;


            case NEEDED_PERMISSION_REQUEST_CODE:

                if (grantResults.length < 1) {
                    break;
                }

                for (int i = 0; i < permissions.length; i++) {
                    if (permissions[i] == android.Manifest.permission.ACCESS_FINE_LOCATION
                            && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        // permission was granted, yay! Do the
                        // contacts-related task you need to do.

                        mLocationPermissionGranted = true;

                        Log.d(LOG_TAG, "onRequestPermissionsResult, location permission granted!");
                        updateLocationUI();

                    } else if (permissions[i] == android.Manifest.permission.ACCESS_FINE_LOCATION
                            && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        mLocationPermissionGranted = false;
                        mCurrentLocation = null;
                        mCurrentLocationMarker.setVisible(false);
                        Log.d(LOG_TAG, "onRequestPermissionsResult, location permission denied!");
                    }

                    if (permissions[i] == Manifest.permission.READ_EXTERNAL_STORAGE
                            && grantResults[i] == PackageManager.PERMISSION_GRANTED) {

                        Log.d(LOG_TAG, "onRequestPermissionsResult, mIsStoragePermissionGranted granted!");
                        mIsStoragePermissionGranted = true;
                    } else if (permissions[i] == Manifest.permission.READ_EXTERNAL_STORAGE
                            && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Log.d(LOG_TAG, "onRequestPermissionsResult, mIsStoragePermissionGranted denied!");
                        mIsStoragePermissionGranted = false;
                    }

                }


                break;


        }
    }


    @Override
    public void onCameraMoveCanceled() {

    }

    @Override
    public void onCameraMove() {
        Log.d(LOG_TAG, "onCameraMove...");
    }

    @Override
    public void onCameraMoveStarted(int reason) {
        Log.d(LOG_TAG, "onCameraMoveStarted...");

        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            mRecyclerViewAdapter.closeContextActionMenu();
        }

    }

    @Override
    public void onMapClick(LatLng latLng) {

        Log.d(LOG_TAG, "onMapClick...");

        if (mAddMenuActionMode != null) {
            mAddNewPhotoMarker.setPosition(latLng);
        }
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
        Log.d(LOG_TAG, "onSaveInstanceState");


        if (mGoogleMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mGoogleMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mCurrentLocation);

            if (mRecyclerViewAdapter.getCursor() != null && mRecyclerViewAdapter.getCursor().getCount() > 0) {
                int position = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
                mRecyclerViewAdapter.getCursor().moveToPosition(position);
                String filename = mRecyclerViewAdapter.getCursor().getString(mRecyclerViewAdapter.getCursor().getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_FILENAME));

                outState.putString(KEY_IMAGE_FILENAME, filename);
            }


            if (mAddNewPhotoMarker != null) {
                outState.putParcelable(KEY_ADD_MARKER, mAddNewPhotoMarker.getPosition());
            }

            if (mSlidingUpLayout.getPanelState() == null) {
                outState.putSerializable(KEY_PANEL_STATE, mPanelState);
            } else {
                outState.putSerializable(KEY_PANEL_STATE, mSlidingUpLayout.getPanelState());
            }

            //outState.putBoolean(KEY_LOCATION_MARKER_VISIBLE, mCurrentLocationMarker.isVisible());
            //outState.putFloat(KEY_LOCATION_MARKER_ROTATION, mCurrentLocationMarker.getRotation());

            mRecyclerViewAdapter.onSaveInstanceState(outState);

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


        if (mCurrentLocation == null && mGoogleMap != null && mWidgetCameraLocation == null) {

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 16f);
            mGoogleMap.moveCamera(cameraUpdate);

            if (mAddMenuActionMode != null && mAddNewPhotoMarker != null) {
                mAddNewPhotoMarker.setPosition(latLng);
            }

        }


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

        resolveAllNeededPermissions();

        updateLocationUI();


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

                                if (!locSettingsStates.isGpsUsable()) {

                                    mCurrentLocationMarker.setVisible(false);

                                    if (!mLocationSettingsEntered && mLocationCheckDialog.getDialog() == null) {
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

        }


        //resolveStoragePermission();

        if (!mIsStoragePermissionGranted) {
            Log.d(LOG_TAG, "startLocationUpdates, Storage permission NOT granted!");
            return;
        }

        if (isConfigurationChange) {
            getSupportLoaderManager().initLoader(IMAGESEARCH_LOADER, null, this);
        } else {
            getSupportLoaderManager().restartLoader(IMAGESEARCH_LOADER, null, this);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(LOG_TAG, "onActivityResult...");

        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                Log.i(LOG_TAG, "Place: " + place.getName());

                // Animate camera to the bounds
                try {
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(place.getLatLng()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);

                Log.i(LOG_TAG, status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
                Log.i(LOG_TAG, "onActivityResult, result cancelled");
            }
        } else if (requestCode == CAMERA_ACTIVITY_REQUEST_CODE) {
            Log.d(LOG_TAG, "onActivityResult, CAMERA_ACTIVITY_REQUEST_CODE");
            //getSupportLoaderManager().initLoader(IMAGESEARCH_LOADER,null,this);
        }

    }


    protected void stopLocationUpdates() {
        Log.d(LOG_TAG, "stopLocationUpdates...");
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);

        if (mLocationCallback != null) {
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


            float[] baseAzimuthVector = new float[]{0, 0, 1, -1}; //w,x,y,z
            float[] northVector = new float[]{0, 0, 1, 0};
            float[] h = new float[]{sensorEvent.values[3], sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]};
            float[] hprime = new float[]{h[0], -h[1], -h[2], -h[3]};

            float[] rotationAzimuthVector = quatmultiply(quatmultiply(h, baseAzimuthVector), hprime);
            float[] normRotationVector = normalizeVector(rotationAzimuthVector);
            normRotationVector[3] = 0;

            float angle = getAngleBetweenVectors(northVector, normRotationVector);
            angle *= Math.signum(rotationAzimuthVector[1]);
            mAzimuth = (float) ((Math.toDegrees(angle) + 360) % 360);


            if (mCurrentLocation != null && mGoogleMap != null && mIsRotationVectorEnabled) {
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

        Log.d(LOG_TAG, "onCreateLoader");

        if (id == IMAGESEARCH_LOADER && mIsStoragePermissionGranted) {


            double currentLat = mGoogleMap.getCameraPosition().target.latitude;
            double currentLon = mGoogleMap.getCameraPosition().target.longitude;

            Location currentLocation = new Location(LocationManager.GPS_PROVIDER);
            currentLocation.setLongitude(currentLon);
            currentLocation.setLatitude(currentLat);

            /*
            ((<lat> - LAT_COLUMN) * (<lat> - LAT_COLUMN) +
            (<lng> - LNG_COLUMN) * (<lng> - LNG_COLUMN) * <fudge>)
             */

            double fudge = Math.pow(Math.cos(Math.toRadians(currentLat)), 2);
            Uri imageUri = ContentProviderDbSchema.ImageTextures.buildImageTextureUriWithUserId(ContentProviderOpenHelper.DEFAULT_USER_ID);
            String orderby = "( (" + currentLat + " - " + ContentProviderDbSchema.ImageTextures.COL_LAT + ") * (" + currentLat + " - " + ContentProviderDbSchema.ImageTextures.COL_LAT + ") + (" +
                    currentLon + " - " + ContentProviderDbSchema.ImageTextures.COL_LON + ") * (" + currentLon + " - " + ContentProviderDbSchema.ImageTextures.COL_LON + ") * " + fudge + " ) " +
                    " ASC ";
            //Location.distanceBetween(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude(),);


            Location wLoc = CameraRenderer.calculateDestinationLocation(currentLocation, 270, mCameraRadius);
            Location eLoc = CameraRenderer.calculateDestinationLocation(currentLocation, 90, mCameraRadius);

            double decDegrees = (mCameraRadius * 0.01f) / 1113.2f;

            double nLat = (currentLat + decDegrees);
            double sLat = (currentLat - decDegrees);


            String selection = ContentProviderDbSchema.ImageTextures.COL_LON + " > " + wLoc.getLongitude() + " AND " + ContentProviderDbSchema.ImageTextures.COL_LON + " < " + eLoc.getLongitude() +
                    " AND " + ContentProviderDbSchema.ImageTextures.COL_LAT + " > " + sLat + " AND " + ContentProviderDbSchema.ImageTextures.COL_LAT + " < " + nLat;

            Log.d(LOG_TAG, "onCreateLoader, selection = " + selection);
            Log.d(LOG_TAG, "onCreateLoader, p1 = " + String.format("%3.7f", wLoc.getLatitude()) + ", " + String.format("%3.7f", wLoc.getLongitude()) + " p2 = " + String.format("%3.7f", eLoc.getLatitude()) + ", " + String.format("%3.7f", eLoc.getLongitude()));

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
        Long id;
        double lat;
        double lon;
        float[] distance = new float[3];

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Straggle");
        mRecyclerViewAdapter.swapCursor(data);

        if (loader.getId() == IMAGESEARCH_LOADER && mIsStoragePermissionGranted) {
            if (data.getCount() == 0) {
                mSlidingUpLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }


            Log.d(LOG_TAG, "onLoadFinished: number of items in cursor = " + data.getCount());


            toggleUpdate = !toggleUpdate;

            if (mWidgetCameraLocation != null) {
                int position = mRecyclerViewAdapter.getPositionFromFilename(mWidgetItemFilename);
                Log.d(LOG_TAG, "onLoadFinished: mWidgetCameraLocation exists and item position is " + position);
                if (position > -1) {
                    mRecyclerView.getLayoutManager().scrollToPosition(position);
                }
            } else if (isConfigurationChange) {

                if (mRestoreCurrentFilename != null) {
                    Log.d(LOG_TAG, "onLoadFinished: mRestoreCurrentFilename = " + mRestoreCurrentFilename);

                    int position = mRecyclerViewAdapter.getPositionFromFilename(mRestoreCurrentFilename);
                    if (position > -1) {
                        mRecyclerView.getLayoutManager().scrollToPosition(position);
                    }
                }


                Log.d(LOG_TAG, "onLoadFinished: mRestorePanelState = " + mRestorePanelState);
                if (mRestorePanelState != null && mRestorePanelState != SlidingUpPanelLayout.PanelState.DRAGGING) {
                    mSlidingUpLayout.setPanelState(mRestorePanelState);

                }

                // mSlidingUpLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);

            }

            data.moveToPosition(-1);

            while (data.moveToNext()) {
                //Log.d(LOG_TAG, "")
                id = data.getLong(data.getColumnIndex(ContentProviderDbSchema.ImageTextures._ID));
                lat = data.getDouble(data.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_LAT));
                lon = data.getDouble(data.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_LON));

                Location.distanceBetween(currentLat, currentLon, lat, lon, distance);

                if (distance[0] > mCameraRadius) {
                    Log.d(LOG_TAG, "onLoadFinished: distance > cameraRadius : " + mCameraRadius);
                    break;
                }


                Log.d(LOG_TAG, "onLoadFinished: distance = " + distance[0]);


                filename = data.getString(data.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_FILENAME));
                final File mediaFile = new File(mediaStorageDir.getPath() + File.separator + filename);

                /*
                Uri uri = ContentProviderDbSchema.ImageTextures.CONTENT_URI;
                ContentValues cv = new ContentValues(1);
                int viewType = mRecyclerViewAdapter.getItemViewType(data.getPosition());
                cv.put(ContentProviderDbSchema.ImageTextures.COL_ASPECT_RATIO, viewType);
                String where = ContentProviderDbSchema.ImageTextures.COL_FILENAME + " = ?";
                String [] whereArgs = new String [] {filename};
                getContentResolver().update(uri, cv, where, whereArgs);
                */

                if (!mediaFile.exists()) {
                    if (mITmap.containsKey(filename)) {
                        mClusterManager.removeItem(mITmap.get(filename));
                        mITmap.remove(filename);
                    }


                    String deletehash = data.getString(data.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_DELETE_HASH));

                    if (deletehash != null) {
                        Log.d(LOG_TAG, "Deleting non-existent image record from content provider");

                        mUploadServiceIntent.putExtra("tag", UploadIntentService.DELETEIMAGEANDRECORD);
                        mUploadServiceIntent.putExtra(UploadIntentService.DELETEIMAGEANDRECORD, deletehash);
                        mUploadServiceIntent.putExtra(UploadIntentService.DELETEIMAGEANDRECORD_ID, id);
                        startService(mUploadServiceIntent);
                    }


                } else if (!mITmap.containsKey(filename)) {
                    Bitmap bp = TextureHelper.decodeSampledBitmapFromFile(mediaFile.getAbsolutePath(), 48, 48);

                    LatLng latlng = new LatLng(lat, lon);


                    ImageBubbleIcon newImageIcon = new ImageBubbleIcon(latlng, TextureHelper.getSquareClip(bp));

                    mClusterManager.addItem(newImageIcon);

                    newImageIcon.mToggleUpdate = toggleUpdate;

                    mITmap.put(filename, newImageIcon);
                } else {
                    //mITmap.get(filename).setTag(toggleUpdate);
                    mITmap.get(filename).mToggleUpdate = toggleUpdate;
                }
            }


            ArrayList<String> deleteList = new ArrayList<>();

            //Remove unneeded markers
            for (String f : mITmap.keySet()) {

                //Marker m = mITmap.get(f);
                ImageBubbleIcon m = mITmap.get(f);

                if (m.mToggleUpdate != toggleUpdate) {
                    deleteList.add(f);
                    //mITmap.remove(f);
                    mClusterManager.removeItem(m);


                }
            }

            for (String f : deleteList) {
                mITmap.remove(f);
            }

            mClusterManager.cluster();


        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(LOG_TAG, "onLoaderReset...");
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


        mSlidingUpLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);

        return true;
    }

    @Override
    public void onClusterInfoWindowClick(Cluster<ImageBubbleIcon> cluster) {

    }

    @Override
    public boolean onClusterItemClick(ImageBubbleIcon imageBubbleIcon) {

        mSlidingUpLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);

        return false;
    }

    @Override
    public void onClusterItemInfoWindowClick(ImageBubbleIcon imageBubbleIcon) {

    }

    @Override
    public void onCameraIdle(float cameraRadius) {


        mCurrentCameraPosition = mGoogleMap.getCameraPosition();

        mCameraRadius = cameraRadius;


        if (mIsStoragePermissionGranted) {
            Log.d(LOG_TAG, "onCameraIdle, mCameraRadius : " + mCameraRadius);
            getSupportLoaderManager().restartLoader(IMAGESEARCH_LOADER, null, MainActivity.this);

        }


        //mClusterManager.cluster();
    }

    @Override
    public void onMarkerClickListener(Marker marker) {

        if (mCurrentLocationMarker != null
                && marker.getId().equalsIgnoreCase(mCurrentLocationMarker.getId())
                && mTapMotionEvent != null
                && mAddMenuActionMode == null
                && !mRecyclerViewAdapter.isContextActionMenu()) {
            Log.d(LOG_TAG, "CurrentLocationMarker x = " + mTapMotionEvent.getX() + ", y = " + mTapMotionEvent.getY());


            float finalRadius = (float) Math.hypot(mTapMotionEvent.getX(), mTapMotionEvent.getY());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Animator anim = ViewAnimationUtils.createCircularReveal(mCircularRevealView, (int) mTapMotionEvent.getX(), (int) mTapMotionEvent.getY() - 152, 0, finalRadius);
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
                        startActivityForResult(i, CAMERA_ACTIVITY_REQUEST_CODE);
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
            //No circular reveal animation for below Lollipop devices
            else {
                Intent i = new Intent(MainActivity.this, CameraActivity.class);
                startActivityForResult(i, CAMERA_ACTIVITY_REQUEST_CODE);
            }


        }

    }

}
