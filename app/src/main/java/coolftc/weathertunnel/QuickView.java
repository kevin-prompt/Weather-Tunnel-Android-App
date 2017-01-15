package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;
import java.util.ArrayList;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.Html;
import android.view.View;
import android.widget.RemoteViews;

public class QuickView extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        int allWidgets = appWidgetIds.length;
        SharedPreferences widgetMap = context.getSharedPreferences(SP_WIDGET_MAP, 0);
        ExpClass.LogIN(KEVIN_SPEAKS, "Updating " + String.valueOf(allWidgets) + " Widgets.");
        
        /* It would be nice if we could update the DB before refreshing the widgets, but putting in
         * a wait (for the database to be update) is not desirable, since this method is also part 
         * of the widget selection process (so a user will see any delay). */

        // update all the widgets using the saved widgetid/databaseid mapping.
        for (int awId : appWidgetIds) {
            String dbid = widgetMap.getString(String.valueOf(awId), "");
            if(dbid.length() > 0){
                updateAppWidget(context, appWidgetManager, awId, dbid);
            }
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
    
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        SharedPreferences widgetMap = context.getSharedPreferences(SP_WIDGET_MAP, Context.MODE_PRIVATE);
        
        /**
         * Delete all the widgets using the saved widgetid/databaseid mapping.
         * Note: This only marks the status as no longer having a widget (and blanks out
         * the mapping of widgetid to databaseid).  It is possible that a record has
         * been deleted from the status table too, and now is an orphan. Orphan clean up
         * will be done as part of the normal status delete. */
        for (int awId : appWidgetIds) {
            String dbid = widgetMap.getString(String.valueOf(awId), "");
            if(dbid.length() > 0){
                setOnWidget(dbid, false, context);
                SharedPreferences.Editor ali = widgetMap.edit();
                ali.remove(String.valueOf(awId));
                ali.apply();   // cannot use .apply() on earlier versions of Android.
            }
        }
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        /**
         * This is called after all widgets are gone, to clean up any left over data.
         * First remove any widget/dbid references in the shared preferences data.
         * Second reset any existing OnWidget database fields to false.         */
        SharedPreferences widgetMap = context.getSharedPreferences(SP_WIDGET_MAP, Context.MODE_PRIVATE);
        SharedPreferences.Editor ali = widgetMap.edit();
        ali.clear();
        ali.apply();
        ArrayList<String> dbids = getAnyOnWidgets(context);
        for(int i = 0; i < dbids.size(); ++i){ setOnWidget(dbids.get(i), false, context); }

        super.onDisabled(context);
    }

    private void setOnWidget(CharSequence id, boolean value, Context context) {
        /**
         * Let the status table know that a widget is/isn't using this data. This will
         * keep a database item from really being deleted if it is still backing a widget. */
        ClimateDB climate = new ClimateDB(context);      // Be sure to close this before leaving.
        int onwidget = value?ClimateDB.SQLITE_TRUE:ClimateDB.SQLITE_FALSE;
        try {
            SQLiteDatabase db = climate.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(ClimateDB.STATUS_ONWIDGET, onwidget);
            String[] filler = {};
            String where = "_id = " + id;
            db.update(ClimateDB.STATUS_TABLE, values, where, filler);
            climate.close();
        } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".setOnWidget"); climate.close(); }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, CharSequence id) {
        /**
         * Used at widget creation/update to apply the chosen city data to widget.
         */
        // Get the data from the Database. The GUIs are expected to swap State code for Country. Use current time.
        Status place = queryWeatherStation(context, id);
        String holdState = place.State;
        if(holdState.length() == 0){ holdState = place.Country; }
        CharSequence holdTime = KTime.ParseNow(DB_fmtDateTime, place.TimeZone); 
        
        // Generate a remote view of the widget and create an onClick event.
        // Note: Because the alert value changes we need to set the FLAG_UPDATE_CURRENT when generating a pending
        // intent that might have had a different alert value to begin with.
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.quickview_wgt);
        Intent localAct = new Intent(context, Weather.class);
        localAct.setAction(place.City + id); // This is required to force the Intent to be unique (could be any string).
        localAct.putExtra(IN_CITY_ID, id);
        Boolean holdalert = place.IsAlert == 1;
        localAct.putExtra(IN_ALERT_FLG, holdalert);
        localAct.putExtra(IN_AM_WIDGET, true);
        PendingIntent pLocalAct = PendingIntent.getActivity(context, 0, localAct, PendingIntent.FLAG_UPDATE_CURRENT);
        
        // Fill in the widget data.
        views.setTextViewText(R.id.wgtCity, Html.fromHtml("<b><i>" + place.City + ", </i></b>"));
        views.setTextViewText(R.id.wgtState,  Html.fromHtml("<b><i>" + holdState + " </i></b>"));
        views.setTextViewText(R.id.wgtSpecial, place.Special);
        views.setTextViewText(R.id.wgtTimeLocal, holdTime); // used mostly for debug, not visible in prod (and need settings check if prod).
        views.setTextViewText(R.id.wgtTemperature, xtrTemperature(context, place.Temperature, TMP_ROUND_WHOLE));
        views.setImageViewResource(R.id.wgtCondition, getWgtResource(place.ConditionIcon));
        if(place.IsAlert==1) { 
            views.setViewVisibility(R.id.wgtWarning, View.VISIBLE); 
        } else { 
            views.setViewVisibility(R.id.wgtWarning, View.INVISIBLE); 
        }
        views.setOnClickPendingIntent(R.id.wgtWidget, pLocalAct);
        
        // Tell the widget manager
        appWidgetManager.updateAppWidget(appWidgetId, views);
        /* There is a bug in the emulator where upon a fresh start up with new data, this
         * updateAppWidget() call has no effect and the widgets will just sit there.  Use
         * Ctrl-F11 on the emulator and then things will start working as expected.    */
    }
    
    // Translate the condition into a graphic.
    private static int getWgtResource(String cond) {
        if(cond.equalsIgnoreCase(DB_CondClear) || cond.equalsIgnoreCase(DB_CondSun) || cond.equalsIgnoreCase(DB_CondMostSun)){
            return R.drawable.wgt_clear;
        }
        if(cond.equalsIgnoreCase(DB_CondClearN)){
            return R.drawable.wgt_clrnight;
        }
        if(cond.equalsIgnoreCase(DB_CondMostCloud) || cond.equalsIgnoreCase(DB_CondPartCloud) || cond.equalsIgnoreCase(DB_CondPartSun)){
            return R.drawable.wgt_ptcloudy;
        }
        if(cond.equalsIgnoreCase(DB_CondPartCloudN)){
            return R.drawable.wgt_cldnight;
        }
        if(cond.equalsIgnoreCase(DB_CondCloud) || cond.equalsIgnoreCase(DB_CondFog) || cond.equalsIgnoreCase(DB_CondHazy)){
            return R.drawable.wgt_cloudy;
        }
        if(cond.equalsIgnoreCase(DB_CondRain) || cond.equalsIgnoreCase(DB_CondRainP) || 
           cond.equalsIgnoreCase(DB_CondSleet) || cond.equalsIgnoreCase(DB_CondSleetP)){
            return R.drawable.wgt_rain;
        }
        if(cond.equalsIgnoreCase(DB_CondThunder) || cond.equalsIgnoreCase(DB_CondThunderP)){
            return R.drawable.wgt_tstorms;
        }
        if(cond.equalsIgnoreCase(DB_CondSnow) || cond.equalsIgnoreCase(DB_CondSnowP) || 
           cond.equalsIgnoreCase(DB_CondFlurry) || cond.equalsIgnoreCase(DB_CondFlurryP)){
            return R.drawable.wgt_snow;
        }        
        return R.drawable.wgt_ptcloudy;
    }

    // Fill in the Status fields from the database with only what is needed for the widget.
    private static Status queryWeatherStation(Context context, CharSequence id){
        Status dataPoint = new Status();
        ClimateDB climate = new ClimateDB(context);      // Be sure to close this before leaving.
        Cursor cursor = null;
        try {
            SQLiteDatabase db = climate.getReadableDatabase();
            String[] filler = {};
            cursor = db.rawQuery(DB_StatusID + id, filler);
            cursor.moveToNext();
            dataPoint.City = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_CITY));
            dataPoint.ConditionIcon = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_CICON));
            dataPoint.Country = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_CNTY));
            dataPoint.Id = cursor.getInt(cursor.getColumnIndex(ClimateDB.STATUS_ID));
            dataPoint.Special = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_NOTE));
            dataPoint.State = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_STATE));
            dataPoint.Temperature = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_TEMP));
            dataPoint.TimeZone = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_TZONE));
            dataPoint.IsAlert = cursor.getInt(cursor.getColumnIndex(ClimateDB.STATUS_ALERT));
            cursor.close();
            climate.close();
        } catch (Exception ex){ ExpClass.LogEX(ex, "QuickView::queryWeatherStation"); if(cursor!=null)cursor.close(); climate.close(); }
        return dataPoint;
    }
    
    // Find all database ids where the OnWidget is true.
    private ArrayList<String> getAnyOnWidgets(Context context) {
        ArrayList<String> dataPoints = new ArrayList<>();
        ClimateDB climate = new ClimateDB(context);      // Be sure to close this before leaving.
        Cursor cursor = null;
        try {
            SQLiteDatabase db = climate.getReadableDatabase();
            String[] filler = {};
            cursor = db.rawQuery(DB_StatusAnyWidget, filler);
            while(cursor.moveToNext()){
                dataPoints.add(cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_ID)));
            }
            cursor.close();
            climate.close();
        } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getAnyOnWidgets"); if(cursor!=null)cursor.close(); climate.close(); }
        return dataPoints;
    }

    // Proper formatting for the Temperature.
    private static String xtrTemperature(Context context, String tmp, int precision){
        String outFrmt = "";
        int precisionNbr = 0;
        double holdTemp;
        
        try { holdTemp = Double.parseDouble(tmp); } 
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, "QuickView::xtrTemperature"); holdTemp = 0; }
        if(Settings.getUseCelsius(context)) { 
            // Rounding trick: Math.floor(value * X + .5) X = power of 10 (precision where 10=.1, 100=.01, etc.).
            holdTemp = Math.floor(((holdTemp - 32) * 5 / 9) * precision + 0.5) / precision;
        } else {
            holdTemp = Math.floor(holdTemp * precision + 0.5) / precision;
        }
        for(int place = precision;  place > 1; place /= 10) { precisionNbr++;  }
        outFrmt += "%1." + precisionNbr + "f°";
        return String.format(outFrmt, holdTemp);
    }
}
