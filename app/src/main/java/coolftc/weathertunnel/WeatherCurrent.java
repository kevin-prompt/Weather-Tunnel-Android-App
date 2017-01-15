package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import android.app.Activity;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class WeatherCurrent extends Fragment {
    private String recID = LOC_CURRENT_ID;
    private boolean refresh = false;
    AsyncTask<String, Boolean, Status> aTask;
    boolean userCancelled = true;
    Integer lastTab = 0;

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
            refresh = extras.getBoolean(IN_FORCE_UPDT);
        }
        
        if(!((Weather)getActivity()).getData().isDataLoaded){
            aTask = new StatusUpdateTask(getActivity(), getResources().getString(R.string.app_name), refresh).execute(recID);
        } else {
            showDetails(((Weather)getActivity()).getData());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.current, container, false);
    }
    
    /** 
     * It would be nice if the Widget reflected the latest data seen in the detail screen.
     * We started the database update service when we came in, so by now it should be done. 
     * This is a direct update to the widgets, not just a request.
     */
    @Override
    public void onPause() {
        UpdateWidget(recID);
        super.onPause();
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
    private class StatusUpdateTask extends AsyncTask<String, Boolean, Status> {
        private ProgressDialog progressDialog;
        private Context context;
        private String title;
        private boolean noNetwork = false;
        private boolean forcedRefresh = false;
        
        StatusUpdateTask(Activity activity, String name, boolean refresh){
            context = activity;
            ConnectivityManager net = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo priNet = net.getActiveNetworkInfo();
            noNetwork = (priNet==null || !priNet.isConnected());
            forcedRefresh = refresh;
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
                // If the refresh is manually requested, we go ahead and force the server cache to refresh.
                if(forcedRefresh) place.Update(UPD_STATIONS_FORCE, context);
                place.Update(UPD_STATIONS_ALL, context);
            }
            return place;
        }
    }

    /**
     * This reaches out directly to the widget and updates it.
     */
    private void UpdateWidget(String dbid){
        // Update all the Widgets.
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getActivity());
        SharedPreferences widgetMap = getActivity().getSharedPreferences(SP_WIDGET_MAP, Context.MODE_PRIVATE);
        Map<String, ?> prefList = widgetMap.getAll();
        for (Entry<String, ?> entry : prefList.entrySet()) {
            int widgetId = Integer.parseInt(entry.getKey());
            String holdid = (String)entry.getValue();
            if(dbid.equalsIgnoreCase(holdid)){
                ExpClass.LogIN(KEVIN_SPEAKS, "Updating from Detail: Widget #" + entry.getKey());
                QuickView.updateAppWidget(getActivity(), appWidgetManager, widgetId, dbid);
            }
        } 
    }
    
    /**
     * This will populate the display.
     */
    private void showDetails(Status place) {
        TextView holdView;
        ImageView holdImg;
        String holdDesc;
        String holdWind;
        String holdDist;
        Resources res = getResources();
        String dtFmt;

        if(!place.isDataLoaded) return;

        // ******************************************************************************************
        // Sending an intent will force a service refresh (and update the database with correct data).
        // This way the slate and widget data will reflect the data seen in the detail screen.
        Intent intentSrv = new Intent(getActivity(), PulseStatus.class);
        getActivity().startService(intentSrv);

        // ******************************************************************************************
        // Location
        holdView = (TextView)getView().findViewById(R.id.txtCity);
        holdView.setText(place.City + ",  " + place.State);
       
        holdView = (TextView)getView().findViewById(R.id.txtSpecial);
        if(place.Special.length() == 0){
            holdView.setVisibility(View.INVISIBLE);
        } else {
            holdView.setText(place.Special);
            holdView.setVisibility(View.VISIBLE);
        }

        holdView = (TextView)getView().findViewById(R.id.txtLocalTime);
        dtFmt = Settings.getUse24Clock(getActivity())?DB_fmtDateTime24:DB_fmtDateTime;
        holdView.setText(KTime.ParseNow(dtFmt, place.TimeZone));
        
        // If the day is tomorrow or yesterday, add "next" or "past" day label.
        Calendar thereDate = Calendar.getInstance(java.util.TimeZone.getTimeZone(place.TimeZone));
        Calendar hereDate = Calendar.getInstance();
        if(hereDate.get(Calendar.DATE) == thereDate.get(Calendar.DATE)){
            holdView = (TextView)getView().findViewById(R.id.lblLocalTimeNextDay);
            holdView.setVisibility(View.GONE);
        }
        if(hereDate.get(Calendar.DATE) < thereDate.get(Calendar.DATE)){
            holdView = (TextView)getView().findViewById(R.id.lblLocalTimeNextDay);
            int holdcolor = place.isNight()?res.getColor(R.color.ClearNight):res.getColor(R.color.BrightChar);
            holdView.setTextColor(holdcolor);
            int holdLabel = hereDate.get(Calendar.MONTH) == thereDate.get(Calendar.MONTH)?R.string.lblLocalTimeNextDay:R.string.lblLocalTimePastDay;
            holdView.setText(holdLabel);
            holdView.setVisibility(View.VISIBLE);
        }
        if(hereDate.get(Calendar.DATE) > thereDate.get(Calendar.DATE)){
            holdView = (TextView)getView().findViewById(R.id.lblLocalTimeNextDay);
            int holdcolor = place.isNight()?res.getColor(R.color.ClearNight):res.getColor(R.color.BrightChar);
            holdView.setTextColor(holdcolor);
            int holdLabel = hereDate.get(Calendar.MONTH) == thereDate.get(Calendar.MONTH)?R.string.lblLocalTimePastDay:R.string.lblLocalTimeNextDay;
            holdView.setText(holdLabel);
            holdView.setVisibility(View.VISIBLE);
        }

        holdView = (TextView)getView().findViewById(R.id.txtElevation);
        holdView.setText(xtrDistance((place.getElevation()),TMP_ROUND_WHOLE, MEASURE_DIST.FEET, true));
        
        holdView = (TextView)getView().findViewById(R.id.txtPrivateDist);
        holdDist = xtrDistance((place.getPrivateDist()),TMP_ROUND_WHOLE, MEASURE_DIST.MILES, false);
        if(Settings.getShowStations(getActivity())) {
            holdDist = place.PrivateCode.replace("pws:", "") + "  (" + holdDist + ")";
        }
        holdView.setText(holdDist);
        
        holdView = (TextView)getView().findViewById(R.id.txtPublicDist);
        holdDist = xtrDistance((place.getPublicDist()),TMP_ROUND_WHOLE, MEASURE_DIST.MILES, false);
        if(Settings.getShowStations(getActivity())) {
            holdDist = place.PublicCode + "  (" + holdDist + ")";
        }
        holdView.setText(holdDist);
        
        // ******************************************************************************************
        // Time & Temperature
        holdImg = (ImageView)getView().findViewById(R.id.imgCondition);
        holdImg.setImageDrawable(res.getDrawable(xtrConditionResource(place.ConditionIcon)));
        
        // To fit descriptions we need to do a little formatting.  If too long, cut it up, if too short pad.
        holdView = (TextView)getView().findViewById(R.id.txtCondition);
        String holdCondtxt = place.Condition.replace(COND_TSTORM_LONG, COND_TSTORM_SHRT);
        if(place.Condition.length()>12){
            int pos = place.Condition.substring(0, 12).lastIndexOf(" ");
            if(pos > -1) 
                holdCondtxt = place.Condition.substring(0, pos) + "\n" + place.Condition.substring(pos+1);
            else
                holdCondtxt = place.Condition.replaceFirst(" ", "\n");
        }
        if(place.Condition.length()<4){
            holdCondtxt = "  " + holdCondtxt;
        }
        holdView.setText(holdCondtxt);
        
        holdView = (TextView)getView().findViewById(R.id.txtSeason);
        holdView.setText(xtrSeason(place.LocalTime, place.TimeZone, place.getLatitude()));
        
        holdView = (TextView)getView().findViewById(R.id.txtTemperature);
        holdView.setText(xtrTemperature(place.Temperature, TMP_ROUND_WHOLE));
        
        holdView = (TextView)getView().findViewById(R.id.txtFeelsLike);
        String feelsLike = xtrHumidity(place.getHumidity(), place.getTemperature());
        if(feelsLike.length() == 0)
            feelsLike = xtrWindTemp(place.getWindSpeed(), place.getTemperature());
        if(feelsLike.length() == 0){ 
            holdView.setVisibility(View.INVISIBLE); 
        }else{ 
            holdView.setText(feelsLike); 
            holdView.setVisibility(View.VISIBLE);
        }
        
        holdView = (TextView)getView().findViewById(R.id.txtTempIsAvg);
        String holdAverage = xtrTempIsAverage(place.getTemperature(), place.getTempAveMin(), place.getTempAveMax(), place.TimeZone);
        holdView.setText(holdAverage);
       
        holdView = (TextView)getView().findViewById(R.id.lblTempIsAvg);
        if(holdAverage.equalsIgnoreCase(TEMP_AVE_NORM)){
            holdView.setVisibility(View.GONE);
        } else {
            holdView.setVisibility(View.VISIBLE);
        }
        
        holdView = (TextView)getView().findViewById(R.id.txtAirMoist);
        if(place.getDewPoint() == NMBR_NA || place.getHumidity() == NMBR_NA ||
                xtrDewPoint(place.getDewPoint(), place.getHumidity(), place.ConditionIcon).equalsIgnoreCase(DEW_NORMAL)){
            holdView.setVisibility(View.GONE);
        } else {
            holdView.setText(xtrDewPoint(place.getDewPoint(), place.getHumidity(), place.ConditionIcon));
            holdView.setVisibility(View.VISIBLE);
        }

        holdView = (TextView)getView().findViewById(R.id.txtObservation);
        holdView.setText(xtrObservedTime(place.ObservedTime, place.TimeZone, Settings.getDateDisplayFormat(getActivity(), DATE_FMT_OBSERVED)));
        
        // ******************************************************************************************
        // Conditions                         
        holdView = (TextView)getView().findViewById(R.id.txtVisibility);
        holdView.setText(xtrDistance(place.getVisibility(), TMP_ROUND_TENTH, MEASURE_DIST.MILES, false));
        
        holdView = (TextView)getView().findViewById(R.id.txtPressure);
        if(place.getPressure() == NMBR_NA){
            holdView.setText(DISP_NA); 
        } else {
            holdView.setText(xtrPressure(place.getPressure()) + " (" + xtrPressureDiff(place.getPressure()) + ")");
        }
        
        holdView = (TextView)getView().findViewById(R.id.txtHumidity);
        if(place.getHumidity() == NMBR_NA){
            holdView.setText(DISP_NA); 
        } else {
            holdView.setText(place.Humidity + "%");
        }
        
        holdView = (TextView)getView().findViewById(R.id.txtTempAverages);
        if(place.getTempAveMax() == NMBR_NA || place.getTempAveMin() == NMBR_NA){
            holdView.setText(DISP_NA); 
        } else {
            holdView.setText(xtrTemperature(place.getTempAveMax(), TMP_ROUND_WHOLE) + "  /  " + xtrTemperature(place.getTempAveMin(), TMP_ROUND_WHOLE));
        }
        
        holdView = (TextView)getView().findViewById(R.id.txtTempRecords);
        if(place.getTempRecMax() == NMBR_NA || place.getTempRecMin() == NMBR_NA || place.getTempRecMaxYY() == NMBR_NA || place.getTempRecMinYY() == NMBR_NA){
            holdView.setText(DISP_NA); 
        } else {
            holdView.setText(xtrTemperature(place.getTempRecMax(), TMP_ROUND_WHOLE) + " (" + place.TempRecMaxYY + ")  /  " + xtrTemperature(place.getTempRecMin(), TMP_ROUND_WHOLE) + " (" + place.TempRecMinYY + ")");
        }
        
        holdView = (TextView)getView().findViewById(R.id.txtDewPoint);
        if(place.getDewPoint() == NMBR_NA){
            holdView.setText(DISP_NA); 
        } else {
            holdView.setText(xtrTemperature(place.DewPoint, TMP_ROUND_WHOLE) + " (" + xtrDewPoint(place.getDewPoint(), place.getHumidity(), place.ConditionIcon) + ")");
        }
        
        holdView = (TextView)getView().findViewById(R.id.txtPressureRate);
        String holdPressD = PRESSURE_STEADY;
        if(place.PressureDelta.equalsIgnoreCase("-")) holdPressD = PRESSURE_DOWN;
        if(place.PressureDelta.equalsIgnoreCase("+")) holdPressD = PRESSURE_RISE;
        holdView.setText(holdPressD);
        
        holdView = (TextView)getView().findViewById(R.id.txtWindCond);
        holdView.setText(xtrWindCond(place.getWindSpeed(), place.getWindGusts(), TMP_ROUND_WHOLE));
        
        holdView = (TextView)getView().findViewById(R.id.txtWind);
        holdView.setText(xtrWindSpeed(place.getWindSpeed(), place.getWindGusts(), TMP_ROUND_WHOLE));
        
        holdView = (TextView)getView().findViewById(R.id.txtWindDir);
        holdView.setText(xtrFullDirection(place.WindDir, place.getWindSpeed(), place.getWindGusts()));
        
        holdView = (TextView)getView().findViewById(R.id.txtRain);
        holdView.setText(xtrDistance((place.getPrecip24()),TMP_ROUND_HUNDRED, MEASURE_DIST.INCHES, false) + " (24 hrs)");

        if(place.ALData.size() == 0){
            holdView = (TextView)getView().findViewById(R.id.txtWAlertMsg);
            holdView.setVisibility(View.INVISIBLE);
        }else{
            try {
                holdView = (TextView)getView().findViewById(R.id.txtWAlertMsg);
                Calendar work = KTime.ParseToCalendar(place.ALData.get(0).getExpire(), DB_inputDate3339, DATA_NA);
                holdView.setText(place.ALData.get(0).getTitleCut(20, "") + DateFormat.format(Settings.getDateDisplayFormat(getActivity(), DATE_FMT_ALERT_CURR), KTime.ConvertTimezone(work, place.TimeZone)));
                holdView.setVisibility(View.VISIBLE);
            } catch (ExpParseToCalendar ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".showDetails"); holdView.setVisibility(View.GONE); }
        }
 
        
        // ******************************************************************************************
        // Forecast
        // Assume the first FCData record (index zero) is today's data.
        holdView = (TextView)getView().findViewById(R.id.txtHighTemp);
        if(place.FCData.size() > 0){
            holdView.setText(xtrTemperature(place.FCData.get(0).HighTemperature, TMP_ROUND_WHOLE) + "  ");
        } else {
            holdView.setText("N/A  ");
        }

        holdView = (TextView)getView().findViewById(R.id.txtLowTemp);
        if(place.FCData.size() > 0){
            holdView.setText(xtrTemperature(place.FCData.get(0).LowTemperature, TMP_ROUND_WHOLE) + "  ");
        } else {
            holdView.setText("N/A  ");
        }
        
        if(place.FCData.size() > 0){
            if(!place.FCData.get(0).PercipitationChance.equalsIgnoreCase("0")){
                holdView = (TextView)getView().findViewById(R.id.txtRainChance);
                holdView.setText(place.FCData.get(0).PercipitationChance + "%");
                if(place.getTemperature() < 33 && place.getTemperature() != NMBR_NA){ // If no valid temperature, just say it is rain
                    holdView = (TextView)getView().findViewById(R.id.lblRainChance);
                    holdView.setText(res.getString(R.string.lblForecastSnow));
                } else {
                    holdView = (TextView)getView().findViewById(R.id.lblRainChance);
                    holdView.setText(res.getString(R.string.lblForecastRain));
                }
            }
        }

        holdView = (TextView)getView().findViewById(R.id.txtForeCastDay);
        holdView.setText(xtrForecastTime(place.ForecastUpdt, place.TimeZone, Settings.getDateDisplayFormat(getActivity(), DATE_FMT_SHORT)));
        
        holdView = (TextView)getView().findViewById(R.id.txtForeCastTime);
        dtFmt = Settings.getUse24Clock(getActivity())?DB_fmtDateTime24:DB_fmtDateTime;
        holdView.setText("@" + xtrForecastTime(place.ForecastUpdt, place.TimeZone, dtFmt));
        
        holdImg = (ImageView)getView().findViewById(R.id.imgMoonPhase);
        holdImg.setImageDrawable(res.getDrawable(xtrConditionResource(xtrMoonPhase(place.getMoonAge(), place.getMoonVisible()))));
       
        holdView = (TextView)getView().findViewById(R.id.txtMoonCond);
        holdView.setText(xtrMoonPhase(place.getMoonAge(), place.getMoonVisible()));
        
        // Assume the first/second FCText records (index zero & one) are today's and tonight's forecast.
        if(place.FCText.size() > 0){
            holdImg = (ImageView)getView().findViewById(R.id.imgTodayCond);
            holdImg.setImageDrawable(res.getDrawable(xtrConditionResource(place.FCText.get(0).ConditionIcon)));
        
            holdView = (TextView)getView().findViewById(R.id.txtTodayCond);
            holdView.setText(place.FCText.get(0).conditonFull());

            holdView = (TextView)getView().findViewById(R.id.txtTodayTitle);
            holdView.setText(place.FCText.get(0).Title + " ");  // extra space for italic
            
            holdView = (TextView)getView().findViewById(R.id.txtTodayText);
            holdDesc = place.FCText.get(0).getDescription(Settings.getUseCelsius(getActivity()));
            holdView.setText(holdDesc);
            
            if(place.FCData.size() > 0){
                holdView = (TextView)getView().findViewById(R.id.txtTodayWind);
                holdWind = place.FCData.get(0).getExpectedWind(Settings.getUseMetric(getActivity()));
                holdWind += holdDesc.length() < 55 ? "\n" : ""; // Sometimes the forecasts get bunched up on the screen
                holdView.setText(holdWind);
            }
        }
        
        if(place.FCText.size() > 1){
            holdImg = (ImageView)getView().findViewById(R.id.imgTonightCond);
            holdImg.setImageDrawable(res.getDrawable(xtrConditionResource(place.FCText.get(1).ConditionIcon)));
        
            holdView = (TextView)getView().findViewById(R.id.txtTonightCond);
            holdView.setText(place.FCText.get(1).conditonFull());
            
            holdView = (TextView)getView().findViewById(R.id.txtTonightTitle);
            holdView.setText(place.FCText.get(1).Title + " ");  // extra space for italic
            
            holdView = (TextView)getView().findViewById(R.id.txtTonightText);
            holdDesc = place.FCText.get(1).getDescription(Settings.getUseCelsius(getActivity()));
            holdView.setText(holdDesc);
            
            if(place.FCData.size() > 1){
                holdView = (TextView)getView().findViewById(R.id.txtTonightWind);
                holdWind = place.FCData.get(1).getExpectedWind(Settings.getUseMetric(getActivity()));
                holdWind += holdDesc.length() < 55 ? "\n" : ""; // Sometimes the forecasts get bunched up on the screen
                holdView.setText(holdWind);
            }
        }
        
        dtFmt = Settings.getUse24Clock(getActivity())?DB_fmtDateTime24:DB_fmtDateTime;
        holdView = (TextView)getView().findViewById(R.id.txtSunRiseTime);
        holdView.setText(xtrForecastTime(place.Sunrise, place.TimeZone, dtFmt));
        holdView = (TextView)getView().findViewById(R.id.txtSunSetTime);
        holdView.setText(xtrForecastTime(place.Sunset, place.TimeZone, dtFmt));
        
        holdImg = (ImageView)getView().findViewById(R.id.imgSunRise);
        holdImg.setImageDrawable(res.getDrawable(R.drawable.sunrise));
        
        holdImg = (ImageView)getView().findViewById(R.id.imgSunSet);
        holdImg.setImageDrawable(res.getDrawable(R.drawable.sunset));
        
    }


    /* ******************************************************************************
     * The "xtr" functions provide additional analysis of the raw weather information.
     * Pressure:    The pressure is stored as millibar/hectopascal, but can be converted to inches
     *              of mercury.
     * PressureDiff:This calculates the difference of the current pressure from sea level pressure.
     *              A lower than normal pressure implies bad weather.
     * Humidity:    At high temperatures, high relative humidity can be estimated to increase the 
     *              effective temperature a body feels. 
     * DewPoint:    This expresses how the air feels, ranging from dry and normal, to muggy and sticky. 
     *              The Dew Point is the temperature where H20 will stop evaporating from the body,
     *              So the closer it gets to 98.6, the more sweat is left on our skin.  As a practical
     *              matter, even if the air is DRY, a relative humidity >90% probably means it is raining,
     *              or at least foggy, so we fall back on it being NORMAL (same if condition is rain/snow).
     * Wind:        At low temperatures, increased wind speed can be estimated to decrease the
     *              effective temperature a body feels.
     * WindCond:    Translates the wind speed into a readable term for quick human comprehension. I like 
     *              to used wind gusts for this type of thing because they do not depend on the wind 
     *              being measured RIGHT when the person looked for it.  They have more history.
     * Visibility:  The visibility can be either mile/miles or kilometer/kilometers.
     * Distance:    There are a couple of data items that can be either miles or kilometers, and that
     *              are displayed either as a whole number or one with more precision. 
     * Observed Time: This will format the timestamp to our liking.
     * Forecast Time: A general time/date formating routine.
     * Season:      Calculates the current season (Spring/Summer/Autumn/Winter) based on time & location. Can
     *              be either Meteorological or Astronomical reckoning.
     * Conditions:  This will return the drawing resource id for the icon to be used.
     * Moon Phase:  The lunar phase lasts about 29.5 days, and moves from no illumination, to full illumination
     *              and back.  By knowing the age, we can know which side of the moon is illuminated.  While
     *              the amount illuminated is roughly mapped to one of the 8 images.
     * Temperature: The temperature can be displayed in either fahrenheit or celsius
     * TempIsAverage: This tries to determine if the current temperature should be considered outside a reasonable
     *              estimate of what is the past average temperature for that day.  If the temperature is within
     *              50% of the traditional range, adjusted for time of day, it is Normal.  Otherwise is will be
     *              considered Above or Below average.
     * GetAdRequest: This will build an ad request based on some data from the location being viewed.
     */
    private String xtrPressure(double pressure) {
        if(pressure == NMBR_NA) return DISP_NA;
        String outFrmt = "";
        int precisionNbr = 0;
        int precision = TMP_ROUND_TENTH;     // default pressure (hPa) to 1 decimal place
        String SI = "hPa";
        if(Settings.getUseInches(getActivity())){
            pressure = pressure * 0.02953;  // May be temperature dependent, but no one reports that way. 
            precision = TMP_ROUND_HUNDRED;    // inches to 2 decimal places
            SI = "inHg";
        } 
        // Rounding trick: Math.floor(value * X + .5) X = power of 10 (precision where 10=.1, 100=.01, etc.).
        pressure = Math.floor((pressure * precision + 0.5)) / precision;
        for(int place = precision;  place > 1; place /= 10) { precisionNbr++;  }
        outFrmt += "%1." + precisionNbr + "f " + SI;
        return String.format(outFrmt, pressure);
    }
    private String xtrPressureDiff(double pressure) {
        if(pressure == NMBR_NA) return "0.0";
        double diffPressure = 0.0;
        if(Settings.getUseInches(getActivity())){
            pressure = pressure * 0.02953; // Some evidence that temperature dependent, but no one reports that way.
            diffPressure = pressure - XTR_STD_PRESSURE_IN;
        } else {
            diffPressure = pressure - XTR_STD_PRESSURE;
        }
        String frmtDiffPressure = String.format(Locale.getDefault(),"%+1.2f", diffPressure);
        return frmtDiffPressure;
    }
    private String xtrHumidity(Integer humidity, double temperature) {
        // Note: if either is NMBR_NA, empty string returned.
        if(humidity < 40 || temperature < 80) return "";
        double hi = -42.379 + (2.04901523 * temperature) + (10.14333127 * humidity) + (-0.22475541 * temperature * humidity) +
            (-0.00683783 * temperature * temperature) + (-0.05481717 * humidity * humidity) + 
            (0.00122874 * temperature * temperature * humidity) + (0.00085282 * temperature * humidity * humidity) +
            (-0.00000199 * temperature * temperature  * humidity * humidity);
        if((hi - temperature) < 1.5) return "";
        return "(" + xtrTemperature(hi, TMP_ROUND_TENTH) + ")";
    }
    private String xtrDewPoint(Double dewPoint, Integer humidity, String cond) {
        boolean canBeDry = true;
        if(cond.equals(DB_CondFlurry) || cond.equals(DB_CondFog) || cond.equals(DB_CondRain) || 
           cond.equals(DB_CondSleet) || cond.equals(DB_CondSnow) || cond.equals(DB_CondThunder)){
            canBeDry = false;
        }
        
        String feels = DEW_NORMAL;
        if(dewPoint == NMBR_NA || humidity == NMBR_NA){ return DEW_NORMAL; }
        if(dewPoint < 45.1 && humidity < 85 && canBeDry){ feels = DEW_DRY; }
        if(dewPoint >= 45.1 && dewPoint < 60.1){ feels = DEW_NORMAL; }
        if(dewPoint >= 60.1 && dewPoint < 70.1){ feels = DEW_MUGGY; }
        if(dewPoint >= 70.1 && dewPoint < 80.1){ feels = DEW_STICKY; }
        if(dewPoint >= 80.1){ feels = DEW_OPPRESIVE; }
        
        return feels;
    }
    private String xtrWindTemp(double windSpeed, double temperature) {
        if(windSpeed < 4 || temperature > 49 || temperature == NMBR_NA) return "";
        double wchill = 35.74 + (0.6215 * temperature) - 
                        (35.75 * Math.pow(windSpeed, 0.16)) + 
                        (0.4275 * temperature * Math.pow(windSpeed, 0.16));
        return "(" + xtrTemperature(wchill, TMP_ROUND_WHOLE) + ")";
    }
    private String xtrWindCond(double windSpeed, double windGusts, int precision) {
        String speed = WIND_CALM;
        double holdWind = Math.max(windSpeed, windGusts);
        
        if(holdWind == 0) speed = WIND_NONE;
        if(holdWind < 4.0) { speed = WIND_CALM; }
        if(holdWind >= 4.0 && holdWind < 8.0) { speed = WIND_BREEZY; }
        if(holdWind >= 8.0 && holdWind < 19.0) { speed = WIND_WINDY; }
        if(holdWind >= 19.0 && holdWind < 32.0) { speed = WIND_BLUSTERY; }
        if(holdWind >= 32.0 && holdWind < 39.0) { speed = WIND_GUSTY; }
        if(holdWind >= 39.0 && holdWind < 55.0) { speed = WIND_GALE; }
        if(holdWind >= 55.0 && holdWind < 65.0) { speed = WIND_STORM; }
        if(holdWind >= 65.0) { speed = WIND_DESTROY; }
        
        return speed;
    }
    private String xtrWindSpeed(double windSpeed, double windGusts, int precision) {
        int precisionNbr = 0;
        String measure = "mph";
        String outFrmt = "";
        // For not valid/available wind, we will just go with zero.
        if(windSpeed < 0.0) windSpeed = 0.0;
        if(windGusts < 0.0) windGusts = 0.0;
        if(Settings.getUseMetric(getActivity())) { 
            // Rounding trick: Math.floor(value * X + .5) X = power of 10 (precision where 10=.1, 100=.01, etc.).
            windSpeed = Math.floor((windSpeed * 1.609344) * precision + 0.5) / precision;
            windGusts = Math.floor((windGusts * 1.609344) * precision + 0.5) / precision;
            measure = "kph";
        }
        
        for(int place = precision;  place > 1; place /= 10) { precisionNbr++;  }
        outFrmt += "%1." + precisionNbr + "f (max %1." + precisionNbr + "f) %s";
        return String.format(outFrmt, windSpeed, windGusts, measure);
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
            if(distance < 1.0 && precision == TMP_ROUND_WHOLE){
                distance = 1.0;
                outFrmt = "~";
            }
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
        
        outFrmt += "%1." + precisionNbr + "f %s";
        return String.format(outFrmt, distance, measure_ds);
    }
    private String xtrFullDirection(String windDir, double windSpeed, double windGusts) {
        if((windSpeed <= 0 && windGusts <= 0) || windDir.length() == 0) return DISP_NA;
        
        if(windDir.equalsIgnoreCase("N")) return "North";
        if(windDir.equalsIgnoreCase("NW")) return "North West";
        if(windDir.equalsIgnoreCase("NNW")) return "North by North West";
        if(windDir.equalsIgnoreCase("NE")) return "North East";
        if(windDir.equalsIgnoreCase("NNE")) return "North by North East";
        
        if(windDir.equalsIgnoreCase("W")) return "West";
        if(windDir.equalsIgnoreCase("WNW")) return "West by North West";
        if(windDir.equalsIgnoreCase("WSW")) return "West by South West";

        if(windDir.equalsIgnoreCase("E")) return "East";
        if(windDir.equalsIgnoreCase("ENE")) return "East by North East";
        if(windDir.equalsIgnoreCase("ESE")) return "East by South East";
        
        if(windDir.equalsIgnoreCase("S")) return "South";
        if(windDir.equalsIgnoreCase("SW")) return "South West";
        if(windDir.equalsIgnoreCase("SSW")) return "South by South West";
        if(windDir.equalsIgnoreCase("SE")) return "South East";
        if(windDir.equalsIgnoreCase("SSE")) return "South by South East";
        
        return windDir;
    }
    private String xtrObservedTime(String localTime, String timeZone, String format) {
        try {
            return (String) KTime.ParseToFormat(localTime, DB_inputDate3339, timeZone, format);
        } catch (ExpParseToCalendar ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".xtrObservedTime");
            return "Observation date and time not available.";
        }
    }
    private String xtrForecastTime(String localTime, String timeZone, String format) {
        try {
            return (String) KTime.ParseToFormat(localTime, DB_inputDate3339, timeZone, format);
        } catch (ExpParseToCalendar ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".xtrForecastTime");
            // If a problem, just use current.
            return (String) KTime.ParseNow(format, timeZone);
        }
    }
    private String xtrSeason(String localTime, String timeZone, double latitude) {
        String season = "";
        try{
            boolean northern_hemisphere = latitude >= 0 ? true : false; 
            Calendar localDate = Calendar.getInstance();//KTime.ParseToCalendar(localTime, DB_inputDate3339, timeZone);
            if(Settings.getUseMeteorological(getActivity())){
                switch(localDate.get(Calendar.MONTH)){
                case Calendar.MARCH: case Calendar.APRIL: case Calendar.MAY:
                    season = northern_hemisphere ? SEASON_SPRING : SEASON_AUTUMN;
                    break;
                case Calendar.JUNE: case Calendar.JULY: case Calendar.AUGUST:
                    season = northern_hemisphere ? SEASON_SUMMER : SEASON_WINTER;
                    break;
                case Calendar.SEPTEMBER: case Calendar.OCTOBER: case Calendar.NOVEMBER:
                    season = northern_hemisphere ? SEASON_AUTUMN : SEASON_SPRING;
                    break;
                case Calendar.DECEMBER: case Calendar.JANUARY: case Calendar.FEBRUARY:
                    season = northern_hemisphere ? SEASON_WINTER : SEASON_SUMMER;
                    break;
                }
            }else{  // Use Astronomical reckoning, more work than Meteorological
                switch(localDate.get(Calendar.MONTH)){
                case Calendar.APRIL: case Calendar.MAY:
                    season = northern_hemisphere ? SEASON_SPRING : SEASON_AUTUMN;
                    break;
                case Calendar.JULY: case Calendar.AUGUST:
                    season = northern_hemisphere ? SEASON_SUMMER : SEASON_WINTER;
                    break;
                case Calendar.OCTOBER: case Calendar.NOVEMBER:
                    season = northern_hemisphere ? SEASON_AUTUMN : SEASON_SPRING;
                    break;
                case Calendar.JANUARY: case Calendar.FEBRUARY:
                    season = northern_hemisphere ? SEASON_WINTER : SEASON_SUMMER;
                    break;
                case Calendar.MARCH:
                    if(localDate.get(Calendar.DAY_OF_MONTH) < 20){
                        season = northern_hemisphere ? SEASON_WINTER : SEASON_SUMMER;
                    }else{
                        season = northern_hemisphere ? SEASON_SPRING : SEASON_AUTUMN;
                    }
                    break;
                case Calendar.JUNE:
                    if(localDate.get(Calendar.DAY_OF_MONTH) < 21){
                        season = northern_hemisphere ? SEASON_SPRING : SEASON_AUTUMN;
                    }else{
                        season = northern_hemisphere ? SEASON_SUMMER : SEASON_WINTER;
                    }
                    break;
                case Calendar.SEPTEMBER:
                    if(localDate.get(Calendar.DAY_OF_MONTH) < 22){
                        season = northern_hemisphere ? SEASON_SUMMER : SEASON_WINTER;
                    }else{
                        season = northern_hemisphere ? SEASON_AUTUMN : SEASON_SPRING;
                    }
                    break;
                case Calendar.DECEMBER:
                    if(localDate.get(Calendar.DAY_OF_MONTH) < 21){
                        season = northern_hemisphere ? SEASON_AUTUMN : SEASON_SPRING;
                    }else{
                        season = northern_hemisphere ? SEASON_WINTER : SEASON_SUMMER;
                    }
                    break;
                }
            }
            return season;
        } catch(Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".xtrSeason");
            return season;
        }
    }
    private int xtrConditionResource(String cond){
        int holdDrawID = R.drawable.clear;
        if(cond.equals(DB_CondClear))       { holdDrawID = R.drawable.clear; }
        if(cond.equals(DB_CondClearN))      { holdDrawID = R.drawable.nt_clear; }
        if(cond.equals(DB_CondCloud))       { holdDrawID = R.drawable.cloudy; }
        if(cond.equals(DB_CondFlurry))      { holdDrawID = R.drawable.flurries; }
        if(cond.equals(DB_CondFlurryP))     { holdDrawID = R.drawable.flurries; }
        if(cond.equals(DB_CondFog))         { holdDrawID = R.drawable.fog; }
        if(cond.equals(DB_CondHazy))        { holdDrawID = R.drawable.fog; }
        if(cond.equals(DB_CondMostCloud))   { holdDrawID = R.drawable.partlycloudy; }
        if(cond.equals(DB_CondMostSun))     { holdDrawID = R.drawable.partlycloudy; }
        if(cond.equals(DB_CondPartCloud))   { holdDrawID = R.drawable.partlycloudy; }
        if(cond.equals(DB_CondPartCloudN))  { holdDrawID = R.drawable.nt_partlycloudy; }
        if(cond.equals(DB_CondPartSun))     { holdDrawID = R.drawable.partlycloudy; }
        if(cond.equals(DB_CondRain))        { holdDrawID = R.drawable.rain; }
        if(cond.equals(DB_CondRainP))       { holdDrawID = R.drawable.rain; }
        if(cond.equals(DB_CondSleet))       { holdDrawID = R.drawable.rain; }
        if(cond.equals(DB_CondSleetP))      { holdDrawID = R.drawable.rain; }
        if(cond.equals(DB_CondSnow))        { holdDrawID = R.drawable.flurries; }
        if(cond.equals(DB_CondSnowP))       { holdDrawID = R.drawable.flurries; }
        if(cond.equals(DB_CondSun))         { holdDrawID = R.drawable.clear; }
        if(cond.equals(DB_CondThunder))     { holdDrawID = R.drawable.tstorms; }
        if(cond.equals(DB_CondThunderP))    { holdDrawID = R.drawable.tstorms; }
        if(cond.equals(DB_CondUnknown))     { holdDrawID = R.drawable.partlycloudy; }
        if(cond.equals(DB_MoonNew))         { holdDrawID = R.drawable.newmoon; }
        if(cond.equals(DB_MoonWaxCrescent)) { holdDrawID = R.drawable.wax_crecent; }
        if(cond.equals(DB_MoonFirstQtr))    { holdDrawID = R.drawable.firstqtr; }
        if(cond.equals(DB_MoonWaxGibbous))  { holdDrawID = R.drawable.wax_gibbous; }
        if(cond.equals(DB_MoonFull))        { holdDrawID = R.drawable.fullmoon; }
        if(cond.equals(DB_MoonWanGibbous))  { holdDrawID = R.drawable.wan_gibbous; }
        if(cond.equals(DB_MoonLastQtr))     { holdDrawID = R.drawable.lastqtr; }
        if(cond.equals(DB_MoonWanCrescent)) { holdDrawID = R.drawable.wan_crecent; }
        return holdDrawID;
    }
    private String xtrMoonPhase(int age, int visible){
        boolean waxing = (age < 15);
        if(visible == 0)   return DB_MoonNew;
        if(visible == 100 || visible == 99) return DB_MoonFull;
        if(visible >= 45 && visible <= 55 ){
            if(waxing){
                return DB_MoonFirstQtr;
            } else {
                return DB_MoonLastQtr;
            }
        }
        if(visible > 0 && visible < 45){
            if(waxing){
                return DB_MoonWaxCrescent;
            } else {
                return DB_MoonWanCrescent;
            }
        }
        if(visible > 55 && visible < 99){
            if(waxing){
                return DB_MoonWaxGibbous;
            } else {
                return DB_MoonWanGibbous;
            }
        }
        return DB_MoonNew; // default, should not get here.
    }
    private String xtrTemperature(String tmp, int precision){
        String outFrmt = "";
        int precisionNbr = 0;
        double holdTemp;
        
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
    private String xtrTemperature(double tmp, int precision){
        String outFrmt = "";
        int precisionNbr = 0;
        double holdTemp = tmp;
        
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
    private String xtrTempIsAverage(double temp, double tempAveMin, double tempAveMax, String timezone) {
        // Simple cases
        if(temp < tempAveMin) return TEMP_AVE_LOW;
        if(temp > tempAveMax) return TEMP_AVE_HIGH;

        // The idea here is that the if the temperature is in a range from the midpoint of 50% of the total
        // range, the temperature is within the average.  Given that temperatures are higher during the day 
        // and lower at night, the "50%" range floats with the time of day.
        // A = midpoint, N = Range Size, A_Low = range lower limit, A_High = range upper limit, T = current hour
        double A = (tempAveMin + tempAveMax) * .5;
        double N = (tempAveMax - tempAveMin) * .5;
        double T = Calendar.getInstance(TimeZone.getTimeZone(timezone)).get(Calendar.HOUR_OF_DAY);
        if(T > 12) T = T - (2 * (T - 12));
        double T_Low = (12 - T) / 12;
        double T_High = T / 12;
        double A_Low = A - (N * T_Low);
        double A_High = A + (N * T_High);
        if(temp < A_Low) return TEMP_AVE_LOW;
        if(temp > A_High) return TEMP_AVE_HIGH;
        
        return TEMP_AVE_NORM;
    }
}