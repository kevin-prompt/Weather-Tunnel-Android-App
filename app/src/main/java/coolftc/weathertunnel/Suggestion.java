package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

/*********************************************************************************
 * This ContentProvider is used to support a type-ahead feature in the search dialog.
 * When a person is typing in a location, anything from the suggestions database 
 * matching what they are typing in is displayed.  The idea is to speed or help people
 * find locations of interest.  The WU can generally only deal with city names, so  
 * this is a way of extending that.  For example, the word Ballparks or Airports can
 * be offered and then a non-WUAPI call can be made.
 * A blank or a number (like zip code) is ignored for speed.
 * Individual words are searched for, not just the leading alphabetic characters.
 */
public class Suggestion extends ContentProvider {
    // Required Provider Constants.  Authority value is the same as the authoritySuggest resource string.
    public static final String AUTHORITY = "coolftc.android.weather.tunnel.suggestion";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/location");
    // Local Constants
    private static final int NOTHING = 1;
    private static final int NUMBER_ONLY = 2;
    private static final int LOCATION = 3;

    // Database Helper Class
    private SuggestionDB prepaqs;

    @Override
    public boolean onCreate() {
        prepaqs = new SuggestionDB(getContext());  
        return false;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Set up the Database.
        SQLiteDatabase db = prepaqs.getReadableDatabase();
        String[] filler = {};
        Cursor cursor;
        
        // Set up the URI.
        UriMatcher parseUri = new UriMatcher(UriMatcher.NO_MATCH);
        parseUri.addURI(AUTHORITY, "location/#", LOCATION);
        parseUri.addURI(AUTHORITY, "search_suggest_query", NOTHING);
        parseUri.addURI(AUTHORITY, "search_suggest_query/#", NUMBER_ONLY);
               
        // Create a cursor.
        switch (parseUri.match(uri)) {
        case NOTHING:       // Don't bother returning anything if nothing typed into search.
        case NUMBER_ONLY:   // Ignore numeric only input (zip codes), as we don't store them.
            cursor = db.rawQuery(DB_SuggestID + DATA_NA, filler);   // Docs. recommend returning empty cursor instead of null.
            return cursor;
        case LOCATION:  // Request one record based on a specific _ID in query.
            String id = uri.getLastPathSegment();
            cursor = db.rawQuery(DB_SuggestID + id, filler);
            return cursor;
        default:        // Request all records based on the value of the query string.
            String real = DB_SuggestLike.replace("ZZZ", uri.getLastPathSegment());
            cursor = db.rawQuery(real, filler);
            return cursor;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        // Not Currently Supported.
        return 0;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        // Not Currently Supported.
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        // Not Currently Supported.
        return null;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Not Currently Supported.
        return 0;
    }

}
