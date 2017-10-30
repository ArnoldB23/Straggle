package roca.bajet.com.straggle;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SwappingHolder;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import roca.bajet.com.straggle.data.ContentProviderDbSchema;
import roca.bajet.com.straggle.data.ContentProviderOpenHelper;
import roca.bajet.com.straggle.upload.ApiUtils;
import roca.bajet.com.straggle.upload.DeleteImageResponse;
import roca.bajet.com.straggle.upload.ImgurService;
import roca.bajet.com.straggle.upload.PostImageResponse;
import roca.bajet.com.straggle.upload.UploadIntentService;
import roca.bajet.com.straggle.upload.UploadNotificationHelper;
import roca.bajet.com.straggle.util.TextureHelper;

/**
 * Created by Arnold on 5/12/2017.
 */

public class ImageCursorRecyclerViewAdapter extends CursorRecyclerViewAdapter<ImageCursorRecyclerViewAdapter.ViewHolder> {

    private Context mContext;
    private File mediaStorageDir;
    private boolean toggleSelectAll = false;
    private HashMap<Long, String> mFileNameListId = new HashMap<>();
    public ActionMode mActionMode;
    private Intent mUploadServiceIntent;
    private UploadNotificationHelper mNotificationHelper;

    public static final String TOGGLE_SELECT_ALL_KEY = "TOGGLE_SELECT_ALL_KEY";
    public static final String FILE_NAME_LIST_ID_KEY = "FILE_NAME_LIST_ID_KEY";
    public static final String SELECTABLE_MODE_KEY = "SELECTABLE_MODE_KEY";

    public static final int INVALID_VIEW_TYPE = -1;
    public static final int LANDSCAPE_VIEW_TYPE = 0;
    public static final int PORTRAIT_VIEW_TYPE = 1;
    public static final String LOG_TAG = "ImageCursorAdapter";

    public interface OnClickCallBack{
        void onClick(int position);
    }

    private OnClickCallBack mCallBack;

    private MultiSelector mMultiSelector = new MultiSelector();
    private ModalMultiSelectorCallback mActionModeCallback
            = new ModalMultiSelectorCallback(mMultiSelector) {

        private final String LOG_TAG = "mActionModeCallback";
        private Long DEFAULT_USER_ID = ContentProviderOpenHelper.DEFAULT_USER_ID;


        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            super.onCreateActionMode(actionMode, menu);
            actionMode.getMenuInflater().inflate(R.menu.main_context_menu, menu);

            mActionMode = actionMode;

            MenuItem selectAllItem = menu.findItem(R.id.action_select_all);
            if (toggleSelectAll) {

                selectAllItem.setIcon(mContext.getDrawable(R.drawable.cancel_icon_48));
            } else {

                selectAllItem.setIcon(mContext.getDrawable(R.drawable.select_all_icon_48));
            }

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return super.onPrepareActionMode(mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {


            switch (item.getItemId()) {
                case R.id.action_select_all:

                    toggleSelectAll = !toggleSelectAll;

                    if (toggleSelectAll) {
                        mCursor.moveToPosition(-1);
                        while (mCursor.moveToNext()) {
                            Long id = mCursor.getLong(mCursor.getColumnIndex(ContentProviderDbSchema.ImageTextures._ID));
                            String filename = mCursor.getString(mCursor.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_FILENAME));

                            mMultiSelector.setSelected(mCursor.getPosition(), id, true);
                            mFileNameListId.put(id, filename);
                        }

                        item.setIcon(mContext.getDrawable(R.drawable.cancel_icon_48));

                        Log.d(LOG_TAG, "Action Select All, count: " + mMultiSelector.getSelectedPositions().size());
                    } else {

                        mFileNameListId.clear();
                        mMultiSelector.clearSelections();
                        item.setIcon(mContext.getDrawable(R.drawable.select_all_icon_48));

                    }


                    return true;

                case R.id.action_share_pictures:

                    Log.d(LOG_TAG, "Action Share, count: " + mMultiSelector.getSelectedPositions().size());

                    if (mFileNameListId.size() == 1) {
                        Set<Map.Entry<Long, String>> setSentry = mFileNameListId.entrySet();
                        Map.Entry<Long, String> singleEntry = setSentry.iterator().next();

                        Intent shareIntent = new Intent();

                        String filename = singleEntry.getValue();
                        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + filename);
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mediaFile));
                        shareIntent.setType("image/*");
                        mContext.startActivity(Intent.createChooser(shareIntent, mContext.getResources().getText(R.string.send_to_single)));
                    } else if (mFileNameListId.size() > 1) {
                        ArrayList<Uri> imageUris = new ArrayList<>();


                        for (Map.Entry<Long, String> entry : mFileNameListId.entrySet()) {
                            String filename = entry.getValue();
                            File mediaFile = new File(mediaStorageDir.getPath() + File.separator + filename);

                            imageUris.add(Uri.fromFile(mediaFile));
                        }

                        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                        shareIntent.setType("image/*");
                        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);

                        mContext.startActivity(Intent.createChooser(shareIntent, mContext.getResources().getText(R.string.send_to_multiple)));
                    }

                    mode.finish();
                    mMultiSelector.clearSelections();
                    mFileNameListId.clear();

                    return true;

                case R.id.action_delete:

                    for (Integer position : mMultiSelector.getSelectedPositions()) {

                        mCursor.moveToPosition(position);
                        final Long id = mCursor.getLong(mCursor.getColumnIndex(ContentProviderDbSchema.ImageTextures._ID));
                        String filename = mCursor.getString(mCursor.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_FILENAME));
                        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + filename);

                        if (mediaFile.exists()) {
                            mediaFile.delete();
                        }


                        String deletehash = mCursor.getString(mCursor.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_DELETE_HASH));

                        //mImgurService todo:

                        if (deletehash != null) {
                            mUploadServiceIntent.putExtra("tag", UploadIntentService.DELETEIMAGE);
                            mUploadServiceIntent.putExtra(UploadIntentService.DELETEIMAGE, deletehash);
                            mContext.startService(mUploadServiceIntent);
                        }


                        /*
                        mImgurService.deleteImage(BuildConfig.IMGUR_AUTHORIZATION, deletehash).enqueue(new Callback<DeleteImageResponse>() {
                            @Override
                            public void onResponse(Call<DeleteImageResponse> call, Response<DeleteImageResponse> response) {

                                if (response.isSuccessful())
                                {
                                    Log.d(LOG_TAG, "Action Delete, onResponse, Successful HTTP response");

                                    ContentValues cv = new ContentValues();
                                    cv.putNull(ContentProviderDbSchema.ImageTextures.COL_URL);
                                    cv.putNull(ContentProviderDbSchema.ImageTextures.COL_DELETE_HASH);

                                    Uri updateIdUri = ContentProviderDbSchema.ImageTextures.buildImageTextureUriWithUserId(DEFAULT_USER_ID);
                                    String where = ContentProviderDbSchema.ImageTextures._ID + " = ?";
                                    String selectionArgs [] = {String.valueOf(id)};
                                    int updated = mContext.getContentResolver().update(updateIdUri, cv, where, selectionArgs);

                                    Log.d(LOG_TAG, "Action Delete, onResponse, updated : " + updated);


                                }else{
                                    Log.d(LOG_TAG, "Action Delete, onResponse, Failed HTTP response code : "  + response.code());
                                }
                            }

                            @Override
                            public void onFailure(Call<DeleteImageResponse> call, Throwable t) {
                                Log.d(LOG_TAG, "Action Delete, onFailure, " + t.toString());
                            }
                        });

                        */


                        Uri deleteIdUri = ContentProviderDbSchema.ImageTextures.buildImageTextureUriWithUserId(DEFAULT_USER_ID);
                        String where = ContentProviderDbSchema.ImageTextures._ID + " = ?";
                        String selectionArgs[] = {String.valueOf(id)};
                        int deleted = mContext.getContentResolver().delete(deleteIdUri, where, selectionArgs);

                        Log.d(LOG_TAG, "Action Delete, deleting position: " + position + ", success: " + deleted);


                        notifyItemChanged(position);
                    }

                    Log.d(LOG_TAG, "Action Delete, count: " + mMultiSelector.getSelectedPositions().size());

                    mode.finish();
                    mMultiSelector.clearSelections();
                    mFileNameListId.clear();

                    return true;

                case R.id.action_upload:


                    for (Integer position : mMultiSelector.getSelectedPositions()) {
                        mCursor.moveToPosition(position);
                        final Long id = mCursor.getLong(mCursor.getColumnIndex(ContentProviderDbSchema.ImageTextures._ID));

                        String url = mCursor.getString(mCursor.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_URL));


                        if (url == null) {
                            String filename = mCursor.getString(mCursor.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_FILENAME));

                            //todo: mImgurService
                            mUploadServiceIntent.putExtra("tag", UploadIntentService.POSTIMAGE);
                            mUploadServiceIntent.putExtra(UploadIntentService.POSTIMAGE, filename);
                            mUploadServiceIntent.putExtra(UploadIntentService.POSTIMAGE_ID, id);
                            mContext.startService(mUploadServiceIntent);
                        }
                        //URL already exists
                        else {

                            Log.d(LOG_TAG, "postImage URL already exists");
                            mNotificationHelper.createUploadedNotification(url);
                        }


                    }

                    Log.d(LOG_TAG, "Action upload, count: " + mMultiSelector.getSelectedPositions().size());

                    mode.finish();
                    mMultiSelector.clearSelections();
                    mFileNameListId.clear();

                    return true;

                case R.id.action_upload_delete:

                    for (Integer position : mMultiSelector.getSelectedPositions()) {
                        mCursor.moveToPosition(position);
                        final Long id = mCursor.getLong(mCursor.getColumnIndex(ContentProviderDbSchema.ImageTextures._ID));

                        String url = mCursor.getString(mCursor.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_URL));
                        String deletehash = mCursor.getString(mCursor.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_DELETE_HASH));


                        if (url == null) {
                            Log.d(LOG_TAG, "Delete Image URL doesn't exist");
                            break;

                        }
                        //URL already exists
                        else {

                            //todo: mImgurService
                            mUploadServiceIntent.putExtra("tag", UploadIntentService.DELETEIMAGEANDRECORD);
                            mUploadServiceIntent.putExtra(UploadIntentService.DELETEIMAGEANDRECORD, deletehash);
                            mUploadServiceIntent.putExtra(UploadIntentService.DELETEIMAGEANDRECORD_ID, id);
                            mContext.startService(mUploadServiceIntent);

                        }


                    }

                    Log.d(LOG_TAG, "Action upload delete, count: " + mMultiSelector.getSelectedPositions().size());

                    mode.finish();
                    mMultiSelector.clearSelections();
                    mFileNameListId.clear();

                    return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            super.onDestroyActionMode(mode);

            mMultiSelector.clearSelections();
            mFileNameListId.clear();

            notifyDataSetChanged();

            toggleSelectAll = false;
        }
    };

    public ImageCursorRecyclerViewAdapter(Context context, Cursor cursor) {
        super(context, cursor);

        mContext = context;

        mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Straggle");
        mUploadServiceIntent = new Intent(mContext, UploadIntentService.class);
        mNotificationHelper = new UploadNotificationHelper(mContext);

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

        viewHolder.filename = filename;
        viewHolder.id = data.getLong(data.getColumnIndex(ContentProviderDbSchema.ImageTextures._ID));

        viewHolder.aspect_ratio = data.getInt(data.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_ASPECT_RATIO));


        if (viewHolder.aspect_ratio <= TextureHelper.ARATIO_9X16) {
            viewHolder.mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            //Picasso.with(mContext).load(mediaFile).into(viewHolder.mImageView);

            Glide.with(mContext).load(mediaFile).into(viewHolder.mImageView)
                    .onLoadFailed(mContext.getDrawable(R.drawable.placeholder));
        } else {
            viewHolder.mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            //Picasso.with(mContext).load(mediaFile).into(viewHolder.mImageView);
            Glide.with(mContext).load(mediaFile).into(viewHolder.mImageView)
                    .onLoadFailed(mContext.getDrawable(R.drawable.placeholder));
        }


    }

    @Override
    public int getItemViewType(int position) {

        if (mCursor.moveToPosition(position)) {
            String filename = mCursor.getString(mCursor.getColumnIndex(ContentProviderDbSchema.ImageTextures.COL_FILENAME));
            File mediaFile = new File(mediaStorageDir.getPath() + File.separator + filename);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mediaFile.getAbsolutePath(), options);


            int aratioNum = TextureHelper.getBestAspectRatio(options);

            return aratioNum;

            /*

            if ( aratioNum <= TextureHelper.ARATIO_9X16)
            {
                return PORTRAIT_VIEW_TYPE;
            }
            else{
                return LANDSCAPE_VIEW_TYPE;
            }

            */
        } else {
            return INVALID_VIEW_TYPE;
        }

    }


    public void onSaveInstanceState(Bundle outstate) {
        outstate.putSerializable(FILE_NAME_LIST_ID_KEY, mFileNameListId);
        outstate.putBoolean(TOGGLE_SELECT_ALL_KEY, toggleSelectAll);
        Bundle multiSelState = mMultiSelector.saveSelectionStates();
        outstate.putAll(multiSelState);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        toggleSelectAll = savedInstanceState.getBoolean(TOGGLE_SELECT_ALL_KEY);


        mMultiSelector.restoreSelectionStates(savedInstanceState);

        if (mMultiSelector.isSelectable()) {
            ((AppCompatActivity) mContext).startSupportActionMode(mActionModeCallback);
        }
        mMultiSelector.restoreSelectionStates(savedInstanceState);


        mFileNameListId = (HashMap<Long, String>) savedInstanceState.getSerializable(FILE_NAME_LIST_ID_KEY);

    }

    public void closeContextActionMenu() {
        mMultiSelector.clearSelections();
        mFileNameListId.clear();
        notifyDataSetChanged();

        if (mMultiSelector.isSelectable()) {
            mActionMode.finish();
        }

    }

    public boolean isContextActionMenu() {
        if (mMultiSelector.isSelectable()) {
            return true;
        }

        return false;
    }

    public void setOnClickCallBackListener(OnClickCallBack cb)
    {
        mCallBack = cb;
    }


    public class ViewHolder extends SwappingHolder implements View.OnLongClickListener, View.OnClickListener {

        private final String LOG_TAG = "ViewHolder";

        public ImageView mImageView;
        public int aspect_ratio;
        public Long id;
        public String filename;
        public ImageView mSelectedCheckImageView;

        public ViewHolder(View view) {
            super(view, mMultiSelector);

            view.setLongClickable(true);
            view.setClickable(true);

            view.setOnClickListener(this);
            view.setOnLongClickListener(this);

            mImageView = (ImageView) view.findViewById(R.id.item_imageview);

            //mSelectedCheckImageView = (ImageView) view.findViewById(R.id.selected_check_imageview);
            //mSelectedCheckImageView.setVisibility(View.INVISIBLE);
        }


        @Override
        public boolean onLongClick(View view) {

            Log.d(LOG_TAG, "onLongClick");

            if (!mMultiSelector.isSelectable()) {

                Log.d(LOG_TAG, "onLongClick, entering Context Action mode and adding id:" + id);

                ((AppCompatActivity) mContext).startSupportActionMode(mActionModeCallback);
                mMultiSelector.setSelectable(true);
                mMultiSelector.setSelected(ViewHolder.this, true);


                mFileNameListId.put(id, filename);

                //mSelectedCheckImageView.setVisibility(View.VISIBLE);


                return true;
            }
            return false;
        }

        @Override
        public void onClick(View view) {
            Log.d(LOG_TAG, "onClick");

            if (mMultiSelector.tapSelection(ViewHolder.this)) {

                if (mMultiSelector.isSelected(getAdapterPosition(), id)) {
                    Log.d(LOG_TAG, "onClick, selected id: " + id);
                    mFileNameListId.put(id, filename);
                    //mSelectedCheckImageView.setVisibility(View.VISIBLE);

                } else {
                    Log.d(LOG_TAG, "onClick, unselected id: " + id);
                    mFileNameListId.remove(id);
                    //mSelectedCheckImageView.setVisibility(View.INVISIBLE);
                }
            }
            //Not in selectable mode
            else {
                Log.d(LOG_TAG, "onClick, normal mode");

                if (mCallBack != null)
                {
                    mCallBack.onClick(getAdapterPosition());
                }
            }

        }

    }
}
