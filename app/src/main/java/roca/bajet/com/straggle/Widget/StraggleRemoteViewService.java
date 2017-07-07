package roca.bajet.com.straggle.widget;

import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

import roca.bajet.com.straggle.CameraRenderer;
import roca.bajet.com.straggle.MainActivity;
import roca.bajet.com.straggle.R;
import roca.bajet.com.straggle.data.ContentProviderDbSchema;
import roca.bajet.com.straggle.util.TextureHelper;

/**
 * Created by Arnold on 6/8/2017.
 */

public class StraggleRemoteViewService extends RemoteViewsService {

    private Cursor data = null;
    private static final String LOG_TAG = "RemoteViewService";
    public static float SEARCH_RADIUS =  5000; //5km
    private ArrayList<String> mIdList;
    private int mDefaultUser;
    private Uri imageUri;


    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {

            private final String[] IMAGESEARCH_COLUMNS = {
                    ContentProviderDbSchema.ImageTextures._ID,
                    ContentProviderDbSchema.ImageTextures.COL_FILENAME,
                    ContentProviderDbSchema.ImageTextures.COL_LAT,
                    ContentProviderDbSchema.ImageTextures.COL_LON
            };

            @Override
            public void onCreate() {
                Log.d(LOG_TAG, "onCreate...");
                mIdList = new ArrayList<>();



                String sel = ContentProviderDbSchema.Users.COL_USERNAME + " = 'DEFAULT_USER'";
                Cursor c = getContentResolver().query(ContentProviderDbSchema.Users.CONTENT_URI, null, sel, null, null);
                if (c.moveToFirst()) {
                    mDefaultUser = c.getInt(c.getColumnIndex(ContentProviderDbSchema.Users._ID));
                    Log.d(LOG_TAG, "DEFAULT_USER_ID: " + mDefaultUser);
                }
                c.close();

                imageUri = ContentProviderDbSchema.ImageTextures.buildImageTextureUriWithUserId(mDefaultUser);
            }

            @Override
            public void onDataSetChanged() {

                Log.d(LOG_TAG, "onDataSetChanged...");

                if ( data != null )
                {
                    data.close();
                }

                final long identityToken = Binder.clearCallingIdentity();

                String [] locationColumns = new String [] {ContentProviderDbSchema.CurrentLocation.COL_LAT, ContentProviderDbSchema.CurrentLocation.COL_LON};
                Cursor locationCursor = getContentResolver().query(ContentProviderDbSchema.CurrentLocation.CONTENT_URI, locationColumns , null, null, null);

                locationCursor.moveToFirst();

                Double currentLat = locationCursor.getDouble(locationCursor.getColumnIndex(ContentProviderDbSchema.CurrentLocation.COL_LAT));
                Double currentLon = locationCursor.getDouble(locationCursor.getColumnIndex(ContentProviderDbSchema.CurrentLocation.COL_LON));
                locationCursor.close();

                if (currentLat != null && currentLon != null)
                {
                    Location currentLocation = new Location(LocationManager.GPS_PROVIDER);
                    currentLocation.setLongitude(currentLon);
                    currentLocation.setLatitude(currentLat);

                    double fudge = Math.pow(Math.cos(Math.toRadians(currentLat)),2);


                    String orderby = "( (" + currentLat + " - " + ContentProviderDbSchema.ImageTextures.COL_LAT + ") * (" + currentLat + " - " + ContentProviderDbSchema.ImageTextures.COL_LAT + ") + (" +
                            currentLon + " - " + ContentProviderDbSchema.ImageTextures.COL_LON + ") * (" + currentLon + " - " + ContentProviderDbSchema.ImageTextures.COL_LON + ") * " + fudge + " ) " +
                            " ASC ";
                    //Location.distanceBetween(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude(),);


                    Location wLoc = CameraRenderer.calculateDestinationLocation(currentLocation, 270, SEARCH_RADIUS);
                    Location eLoc = CameraRenderer.calculateDestinationLocation(currentLocation, 90, SEARCH_RADIUS);

                    double decDegrees = (SEARCH_RADIUS * 0.01f)/1113.2f;

                    double nLat = (currentLat + decDegrees);
                    double sLat = (currentLat - decDegrees);

                    String selection = ContentProviderDbSchema.ImageTextures.COL_LON + " > " + wLoc.getLongitude() + " AND " +  ContentProviderDbSchema.ImageTextures.COL_LON + " < " + eLoc.getLongitude() +
                            " AND " + ContentProviderDbSchema.ImageTextures.COL_LAT + " > " + sLat + " AND " + ContentProviderDbSchema.ImageTextures.COL_LAT + " < " + nLat;


                    Log.d(LOG_TAG, "onDataSetChanged, selection = " + selection);
                    Log.d(LOG_TAG, "onDataSetChanged, p1 = " + String.format("%3.7f",wLoc.getLatitude()) + ", " + String.format("%3.7f",wLoc.getLongitude()) + " p2 = " + String.format("%3.7f",eLoc.getLatitude()) + ", " + String.format("%3.7f",eLoc.getLongitude()) );


                    Cursor potentialLocations = getContentResolver().query(imageUri, IMAGESEARCH_COLUMNS, selection, null, orderby);

                    double lat;
                    double lon;
                    float [] distance = new float [3];
                    LinkedList<String> nearLocationList = new LinkedList<>();

                    while(potentialLocations.moveToNext()) {

                        lat = potentialLocations.getDouble(potentialLocations.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_LAT));
                        lon = potentialLocations.getDouble(potentialLocations.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_LON));

                        Location.distanceBetween(currentLat, currentLon, lat, lon, distance);

                        if (distance[0] > SEARCH_RADIUS) {
                            Log.d(LOG_TAG, "Distance: " + distance[0] + " > " + SEARCH_RADIUS);
                            break;
                        }

                        nearLocationList.add(String.valueOf(potentialLocations.getInt(potentialLocations.getColumnIndex(ContentProviderDbSchema.ImageTextures._ID))));

                    }

                    potentialLocations.close();
                    String chosenLocationStr = nearLocationList.poll();

                    if (chosenLocationStr != null)
                    {
                        for(String location : nearLocationList)
                        {
                            chosenLocationStr += "," + location;
                        }

                        String chosenSelection = ContentProviderDbSchema.ImageTextures._ID + " IN ( " + chosenLocationStr + " ) ";

                        Log.d(LOG_TAG, "onDataSetChanged, chosenSelection: " + chosenSelection);



                       data = getContentResolver().query(ContentProviderDbSchema.ImageTextures.CONTENT_URI, IMAGESEARCH_COLUMNS, chosenSelection, null, null);

                        Log.d(LOG_TAG, "Count: " + data.getCount());

                    }
                    else{
                        data = null;
                    }

                }

                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                Log.d(LOG_TAG, "onDestroy...");

                if (data != null)
                {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {

                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }



                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_list_item);

                String filename = data.getString(data.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_FILENAME));

                File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Straggle");
                File mediaFile = new File(mediaStorageDir.getPath() + File.separator + filename);

                //Todo:
                views.setImageViewBitmap(R.id.image_item, TextureHelper.decodeSampledBitmapFromFile(mediaFile.getAbsolutePath(), 250, 250));

                double lat = data.getDouble(data.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_LAT));
                double lon = data.getDouble(data.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_LON));

                Location imageLocation = new Location(LocationManager.GPS_PROVIDER);
                imageLocation.setLatitude(lat);
                imageLocation.setLongitude(lon);

                final Intent fillInIntent = new Intent();



                fillInIntent.putExtra(MainActivity.EXTRA_IMAGE_LOCATION, imageLocation);
                fillInIntent.putExtra(MainActivity.EXTRA_IMAGE_FILENAME, filename);

                views.setOnClickFillInIntent(R.id.image_item, fillInIntent);


                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int i) {
                if (data.moveToPosition(i))
                {

                    return Long.valueOf(data.getInt(data.getColumnIndex(ContentProviderDbSchema.ImageTextures._ID)));
                }

                return i;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
