package roca.bajet.com.straggle.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Arnold on 3/6/2017.
 */

public class ContentProviderDbSchema {

    public static final String DB_NAME = "contentprovider.db";
    public static final String TBL_IMAGETEXTURES = "imagetextures";
    public static final String TBL_USERS = "users";

    public static final String AUTHORITY = "roca.bajet.com.straggle";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final String PATH_IMAGETEXTURES = "imagetextures";
    public static final String PATH_USERS = "users";


    public static final String DDL_CREATE_TBL_IMAGETEXTURES =
            "CREATE TABLE " + TBL_IMAGETEXTURES + " ( " +
            ImageTextures._ID + "   INTEGER PRIMARY KEY AUTOINCREMENT, " +
            ImageTextures.COL_FILENAME + "  TEXT, " +
            ImageTextures.COL_LON + "  TEXT, " +
            ImageTextures.COL_LAT + "  TEXT, " +
            " FOREIGN KEY ( " + ImageTextures.COL_USER_ID + " ) REFERENCES " +
            TBL_USERS + " ( " + Users._ID + " ) ON DELETE CASCADE " +
            " ) ";

    public static final String DDL_CREATE_TBL_USERS =
            "CREATE TABLE " + TBL_USERS + " ( " +
            Users._ID + "  INTEGER PRIMARY KEY AUTOINCREMENT, " +
            Users.COL_USERNAME + "  TEXT ";

    public static final String DDL_DROP_TBL_IMAGETEXTURES = "DROP TABLE IF EXITS " + TBL_IMAGETEXTURES;
    public static final String DDL_DROP_TBL_USERS = "DROP TABLE IF EXITS " + TBL_USERS;

    public static final class ImageTextures implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_IMAGETEXTURES).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + "/" + PATH_IMAGETEXTURES;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + "/" + PATH_IMAGETEXTURES;

        public static final String COL_FILENAME = "filename";
        public static final String COL_USER_ID = "user_id";
        public static final String COL_LON = "lon";
        public static final String COL_LAT = "lat";

        public static final String SORT_ORDER_DEFAULT = COL_FILENAME + " ASC";

        public static Uri buildImageTextureUriWithFileName(String filename)
        {
            return CONTENT_URI.buildUpon().appendPath(filename).build();
        }
    }

    public static final class Users implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_USERS).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + "/" + PATH_USERS;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + "/" + PATH_USERS;

        public static final String COL_USERNAME = "username";

        public static final String SORT_ORDER_DEFAULT = BaseColumns._ID + " ASC";

        public static Uri buildUsersUriWithUserName(String username)
        {
            return CONTENT_URI.buildUpon().appendPath(username).build();
        }
    }


}
