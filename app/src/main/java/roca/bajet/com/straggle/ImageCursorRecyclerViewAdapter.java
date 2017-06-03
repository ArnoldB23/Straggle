package roca.bajet.com.straggle;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;

import roca.bajet.com.straggle.data.ContentProviderDbSchema;

/**
 * Created by Arnold on 5/12/2017.
 */

public class ImageCursorRecyclerViewAdapter extends CursorRecyclerViewAdapter<ImageCursorRecyclerViewAdapter.ViewHolder> {

    public Context mContext;
    File mediaStorageDir;

    public ImageCursorRecyclerViewAdapter(Context context, Cursor cursor) {
        super(context, cursor);

        mContext = context;

        mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Straggle");
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(mContext).inflate(R.layout.list_image_item, parent, false);
        ViewHolder vh = new ViewHolder(itemView);

        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Cursor data) {


        String filename = data.getString(data.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_FILENAME));
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + filename);

        //Bitmap bp = BitmapFactory.decodeFile(mediaFile.getAbsolutePath());
        //Bitmap bp = TextureHelper.decodeSampledBitmapFromFile(mediaFile.getAbsolutePath(), 300, 300);
        //viewHolder.mImageView.setImageBitmap(bp);

        Picasso.with(mContext).load(mediaFile.getAbsoluteFile()).into(viewHolder.mImageView);


    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView mImageView;

        public ViewHolder(View view) {
            super(view);

            mImageView = (ImageView) view.findViewById(R.id.item_imageview);
        }
    }
}
