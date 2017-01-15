package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import android.app.AlarmManager;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

/* This is an all in one sort of class, which generates a list by inserting a bunch
 * of rows in the default ListActivity.  If someone picks a city, the database id is
 * passed to the widget manager to do the real work. 
 */
public class QuickViewList extends ListActivity {

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);     // In case user backs out.
        
        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        // If no id, exit.
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
        
        // Create a list of all the locations currently tracked.
        String[] StatusMapFROM = {ClimateDB.STATUS_ID, ClimateDB.STATUS_CITY, ClimateDB.STATUS_STATE, ClimateDB.STATUS_NOTE};
        int[] StatusMapTO = {R.id.qvrow_Id, R.id.qvrowCity, R.id.qvrowState, R.id.qvrowSpecial};
        SimpleAdapter adapter = new SimpleAdapter(this, getWatchCityList(),R.layout.quickview_row, StatusMapFROM, StatusMapTO);
        setListAdapter(adapter);
        
        // What happens when they select an item? Return it to the widget.
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView rowId = (TextView)view.findViewById(R.id.qvrow_Id);
                if(rowId.getText().length() > 0){
                    setChoice(rowId.getText());
                    // Return the original widget id.
                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                    setResult(RESULT_OK, resultValue);
                }
                finish();
            }  
        });
        
    }
    
    /* The regular Widget App Provider class is used to create the widget on the desktop.
     * Once created, we need to update the database to register that the data is being
     * tracked by a widget (so it will not be deleted).  Also, we need to link the widget
     * id to the database id, so we know what data each widget is displaying.
     */
    public void setChoice(CharSequence id){
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        QuickView.updateAppWidget(this, appWidgetManager, widgetId, id);
        setOnWidget(id, true);
        setWidgetAlarm();
        recordAppWidget(id);
    }

    /* This will create a link between the widget id and the database id.  This allows
     * updating of the widgets later. */
    private void recordAppWidget(CharSequence id) {
        SharedPreferences widgetMap = getSharedPreferences(SP_WIDGET_MAP, Context.MODE_PRIVATE);
        SharedPreferences.Editor ali = widgetMap.edit();
        ali.putString(String.valueOf(widgetId), (String) id);
        ali.apply();
    }

    // Let the status table know that a widget is/isn't using this data.
    private void setOnWidget(CharSequence id, boolean value) {
        ClimateDB climate = new ClimateDB(this);      // Be sure to close this before leaving.
        int onwidget = value?ClimateDB.SQLITE_TRUE:ClimateDB.SQLITE_FALSE;
        SQLiteDatabase db = climate.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(ClimateDB.STATUS_ONWIDGET, onwidget);
            String[] filler = {};
            String where = "_id = " + id;
            db.update(ClimateDB.STATUS_TABLE, values, where, filler);
            climate.close();
        } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".setOnWidget"); climate.close(); }
    }
    
    // When the first widget is added, we do not want to wait until the main app runs to start up the Alarm.
    // Alarms added with the same intent as an existing alarm will cancel the previous one, so this is safe.
    // While we are at it, might as well trigger a service update to DB.
    private void setWidgetAlarm(){
        Intent intentSV = new Intent(this, PulseStatus.class);                  // Update DB
        PendingIntent pIntentSV = PendingIntent.getService(this, 0, intentSV, 0);
        AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), UPD_DBASE_TM, pIntentSV);
        startService(intentSV);
    }
    
    // Build up a list of data with column names that will be used to populate the list.
    private ArrayList<Map<String,String>> getWatchCityList(){
        ArrayList<Map<String, String>> dataPoints = new ArrayList<>();
        ClimateDB climate = new ClimateDB(this);      // Be sure to close this before leaving.
        SQLiteDatabase db = climate.getReadableDatabase();
        String[] filler = {};
        Cursor cursor = db.rawQuery(DB_StatusForWidget, filler);
        try {
            while(cursor.moveToNext()){
                Map<String, String> local = new TreeMap<>();
                local.put(ClimateDB.STATUS_ID, cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_ID)));
                local.put(ClimateDB.STATUS_CITY, cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_CITY)) + ", ");
                String holdState = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_STATE));
                if(holdState.length() == 0) holdState = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_CNTY));
                holdState = holdState + " "; // extra space for italics
                local.put(ClimateDB.STATUS_STATE, holdState);
                local.put(ClimateDB.STATUS_NOTE, cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_NOTE)));
                dataPoints.add(local);
            }
            cursor.close();
            climate.close();
        } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getWatchCityList"); cursor.close(); climate.close(); }
        return dataPoints;
    }    
}
