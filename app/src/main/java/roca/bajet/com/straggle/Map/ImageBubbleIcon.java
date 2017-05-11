package roca.bajet.com.straggle.Map;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Created by Arnold on 3/25/2017.
 */

public class ImageBubbleIcon implements ClusterItem {

    public final LatLng mPosition;
    public final Bitmap mBitmap;
    public boolean mToggleUpdate;

    public ImageBubbleIcon (LatLng position, Bitmap bitmap)
    {
        mPosition = position;
        mBitmap = bitmap;
    }


    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getSnippet() {
        return null;
    }
}
