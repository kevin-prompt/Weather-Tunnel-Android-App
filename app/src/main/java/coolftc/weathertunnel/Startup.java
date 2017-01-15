package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;

/**
 * This class exists solely to start up the alarm that fires the service that updates the widgets.
 * Alarms are all shut down upon power cycle, so we need to restart them in case the user does not
 * happen to go into the main application right away.
 */
public class Startup extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent intentSV = new Intent(context, PulseStatus.class);                  
        PendingIntent pIntentSV = PendingIntent.getService(context, 0, intentSV, 0);
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if(getWidgetCnt(context) > 0){    // If widgets on desktop, start AlarmManager to keep them up to date.
            ExpClass.LogIN(KEVIN_SPEAKS, "Turning on Alarms at bootup.");
            alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), UPD_DBASE_TM, pIntentSV);
        } 
    }
    // Returns a count of how many widgets exist
    private int getWidgetCnt(Context context) {
        int cnt = 0;
        Cursor cursor = null;
        ClimateDB climate = new ClimateDB(context); 
        try {
            SQLiteDatabase db = climate.getReadableDatabase();
            String[] filler = {};
            cursor = db.rawQuery(DB_StatusAnyWidget, filler);
            cnt = cursor.getCount();
            cursor.close();
            climate.close();
        } catch (Exception ex){ 
            ExpClass.LogEX(ex, this.getClass().getName() + ".getWidgetCnt"); 
            if(cursor!=null) cursor.close(); 
            climate.close(); 
        }
        return cnt;
    }

}

