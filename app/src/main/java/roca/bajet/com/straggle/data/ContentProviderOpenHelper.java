package roca.bajet.com.straggle.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Arnold on 3/6/2017.
 */

public class ContentProviderOpenHelper extends SQLiteOpenHelper {

    private static final String NAME = ContentProviderDbSchema.DB_NAME;
    private static final int VERSION = 10;
    public static long DEFAULT_USER_ID;
    public static long DEFAULT_CURRENT_LOCATION_ID = 1;

    public ContentProviderOpenHelper(Context c) {
        super(c, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        sqLiteDatabase.execSQL(ContentProviderDbSchema.DDL_CREATE_TBL_USERS);
        sqLiteDatabase.execSQL(ContentProviderDbSchema.DDL_CREATE_TBL_IMAGETEXTURES);
        sqLiteDatabase.execSQL(ContentProviderDbSchema.DDL_CREATE_INDEX);
        sqLiteDatabase.execSQL(ContentProviderDbSchema.DDL_CREATE_TBL_CURRENT_LOCATION);

        ContentValues cv = new ContentValues();
        cv.put(ContentProviderDbSchema.Users.COL_USERNAME, "DEFAULT_USER");
        sqLiteDatabase.insert(ContentProviderDbSchema.TBL_USERS, null, cv);

        cv.clear();
        cv.put(ContentProviderDbSchema.CurrentLocation._ID, DEFAULT_CURRENT_LOCATION_ID);
        sqLiteDatabase.insert(ContentProviderDbSchema.TBL_CURRENT_LOCATION, null, cv);


    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(ContentProviderDbSchema.DDL_DROP_TBL_IMAGETEXTURES);
        sqLiteDatabase.execSQL(ContentProviderDbSchema.DDL_DROP_TBL_USERS);
        sqLiteDatabase.execSQL(ContentProviderDbSchema.DDL_DROP_TBL_LOCATION);
        sqLiteDatabase.execSQL(ContentProviderDbSchema.DDL_DROP_INDEX);
        onCreate(sqLiteDatabase);
    }
}
