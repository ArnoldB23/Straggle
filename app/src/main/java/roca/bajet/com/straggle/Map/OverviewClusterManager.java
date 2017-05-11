package roca.bajet.com.straggle.Map;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

/**
 * Created by Arnold on 3/25/2017.
 */

public class OverviewClusterManager<T extends ClusterItem> extends ClusterManager {

    private GoogleMap mGoogleMap;
    private Context mContext;
    private onClusterManagerCallback mOnClusterManagerCallback;

    public interface onClusterManagerCallback {
        void onCameraIdle (float cameraRadius);
        void onMarkerClickListener (Marker marker);
    }

    public OverviewClusterManager(Context context, GoogleMap map) {
        super(context, map);

        mContext = context;
        mGoogleMap = map;
    }

    public void setOnClusterManagerCallback(onClusterManagerCallback cm)
    {
        mOnClusterManagerCallback = cm;
    }

    @Override
    public void onCameraIdle() {
        float zoom = mGoogleMap.getCameraPosition().zoom;
        double d = 87601515 * Math.pow(0.50269686,zoom);

        float cameraRadius = (float) d/2;

        if (mOnClusterManagerCallback != null)
        {
            mOnClusterManagerCallback.onCameraIdle(cameraRadius);
        }

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (mOnClusterManagerCallback != null)
        {
            mOnClusterManagerCallback.onMarkerClickListener(marker);
        }

        return super.onMarkerClick(marker);
    }
}
