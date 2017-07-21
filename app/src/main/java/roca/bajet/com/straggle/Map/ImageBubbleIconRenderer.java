package roca.bajet.com.straggle.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import java.util.ArrayList;

import roca.bajet.com.straggle.R;

/**
 * Created by Arnold on 3/25/2017.
 */

public class ImageBubbleIconRenderer extends DefaultClusterRenderer<ImageBubbleIcon> {
    private final IconGenerator mIconGenerator;
    private final IconGenerator mClusterIconGenerator;
    private final ImageView mClusterImageView;
    private final ImageView mImageView;
    private final int mDimension;
    private final Context mContext;


    public ImageBubbleIconRenderer(Context context, GoogleMap map, ClusterManager<ImageBubbleIcon> clusterManager) {
        super(context, map, clusterManager);

        mContext = context;
        mIconGenerator = new IconGenerator(mContext);
        mImageView = new ImageView(mContext);
        int padding = (int) mContext.getResources().getDimension(R.dimen.custom_profile_padding);
        mImageView.setPadding(padding, padding, padding, padding);
        mIconGenerator.setContentView(mImageView);

        mDimension = (int) mContext.getResources().getDimension(R.dimen.custom_profile_image);
        mClusterIconGenerator = new IconGenerator(mContext);

        View multiImageBubble = LayoutInflater.from(mContext).inflate(R.layout.cluster_imageview, null);
        mClusterIconGenerator.setContentView(multiImageBubble);
        mClusterImageView = (ImageView) multiImageBubble.findViewById(R.id.cluster_imageview);
    }

    @Override
    protected void onBeforeClusterItemRendered(ImageBubbleIcon bubbleIcon, MarkerOptions markerOptions) {
        // Draw a single bubble.

        mImageView.setImageBitmap(bubbleIcon.mBitmap);
        Bitmap icon = mIconGenerator.makeIcon();
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
        markerOptions.snippet(mContext.getString(R.string.cluster_item_title));

    }


    @Override
    protected void onBeforeClusterRendered(Cluster<ImageBubbleIcon> cluster, MarkerOptions markerOptions) {
        int width = mDimension;
        int height = mDimension;

        ArrayList<Drawable> bitmaps = new ArrayList<>(Math.min(4, cluster.getSize()));

        for (ImageBubbleIcon bubbleIcon : cluster.getItems())
        {
            if (bitmaps.size() == 4) break;
            BitmapDrawable d = new BitmapDrawable(mContext.getResources(), bubbleIcon.mBitmap);
            d.setBounds(0,0,width,height);
            bitmaps.add(d);
        }

        //Bitmap layeredBitmap = TextureHelper.getLayeredBitmap( bitmaps.toArray(new Bitmap [bitmaps.size()]), mContext);
        //mClusterImageView.setImageBitmap(layeredBitmap);

        MultiDrawable multiDrawable = new MultiDrawable(bitmaps);
        multiDrawable.setBounds(0, 0, width, height);


        mClusterImageView.setImageDrawable(multiDrawable);
        Bitmap icon = mClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
        markerOptions.snippet("Photo cluster of " + String.valueOf(bitmaps.size()));

    }



    @Override
    protected boolean shouldRenderAsCluster(Cluster cluster) {
        // Always render clusters.
        return cluster.getSize() > 1;
    }


}
