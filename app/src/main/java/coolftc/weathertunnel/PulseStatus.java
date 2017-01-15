package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;
import java.io.IOException;
import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/* Android works to keep itself operating smoothly by aggressively cleaning up tasks that do not seem
 * to be getting used.  This means Services/Threads cannot expect to be long running, even if they do
 * not use up a lot of resources.  Services/Threads should do something and then exit.  If that thing
 * needs to be done periodically, use the AlarmManager to schedule calls to it.   
 */
public class PulseStatus extends IntentService {

    private static final String SRV_NAME = "PulseService";  // Name can be used for debugging.
    private ClimateDB climate;
    @SuppressWarnings("unused")
    boolean isStale;    // Not using this at the moment.

    public PulseStatus() {
        super(SRV_NAME);
    }

    @Override
    protected void onHandleIntent(Intent hint) {

        Context appContext = getApplicationContext();
        climate = new ClimateDB(appContext);  // Be sure to close this before leaving the thread.
        ConnectivityManager network = (ConnectivityManager)appContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        try{
            ExpClass.LogIN(KEVIN_SPEAKS, "The Pulse Status is starting!");
    
            // Don't bother with any of this if the network is not available.
            NetworkInfo priNet = network.getActiveNetworkInfo();
            if(priNet==null || !priNet.isConnected()){ 
                climate.close();
                return; 
            }
            
            // Get the items to update
            Status[] stations = queryWeatherStations();
            
            // Check if the current location has changed
            if(stations[0].Special.equalsIgnoreCase(LOC_SPECIAL_UPDT)){
                String where = stations[0].Latitude + "," + stations[0].Longitude;
                Status update =  new Status();
                if(update.Search(appContext, where, "", LOC_TYPE_EXACT) == 1){
                    update.Id = Integer.parseInt(LOC_CURRENT_ID);
                    update.Special = LOC_SPECIAL;
                    chgWatchItem(update);
                    stations[0] = update;
                }
            }
            
            // Debounce: Do not bother updating the weather stations if done < DB_PULSE_DEBOUNCE msecs ago
            try{
                if(KTime.CalcDateDifference(stations[0].LocalTime, (String) KTime.ParseNow(DB_fmtDate3339k, stations[0].TimeZone), DB_inputDate3339) < DB_PULSE_DEBOUNCE){
                    ExpClass.LogIN(KEVIN_SPEAKS, "The Pulse Status is exiting: it recently updated the data.");
                    climate.close();
                    return;                
                }
            } catch (ExpParseToCalendar ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".badStatusDate"); } // Sometimes the date may be invalid, just move along.
            
            // Update the status items
            Status[] stationsUpdate;
            stationsUpdate = updateWeatherStations(appContext, stations);
            
            // Save the updates
            saveWeatherStations(stationsUpdate);
            climate.close();
            ExpClass.LogIN(KEVIN_SPEAKS, "The Pulse Status is exiting!");
            
        }catch (IOException ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".run-IOExp"); 
            climate.close();
        }catch (Exception ex){
            ExpClass.LogEX(ex, this.getClass().getName() + ".run-OthExp"); 
            climate.close();
        }
    }

    // Returns the list of current locations being watched.  To avoid cursor management issues, the data is returned directly.
    private Status[] queryWeatherStations() throws Exception{
        SQLiteDatabase db = climate.getReadableDatabase();
        String[] filler = {};
        Cursor cursor = db.rawQuery(DB_StatusAll, filler);
        try{
            Status[] dataPoints = new Status[cursor.getCount()];
            int i = 0;
            while(cursor.moveToNext()){
                Status local = new Status();
                local.Id = cursor.getInt(cursor.getColumnIndex(ClimateDB.STATUS_ID));
                local.PublicCode = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_STAT_PUB));
                local.PrivateCode = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_STAT_PRI));
                local.PrivateCodeB = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_STAT_PRI2));
                local.PublicDist = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_DIST_PUB));
                local.PrivateDist = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_DIST_PRI));
                local.PrivateDistB = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_DIST_PRI2));
                local.Zip = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_ZIP));
                local.City = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_CITY));
                local.State = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_STATE));
                local.Country = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_CNTY));
                local.Latitude = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_LATI));
                local.Longitude = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_LONG));
                local.TimeZone = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_TZONE));
                local.LocalTime = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_TIME));
                local.Temperature = Float.toString(cursor.getFloat(cursor.getColumnIndex(ClimateDB.STATUS_TEMP)));
                local.Special = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_NOTE));
                local.Condition = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_COND));
                local.ConditionIcon = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_CICON));
                local.IsAlert = cursor.getInt(cursor.getColumnIndex(ClimateDB.STATUS_ALERT));
                dataPoints[i++] = local;
            }
            cursor.close();
            return dataPoints;
        } catch(Exception ex){ cursor.close(); ExpClass.LogEX(ex, this.getClass().getName() + ".queryWeatherStations"); throw ex; }
    }
    
    /* Update the location for an item. */
    private long chgWatchItem(Status item) {
        SQLiteDatabase db = climate.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ClimateDB.STATUS_STAT_PRI, item.PrivateCode);
        values.put(ClimateDB.STATUS_STAT_PRI2, item.PrivateCodeB);
        values.put(ClimateDB.STATUS_STAT_PUB, item.PublicCode);
        values.put(ClimateDB.STATUS_DIST_PUB, item.getPublicDist());
        values.put(ClimateDB.STATUS_DIST_PRI, item.getPrivateDist());
        values.put(ClimateDB.STATUS_DIST_PRI2, item.getPrivateDistB());
        values.put(ClimateDB.STATUS_ZIP, item.Zip);
        values.put(ClimateDB.STATUS_CITY, item.City);
        values.put(ClimateDB.STATUS_STATE, item.State.length()==0?item.Country:item.State); // non-US has no state
        values.put(ClimateDB.STATUS_CNTY, item.Country);
        values.put(ClimateDB.STATUS_LATI, item.Latitude);
        values.put(ClimateDB.STATUS_LONG, item.Longitude);
        values.put(ClimateDB.STATUS_TZONE, item.TimeZone);
        values.put(ClimateDB.STATUS_TIME, item.LocalTime);
        values.put(ClimateDB.STATUS_TEMP, item.getTemperature());
        values.put(ClimateDB.STATUS_NOTE, item.Special);
        values.put(ClimateDB.STATUS_COND, item.Condition);
        values.put(ClimateDB.STATUS_CICON, item.ConditionIcon);
        values.put(ClimateDB.STATUS_ALERT, item.IsAlert);
        String where = "_id = " + item.Id;
        String[] filler = {};
        return db.update(ClimateDB.STATUS_TABLE, values, where, filler);
    }
    
    // Get the latest data off of the net.
    private Status[] updateWeatherStations(Context context, Status[] stations) throws IOException {
        isStale = false;
        Status[] updates = new Status[stations.length];
        for(int i = 0; i < stations.length; ++i){
            stations[i].Update(UPD_STATIONS_CLOSEST_SL, context);
            updates[i] = stations[i];
            if(stations[i].isDataStale) isStale = true;
        }
        return updates;
    }
    
    // Save the latest conditions to the status table.
    private void saveWeatherStations(Status[] items) {
        SQLiteDatabase db = climate.getWritableDatabase();
        for (Status item : items) {
            ContentValues values = new ContentValues();
            values.put(ClimateDB.STATUS_TEMP, item.getTemperature());
            values.put(ClimateDB.STATUS_COND, item.Condition);
            values.put(ClimateDB.STATUS_CICON, item.ConditionIcon);
            values.put(ClimateDB.STATUS_TIME, item.LocalTime);
            values.put(ClimateDB.STATUS_ALERT, item.IsAlert);
            String where = "_id = " + item.Id;
            String[] filler = {};
            db.update(ClimateDB.STATUS_TABLE, values, where, filler);
        }
    }

}
