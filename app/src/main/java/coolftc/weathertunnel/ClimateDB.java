package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/** 
 *  This class allows access to the database & tables that hold the climate information.
 *  NOTE: There are 3 primary objects you will work with.  The ClimateDB, Readable or
 *  Writable Databases and Cursors.
 *  Cursor   - Call close() as soon as possible (unless using a managed cursor, then never call close()).
 *  Database - Never call close().  If using multiple ClimateDB's (SQLiteOpenHelper) across threads, 
 *              you may get "database is locked" exceptions on occasion.
 *  ClimateDB- Call close() as little as possible, but always before the context ends. Try to create 
 *              as few of these as possible. If you just have one, the "database is locked" problems
 *              should be very rare.
 * 
 *  Note: Creating the ClimateDB does not try to do a create/upgrade.  That only 
 *  happens upon first read/write database call.  So having (at least the first) 
 *  read/write database call in an asynchronous thread will be good for performance.  
 */
class ClimateDB extends SQLiteOpenHelper {
    // Database Info
    private static final String DATABASE_NAME = "climate.db";
    private static final int DATABASE_VERSION = 2;
    // Status Table & Attributes
    static final String STATUS_TABLE = "status";
    static final String STATUS_ID = BaseColumns._ID;
    static final String STATUS_ORDER = "usersort";
    static final String STATUS_STAT_PUB = "pub";
    static final String STATUS_STAT_PRI = "pri";
    static final String STATUS_STAT_PRI2 = "pri2";
    static final String STATUS_DIST_PUB = "pubdist";
    static final String STATUS_DIST_PRI = "pridist";
    static final String STATUS_DIST_PRI2 = "pridist2";
    static final String STATUS_ZIP = "zip";
    static final String STATUS_CITY = "city";
    static final String STATUS_STATE = "state";
    static final String STATUS_CNTY = "country";
    static final String STATUS_LATI = "latitude";
    static final String STATUS_LONG = "longitude";
    static final String STATUS_TZONE = "timezone";
    static final String STATUS_TIME = "date";
    static final String STATUS_TEMP = "temperature";
    static final String STATUS_NOTE = "special";
    static final String STATUS_COND = "condition";
    static final String STATUS_CICON = "conditionicon";
    static final String STATUS_ALERT = "alert";
    static final String STATUS_ONSLATE = "onslate";
    static final String STATUS_ONWIDGET = "onwidget";
    // API Cache Table & Attributes
    static final String APICACHE_TABLE = "apicache";
    static final String APICACHE_TYPE = "apitype";
    static final String APICACHE_LINK = "stationcode";
    static final String APICACHE_TIME = "lastupdate";
    static final String APICACHE_DATA = "response";
   
    // Extra helper data
    // see http://www.sqlite.org/datatype3.html for information about sqlite datatypes.
    private static final String TABLE_TYPE_TEXT = " text";
    private static final String TABLE_TYPE_INT = " integer";
    //private static final String TABLE_TYPE_DATE = " date";      // ends up "numeric" in table
    private static final String TABLE_TYPE_FLOAT = " real";
    private static final String TABLE_TYPE_BOOL = " boolean";   // ends up "numeric" in table
    private static final String TABLE_DELIMIT = ",";
    static final int SQLITE_TRUE = 1;                    // Boolean is not supported in the
    static final int SQLITE_FALSE = 0;                   // db, so we have to improvise.
    
    ClimateDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    ClimateDB(Context context, CursorFactory cf) {
        super(context, DATABASE_NAME, cf, DATABASE_VERSION);
    }

    /* This method is called when a database is not found, so we create the tables here. */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Status Table.
        db.execSQL("create table " + STATUS_TABLE + " (" +
                STATUS_ID + TABLE_TYPE_INT + " primary key autoincrement" + TABLE_DELIMIT +
                STATUS_ORDER + TABLE_TYPE_INT + TABLE_DELIMIT +
                STATUS_STAT_PUB + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                STATUS_STAT_PRI + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                STATUS_STAT_PRI2 + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                STATUS_DIST_PUB + TABLE_TYPE_INT + TABLE_DELIMIT +
                STATUS_DIST_PRI + TABLE_TYPE_INT + TABLE_DELIMIT +
                STATUS_DIST_PRI2 + TABLE_TYPE_INT + TABLE_DELIMIT +
                STATUS_ZIP + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                STATUS_CITY + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                STATUS_STATE + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                STATUS_CNTY + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                STATUS_LATI + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                STATUS_LONG + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                STATUS_TZONE + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                STATUS_TIME + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                STATUS_TEMP + TABLE_TYPE_FLOAT + TABLE_DELIMIT +
                STATUS_NOTE + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                STATUS_COND + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                STATUS_CICON +  TABLE_TYPE_TEXT + TABLE_DELIMIT +
                STATUS_ALERT + TABLE_TYPE_BOOL + TABLE_DELIMIT +
                STATUS_ONSLATE + TABLE_TYPE_BOOL + TABLE_DELIMIT +
                STATUS_ONWIDGET + TABLE_TYPE_BOOL + ");");
        
        // Initialize Status Table
        newWatchItem(db, LOC_SanFrancico, 0);
        newWatchItem(db, LOC_MexicoCity, 1);
        newWatchItem(db, LOC_Portland, 2);
        newWatchItem(db, LOC_HongKong, 3);
        
        // Create API Cache Table
        db.execSQL("create table " + APICACHE_TABLE + " (" +
                APICACHE_TYPE + TABLE_TYPE_TEXT + " primary key" + TABLE_DELIMIT +
                APICACHE_LINK + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                APICACHE_TIME + TABLE_TYPE_INT + TABLE_DELIMIT +
                APICACHE_DATA +  TABLE_TYPE_TEXT + ");");
        // Initialize API Cache Table
        newCacheType(db, APICACHE_TYPE_PUB);
        newCacheType(db, APICACHE_TYPE_PRI);
        
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // When there is an older database found, this method is called on startup.
        // The version number presented is the basis for "older".
        
        // This upgrade will wipe all data.  Remove the tables.
        db.execSQL("DROP TABLE IF EXISTS " + STATUS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + APICACHE_TABLE);
        
        // Recreates the database with a new version
        onCreate(db);
    } 

    /* Each time the API is going to be called, it checks to see if that type of API has
     * a cache record.  If it does, then it checks if the key (link) data matches. If
     * there is a match and it is not too old, that data is used instead of making a call
     * to the API.  This is a single record cache, so there is one record for each type. */
    private void newCacheType(SQLiteDatabase db, String type) {
        ContentValues values = new ContentValues();
        values.put(ClimateDB.APICACHE_TYPE, type);
        values.put(ClimateDB.APICACHE_LINK, "");
        values.put(ClimateDB.APICACHE_TIME, 0);
        values.put(ClimateDB.APICACHE_DATA, "");
        db.insert(ClimateDB.APICACHE_TABLE, null, values);
    }
    
    /* Initialize the Slate with a few locations so it does not look naked. */
    private long newWatchItem(SQLiteDatabase db, String[] items, int order){
        ContentValues values = new ContentValues();
        values.put(ClimateDB.STATUS_ORDER, order);
        values.put(ClimateDB.STATUS_STAT_PUB, items[0]);
        values.put(ClimateDB.STATUS_STAT_PRI, items[1]);
        values.put(ClimateDB.STATUS_STAT_PRI2, items[2]);
        values.put(ClimateDB.STATUS_DIST_PUB, getDistance(items[3]));
        values.put(ClimateDB.STATUS_DIST_PRI, getDistance(items[4]));
        values.put(ClimateDB.STATUS_DIST_PRI2, getDistance(items[5]));
        values.put(ClimateDB.STATUS_ZIP, items[6]);
        values.put(ClimateDB.STATUS_CITY, items[7]);
        values.put(ClimateDB.STATUS_STATE, items[8].length()==0?items[9]:items[8]); // non-US has no state
        values.put(ClimateDB.STATUS_CNTY, items[9]);
        values.put(ClimateDB.STATUS_LATI, items[10]);
        values.put(ClimateDB.STATUS_LONG, items[11]);
        values.put(ClimateDB.STATUS_TZONE, items[12]);
        values.put(ClimateDB.STATUS_TIME, items[13]);
        // These are all values that are dynamic, so we can leave they the same for all initializations
        values.put(ClimateDB.STATUS_TEMP, 0);
        values.put(ClimateDB.STATUS_NOTE, "");
        values.put(ClimateDB.STATUS_COND, "Scattered Clouds");
        values.put(ClimateDB.STATUS_CICON, "partlycloudy");
        values.put(ClimateDB.STATUS_ALERT, 0);
        values.put(ClimateDB.STATUS_ONSLATE, 1);
        values.put(ClimateDB.STATUS_ONWIDGET, 0);
        return db.insert(ClimateDB.STATUS_TABLE, null, values);
    } 
    
    private long getDistance(String dist){
        try { return Integer.parseInt(dist); } 
        catch (NumberFormatException ex){ return 1000; }
    }
    
}
