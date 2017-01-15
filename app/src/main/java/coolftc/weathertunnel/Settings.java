package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener{
    
    // Setting Screen Constants
    private static final String PREF_CHECKLOCATION = "1001";
    private static final boolean DEFAULT_CHECKLOCATION = true;
    private static final String PREF_USECELSIUS = "1002";
    private static final boolean DEFAULT_USECELSIUS = false;
    private static final String PREF_USEMETRIC = "1003";
    private static final boolean DEFAULT_USEMETRIC = false;
    private static final String PREF_USEINCHES = "1004";
    private static final boolean DEFAULT_USEINCHES = false;
    private static final String PREF_USEMETEOROLOGICAL = "1005";
    private static final boolean DEFAULT_USEMETEOROLOGICAL = false;
    private static final String PREF_USE24CLOCK = "1006";
    private static final boolean DEFAULT_USE24CLOCK = false;
    private static final String PREF_USEGPSLOCATION = "1007";
    private static final boolean DEFAULT_USEGPSLOCATION = false;
    private static final String PREF_PICKSHORTDATEFMT = "1008";
    private static final String DEFAULT_PICKSHORTDATEFMT = DB_fmtDateShrtMiddle;
    private static final String PREF_SHOWSTATIONS = "1009";
    private static final boolean DEFAULT_SHOWSTATIONS = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
    
    // When false, stop checking for location updates.
    public static boolean getCheckLocation(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_CHECKLOCATION, DEFAULT_CHECKLOCATION);
    }
    
    // Allow location updates to be adjusted programmatically.
    // Note: This edit does not trigger the onSharedPreferenceChanged().
    public static void setCheckLocation(Context context, boolean check) {
        SharedPreferences.Editor ali = PreferenceManager.getDefaultSharedPreferences(context).edit();
        ali.putBoolean(PREF_CHECKLOCATION, check);
        ali.apply();
    }
    
    // When false, stop using GPS for location updates.
    public static boolean getUseGPSLocation(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_USEGPSLOCATION, DEFAULT_USEGPSLOCATION);
    }
    
    // When true, display temperature in celsius.
    public static boolean getUseCelsius(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_USECELSIUS, DEFAULT_USECELSIUS);
    }
    
    // When true, display distance/speed in kilometers.
    public static boolean getUseMetric(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_USEMETRIC, DEFAULT_USEMETRIC);
    }
    
    // When true, display atmospheric pressure in terms of inches of mercury (instead of millibars).
    public static boolean getUseInches(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_USEINCHES, DEFAULT_USEINCHES);
    }
    
    // When true, display seasons using Meteorological instead of Astronomical reckoning.
    public static boolean getUseMeteorological(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_USEMETEOROLOGICAL, DEFAULT_USEMETEOROLOGICAL);
    }
    
    // When true, display time in a 24 hour format.
    public static boolean getUse24Clock(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_USE24CLOCK, DEFAULT_USE24CLOCK);
    }
    
    // Select how the day/month/year is ordered in displaying dates.
    public static String getPickShortDateFmt(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_PICKSHORTDATEFMT, DEFAULT_PICKSHORTDATEFMT);
    }

    // When true, display the public and private weather stations codes on the detail screen.
    public static boolean getShowStations(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_SHOWSTATIONS, DEFAULT_SHOWSTATIONS);
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Start/Stopping the Current Location Shows/Hides the matching row on the slate.
        // This provides a way to remove the default row when it is not relevant, e.g. no location services on. 
        if (key.equals(PREF_CHECKLOCATION)) {
            boolean location = sharedPreferences.getBoolean(PREF_CHECKLOCATION, DEFAULT_CHECKLOCATION);
            if(location) {
                chgStateSlate(this, 1);
            } else {
                chgStateSlate(this, 0);
            }
        }
    }
    
    /* Changing this value will affect the visibility of the current location. */
    private void chgStateSlate(Context context, int value){
        ClimateDB climate = new ClimateDB(context); // Be sure to close this before leaving.
        try{
            SQLiteDatabase db = climate.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(ClimateDB.STATUS_ONSLATE, value);
            String where = "_id = 1";
            String[] filler = {};
            db.update(ClimateDB.STATUS_TABLE, values, where, filler);
            climate.close();
        }catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".chgStateSlate"); climate.close();}   
    }

    /*
     * There are a number of variations on how dates are displayed, based on what the user has specified.
     * This method brings some of those together in one place for ease of management and use around the App.
     * ENUMs generate a lot of overhead in Java, so we will stick to constant int as the date type.
     */
    public static String getDateDisplayFormat(Context context, int datetype) {
        
        String holdFmt;
        String fmt = getPickShortDateFmt(context);
        Resources res = context.getResources();
        
        switch (datetype){
        
        case DATE_FMT_SHORT:
            holdFmt = DB_fmtDateShrtMiddle;
            if(fmt.equalsIgnoreCase("big")) holdFmt = DB_fmtDateShrtBig;
            if(fmt.equalsIgnoreCase("sml")) holdFmt = DB_fmtDateShrtLittle;
            return holdFmt;
            
        case DATE_FMT_OBSERVED:
            try {
                holdFmt = "'" + res.getString(R.string.lblObserved) + " 'E ";
                if(fmt.equalsIgnoreCase("big")) holdFmt += DB_fmtDateMonthBig;
                if(fmt.equalsIgnoreCase("mid")) holdFmt += DB_fmtDateMonthMiddle;
                if(fmt.equalsIgnoreCase("sml")) holdFmt += DB_fmtDateMonthLittle;
                holdFmt += "' " + res.getString(R.string.lblAt) + " '";
                holdFmt += getUse24Clock(context) ? DB_fmtDateTime24 : DB_fmtDateTime;
                return holdFmt;
            } catch (Exception ex) { ExpClass.LogEX(ex, "Settings.chgStateSlate-DATE_FMT_OBSERVED"); return "'Observed 'E MMM dd, yyyy ' at ' h:mmaa"; }
            
        case DATE_FMT_ALERT_CURR:
            try {
                holdFmt = "' -:- " + res.getString(R.string.lblUntil) + " '";
                holdFmt += getUse24Clock(context) ? DB_fmtDateTime24 : DB_fmtDateTime;
                if(fmt.equalsIgnoreCase("sml"))
                    holdFmt += " " + DB_fmtDateNoYearLittle;
                else
                    holdFmt += " " + DB_fmtDateNoYearBigMid;
                return holdFmt;
            } catch (Exception ex) { ExpClass.LogEX(ex, "Settings.chgStateSlate-DATE_FMT_ALERT_CURR"); return "' -:- Until 'h:mmaa MMM dd"; }
            
        case DATE_FMT_ALERT_EXP:
            try {
                holdFmt = DB_fmtLongMonthMiddle;
                if(fmt.equalsIgnoreCase("big")) holdFmt = DB_fmtLongMonthBig;
                if(fmt.equalsIgnoreCase("sml")) holdFmt = DB_fmtLongMonthLittle;
                holdFmt += " @ " + (getUse24Clock(context) ? DB_fmtDateTime24 : DB_fmtDateTime);
                return holdFmt;
            } catch (Exception ex) { ExpClass.LogEX(ex, "Settings.chgStateSlate-DATE_FMT_ALERT_EXP"); return "MMMM dd, yyyy @ h:mmaa"; }
            
        default:
            throw new IllegalArgumentException();
        
        }
    }
    
}




