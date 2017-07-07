package roca.bajet.com.straggle.widget;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import roca.bajet.com.straggle.data.ContentProviderDbSchema;
import roca.bajet.com.straggle.data.ContentProviderOpenHelper;

/**
 * Created by Arnold on 6/6/2017.
 */

public class WidgetLocationService extends GcmTaskService implements LocationListener {

    private GoogleApiClient mGoogleApiClient;
    private final static String LOG_TAG = "WidgetLocService";
    private static final int LOCATION_REQUEST_INTERVAL = 10000;
    private static final int LOCATION_REQUEST_FASTEST_INTERVAL = 5000;
    private LocationRequest mLocationRequest;
    private BroadcastReceiver mSleepWakeBroadcastReceiver;
    private static String periodicTag = "periodic";
    private static String oneTimeTag = "onetime";
    private CountDownLatch mCountDownLatch;
    private Location mCurrentLocation;
    private float [] distance = new float[3];


    public static OneoffTask createOneOffLocationTask(Context c)
    {


        OneoffTask oneoffTask = new OneoffTask.Builder()
                .setService(WidgetLocationService.class)
                .setRequiredNetwork(Task.NETWORK_STATE_ANY)
                .setTag(oneTimeTag)
                .setUpdateCurrent(false)
                .setRequiresCharging(false)
                .setExecutionWindow(0L, 10L)
                .build();

        GcmNetworkManager.getInstance(c).schedule(oneoffTask);

        Log.d(LOG_TAG, "createOneOffLocationTask...");

        return oneoffTask;

    }

    public static PeriodicTask createPeriodicLocationTask(Context c) {
        //long period = 3600L; //1 hr = 3600 sec
        //long flex = 1800L;

        long period = 10L; //
        long flex = 5L;



        GcmNetworkManager.getInstance(c).cancelTask(periodicTag,WidgetLocationService.class);

        // create a periodic task to pull stocks once every hour after the app has been opened. This
        // is so Widget data stays up to date.
        PeriodicTask periodicTask = new PeriodicTask.Builder()
                .setService(WidgetLocationService.class)
                .setPeriod(period)
                .setFlex(flex)
                .setTag(periodicTag)
                .setRequiredNetwork(Task.NETWORK_STATE_ANY)
                .setRequiresCharging(false)
                .build();
        // Schedule task with tag "periodic."
        GcmNetworkManager.getInstance(c).schedule(periodicTask);

        Log.d(LOG_TAG, "createPeriodicLocationTask...");

        return periodicTask;
    }

    public static void stopPeriodicLocationTask(Context c)
    {
        GcmNetworkManager.getInstance(c).cancelTask(periodicTag, WidgetLocationService.class);
        //GcmNetworkManager.getInstance(c).cancelAllTasks(WidgetLocationService.class);

    }

    private void createLocationRequest() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(LOCATION_REQUEST_INTERVAL);
        mLocationRequest.setFastestInterval(LOCATION_REQUEST_FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


            Log.d(LOG_TAG, "startLocationUpdates, ACCESS_FINE_LOCATION and/or ACCESS_COURSE_LOCATION not granted!");
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {

        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }


    @Override
    public void onLocationChanged(Location loc) {

        Log.d(LOG_TAG,"onLocationChanged, " + String.format("%3.7f",loc.getLatitude()) + ", " + String.format("%3.7f",loc.getLongitude()));


        if (mCountDownLatch.getCount() > 0)
        {
            mCurrentLocation = loc;

            ContentValues cv = new ContentValues();
            cv.put(ContentProviderDbSchema.CurrentLocation.COL_LAT, loc.getLatitude());
            cv.put(ContentProviderDbSchema.CurrentLocation.COL_LON, loc.getLongitude());
            cv.put(ContentProviderDbSchema.CurrentLocation.COL_UTC_TIMESTAMP, System.currentTimeMillis());

            String where = ContentProviderDbSchema.CurrentLocation._ID + " = ?";

            getContentResolver().update(ContentProviderDbSchema.CurrentLocation.CONTENT_URI, cv, where, new String [] {String.valueOf(ContentProviderOpenHelper.DEFAULT_CURRENT_LOCATION_ID)});
            sendBroadcast(new Intent(StraggleWidgetProvider.ACTION_LOCATION_DATA_UPDATED));

            if (mCountDownLatch != null)
            {
                mCountDownLatch.countDown();
            }
        }


    }

    @Override
    public void onInitializeTasks ()
    {
        Log.d(LOG_TAG,"onInitializeTasks");


    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId)
    {
        Log.d(LOG_TAG,"onStartCommand");

        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(LOG_TAG,"onCreate");

        createLocationRequest();

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        
    }

    @Override
    public void onDestroy ()
    {
        Log.d(LOG_TAG,"onDestroy");


        super.onDestroy();
    }


    @Override
    public int onRunTask(TaskParams taskParams) {

        int result = GcmNetworkManager.RESULT_FAILURE;


        if ( mGoogleApiClient.blockingConnect().isSuccess() && mGoogleApiClient != null )
        {
            mCountDownLatch = new CountDownLatch(1);

            startLocationUpdates();

            try {
                mCountDownLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (mGoogleApiClient.isConnected())
            {
                stopLocationUpdates();
                mGoogleApiClient.disconnect();
            }else{
                Log.d(LOG_TAG, "onRunTask, mGoogleApieClient NOT connected!");
            }


            result = GcmNetworkManager.RESULT_SUCCESS;
        }

        return result;
    }



}
