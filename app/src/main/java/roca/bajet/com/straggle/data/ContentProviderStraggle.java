package roca.bajet.com.straggle.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import static roca.bajet.com.straggle.data.ContentProviderDbSchema.AUTHORITY;
import static roca.bajet.com.straggle.data.ContentProviderDbSchema.ImageTextures;
import static roca.bajet.com.straggle.data.ContentProviderDbSchema.PATH_IMAGETEXTURES;
import static roca.bajet.com.straggle.data.ContentProviderDbSchema.PATH_USERS;
import static roca.bajet.com.straggle.data.ContentProviderDbSchema.TBL_IMAGETEXTURES;
import static roca.bajet.com.straggle.data.ContentProviderDbSchema.TBL_USERS;
import static roca.bajet.com.straggle.data.ContentProviderDbSchema.Users;

/**
 * Created by Arnold on 3/6/2017.
 */

public class ContentProviderStraggle extends ContentProvider {
    private static final String LOG_TAG = "ContentProviderMovie";

    private static final int URI_MATCH_IMAGETEXTURES = 1;
    private static final int URI_MATCH_IMAGETEXTURES_ID = 2;
    private static final int URI_MATCH_IMAGETEXTURES_FILENAME = 3;
    private static final int URI_MATCH_USERS = 4;
    private static final int URI_MATCH_USERS_ID = 5;
    private static final int URI_MATCH_USERS_USERNAME = 6;

    private static final UriMatcher sUriMatcher;
    private static final SQLiteQueryBuilder sImageTexturesJoinUserQueryBuilder;
    private static final String sImageTexturesJoinUserStr = TBL_IMAGETEXTURES +
            " INNER JOIN " + TBL_USERS +
            " ON " + TBL_IMAGETEXTURES + "." + ImageTextures.COL_USER_ID +
            " = " + TBL_USERS + "." + Users._ID;
    private ContentProviderOpenHelper mHelper = null;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, PATH_IMAGETEXTURES, URI_MATCH_IMAGETEXTURES);
        sUriMatcher.addURI(AUTHORITY, PATH_IMAGETEXTURES + "/#", URI_MATCH_IMAGETEXTURES_ID);
        sUriMatcher.addURI(AUTHORITY, PATH_IMAGETEXTURES + "/*", URI_MATCH_IMAGETEXTURES_FILENAME);
        sUriMatcher.addURI(AUTHORITY, PATH_USERS, URI_MATCH_USERS);
        sUriMatcher.addURI(AUTHORITY, PATH_USERS + "/#", URI_MATCH_USERS_ID);
        sUriMatcher.addURI(AUTHORITY, PATH_USERS + "/*", URI_MATCH_USERS_USERNAME);

        sImageTexturesJoinUserQueryBuilder = new SQLiteQueryBuilder();
        sImageTexturesJoinUserQueryBuilder.setTables(sImageTexturesJoinUserStr);
    }


    @Override
    public boolean onCreate() {
        mHelper = new ContentProviderOpenHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteDatabase db = mHelper.getReadableDatabase();
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        String where;
        Cursor cursor;

        switch (sUriMatcher.match(uri))
        {
            case URI_MATCH_IMAGETEXTURES:
                builder.setTables(TBL_IMAGETEXTURES);
                if (TextUtils.isEmpty(sortOrder))
                {
                    sortOrder = ImageTextures.SORT_ORDER_DEFAULT;
                }

                cursor = builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case URI_MATCH_IMAGETEXTURES_ID:
                builder.setTables(TBL_IMAGETEXTURES);

                if (selection != null)
                {
                    where = ImageTextures.COL_USER_ID + " = ? AND " + selection;
                }
                else{
                    where = ImageTextures.COL_USER_ID + " = ?";
                }


                cursor = builder.query(db, projection, where, new String[] {uri.getLastPathSegment()}, null, null, sortOrder);
                break;

            case URI_MATCH_IMAGETEXTURES_FILENAME:
                builder.setTables(TBL_IMAGETEXTURES);

                where = ImageTextures.COL_FILENAME + " = ?";
                cursor = builder.query(db, projection, where, new String[] {uri.getLastPathSegment()}, null, null, sortOrder);
                break;
            case URI_MATCH_USERS:
                builder.setTables(TBL_USERS);
                if (TextUtils.isEmpty(sortOrder))
                {
                    sortOrder = Users.SORT_ORDER_DEFAULT;
                }

                cursor = builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case URI_MATCH_USERS_ID:
                builder.setTables(TBL_USERS);

                where = Users._ID + " = ?";
                cursor = builder.query(db, projection, where, new String[] {uri.getLastPathSegment()}, null, null, sortOrder);

                break;
            case URI_MATCH_USERS_USERNAME:
                builder.setTables(TBL_USERS);

                where = Users.COL_USERNAME + " = ?";
                cursor = builder.query(db, projection, where, new String[] {uri.getLastPathSegment()}, null, null, sortOrder);

                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {

        switch (sUriMatcher.match(uri))
        {
            case URI_MATCH_IMAGETEXTURES:
                return ImageTextures.CONTENT_TYPE;
            case URI_MATCH_IMAGETEXTURES_ID:
                return ImageTextures.CONTENT_ITEM_TYPE;
            case URI_MATCH_IMAGETEXTURES_FILENAME:
                return ImageTextures.CONTENT_ITEM_TYPE;
            case URI_MATCH_USERS:
                return Users.CONTENT_TYPE;
            case URI_MATCH_USERS_ID:
                return Users.CONTENT_ITEM_TYPE;
            case URI_MATCH_USERS_USERNAME:
                return Users.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {

        final SQLiteDatabase db = mHelper.getReadableDatabase();
        long id = 0;

        switch (sUriMatcher.match(uri))
        {
            case URI_MATCH_IMAGETEXTURES:
                id = db.insert(TBL_IMAGETEXTURES, null, contentValues);
                break;
            case URI_MATCH_IMAGETEXTURES_ID:
                contentValues.put(ImageTextures.COL_USER_ID, uri.getLastPathSegment());
                id = db.insert(TBL_IMAGETEXTURES, null, contentValues);
                break;

            case URI_MATCH_IMAGETEXTURES_FILENAME:
                contentValues.put(ImageTextures.COL_FILENAME, uri.getLastPathSegment());
                id = db.insert(TBL_IMAGETEXTURES, null, contentValues);
                break;
            case URI_MATCH_USERS:
                id = db.insert(TBL_USERS, null, contentValues);
                break;
            case URI_MATCH_USERS_ID:
                contentValues.put(Users._ID, uri.getLastPathSegment());
                id = db.insert(TBL_USERS, null, contentValues);
                break;
            case URI_MATCH_USERS_USERNAME:
                contentValues.put(Users.COL_USERNAME, uri.getLastPathSegment());
                id = db.insert(TBL_USERS, null, contentValues);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        if (id > 0 )
        {
            return getUriForId(id, uri);
        }


        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int deleteCount = 0;

        switch (sUriMatcher.match(uri))
        {
            case URI_MATCH_IMAGETEXTURES:
                deleteCount = db.delete(TBL_IMAGETEXTURES, selection, selectionArgs);
                break;
            case URI_MATCH_IMAGETEXTURES_ID: {
                String where = ImageTextures.COL_USER_ID+ " = ?";
                String[] concateSelectionArgs;
                if (!TextUtils.isEmpty(selection)) {
                    where += " AND " + selection;
                    concateSelectionArgs = new String[selectionArgs.length + 1];
                    concateSelectionArgs[0] = uri.getLastPathSegment();
                    for (int i = 0; i < concateSelectionArgs.length; i++) {
                        concateSelectionArgs[i + 1] = selectionArgs[i];
                    }
                } else {
                    concateSelectionArgs = new String[]{uri.getLastPathSegment()};
                }


                deleteCount = db.delete(TBL_IMAGETEXTURES, where, concateSelectionArgs);
                break;
            }
            case URI_MATCH_IMAGETEXTURES_FILENAME: {
                String where = ImageTextures.COL_FILENAME + " = ?";
                String[] concateSelectionArgs;
                if (!TextUtils.isEmpty(selection)) {
                    where += " AND " + selection;
                    concateSelectionArgs = new String[selectionArgs.length + 1];
                    concateSelectionArgs[0] = uri.getLastPathSegment();
                    for (int i = 0; i < concateSelectionArgs.length; i++) {
                        concateSelectionArgs[i + 1] = selectionArgs[i];
                    }
                } else {
                    concateSelectionArgs = new String[]{uri.getLastPathSegment()};
                }


                deleteCount = db.delete(TBL_IMAGETEXTURES, where, concateSelectionArgs);
                break;
            }
            case URI_MATCH_USERS:
                deleteCount = db.delete(TBL_USERS, selection, selectionArgs);
                break;
            case URI_MATCH_USERS_ID: {
                String where = Users._ID + " = ?";
                String [] concateSelectionArgs;
                if ( !TextUtils.isEmpty(selection)){
                    where += " AND " + selection;
                    concateSelectionArgs = new String [selectionArgs.length+1];
                    concateSelectionArgs[0] = uri.getLastPathSegment();
                    for (int i = 0; i < concateSelectionArgs.length; i++)
                    {
                        concateSelectionArgs[i+1] = selectionArgs[i];
                    }
                }
                else {
                    concateSelectionArgs = new String []{uri.getLastPathSegment()};
                }


                deleteCount = db.delete(TBL_USERS, where ,  concateSelectionArgs);
                break;
            }
            case URI_MATCH_USERS_USERNAME: {
                String where = Users.COL_USERNAME + " = ?";
                String [] concateSelectionArgs;
                if ( !TextUtils.isEmpty(selection)){
                    where += " AND " + selection;
                    concateSelectionArgs = new String [selectionArgs.length+1];
                    concateSelectionArgs[0] = uri.getLastPathSegment();
                    for (int i = 0; i < concateSelectionArgs.length; i++)
                    {
                        concateSelectionArgs[i+1] = selectionArgs[i];
                    }
                }
                else {
                    concateSelectionArgs = new String []{uri.getLastPathSegment()};
                }


                deleteCount = db.delete(TBL_USERS, where ,  concateSelectionArgs);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        if (deleteCount > 0)
        {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return deleteCount;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int updateCount = 0;

        switch (sUriMatcher.match(uri))
        {
            case URI_MATCH_IMAGETEXTURES:
                updateCount = db.update(TBL_IMAGETEXTURES, contentValues, selection, selectionArgs);
                break;
            case URI_MATCH_IMAGETEXTURES_ID: {
                String where = ImageTextures.COL_USER_ID + " = ?";

                updateCount = db.update(TBL_IMAGETEXTURES, contentValues, where, new String [] {uri.getLastPathSegment()});
                break;
            }
            case URI_MATCH_IMAGETEXTURES_FILENAME: {
                String where = ImageTextures.COL_FILENAME + " = ?";
                updateCount = db.update(TBL_IMAGETEXTURES, contentValues, where, new String [] {uri.getLastPathSegment()});
                break;
            }
            case URI_MATCH_USERS:
                updateCount = db.update(TBL_USERS, contentValues, selection, selectionArgs);
                break;
            case URI_MATCH_USERS_ID: {
                String where = Users._ID + " = ?";

                updateCount = db.update(TBL_USERS, contentValues, where, new String [] {uri.getLastPathSegment()});
                break;
            }
            case URI_MATCH_USERS_USERNAME: {
                String where = Users.COL_USERNAME + " = ?";
                updateCount = db.update(TBL_USERS, contentValues, where, new String [] {uri.getLastPathSegment()});
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        if (updateCount > 0)
        {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return updateCount;
    }

    private Uri getUriForId(long id, Uri uri) {
        if (id > 0) {
            Uri itemUri = ContentUris.withAppendedId(uri, id);

            // notify all listeners of changes:
            getContext().
                    getContentResolver().
                    notifyChange(itemUri, null);

            return itemUri;
        }
        // s.th. went wrong:
        throw new SQLException(
                "Problem while inserting into uri: " + uri);
    }
}
