package coolftc.weathertunnel;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;


/** 
 *  This class allows access to the database & tables that hold the climate information.
 *  NOTE: There are 3 primary objects you will work with.  The SuggestionDB, Readable or
 *  Writable Databases and Cursors.
 *  Cursor   - call close() as soon as possible (unless using a managed cursor, then never call close()).
 *  Database - never call close().
 *  SuggestionDB- call close() as little as possible, but always before the context ends. 
 * 
 *  Note: Creating the a SuggestionDB does not try to do a create/upgrade.  That only 
 *  happens upon first read/write database call.  So having (at least the first) 
 *  read/write database call in an async. thread will be good for performance.  
 */
class SuggestionDB extends SQLiteOpenHelper {
    // Database Info
    private static final String DATABASE_NAME = "prepaqlist.db";
    private static final int DATABASE_VERSION = 1;
    // Suggest Table & Attributes
    private static final String SUGGEST_TABLE = "suggest";
    private static final String SUGGEST_ID = BaseColumns._ID;
    static final String SUGGEST_TEXT = SearchManager.SUGGEST_COLUMN_TEXT_1;
    static final String SUGGEST_SUPPORT = "support";
    
    // Extra helper data
    // see http://www.sqlite.org/datatype3.html for information about sqlite datatypes.
    private static final String TABLE_TYPE_TEXT = " text";
    private static final String TABLE_TYPE_INT = " integer";
    //private static final String TABLE_TYPE_DATE = " date";      // ends up "numeric" in table
    //private static final String TABLE_TYPE_FLOAT = " real";
    //private static final String TABLE_TYPE_BOOL = " boolean";   // ends up "numeric" in table
    private static final String TABLE_DELIMIT = ",";
    public static final int SQLITE_TRUE = 1;                    // Boolean is not supported in the
    public static final int SQLITE_FALSE = 0;                   // db, so we have to improvise.

    SuggestionDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    public SuggestionDB(Context context, CursorFactory cf) {
        super(context, DATABASE_NAME, cf, DATABASE_VERSION);
    }

    /* This method is called when a database is not found, so we create the tables here. */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Suggest Table.
        db.execSQL("create table " + SUGGEST_TABLE + " (" +
                SUGGEST_ID + TABLE_TYPE_INT + " primary key autoincrement" + TABLE_DELIMIT +
                SUGGEST_TEXT + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                SUGGEST_SUPPORT +  TABLE_TYPE_TEXT + ");");
        
        // Initialize Suggestion Table
        //newSuggestion(db, PQ_BALLPARKS); We will activate these in version 2
        //newSuggestion(db, PQ_AIRPORTS);        
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // When there is an older database found, this method is called on startup.
        // The version number presented is the basis for "older".
        
        // This upgrade will wipe all data.  Remove the tables.
        db.execSQL("DROP TABLE IF EXISTS " + SUGGEST_TABLE);
        
        // Recreates the database with a new version
        onCreate(db);
    }
    
    /* Suggestions come in pairs, the first value is what will be suggested, the second value
     * is any supporting data, like the location data for the suggestion. */
    @SuppressWarnings("unused")
    private void newSuggestion(SQLiteDatabase db, String[] items) {
        
        for(int i = 0; i < items.length; i += 2){
            ContentValues values = new ContentValues();
            values.put(SuggestionDB.SUGGEST_TEXT, items[i]);
            values.put(SuggestionDB.SUGGEST_SUPPORT, items[i+1]);
            db.insert(SuggestionDB.SUGGEST_TABLE, null, values);
        }
    }
    
    

}
