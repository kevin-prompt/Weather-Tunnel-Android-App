package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * NOTE: This tab fragment relies on the Current weather tab being always shown first.  It uses
 * the data generated and passed by the Current Weather tab. 
 */
public class WeatherForecast extends ListFragment {
    private String recID = LOC_CURRENT_ID;
    AsyncTask<String, Boolean, Status> aTask;
    boolean userCancelled = true;
    Integer lastTab = 1;

    /**
     * On creation, get the key and trigger the asyncTask to load data.  The data will be saved
     * for later switching back to this tab, so we only load from the API once.  We also need to
     * save off a flag indicating if this is the current tab.  On context change the system will
     * just try to recreate the tab and we only want to call the asyncTask if we are on the tab.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
            lastTab = savedInstanceState.getInt(TAB_ID_SHOWING);
        }
        Bundle extras = getActivity().getIntent().getExtras();
        if(extras != null){
            recID = extras.getString(IN_CITY_ID);
        }
        
        if(!((Weather)getActivity()).getData().isDataLoaded){
            aTask = new StatusUpdateTask2(getActivity(), getResources().getString(R.string.app_name)).execute(recID);
        } else {
            showDetails(((Weather)getActivity()).getData());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.forecast, container, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) { 
        super.onSaveInstanceState(outState);
        // If you need to remember anything, put it in the outState
        outState.putInt(TAB_ID_SHOWING, ((Weather)getActivity()).getSupportActionBar().getSelectedNavigationIndex()); 
    } 
     
    /**
     * Orientation changes (and other things) recreate an Activity.  So each time one happens it will 
     * kill the fragment.  If we have the AsyncTask going while that happens it can crash the app.  Also,
     * multiple recreates can happen at times, causing a series of start/stop requests from the OS.
     * We want to stop the AsyncTask but not as if a user did (where they exit to the Slate).
     */
    @Override
    public void onDestroyView() {
        userCancelled = false;
        if(aTask!=null) aTask.cancel(true);
        super.onDestroyView();
    }

    /*
     * The nested AsyncTask class is used to off-load the network call to a separate thread.
     */
    private class StatusUpdateTask2 extends AsyncTask<String, Boolean, Status> {
        private ProgressDialog progressDialog;
        private Context context;
        private String title;
        private boolean noNetwork = false;
        
        public StatusUpdateTask2(Activity activity, String name){
            context = activity;
            ConnectivityManager net = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo priNet = net.getActiveNetworkInfo();
            noNetwork = (priNet==null || !priNet.isConnected());
            title = name;
        }

        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context, title, "Loading...", true, true);
            progressDialog.setOnDismissListener(new OnDismissListener() { @Override public void onDismiss(DialogInterface dialog) { cancel(true); } });
        }

        protected void onPostExecute(coolftc.weathertunnel.Status result) {
            showDetails(result);
            progressDialog.dismiss();
            ((Weather)getActivity()).setData(result);    // Keep the data around for later use.
        }

        protected void onCancelled() {
            //Toast.makeText(context, R.string.msgUserCancel, Toast.LENGTH_LONG).show();
            if(userCancelled){
                Intent intent = new Intent(context, Slate.class); 
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
                startActivity(intent);
                getActivity().finish();
            }
        }

        protected void onProgressUpdate(Boolean... values) {
            if(!values[0]) Toast.makeText(context, R.string.msgNoNet, Toast.LENGTH_LONG).show();
        }

        protected coolftc.weathertunnel.Status doInBackground(String... recID) {
            coolftc.weathertunnel.Status place = new coolftc.weathertunnel.Status();
            place.LoadLocation(context, recID[0]);

            if(noNetwork) {
                publishProgress(false);
            } else {
                place.Update(UPD_STATIONS_ALL, context);
            }
            return place;
        }
    }

    private void showDetails(Status place) {
        
        if(!place.isDataLoaded) return;
        
        List<Map<String, String>> details = new ArrayList<Map<String, String>>();
        
        if(place.FCData.size() == 0) return;
        // Forecast Data Binding Map
        String[] StatusMapFROM = {FC_LOC_ID, FC_CONDICON, FC_COND, FC_TITLE, FC_DESC, FC_TEMP_HIGH, FC_TEMP_LOW, FC_WIND, FC_RAIN, FC_RAIN_LBL};
        int[] StatusMapTO = {R.id.rowf_Id, R.id.rowfCondImg, R.id.rowfCondition, R.id.rowfTitleDay, R.id.rowfDescDay, R.id.rowfHighTemp, R.id.rowfLowTemp, R.id.rowfWindCond, R.id.rowfRainChance, R.id.lblfRainChance};
        
        for(Integer i = 0; i < place.FCText.size(); ++i){ place.FCText.get(i).Hide = false; }    
        for(Integer i = 0; i < place.FCData.size(); ++i){
            Map<String, String> hold = new TreeMap<String, String>();
            hold.put(FC_LOC_ID, i.toString());
            hold.put(FC_CONDICON, place.FCData.get(i).ConditionIcon);
            hold.put(FC_COND, place.FCData.get(i).getCondition());
            hold.put(FC_TITLE, getDayTitle(place.FCData.get(i), true, place.TimeZone) + " "); // The usual space for italics.
            hold.put(FC_DESC, getDayDesc(i, place));
            hold.put(FC_WIND, place.FCData.get(i).getExpectedWind(Settings.getUseMetric(getActivity())));
            hold.put(FC_TEMP_HIGH, xtrTemperature(place.FCData.get(i).HighTemperature, TMP_ROUND_WHOLE));
            hold.put(FC_TEMP_LOW, xtrTemperature(place.FCData.get(i).LowTemperature, TMP_ROUND_WHOLE));
            if(place.FCData.get(i).PercipitationChance.equalsIgnoreCase("0")){  // If no precipitation, signal not to display
                hold.put(FC_RAIN, "");
                hold.put(FC_RAIN_LBL, "");                
            } else {
                String qpf = "";
                if(place.FCData.get(i).getPercipitationAmt() > 0.0)
                    qpf = "(" + xtrDistance(place.FCData.get(i).getPercipitationAmt(),TMP_ROUND_HUNDRED, MEASURE_DIST.INCHES, false) + ")";
                hold.put(FC_RAIN, place.FCData.get(i).PercipitationChance + "%   " + qpf);
                // Try to use the low, then high temperature, then default to rain (last in gets priority).
                double checkTemp = 55.0;
                if(place.FCData.get(i).getHighTemperature()!= NMBR_NA) checkTemp = place.FCData.get(i).getHighTemperature();
                if(place.FCData.get(i).getLowTemperature() != NMBR_NA) checkTemp = place.FCData.get(i).getLowTemperature();
                if(checkTemp < 33){
                    hold.put(FC_RAIN_LBL, FC_SNOW_TXT);
                } else {
                    hold.put(FC_RAIN_LBL, FC_RAIN_TXT);                
                }
            }
            details.add(hold);
        }
        
       
        SimpleAdapter adapter = new SimpleAdapter(getActivity(), details, R.layout.forecast_row, StatusMapFROM, StatusMapTO);         
        
        adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                boolean rtn = false;
                
                if(view.getId() == R.id.rowfCondImg) {   // Populate the condition graphic based on the condition icon text
                    String cond = textRepresentation;
                    Resources res = getResources();
                    boolean imageOK = false;
                    
                    if(cond.equals(DB_CondClear))    { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.clear)); imageOK = true; }
                    if(cond.equals(DB_CondClearN))   { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.nt_clear)); imageOK = true; }
                    if(cond.equals(DB_CondCloud))    { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.cloudy)); imageOK = true; }
                    if(cond.equals(DB_CondFlurry))   { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.flurries)); imageOK = true; }
                    if(cond.equals(DB_CondFlurryP))  { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.flurries)); imageOK = true; }
                    if(cond.equals(DB_CondFog))      { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.fog)); imageOK = true; }
                    if(cond.equals(DB_CondHazy))     { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.fog)); imageOK = true; }
                    if(cond.equals(DB_CondMostCloud)){ ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.partlycloudy)); imageOK = true; }
                    if(cond.equals(DB_CondMostSun))  { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.partlycloudy)); imageOK = true; }
                    if(cond.equals(DB_CondPartCloud)){ ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.partlycloudy)); imageOK = true; }
                    if(cond.equals(DB_CondPartCloudN)){ ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.nt_partlycloudy)); imageOK = true; }
                    if(cond.equals(DB_CondPartSun))  { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.partlycloudy)); imageOK = true; }
                    if(cond.equals(DB_CondRain))     { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.rain)); imageOK = true; }
                    if(cond.equals(DB_CondRainP))    { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.rain)); imageOK = true; }
                    if(cond.equals(DB_CondSleet))    { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.rain)); imageOK = true; }
                    if(cond.equals(DB_CondSnow))     { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.flurries)); imageOK = true; }
                    if(cond.equals(DB_CondSnowP))    { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.flurries)); imageOK = true; }
                    if(cond.equals(DB_CondSun))      { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.clear)); imageOK = true; }
                    if(cond.equals(DB_CondThunder))  { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.tstorms)); imageOK = true; }
                    if(cond.equals(DB_CondThunderP)) { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.tstorms)); imageOK = true; }
                    if(cond.equals(DB_CondUnknown))  { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.partlycloudy)); imageOK = true; }
                    if(!imageOK){ ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.partlycloudy)); }
                    
                    rtn = true;
                }
                
                // There is a weird bug when hiding data, as the first record seems to be affected by the last record.
                // To work around I default to GONE and force VISIBLE as needed.  Seems to always work.
                if(view.getId() == R.id.rowfRainChance || view.getId() == R.id.lblfRainChance) { 
                    if(!textRepresentation.equalsIgnoreCase("")){
                        ((TextView) view).setVisibility(View.VISIBLE);
                    }
                }

                return rtn;
            }
        });
        // Apply the mapping
        setListAdapter(adapter);
    }
    
    /* ******************************************************************************
     * Additional analysis and transformation of raw weather information.
     */
    
    /* We want to get the description text that applies to the date for which we have weather data.
     * Since the weather descriptions have titles that match the name of the day, we will try to link
     * them up using the name of the day.  Note that the text usually comes in two parts, day/night, 
     * so we concatenate those to match the single day.
     * 
     * In the old days, words like Today, Tonight as well as days of the week (Saturday, etc.) showed up. Also,
     * non-US sites used a formatted date instead of a day name.  Neither of these appears to be the case now. 
     * */
    private String getDayDesc(Integer i, Status data) {
        String dayOfWeek = xtrForecastDayOfWeek(data.FCData.get(i).TimeStamp, data.TimeZone);
        String desc = "";
        
        for(int ndx = 0; ndx < data.FCText.size(); ++ndx){
            if(!data.FCText.get(ndx).Hide){ // if hidden it has been used
                if(data.FCText.get(ndx).Title.contains(dayOfWeek)){
                    if(desc.length() > 0) { desc += "Later, "; } // this second forecast text is for the night.
                    desc += data.FCText.get(ndx).getDescription(Settings.getUseCelsius(getActivity())) + " ";
                    data.FCText.get(ndx).Hide = true;
                }else{
                    // With overlapping days in the 10 day forecast, the simplest approach is just to make some 
                    // assumptions like the text and data forecast data being generally in sync.  Given this, we can
                    // just stop looking for day matches as soon as we do not find one.  This is because as we
                    // process text records we hide them, so the first one read should match each time into this loop.
                    break;
                }
            }
        }
        if(desc.length() == 0) desc = "No weather analysis available.";
        return desc;
    }

    /* We want to generate a title that is the name of the day of the week, unless it is 
     * "Today" or "Tomorrow", then we want those literal names (most of the time). Seeing
     * if the forecast date is today or tomorrow is a bit of code. */
    private String getDayTitle(ForecastData fcData, boolean okTomorrow, String timezone) {

        String title = "";
        // Create Calendars
        Calendar titleDay;
        Calendar todayDay = Calendar.getInstance(java.util.TimeZone.getTimeZone(timezone));
        Calendar tomorrowDay = Calendar.getInstance(java.util.TimeZone.getTimeZone(timezone));
        // Put our dates into the Calendars
        try {
            titleDay = KTime.ParseToCalendar(fcData.TimeStamp, DB_inputDate3339, DATA_NA);
            tomorrowDay.add(Calendar.DAY_OF_MONTH, 1);
        } catch (ExpParseToCalendar ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".getDayTitle");
            return xtrForecastDayOfWeek(fcData.TimeStamp, timezone);
        }
        // Get the specific days in question
        int titleDayKey = titleDay.get(Calendar.DAY_OF_YEAR);
        int todayDayKey = todayDay.get(Calendar.DAY_OF_YEAR);
        int tomorrowDayKey = tomorrowDay.get(Calendar.DAY_OF_YEAR);
        // See if forecast is today, tomorrow or something else.
        if(titleDayKey == todayDayKey) { title = TITLE_TODAY; }
        if(titleDayKey == tomorrowDayKey && okTomorrow) { title = TITLE_NEXTDAY; }
        if(title.length() == 0) { title = xtrForecastDayOfWeek(fcData.TimeStamp, timezone); }    
        return title;
    }

    private String xtrTemperature(String tmp, int precision){
        String outFrmt = "";
        double holdTemp = 0;
        int precisionNbr = 0;
        if(tmp.length() == 0) return "NA";
        try { holdTemp = Double.parseDouble(tmp); } 
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".xtrTemperature"); holdTemp = 0; }
        if(Settings.getUseCelsius(getActivity())) { 
            // Rounding trick: Math.floor(value * X + .5) X = power of 10 (precision where 10=.1, 100=.01, etc.).
            holdTemp = Math.floor(((holdTemp - 32) * 5 / 9) * precision + 0.5) / precision;
        } else {
            holdTemp = Math.floor(holdTemp * precision + 0.5) / precision;
        }
        for(int place = precision;  place > 1; place /= 10) { precisionNbr++;  }
        outFrmt += "%1." + precisionNbr + "f°";
        return String.format(outFrmt, holdTemp);
    }
    
    private String xtrForecastDayOfWeek(String localTime, String timezone) {
        try {
            return (String) KTime.ParseToFormat(localTime, DB_inputDate3339, DATA_NA, DB_fmtDayOfWeek);
        } catch (ExpParseToCalendar ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".xtrForecastDayOfWeek");
            // If a problem, just use current.
            return (String) KTime.ParseNow(DB_fmtDayOfWeek, timezone);
        }
    }
    
    private String xtrDistance(double distance, int precision, MEASURE_DIST measure, boolean abbr) {
        if(distance < 0.0) return DISP_NA;
        String measure_ds = "";
        String outFrmt = "";
        int precisionNbr = 0;
        
        if(Settings.getUseMetric(getActivity())) {
            switch (measure){
            case INCHES:
                distance = distance * 2.54; // convert to centimeters
                break;
            case FEET:
                distance = distance * 0.3048;   // convert to meters
                break;
            case MILES:
                distance = distance * 1.609344;  // convert to kilometers
                break;
            }
        }
        // Rounding trick: Math.floor(value * X + .5) / X :: where X = power of 10 (precision where 1=0, 10=.1, 100=.01, etc.).
        distance = Math.floor(distance * precision + 0.5) / precision;
        switch (measure){
        case INCHES:
            if(Settings.getUseMetric(getActivity())){
                if(abbr){
                    measure_ds = (distance == 1.0) ? "cm" : "cm";
                }else{
                    measure_ds = (distance == 1.0) ? "cm" : "cm";
                }
            }else{
                if(abbr){
                    measure_ds = (distance == 1.0) ? "in" : "in";
                }else{
                    measure_ds = (distance == 1.0) ? "inch" : "inches";
                }
            }            
            break;
        case FEET:
            if(Settings.getUseMetric(getActivity())){
                if(abbr){
                    measure_ds = (distance == 1.0) ? "m." : "m.";
                }else{
                    measure_ds = (distance == 1.0) ? "meter" : "meters";
                }
            }else{
                if(abbr){
                    measure_ds = (distance == 1.0) ? "ft" : "ft";
                }else{
                    measure_ds = (distance == 1.0) ? "foot" : "feet";
                }
            }            
            break;
        case MILES:
            if(Settings.getUseMetric(getActivity())){
                if(abbr){
                    measure_ds = (distance == 1.0) ? "km" : "km";
                }else{
                    measure_ds = (distance == 1.0) ? "kilometer" : "kilometers";
                }
            }else{
                if(abbr){
                    measure_ds = (distance == 1.0) ? "mi" : "mi";
                }else{
                    measure_ds = (distance == 1.0) ? "mile" : "miles";
                }
            }            
           break;
        }
        for(int place = precision;  place > 1; place /= 10) { precisionNbr++;  }
        
        if(distance < 1.0 && precision == TMP_ROUND_WHOLE && measure == MEASURE_DIST.MILES){
            distance = 1.0;
            outFrmt = "~";
        }
        outFrmt += "%1." + precisionNbr + "f %s";
        return String.format(outFrmt, distance, measure_ds);
    }    
}