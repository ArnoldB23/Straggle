package roca.bajet.com.straggle;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
import roca.bajet.com.straggle.Map.OverviewClusterManager;
import roca.bajet.com.straggle.data.ContentProviderDbSchema;
import roca.bajet.com.straggle.data.ContentProviderOpenHelper;
import roca.bajet.com.straggle.util.TextureHelper;

public class MainActivity extends Activity implements OnMapReadyCallback, GoogleMap.OnCameraMoveStartedListener,
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
        ClusterManager.OnClusterItemInfoWindowClickListener<ImageBubbleIcon>
        {

    private TextView mSlidingUpPanel;
    private MapFragment mMapFragment;
    private SlidingUpPanelLayout mSlidingUpLayout;
    private GoogleMap mGoogleMap;
    private float mCameraRadius;
    private SensorManager mSensorManager;
    private Sensor mRotationVectorSensor ;
    private final int ROTATIONSENSORTYPE = Sensor.TYPE_ROTATION_VECTOR;

    private float mAzimuth;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private Location mPreviousCurrentLocation;
    private Marker mCurrentLocationMarker;
    public HashMap<String, ImageBubbleIcon> mITmap;
    private OverviewClusterManager<ImageBubbleIcon> mClusterManager;
    private static final int LOCATION_REQUEST_INTERVAL = 10000;
    private static final int LOCATION_REQUEST_FASTEST_INTERVAL = 5000;

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 36;
    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int IMAGESEARCH_LOADER = 0;
    private static final String [] IMAGESEARCH_COLUMNS = {
            ContentProviderDbSchema.ImageTextures.COL_FILENAME,
            ContentProviderDbSchema.ImageTextures.COL_LAT,
            ContentProviderDbSchema.ImageTextures.COL_LON
    };

    private boolean toggleUpdate;
    public Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.overview_activity);

        mHandler = new Handler();

        String sel = ContentProviderDbSchema.Users.COL_USERNAME + " = 'DEFAULT_USER'";
        Cursor c = getContentResolver().query(ContentProviderDbSchema.Users.CONTENT_URI, null, sel, null, null);
        if (c.moveToFirst())
        {
            ContentProviderOpenHelper.DEFAULT_USER_ID = c.getInt(c.getColumnIndex(ContentProviderDbSchema.Users._ID));
            Log.d(LOG_TAG, "DEFAULT_USER_ID = " + ContentProviderOpenHelper.DEFAULT_USER_ID );
        }

        mSlidingUpPanel = (TextView) findViewById(R.id.sliding_up_panel);
        mSlidingUpLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_up_layout);
        //mSlidingUpLayout.setAnchorPoint(64f);

        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        mMapFragment.getMapAsync(this);

        mSensorManager =(SensorManager)getSystemService(SENSOR_SERVICE);
        mRotationVectorSensor = mSensorManager.getDefaultSensor(ROTATIONSENSORTYPE);

        mITmap = new HashMap<>();

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
    public void onResume() {
        mGoogleApiClient.connect();
        super.onResume();

    }

    @Override
    public void onPause() {
        stopLocationUpdates();
        mGoogleApiClient.disconnect();


        super.onPause();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        //mGoogleMap.setOnCameraIdleListener(this);
        mGoogleMap.setOnCameraMoveStartedListener(this);
        mGoogleMap.setOnCameraMoveListener(this);
        mGoogleMap.setOnCameraMoveCanceledListener(this);


        mClusterManager = new OverviewClusterManager<ImageBubbleIcon>(this, mGoogleMap);
        mClusterManager.setOnClusterManagerCallback(this);
        mClusterManager.setRenderer(new ImageBubbleIconRenderer(getApplicationContext(), mGoogleMap, mClusterManager));
        mClusterManager.setOnClusterClickListener(this);

        mGoogleMap.setOnCameraIdleListener(mClusterManager);
        mGoogleMap.setOnMarkerClickListener(mClusterManager);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mGoogleMap.setMyLocationEnabled(true);



            mCurrentLocationMarker = mGoogleMap.addMarker(new MarkerOptions()
                    .anchor(0.5f,0.5f)
                    .position(new LatLng(0,0))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.north_direction_48))
                    .alpha(0.6f)
                    .flat(true));


            /*
           mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
               @Override
               public boolean onMarkerClick(Marker marker) {

                   if ( marker.getId().equalsIgnoreCase(mCurrentLocationMarker.getId()) )
                   {
                       Intent i = new Intent(MainActivity.this, CameraActivity.class);
                       startActivity(i);
                   }


                   return false;
               }
           });

           */

            //mGoogleMap.setOnMarkerLongClickListener


        }else{
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_FINE_LOCATION);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

        }
    }

    @Override
    public void onCameraIdle() {
        float zoom = mGoogleMap.getCameraPosition().zoom;
        double d = 87601515 * Math.pow(0.50269686,zoom);

        mCameraRadius = (float) d/2;


        if (mCurrentLocation != null)
        {
            getLoaderManager().restartLoader(IMAGESEARCH_LOADER, null, MainActivity.this);
        }



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

        return false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
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

        if (mCurrentLocation == null && mGoogleMap != null)
        {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()), 16f);
            mGoogleMap.moveCamera(cameraUpdate);
        }

        mPreviousCurrentLocation = mCurrentLocation;
        mCurrentLocation = location;

        LatLng latlng = new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());


        mCurrentLocationMarker.setPosition(latlng);



    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(LOCATION_REQUEST_INTERVAL);
        mLocationRequest.setFastestInterval(LOCATION_REQUEST_FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        mSensorManager.registerListener(this, mRotationVectorSensor, 10000);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);

        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == ROTATIONSENSORTYPE) {
            // convert the rotation-vector to a 4x4 matrix. the matrix
            // is interpreted by Open GL as the inverse of the
            // rotation-vector, which is what we want.

            float [] rotationMatrix = new float [16];

            SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values);


            float [] rotateOrientation = new float[3];
            SensorManager.getOrientation(rotationMatrix,rotateOrientation);


            mAzimuth = (float)( Math.toDegrees( rotateOrientation[0] ) + 360 ) % 360;

            //mSlidingUpPanel.setText("Azimuth: " + mAzimuth);

            if (mCurrentLocation != null && mGoogleMap != null)
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

            String selection = ContentProviderDbSchema.ImageTextures.COL_LON + " > " + wLoc.getLongitude() + " AND " +  ContentProviderDbSchema.ImageTextures.COL_LON + " < " + eLoc.getLongitude() +
                    " AND " + ContentProviderDbSchema.ImageTextures.COL_LAT + " > " + sLat + " AND " + ContentProviderDbSchema.ImageTextures.COL_LAT + " < " + nLat;

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



        double currentLat = mGoogleMap.getCameraPosition().target.latitude;
        double currentLon = mGoogleMap.getCameraPosition().target.longitude;
        String filename;
        double lat;
        double lon;
        float [] distance = new float [3];

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Straggle");


        if (loader.getId() == IMAGESEARCH_LOADER)
        {
            toggleUpdate = !toggleUpdate;

            while(data.moveToNext())
            {
                //Log.d(LOG_TAG, "")
                lat = data.getDouble(data.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_LAT));
                lon = data.getDouble(data.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_LON));

                Location.distanceBetween(currentLat, currentLon, lat, lon, distance);



                if (distance[0] > mCameraRadius)
                {
                    Log.d(LOG_TAG, "onLoadFinished: distance > " + mCameraRadius) ;
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

                if ((boolean)m.mToggleUpdate != toggleUpdate)
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


        if (mCurrentLocation != null)
        {
            getLoaderManager().restartLoader(IMAGESEARCH_LOADER, null, MainActivity.this);
        }

        //mClusterManager.cluster();
    }

    @Override
    public void onMarkerClickListener(Marker marker) {

        if ( marker.getId().equalsIgnoreCase(mCurrentLocationMarker.getId()) )
        {
            Intent i = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(i);
        }

    }

}
