package roca.bajet.com.straggle.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Arnold on 3/6/2017.
 */

public class ContentProviderDbSchema {

    public static final String DB_NAME = "contentprovider.db";
    public static final String TBL_IMAGETEXTURES = "imagetextures";
    public static final String TBL_CURRENT_LOCATION = "current_location";
    public static final String TBL_USERS = "users";
    public static final String INDEX_NAME = "index_gps";

    public static final String AUTHORITY = "roca.bajet.com.straggle";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final String PATH_IMAGETEXTURES = "imagetextures";
    public static final String PATH_USERS = "users";
    public static final String PATH_LOCATION = "current_location";


    public static final String DDL_CREATE_TBL_IMAGETEXTURES =
            "CREATE TABLE " + TBL_IMAGETEXTURES + " ( " +
                    ImageTextures._ID + "   INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ImageTextures.COL_FILENAME + "  TEXT, " +
                    ImageTextures.COL_LON + "  REAL, " +
                    ImageTextures.COL_LAT + "  REAL, " +
                    ImageTextures.COL_ANGLE + " REAL, " +
                    ImageTextures.COL_ASPECT_RATIO + " INTEGER, " +
                    ImageTextures.COL_URL + " TEXT, " +
                    ImageTextures.COL_DELETE_HASH + " TEXT, " +
                    ImageTextures.COL_USER_ID + " INTEGER, " +
                    " FOREIGN KEY ( " + ImageTextures.COL_USER_ID + " ) REFERENCES " +
                    TBL_USERS + " ( " + Users._ID + " ) ON DELETE CASCADE " +
                    " ) ";

    public static final String DDL_CREATE_TBL_CURRENT_LOCATION =
            "CREATE TABLE " + TBL_CURRENT_LOCATION + " ( " +
                    CurrentLocation._ID + "   INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    CurrentLocation.COL_LON + "  REAL, " +
                    CurrentLocation.COL_LAT + "  REAL, " +
                    CurrentLocation.COL_UTC_TIMESTAMP + " TEXT" +
                    " ) ";


    public static final String DDL_CREATE_TBL_USERS =
            "CREATE TABLE " + TBL_USERS + " ( " +
                    Users._ID + "  INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    Users.COL_USERNAME + "  TEXT ) ";

    public static final String DDL_CREATE_INDEX = "CREATE INDEX " + INDEX_NAME + " ON " + TBL_IMAGETEXTURES
            + " (" + ImageTextures.COL_LAT + ", " + ImageTextures.COL_LON + ")";

    public static final String DDL_DROP_INDEX = "DROP INDEX IF EXISTS " + INDEX_NAME;

    public static final String DDL_DROP_TBL_IMAGETEXTURES = "DROP TABLE IF EXISTS " + TBL_IMAGETEXTURES;
    public static final String DDL_DROP_TBL_USERS = "DROP TABLE IF EXISTS " + TBL_USERS;
    public static final String DDL_DROP_TBL_LOCATION = "DROP TABLE IF EXISTS " + TBL_CURRENT_LOCATION;


    public static final class ImageTextures implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_IMAGETEXTURES).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + "/" + PATH_IMAGETEXTURES;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + "/" + PATH_IMAGETEXTURES;

        public static final String COL_FILENAME = "filename";
        public static final String COL_USER_ID = "user_id";
        public static final String COL_LON = "lon";
        public static final String COL_LAT = "lat";
        public static final String COL_ANGLE = "angle";
        public static final String COL_ASPECT_RATIO = "aspect_ratio";
        public static final String COL_URL = "url";
        public static final String COL_DELETE_HASH = "delete_hash";

        public static final String SORT_ORDER_DEFAULT = COL_FILENAME + " ASC";

        public static Uri buildImageTextureUriWithFileName(String filename) {
            return CONTENT_URI.buildUpon().appendPath(filename).build();
        }

        public static Uri buildImageTextureUriWithUserId(long userId) {
            return ContentUris.withAppendedId(CONTENT_URI, userId);
        }


    }

    public static final class Users implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_USERS).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + "/" + PATH_USERS;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + "/" + PATH_USERS;

        public static final String COL_USERNAME = "username";

        public static final String SORT_ORDER_DEFAULT = BaseColumns._ID + " ASC";

        public static Uri buildUsersUriWithUserName(String username) {
            return CONTENT_URI.buildUpon().appendPath(username).build();
        }
    }

    public static final class CurrentLocation implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + "/" + PATH_LOCATION;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + "/" + PATH_LOCATION;
        public static final String COL_LON = "lon";
        public static final String COL_LAT = "lat";
        public static final String COL_UTC_TIMESTAMP = "utc_timestamp";
    }


}
