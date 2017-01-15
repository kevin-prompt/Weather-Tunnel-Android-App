package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import org.xmlpull.v1.XmlPullParserException;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Build;
import android.text.format.DateFormat;

/*************************************************************************
 * Data structure representing a location and its current & future climate. 
 */
public class Status {

    int Id = 0;
    private Map<String, String> cityMap = new TreeMap<>();
    private String cityMapType = LOC_TYPE_GENERAL;
    String registration = "";
    boolean isDataLoaded = false;
    boolean isDataStale = false;

    /*
     * IMPORTANT.  Any time a new Status property is added it must also be supported in CopyStatus().  There is a
     * lot of passing of this object around and multiple reads to get data, and CopyStatus() is how it is all done.
     */
    // Location
    String City = "";
    String State = "";
    String Country = "";
    String Zip = "";
    String LocalTime = "";       // Local time at observed weather site.
    String TimeZone = "";
    String Elevation = "";
    String Latitude = "";
    String Longitude = "";
    String PrivateCode = "";     // Personal weather station (pws).
    String PrivateDist = "";     // Distance of location from pws.
    String PrivateCodeB = "";    // Backup pws.
    String PrivateDistB = "";    // Distance of location from Backup pws.
    String PublicCode = "";      // Public weather station (air), usually airport.
    String PublicDist = "";      // Distance of location from air
    String PublicCntry = "";     // Holds the country code for the public station, seems better able to get data for private station.
    String PublicLong = "";      // This field is used for temporary storage until distance is calculated.
    String PublicLati = "";      // This field is used for temporary storage until distance is calculated.
    String Special = "";         // A local note field to store a description of the item.
    int IsSlate = ClimateDB.SQLITE_TRUE;     // When set to 1, item is currently visible on Slate.
    int IsWidget = ClimateDB.SQLITE_FALSE;   // When set to 1, item is currently visible as a Widget.
    private static final int CODE_PRIVATE = 1;
    private static final int CODE_PUBLIC = 2;
    private static final int CODE_PRIVATE_SLIM = 3;
    private static final int CODE_PUBLIC_SLIM = 4;
    private static final int CODE_PRIVATE_DETAIL = 5;
    private static final int CODE_PUBLIC_DETAIL = 6;
    private static final int CODE_PRIVATE_FORCE = 7;
    private static final int CODE_PUBLIC_FORCE = 8;
    // Climate
    String Temperature = "";     // Temperature in degrees fahrenheit
    private String TemperaturePub = ""; // Temperature of the public reporting station
    String TempAveMax = "";      // Temperature average high for this day.
    String TempAveMin = "";      // Temperature average low for this day.
    String TempRecMax = "";      // Temperature record high for this day.
    String TempRecMin = "";      // Temperature record low for this day.
    String TempRecMaxYY = "";    // Year of temperature record high for this day.
    String TempRecMinYY = "";    // Year of temperature record low for this day.
    String ObservedTime = "";    // Time at observation station.
    String Condition = "";       // Descriptive weather condition.
    String ConditionIcon = "";   // Description of weather keyed to icon.
    String Visibility = "";      // Visibility distance in miles.
    String Humidity = "";        // Relative Humidity.
    String WindDir = "";         // Direction Wind is blowing from.
    String WindSpeed = "";       // Average wind speed.
    String WindGusts = "";       // Maximum wind speed.
    String Pressure = "";        // Air Pressure adjusted to sea level in millabars/hectopascal. 1013.25 is normal sea level.
    String PressureDelta = "0";  // Seems to be three states: "-", "0", "+".  Guessing they are falling, steady and rising.
    String DewPoint = "";        // The temperature (fahrenheit) where water will fall from the air.
    String Precip24 = "";        // Precipitation over the last 24 hours in inches.
    // Alert
    int IsAlert = ClimateDB.SQLITE_FALSE;    // Is there currently an alert for this location?
    ArrayList<AlertData> ALData = new ArrayList<>(); // List of all Weather Alerts.
    // Forecast
    String ForecastUpdt = "";    // Last update time of the current forecast information.
    String Sunrise = "";         // The time of the sunrise.
    String Sunset = "";          // The time of the sunset.
    String NightTime = DATA_NA;  // Overrides how to display the condition icon. 0=Day,1=Night,DATA_NA=ignore.
    String MoonVisible = "";     // The percent of the moon visible, e.g. 100 = full.
    String MoonAge = "";         // The days from the last new moon, allows us to know if waxing or waning.
    ArrayList<ForecastSpkn> FCText = new ArrayList<>();  // List of human readable forecast information.
    ArrayList<ForecastData> FCData = new ArrayList<>();  // List of machine readable forecast information.

    // Helper Getters to return specific data types.
    double getTemperature(){
        try { return Double.parseDouble(Temperature); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getTemperature"); return NMBR_NA; }}
    private double getTemperaturePub(){
        try { return Double.parseDouble(TemperaturePub); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getTemperaturePub"); return NMBR_NA; }}
    double getTempAveMax(){
        try { return Double.parseDouble(TempAveMax); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getTempAveMax"); return NMBR_NA; }}
    double getTempAveMin(){
        try { return Double.parseDouble(TempAveMin); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getTempAveMin"); return NMBR_NA; }}
    double getTempRecMax(){
        try { return Double.parseDouble(TempRecMax); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getTempRecMax"); return NMBR_NA; }}
    double getTempRecMin(){
        try { return Double.parseDouble(TempRecMin); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getTempRecMin"); return NMBR_NA; }}
    int getTempRecMaxYY(){
        try { return Integer.parseInt(TempRecMaxYY); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getTempRecMaxYY"); return NMBR_NA; }}
    int getTempRecMinYY(){
        try { return Integer.parseInt(TempRecMinYY); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getTempRecMinYY"); return NMBR_NA; }}
    double getElevation(){
        try { return Double.parseDouble(Elevation); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getElevation"); return NMBR_NA; }}
    double getVisibility(){
        try { return Double.parseDouble(Visibility); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getVisibility"); return NMBR_NA; }}
    int getHumidity(){
        try { return Integer.parseInt(Humidity); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getHumidity"); return NMBR_NA; }}
    double getWindSpeed(){
        try { return Double.parseDouble(WindSpeed); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getWindSpeed"); return NMBR_NA; }}
    double getWindGusts(){
        try { return Double.parseDouble(WindGusts); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getWindGusts"); return NMBR_NA; }}
    double getPressure(){
        try { return Double.parseDouble(Pressure); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getPressure"); return NMBR_NA; }}
    double getDewPoint(){
        try { return Double.parseDouble(DewPoint); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getDewPoint"); return NMBR_NA; }}
    double getPrecip24(){
        try { return Double.parseDouble(Precip24); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getPrecip24"); return NMBR_NA; }}
    int getPublicDist(){
        try { return Integer.parseInt(PublicDist); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getPublicDist"); return NMBR_NA; }}
    int getPrivateDist(){
        try { return Integer.parseInt(PrivateDist); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getPrivateDist"); return NMBR_NA; }}
    int getPrivateDistB(){
        try { return Integer.parseInt(PrivateDistB); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getPrivateDistB"); return NMBR_NA; }}
    private double getPublicLati(){
        try { return Double.parseDouble(PublicLati); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getPublicLati"); return NMBR_NA; }}
    private double getPublicLong(){
        try { return Double.parseDouble(PublicLong); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getPublicLong"); return NMBR_NA; }}
    double getLatitude(){
        try { return Double.parseDouble(Latitude); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getLatitude"); return NMBR_NA; }}
    double getLongitude(){
        try { return Double.parseDouble(Longitude); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getLongitude"); return NMBR_NA; }}
    int getMoonVisible(){
        try { return Integer.parseInt(MoonVisible); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getMoonVisible"); return NMBR_NA; }}
    int getMoonAge(){
        try { return Integer.parseInt(MoonAge); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getMoonAge"); return NMBR_NA; }}
    boolean isNight(){ return CorrectConditionIcon(DATA_NA)!=ClimateDB.SQLITE_TRUE; }
    int getCityListCnt() { return cityMap.size(); }
    String getCityListType() { return cityMapType; }
    private boolean isPwsOnly() { return Special.contains(PWS_CODEKEY); }
    ArrayList<String> getCityList() {
        // Put the list of locations in a form that can be easily used by the Cities class to pick one.
        ArrayList<String> cityList = new ArrayList<>();
//        Iterator<Map.Entry<String, String>> ali = cityMap.entrySet().iterator();
//        while (ali.hasNext()) {
//            Map.Entry<String, String> city = ali.next();
//            cityList.add(city.getKey() + DELIMIT_SPLIT + city.getValue());
//        }
        for (Map.Entry<String, String> city : cityMap.entrySet()) {
            cityList.add(city.getKey() + DELIMIT_SPLIT + city.getValue());
        }
        return cityList;
    }

    /* ******************************************************************************
     * Load up everything we know about a location from the database.  Generally the data
     * stored in the db is location information required to make further API calls for info.
     */
    void LoadLocation(Context context, String id) {
        ClimateDB climate = new ClimateDB(context);  // Be sure to close this before leaving the method.
        try{
            SQLiteDatabase db = climate.getReadableDatabase();
            String[] filler = {};
            Cursor cursor = db.rawQuery(DB_StatusID + id, filler);
            try{
                if(cursor.moveToNext()){
                    Id = cursor.getInt(cursor.getColumnIndex(ClimateDB.STATUS_ID));
                    PublicCode = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_STAT_PUB));
                    PrivateCode = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_STAT_PRI));
                    PrivateCodeB = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_STAT_PRI2));
                    PublicDist = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_DIST_PUB));
                    PrivateDist = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_DIST_PRI));
                    PrivateDistB = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_DIST_PRI2));
                    Zip = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_ZIP));
                    City = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_CITY));
                    State = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_STATE));
                    Country = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_CNTY));
                    Latitude = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_LATI));
                    Longitude = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_LONG));
                    TimeZone = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_TZONE));
                    LocalTime = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_TIME));
                    Temperature = Float.toString(cursor.getFloat(cursor.getColumnIndex(ClimateDB.STATUS_TEMP)));
                    Special = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_NOTE));
                    Condition = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_COND));
                    ConditionIcon = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_CICON));
                    IsAlert = cursor.getInt(cursor.getColumnIndex(ClimateDB.STATUS_ALERT));
                    IsSlate = cursor.getInt(cursor.getColumnIndex(ClimateDB.STATUS_ONSLATE));
                    IsWidget =  cursor.getInt(cursor.getColumnIndex(ClimateDB.STATUS_ONWIDGET));
                }
                cursor.close();
            }catch(Exception ex){
                cursor.close(); ExpClass.LogEX(ex, this.getClass().getName() + ".loadDetailsA");
            }
            climate.close();
        }catch(Exception ex){
            climate.close(); ExpClass.LogEX(ex, this.getClass().getName() + ".loadDetailsB");
        }
    }

    // Find a location and fill in the data.
    int Search(Context context, String where, String link, String type){
        HttpURLConnection webC;
        URL web;
        try {
        // Web Service Call and Other Set Up
        if(where == null || where.length() == 0) return 0;
        cityMapType = type;

        // From the Contacts we might get a physical address, reverse geocoding works.
        ContactAddr geoAddr = null;
        if(type.equalsIgnoreCase(LOC_TYPE_GEOCODE)){
            web = new URL(GMI_Lookup + URLEncoder.encode(where, Charset.defaultCharset().name()));
            webC = (HttpURLConnection) web.openConnection();
            webC.setRequestMethod("GET"); // Available: POST, PUT, DELETE, OPTIONS, HEAD and TRACE
            webC.setRequestProperty("Accept", "application/xml");
            webC.setUseCaches(false);
            webC.setAllowUserInteraction(false);
            webC.setConnectTimeout(FTI_TIMEOUT);
            webC.setReadTimeout(FTI_TIMEOUT);
            webC.connect();
            ExpClass.LogIN(KEVIN_SPEAKS, "Calling Web Service Search: " +  web.getPath());
            int status = webC.getResponseCode();
            if(status == 500){
                ExpClass.LogIN(KEVIN_SPEAKS, "ERROR 500 returned from web service call");
                return 0;   // Climate Web Service exception in REST = an HTTP Server Error.
            }
            GmiGeocodeXPP xmlHandler = new GmiGeocodeXPP();
            xmlHandler.setInput(new InputStreamReader(webC.getInputStream(), "UTF8"));
            xmlHandler.parse();
            geoAddr = xmlHandler.getStatus();
            link =  geoAddr.getGPSLink();
            webC.disconnect();
        }

        // In a couple of conditions we end up with a set of coordinates, just make them the link.
        if(type.equalsIgnoreCase(LOC_TYPE_EXACT)) {
            link = where;
        }

        // Use the AutoComplete API (for cities) or the Prepaq API (for destinations).  If have a link, bypass this step.
        if(link.length() == 0) {
            if(type.equalsIgnoreCase(LOC_TYPE_PREPAQ)) { // Handle PrePaq Data
                web = new URL(WTI_Prepaq + where + getTicket(context));
            } else {
                web = new URL(WUI_Search + where.replace(" ", "%20") + WUI_SearchFmtXml);
            }
            webC = (HttpURLConnection) web.openConnection();
            webC.setRequestMethod("GET"); // Available: POST, PUT, DELETE, OPTIONS, HEAD and TRACE
            webC.setRequestProperty("Accept", "application/xml");
            webC.setUseCaches(false);
            webC.setAllowUserInteraction(false);
            webC.setConnectTimeout(FTI_TIMEOUT);
            webC.setReadTimeout(FTI_TIMEOUT);
            webC.connect();
            ExpClass.LogIN(KEVIN_SPEAKS, "Calling Web Service Search: " +  web.getPath());
            int status = webC.getResponseCode();
            if(status == 500){
                ExpClass.LogIN(KEVIN_SPEAKS, "ERROR 500 returned from web service call");
                return 0;   // Climate Web Service exception in REST = an HTTP Server Error.
            }
            WuiFindCitiesXPP xmlHandler = new WuiFindCitiesXPP();
            xmlHandler.setInput(new InputStreamReader(webC.getInputStream(), "UTF8"));
            xmlHandler.parse();
            webC.disconnect();

            // Check the results.
            switch (xmlHandler.result()){
                case 0: // Some Problem
                    return 0;
                case 1: // Found one, further processing required
                    link = xmlHandler.link();
                    break;
                default:
                    CopyLocations(xmlHandler.pickCity());
                    return cityMap.size();
            }
        }


        // We have a link, so try to get the real data.  There are rare instances where even this will return multiple items.
        web = new URL(WUI_SearchLink + link + getTicket(context));
        webC = (HttpURLConnection) web.openConnection();
        webC.setRequestMethod("GET"); // Available: POST, PUT, DELETE, OPTIONS, HEAD and TRACE
        webC.setRequestProperty("Accept", "application/xml");
        webC.setUseCaches(false);
        webC.setAllowUserInteraction(false);
        webC.setConnectTimeout(FTI_TIMEOUT);
        webC.setReadTimeout(FTI_TIMEOUT);
        webC.connect();
        ExpClass.LogIN(KEVIN_SPEAKS, "Calling Web Service Search: " + web.getPath());
        int status = webC.getResponseCode();
        if(status == 500){
            ExpClass.LogIN(KEVIN_SPEAKS, "ERROR 500 returned from web service call");
            return 0;   // Climate Web Service exception in REST = an HTTP Server Error.
        }
        WuiLocationXPP xmlHandler = new WuiLocationXPP();
        xmlHandler.setInput(new InputStreamReader(webC.getInputStream(), "UTF8"));
        xmlHandler.parse();

        // Check the results.
        switch (xmlHandler.result()){
            case 0: // Some Problem
                return 0;
            case 1: // Found it
                CopyStatus(xmlHandler.getStatus(), link, NMBR_NA);
                Update(UPD_STATIONS_ALL, context);
                GetRealLocation(link, type, geoAddr);
                CopyLocations(xmlHandler.pickCity());   // Need to update in case caller is using city count
                return cityMap.size();
            default: // Multiple choice
                CopyLocations(xmlHandler.pickCity());
                return cityMap.size();
        }


    } catch (IOException ex) {
        ExpClass.LogEX(ex, this.getClass().getName() + ".Search");
        return 0;
    } catch (XmlPullParserException ex) {
        ExpClass.LogEX(ex, this.getClass().getName() + ".Search");
        return 0;
    } catch (IllegalStateException ex) {
        ExpClass.LogEX(ex, this.getClass().getName() + ".Search");
        return 0;
    } catch (ExpClass ex) {
        ExpClass.LogEX(ex, this.getClass().getName() + ".Search");
        return 0;
    }
    }

    /* Updates the condition data. Any valid data in the second call to LoadConditions
     * will overwrite what is there, so call the closest station last. To reduce data
     * transferred, call trimmed down APIs if possible.  The Slim and Detail API calls
     * contain much less data than a full API call.
     * When the FORCE codes are used, there are not updates to local data.  They only
     * force the server cache to be refreshed (within reason).  It is expected that
     * they will be followed by regular calls to update the local storage.
     */
    void Update(int how, Context context){
        // Make sure we have valid distances.  If they do not, the large default value will help to avoid problems.
        int publicDist = 9999; int privateDist = 9999;
        if(getPublicDist() != NMBR_NA) publicDist = getPublicDist();
        if(getPrivateDist() != NMBR_NA) privateDist = getPrivateDist();
        if(publicDist == 9999 && privateDist == 9999) return;   // no valid distances
        // Person Weather Station specific items only report back their data.
        if(isPwsOnly() && how == UPD_STATIONS_ALL) how = UPD_STATIONS_CLOSEST;

        switch(how){
        case UPD_STATIONS_FORCE:
            ForceRefresh(CODE_PRIVATE_FORCE, context);
            ForceRefresh(CODE_PUBLIC_FORCE, context);
            break;
        case UPD_STATIONS_ALL:
            if(getPublicDist() < getPrivateDist()) {
                LoadStatus(CODE_PRIVATE_DETAIL, context);
                LoadStatus(CODE_PUBLIC, context);
            } else {
                LoadStatus(CODE_PUBLIC_DETAIL, context);
                LoadStatus(CODE_PRIVATE, context);
            }
            break;
        case UPD_STATIONS_CLOSEST:
            if(getPublicDist() < getPrivateDist()) {
                LoadStatus(CODE_PUBLIC, context);
            } else {
                LoadStatus(CODE_PRIVATE, context);
            }
            break;
        case UPD_STATIONS_ALL_SL:
            if(getPublicDist() < getPrivateDist()) {
                LoadStatus(CODE_PRIVATE_SLIM, context);
                LoadStatus(CODE_PUBLIC_SLIM, context);
            } else {
                LoadStatus(CODE_PUBLIC_SLIM, context);
                LoadStatus(CODE_PRIVATE_SLIM, context);
            }
            break;
        case UPD_STATIONS_CLOSEST_SL:
            if(getPublicDist() < getPrivateDist()) {
                LoadStatus(CODE_PUBLIC_SLIM, context);
            } else {
                LoadStatus(CODE_PRIVATE_SLIM, context);
            }
            break;
        }
    }

    /*****************************************************************************************
     * Private Helper Functions
     */

    /* This will cause the server cache to be updated from the backing source. No data is returned
     * when this call is made, but we want to hang around for it to complete so we know the cache
     * has the latest data the next time LoadStatus() is called.
     * We also want to clear out any local cached data, so make sure the right cacheType is applied.
     */
    private void ForceRefresh(int type, Context context) {
        String query;
        String cacheType;
        ClimateDB climate = new ClimateDB(context); // Be sure to close this before leaving the thread.
        HttpURLConnection webC;

        try {
            switch (type){
            case CODE_PUBLIC_FORCE:
                if(getPublicDist() == NMBR_NA) { return; }    // empty
                query = WUI_StationFR + PublicCode + getTicket(context);
                cacheType = APICACHE_TYPE_PUB;
                break;
            case CODE_PRIVATE_FORCE:
                if(getPrivateDist() == NMBR_NA) { return; }    // empty
                query = WUI_StationFR + PrivateCode + getTicket(context);
                cacheType = APICACHE_TYPE_PRI;
                break;
            default:
                return;
            }

            URL web = new URL(query.replace(" ", "%20"));
            webC = (HttpURLConnection) web.openConnection();
            webC.setRequestMethod("GET"); // Available: POST, PUT, DELETE, OPTIONS, HEAD and TRACE
            webC.setRequestProperty("Accept", "application/xml");
            webC.setUseCaches(false);
            webC.setAllowUserInteraction(false);
            webC.setConnectTimeout(FTI_TIMEOUT);
            webC.setReadTimeout(FTI_TIMEOUT);
            webC.connect();
            ExpClass.LogIN(KEVIN_SPEAKS, "Calling Web Service Station: " + web.getPath());
            int status = webC.getResponseCode(); // Nothing returned, just side effect.
            if(status != 500){// Climate Web Service exception in REST = an HTTP Server Error.
                ExpClass.LogIN(KEVIN_SPEAKS, "ERROR 500 returned from web service call");
            }

            SQLiteDatabase db = climate.getWritableDatabase();
            NewCacheHit(db, cacheType, query, "");
            climate.close();
        }
        catch (Exception ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".LoadStatus"); climate.close();}
    }

    /* Get all the weather data, based on the public or private code type of the Status. For the large
     * data reads, there exists a local cache, such that jumping in and out of the detail screen  will
     * not pound the network with un-needed requests.
     */
    private void LoadStatus(int type, Context context) {
        // There are different web service calls for Public and Private.
        String query;
        String cacheType = "";
        String rawResponse = "";
        String lastKnownTemp = Temperature;
        ClimateDB climate = new ClimateDB(context); // Be sure to close this before leaving the thread.
        HttpURLConnection webC;

        try {
            SQLiteDatabase db = climate.getWritableDatabase();
            switch (type){
            case CODE_PUBLIC:
                if(getPublicDist() == NMBR_NA) { return; }    // empty
                query = WUI_Station + PublicCode + getTicket(context);
                cacheType = APICACHE_TYPE_PUB;
                break;
            case CODE_PRIVATE:
                if(getPrivateDist() == NMBR_NA) { return; }    // empty
                query = WUI_Station + PrivateCode + getTicket(context);
                cacheType = APICACHE_TYPE_PRI;
                break;
            case CODE_PUBLIC_SLIM:
                if(getPublicDist() == NMBR_NA) { return; }    // empty
                query = WUI_StationSL + PublicCode + getTicket(context);
                break;
            case CODE_PRIVATE_SLIM:
                if(getPrivateDist() == NMBR_NA) { return; }    // empty
                query = WUI_StationSL + PrivateCode + getTicket(context);
                break;
            case CODE_PUBLIC_DETAIL:
                if(getPublicDist() == NMBR_NA) { return; }    // empty
                query = WUI_StationDT + PublicCode + getTicket(context);
                break;
            case CODE_PRIVATE_DETAIL:
                if(getPrivateDist() == NMBR_NA) { return; }    // empty
                query = WUI_StationDT + PrivateCode + getTicket(context);
                break;
            default:
                return;
            }

            // Web Service Call
            if(cacheType.length() > 0){
                rawResponse = GetCacheHit(db, cacheType, query);
            }
            if(rawResponse.length() == 0){
                URL web = new URL(query.replace(" ", "%20"));
                webC = (HttpURLConnection) web.openConnection();
                webC.setRequestMethod("GET"); // Available: POST, PUT, DELETE, OPTIONS, HEAD and TRACE
                webC.setRequestProperty("Accept", "application/xml");
                webC.setUseCaches(false);
                webC.setAllowUserInteraction(false);
                webC.setConnectTimeout(FTI_TIMEOUT);
                webC.setReadTimeout(FTI_TIMEOUT);
                webC.connect();
                ExpClass.LogIN(KEVIN_SPEAKS, "Calling Web Service Station: " + web.getPath());
                int status = webC.getResponseCode();
                if(status != 500){// Climate Web Service exception in REST = an HTTP Server Error.
                    rawResponse = inputStreamToString(webC.getInputStream(), "UTF8");
                    if(cacheType.length() > 0) NewCacheHit(db, cacheType, query, rawResponse);
                } else {
                    ExpClass.LogIN(KEVIN_SPEAKS, "ERROR 500 returned from web service call");
                }
            }
            WuiStationXPP xmlHandler = new WuiStationXPP();
            xmlHandler.setInput(new StringReader(rawResponse));
            xmlHandler.parse();

            // Move the data locally.
            CopyStatus(xmlHandler.getStatus(), type);

            // We should have a Temperature, if not and it is a private station, we should switch to the backup.
            if((type==CODE_PRIVATE || type==CODE_PRIVATE_SLIM) && Temperature.length()==0){
                SwapPrivateStations(db);
            }
            // Reasonableness check: sometimes private stations go crazy, so swap them if temp. too far from public.
            if((Math.abs(getPublicDist()-getPrivateDist()) < 13) && getTemperaturePub() != NMBR_NA && getTemperature() != NMBR_NA){
                if(!Special.contains(PWS_CODEKEY) && Math.abs(getTemperaturePub() - getTemperature()) > 50){
                    SwapPrivateStations(db);
                    Temperature = lastKnownTemp;
                }
            }
            climate.close();
        }
        catch (Exception ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".LoadStatus"); climate.close();}
    }

    /**
     * It is possible to have a weather station that is giving out bad data, or at least data without a temperature.
     * For example, the MADIS stations can be wind only.  Odds are that the backup station is fine, so switch to it.
     * While it is a fair amount of work to bother with this rare problem, the Temperature is an important feature.
     */
    private void SwapPrivateStations(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(ClimateDB.STATUS_STAT_PRI, PrivateCodeB);
        values.put(ClimateDB.STATUS_STAT_PRI2, PrivateCode);
        values.put(ClimateDB.STATUS_DIST_PRI, getPrivateDistB());
        values.put(ClimateDB.STATUS_DIST_PRI2, getPrivateDist());
        String where = "_id = " + Id;
        String[] filler = {};
        db.update(ClimateDB.STATUS_TABLE, values, where, filler);
    }

    /**
     *  For some types of API calls, we cache the last response.  This lets some of the activities that display data from the same source
     *  operate independently (without in memory global variables) but still run quickly by using data that was recently downloaded.
     *  If more types of API calls need to be cached, they can be added to the supported type.  Only one record per type is cached to avoid
     *  the hassle of cache management.
     *  NOTE: We use raw seconds for age and this requires a hack because the db stores a 32bit int but Calendar return msec greater than can
     *  beheld in that size int.  The key is when getting the data, use getLong so the math to restore it works correctly.
     */
    private void NewCacheHit(SQLiteDatabase db, String cacheType, String query, String data) {
        if(cacheType.length()==0 || query.length()==0) return;

        try {
            ContentValues values = new ContentValues();
            values.put(ClimateDB.APICACHE_LINK, query);
            values.put(ClimateDB.APICACHE_TIME, (Calendar.getInstance().getTimeInMillis()/1000)); // msec is a little big for column
            values.put(ClimateDB.APICACHE_DATA, data);
            String where = ClimateDB.APICACHE_TYPE + " like '" + cacheType + "'";
            String[] filler = {};
            db.update(ClimateDB.APICACHE_TABLE, values, where, filler);
        } catch(Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".NewCacheHit"); }
    }
    private String GetCacheHit(SQLiteDatabase db, String cacheType, String query) {
        if(cacheType.length()==0 || query.length()==0) return "";
        String[] filler = {};
        String real = DB_APICACHEHIT.replace("ZZZ", cacheType);
        Cursor cursor = db.rawQuery(real, filler);
        Boolean fail = false;
        try{
            if(cursor.getCount() == 0) fail = true;
            cursor.moveToNext();
            if(!query.equalsIgnoreCase(cursor.getString(cursor.getColumnIndex(ClimateDB.APICACHE_LINK)))) fail = true;
            long age = Calendar.getInstance().getTimeInMillis() - (cursor.getLong(cursor.getColumnIndex(ClimateDB.APICACHE_TIME)) * 1000);
            if(age > DB_APICACHE_STALE) fail = true;
            String rtn = cursor.getString(cursor.getColumnIndex(ClimateDB.APICACHE_DATA));
            cursor.close();
            if(!fail) ExpClass.LogIN(KEVIN_SPEAKS, "Local cache hit for " + cacheType + " where query used = " + query);
            return fail ? "" : rtn;
        } catch(Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".CheckCacheHit"); cursor.close(); return ""; }
    }

    /**
     * Return the ticket to be appended on the end of web service calls.  Do what is needed to find the ticket value.
     */
    private String getTicket(Context context)  throws IOException, IllegalStateException, XmlPullParserException, ExpClass {
        // Check locally, then in shared preferences and finally go get a new one.
        HttpURLConnection webC = null;
        try {
            if (registration.length() == 0) {
                SharedPreferences registered = context.getSharedPreferences(SP_REG_OK, Context.MODE_PRIVATE);
                registration = registered.getString(SP_REG_KEY, "");
                if (registration.length() == 0) {     // Need to register
                    String id = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                    if (id == null) id = "noid-" + Build.VERSION.RELEASE;
                    // Web Services
                    URL web = new URL(WTI_Register + id + "?ticket=" + WTI_TicketVer + context.getResources().getText(R.string.ver_nbr)); // Use the current version as ticket
                    webC = (HttpURLConnection) web.openConnection();
                    webC.setRequestMethod("GET"); // Available: POST, PUT, DELETE, OPTIONS, HEAD and TRACE
                    webC.setRequestProperty("Accept", "application/xml");
                    webC.setUseCaches(false);
                    webC.setAllowUserInteraction(false);
                    webC.setConnectTimeout(FTI_TIMEOUT);
                    webC.setReadTimeout(FTI_TIMEOUT);
                    webC.connect();
                    ExpClass.LogIN(KEVIN_SPEAKS, "Calling Web Service Register: " + web.getPath());
                    int status = webC.getResponseCode();
                    if (status == 500) {
                        throw new ExpClass(18500, this.getClass().getName() + ".getTicket", "NO REGISTRATION", null);
                    }
                    // Want to debut this by looking at the actual data? - uncomment these lines
//                    BufferedReader br = new BufferedReader(new InputStreamReader(webC.getInputStream(), "UTF8"));
//                    StringBuilder sb = new StringBuilder();
//                    String line;
//                    while ((line = br.readLine()) != null) {
//                        sb.append(line).append("\n");
//                    }

                    WuiStationXPP xmlHandler = new WuiStationXPP();
                    xmlHandler.setInput(new InputStreamReader(webC.getInputStream(), "UTF8"));
                    xmlHandler.parse();
                    registration = xmlHandler.getStatus().registration;
                    SharedPreferences.Editor ali = registered.edit();
                    ali.putString(SP_REG_KEY, registration);
                    ali.apply();
                }
                // Add the current version onto the end of the ticket
                registration += WTI_TicketVer + context.getResources().getText(R.string.ver_nbr);
            }
            return "?ticket=" + registration;
        } finally {
            if (webC != null) {
                try {
                    webC.disconnect();
                } catch (Exception ex) {
                    ExpClass.LogEX(ex, this.getClass().getName() + ".GetVersion");
                }
            }
        }
    }

    /**
     * The calls to the web services progressively build the status object. Each pass will
     * replace what is currently in the Status object, unless the incoming data is blank/empty.
     * - The incoming Status object must have successfully loaded its data (isDataLoaded==true).
     * - If a value is expected to be a number, we strip off non-numeric data.
     * - There are a few reasonability tests along the way.
     */
    private void CopyStatus(Status other, int type) { CopyStatus(other, "", type); }
    private void CopyStatus(Status other, String link, int type) {
        // Validation
        if(!other.isDataLoaded) return;
        isDataStale = other.isDataStale;
        // Conditions *************************************************************************
        if(other.Temperature.length() > 0){
            // Sometimes a non-reporting pws will show zero or -999 or -9999 when it really means no-data, so check for
            // reasonableness on zero/large negative degree readings. If the last temperature was less than 25, zero is possible.
            double holdTemp = other.getTemperature();
            if(holdTemp < -500.0) {
                Temperature = "";   // if this happens, switch to a backup station.
            }else{
                if(holdTemp != 0){
                    Temperature = other.Temperature.replaceAll(REGEX_NUMERIC, "");
                } else { // temp says zero, so check previous read was close
                    if(getTemperature() < 25) { Temperature = other.Temperature.replaceAll(REGEX_NUMERIC, ""); }
                }
            }
        }
        if(type==CODE_PUBLIC || type==CODE_PUBLIC_DETAIL || type==CODE_PUBLIC_SLIM) {TemperaturePub = other.Temperature;}
        if(other.TempAveMax.length() > 0)       { TempAveMax = other.TempAveMax; }
        if(other.TempAveMin.length() > 0)       { TempAveMin = other.TempAveMin; }
        if(other.TempRecMax.length() > 0)       { TempRecMax = other.TempRecMax; }
        if(other.TempRecMin.length() > 0)       { TempRecMin = other.TempRecMin; }
        if(other.TempRecMaxYY.length() > 0)     { TempRecMaxYY = other.TempRecMaxYY; }
        if(other.TempRecMinYY.length() > 0)     { TempRecMinYY = other.TempRecMinYY; }
        if(other.ObservedTime.length() > 0)     { ObservedTime = ChgTimeStamp822to3339(other.ObservedTime); }
        // Sometimes pws will have a NOAA abbr.condition (which are 3 chars), just ignore 3 char conditions except "fog".
        if(other.Condition.equalsIgnoreCase(DB_CondFog) || other.Condition.length() > 3) {
            Condition = other.Condition;
        }
        if(other.ConditionIcon.length() > 0)    { ConditionIcon = other.ConditionIcon; }
        if(other.Visibility.length() > 0 && !other.Visibility.equalsIgnoreCase(DATA_NA)) {
            Visibility = other.Visibility.replaceAll(REGEX_NUMERIC, "");
        }
        if(other.Humidity.length() > 0 && !other.Humidity.equalsIgnoreCase(DATA_NA)) {
            int holdHumidity; // Humidity should not be zero, so ignore if that is the case.
            try {
                holdHumidity = Integer.parseInt(other.Humidity.replaceAll(REGEX_NUMERIC, ""));
            } catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".CopyStatus"); holdHumidity = 0; }
            if(holdHumidity > 0) { Humidity = other.Humidity.replaceAll(REGEX_NUMERIC, ""); }
        }
        if(other.WindDir.length() > 0)          { WindDir = other.WindDir; }

        /* The wind speed reported by pws always seem to be under-reported. To compensate, if there is a public value available
         * use it to adjust the value of the pws.  The adjustment is based on the public's wind speed and its distance from the
         * pws.  It is likely that the public stations have much more accurate reading (being mostly airports). */
        if(other.WindSpeed.length() > 0 && !other.WindSpeed.equalsIgnoreCase(DATA_NA)) {
            String holdWndSpeed = other.WindSpeed.replaceAll(REGEX_NUMERIC, "");
            if(type == CODE_PRIVATE && getWindSpeed() > 0){
                int distance = getPublicDist() - getPrivateDist();
                if(distance > 0 && distance < 20){ // if the public is too far away, don't bother
                    double adjust = (50-(distance*2.5))*.01;
                    double holdWndSpd = getWindSpeed()*adjust + other.getWindSpeed()*(1-adjust);
                    // Rounding trick: Math.floor(value * X + .5) X = power of 10 (precision where 10=.1, 100=.01, etc.).
                    holdWndSpd = Math.floor((holdWndSpd * TMP_ROUND_WHOLE + 0.5)) / TMP_ROUND_WHOLE;
                    int precisionNbr = 0;
                    //for(int place = TMP_ROUND_WHOLE;  place > 1; place /= 10) { precisionNbr++;  }
                    String outFrmt = "%1." + precisionNbr + "f";
                    holdWndSpeed = String.format(outFrmt, holdWndSpd);
                }
            }
            WindSpeed = holdWndSpeed;
        }
        if(other.WindGusts.length() > 0 && !other.WindGusts.equalsIgnoreCase(DATA_NA)) {
            WindGusts = other.WindGusts.replaceAll(REGEX_NUMERIC, "");
        }
        if(other.Pressure.length() > 0 && !other.Pressure.equalsIgnoreCase(DATA_NA)) {
            Pressure = other.Pressure.replaceAll(REGEX_NUMERIC, "");
        }
        if(other.PressureDelta.length() > 0 && !other.PressureDelta.equalsIgnoreCase(DATA_NA)) {
            PressureDelta = other.PressureDelta;
        }
        if(other.DewPoint.length() > 0 && !other.DewPoint.equalsIgnoreCase(DATA_NA)) {
            double holdDewpoint; // Dewpoint should not be zero, so ignore if that is the case.
            try {
                holdDewpoint = Double.parseDouble(other.DewPoint.replaceAll(REGEX_NUMERIC, ""));
            } catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".CopyStatus(2)"); holdDewpoint = 0; }
            if(holdDewpoint > 0) { DewPoint = other.DewPoint.replaceAll(REGEX_NUMERIC, ""); }
        }
        if(other.Precip24.length() > 0 && !other.Precip24.equalsIgnoreCase(DATA_NA)) {
            Precip24 = other.Precip24.replaceAll(REGEX_NUMERIC, "");
        }
        // Check for reasonable data.
        if(TempAveMax.length() > 0 && TempAveMin.length() > 0 && getTempAveMin() > getTempAveMax()) { TempAveMin = TempAveMax; }
        if(TempRecMax.length() > 0 && TempRecMin.length() > 0 && getTempRecMin() > getTempRecMax()) { TempRecMin = TempRecMax; }
        if(WindSpeed.length() > 0 && getWindSpeed() > getWindGusts()) WindGusts = WindSpeed;
        if(WindSpeed.length() > 0 && getWindSpeed() <= 0 && getWindGusts() > 0)     WindSpeed = WindGusts;
        if(Pressure.length() > 0 && (getPressure() < 800 || getPressure() > 1100))  Pressure = "";
        if(Precip24.length() > 0 && getPrecip24() < 0)                              Precip24 = "";
        if(Visibility.length() > 0 && getVisibility() < 0)                          Visibility = "";
        // Sometimes the Public Station will get stuck on a condition, we can correct this most egregious one. E.g. its not raining w/ low humidity.
        if(Humidity.length() > 0 && getHumidity() != NMBR_NA && getHumidity() < 70 &&
                (ConditionIcon.equalsIgnoreCase(DB_CondRain) || ConditionIcon.equalsIgnoreCase(DB_CondThunder) || ConditionIcon.equalsIgnoreCase(DB_CondSleet))) {
            Condition = "Clear";
            ConditionIcon = DB_CondClear;
        }
        // In some areas there can be localized Fog, which will NOT show up as a condition unless the public station happens to be fogged in.
        // In these Fog micro-climates, looking at the humidity and dew point while conditions are not stormy can tell us if Fog is present.
        // There should also be some wind present (moving the air across the temp. differentials), but wind is hard to track with these stations.
        // Also requires less than 0.10 inches of precipitation in the last 24 hours (wet ground and warm air creates high humidity but not fog).
        // A actual report of Fog may be a number of conditions, so the corollary, knowing when Fog is not present, is not possible.
        if(getHumidity() >= 95 && getDewPoint() >= 45.1 && getDewPoint() < 60.1 && getPrecip24() < 0.10 &&
                (ConditionIcon.equalsIgnoreCase(DB_CondClear) || ConditionIcon.equalsIgnoreCase(DB_CondClearN) ||
                 ConditionIcon.equalsIgnoreCase(DB_CondSun) || ConditionIcon.equalsIgnoreCase(DB_CondCloud) ||
                 ConditionIcon.equalsIgnoreCase(DB_CondMostCloud) || ConditionIcon.equalsIgnoreCase(DB_CondMostSun) ||
                 ConditionIcon.equalsIgnoreCase(DB_CondPartCloud) || ConditionIcon.equalsIgnoreCase(DB_CondPartSun) ||
                 ConditionIcon.equalsIgnoreCase(DB_CondPartCloudN))) {
            Condition = "Fog";
            ConditionIcon = DB_CondFog;
        }


        // Location *************************************************************************
        if(other.City.length() > 0)         { City = other.City; }
        if(other.State.length() > 0)        { State = other.State; }
        if(other.Country.length() > 0)      { Country = other.Country; }
        if(other.Zip.length() > 0)          { Zip = other.Zip; }
        if(other.LocalTime.length() > 0)    { LocalTime = ChgTimeStamp822to3339(other.LocalTime); }
        if(other.TimeZone.length() > 0)     { TimeZone = other.TimeZone; }
        if(other.Elevation.length() > 0)    {
            // Now reported in meters, so lets convert to feet to keep things consistent
            Elevation = other.Elevation.replaceAll(REGEX_NUMERIC, "");
            Double holdFeet = getElevation() * 3.2808399;
            Elevation = holdFeet.toString();
            if(Elevation.indexOf(".") > 0)  // get rid of any decimal value
                Elevation = Elevation.substring(0, Elevation.indexOf("."));
        }
        if(other.Latitude.length() > 0)     { Latitude = other.Latitude.replaceAll(REGEX_NUMERIC, ""); }
        if(other.Longitude.length() > 0)    { Longitude = other.Longitude.replaceAll(REGEX_NUMERIC, ""); }
        if(other.PrivateCode.length() > 0)  { PrivateCode = other.PrivateCode; }
        if(other.PrivateCodeB.length() > 0) { PrivateCodeB = other.PrivateCodeB; }
        if(other.PrivateDist.length() > 0)  { PrivateDist = other.PrivateDist.replaceAll(REGEX_NUMERIC, ""); }
        if(other.PrivateDistB.length() > 0) { PrivateDistB = other.PrivateDistB.replaceAll(REGEX_NUMERIC, ""); }
        if(link.length() > 0 && getPrivateDist() < 0) { // If the private pws is not valid or present, a good backup is the link.
            PrivateCode = link;
            PrivateDist = "0";
        }
        // Sometimes WU does not return the pws: searched for as the closest, fix that here.
        if(isPwsCode(link) && !link.equalsIgnoreCase(PrivateCode)){
            PrivateCodeB = PrivateCode;
            PrivateDistB = PrivateDist;
            PrivateCode = link;
            PrivateDist = "0";
        }
        if(other.PublicCode.length() > 0)   { PublicCode = other.PublicCode; }
        if(other.PublicCntry.length() > 0)   { PublicCntry = other.PublicCntry; }
        if(other.PublicLati.length() > 0 && other.PublicLong.length() > 0){
            // API does not supply the distance to public station, so calculate it.
            Location amHere = new Location(LOC_APP_KEY);
            amHere.setLatitude(getLatitude());
            amHere.setLongitude(getLongitude());
            Location airThere = new Location(LOC_APP_KEY);
            airThere.setLatitude(other.getPublicLati());
            airThere.setLongitude(other.getPublicLong());
            Double miles = amHere.distanceTo(airThere) * 0.00062137119223733;   // meter to mile conversion
            PublicDist = String.valueOf(miles.intValue());
        }

        // Alerts ****************************************************************************
        // This is one instance where the the pubic and private stations can have different data.
        // Importantly, the closer data may be blank, so just assign the data in this instance.
        ALData = other.ALData;
        IsAlert = other.IsAlert;

        // Forecasts *************************************************************************
        if(other.ForecastUpdt.length() > 0) { ForecastUpdt = GetFullDateFromTime(other.ForecastUpdt, true); }
        if(other.FCData.size() > 0)         { FCData = other.FCData; }
        if(other.FCText.size() > 0)         { FCText = other.FCText; }
        if(other.MoonVisible.length() > 0)  { MoonVisible = other.MoonVisible.replaceAll(REGEX_NUMERIC, ""); }
        if(other.MoonAge.length() > 0)      { MoonAge = other.MoonAge.replaceAll(REGEX_NUMERIC, ""); }
        // Sometimes the pws sunrise/sunset times are way off, but public stations seem to always be right.
        // If the times differ by more than N minutes, go with the public station time.
        if(other.Sunrise.length() > 0){
            if(Sunrise.length() == 0){
                Sunrise = GetFullDateFromTime(other.Sunrise, false);
            }else{
                String holdSuntime = GetFullDateFromTime(other.Sunrise, false);
                long msecDiff = 0;
                try {  msecDiff = KTime.CalcDateDifference(Sunrise, holdSuntime, DB_inputDate3339); }
                catch (ExpParseToCalendar e) { /* no big deal */ }
                if(msecDiff > (30 * 60 * 1000)){ // a 30 minute difference is big, something is wrong
                    if(type != CODE_PUBLIC) holdSuntime = ""; // if it is not public, keep what we have
                }
                if(holdSuntime.length() > 0) Sunrise = holdSuntime;
            }
        }
        if(other.Sunset.length() > 0){
            if(Sunset.length() == 0){
                Sunset = GetFullDateFromTime(other.Sunset, false);
            }else{
                String holdSuntime = GetFullDateFromTime(other.Sunset, false);
                long msecDiff = 0;
                try {  msecDiff = KTime.CalcDateDifference(Sunset, holdSuntime, DB_inputDate3339); }
                catch (ExpParseToCalendar e) { /* no big deal */ }
                if(msecDiff > (30 * 60 * 1000)){ // a 30 minute difference is big, something is wrong
                    if(type != CODE_PUBLIC) holdSuntime = ""; // if it is not public, keep what we have
                }
                if(holdSuntime.length() > 0) Sunset = holdSuntime;
            }
        }
        if(Sunrise.length() > 0 && Sunset.length() > 0){ CorrectConditionIcon(other.NightTime); }
        if((Sunrise.length() == 0 || Sunset.length() == 0) && other.NightTime.equalsIgnoreCase("1") ){
            CorrectConditionIconNight(); // Default is day, so only bother if we know it is night.
        }
        isDataLoaded = true;
    }

    // Copy the list of cities locally into the object.
    private void CopyLocations(Map<String, String> others) {
        cityMap = others;
    }

    /* If the exact longitude and latitude are used, for example when adding contacts or some
     * PrePaqs, the WU-API will convert those to something it can use, which is usually not
     * "exactly" what was entered.  Their database probably only holds a limited number of
     * coordinates. But to show the expected address data, we want to save the exact location.
     * Of course, sometimes the contact data is incomplete, so do not copy bad data.*/
    private void GetRealLocation(String coordinates, String type, ContactAddr extra) {
        int comma = coordinates.indexOf(",");
        if((type.equalsIgnoreCase(LOC_TYPE_EXACT)) && comma > 0){
            Latitude = coordinates.substring(0, comma);
            Longitude = coordinates.substring(comma + 1);
        }
        if((type.equalsIgnoreCase(LOC_TYPE_GEOCODE)) && extra != null){
            if(extra.getLatitude().length() > 0) Latitude = extra.getLatitude();
            if(extra.getLongitude().length() > 0) Longitude = extra.getLongitude();
            if(extra.getCity().length() > 0) City = extra.getCity();
            if(extra.getMuni().length() > 0) State = extra.getMuni();
            if(extra.getPostal().length() > 0) Zip = extra.getPostal();
            if(extra.getContry().length() > 0) Country = extra.getContry();
        }
    }

    /* The WU API gives out the date in a RFC822 style format, but we want to use a RFC3339. That is
     * an easier style for the date helpers. */
    private String ChgTimeStamp822to3339(String rfc822ts){
        try {
            return (String) KTime.ParseToFormat(rfc822ts, DB_inputDate822, DATA_NA, DB_fmtDate3339k);
        } catch (ExpParseToCalendar ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".ChgTimeStamp822to3339"); /* We'll just used the default. */
            return (String) KTime.ParseNow(DB_fmtDate3339k, DATA_NA);
        }
    }

    /* The forecast update time is given as a human readable time, which means we need to
     * convert it into something the computer can use.  This assumes today's date, and the
     * existing time zone for the Status object, then adds in the time. This can handle
     * both a 24 hour clock or time with an AM or PM specification.
     * NOTE:  One problem with this is that a time of 9pm could get the wrong date if this
     * method is used 4 hours later.  If we know a date is in the past we can fix this by
     * subtracting a day when a result ends up being in the future. */
    private String GetFullDateFromTime(String tm, boolean inThePast) {
    try {
        int holdhour;
        int holdmin;
        tm = tm.toUpperCase(Locale.US);
        Calendar holdDate = Calendar.getInstance(java.util.TimeZone.getTimeZone(TimeZone));
        boolean pm = tm.contains("PM");
        boolean am = tm.contains("AM");
        String holdtime = tm.replaceAll(REGEX_TIME, "");
        int delimit = holdtime.indexOf(":");
        // if no delimiter, time is invalid, just return current time
        if(delimit > -1){
            holdhour = Integer.parseInt(holdtime.substring(0, delimit));
            if(pm && holdhour < 12) holdhour += 12;
            if(am && holdhour == 12) holdhour = 0; // midnight is zero in 24 hour clock
            holdmin =  Integer.parseInt(holdtime.substring(delimit+1));
            holdDate.set(Calendar.HOUR_OF_DAY, holdhour);
            holdDate.set(Calendar.MINUTE, holdmin);
        }
        // Fix future dates back to past if applicable.
        Calendar now = Calendar.getInstance(java.util.TimeZone.getTimeZone(TimeZone));
        if(inThePast && now.compareTo(holdDate) == -1)
            holdDate.add(Calendar.DAY_OF_MONTH, -1);

        return (String) DateFormat.format(DB_fmtDate3339k, holdDate);

    } catch (Exception ex) {
        ExpClass.LogEX(ex, this.getClass().getName() + ".GetFullDateFromTime");
        return (String) KTime.ParseNow(DB_fmtDate3339k, TimeZone);
    }
    }

    /* The icons for the Condition can change based on available data.  This first case of this
     * is when icons with the sun are presented, but it is before/after the sun has risen/set.
     * In this case, we want to use non-sun based icons.
     * ALSO: As long as we are calculating day/night, return the result as true = day.
     * The "Slim" API call does not return any time data, but will update the NightTime flag, so let
     * that override the stored time values.*/
    private int CorrectConditionIcon(String isNight){
    try {

        Date current = new Date();
        SimpleDateFormat dateFormatTime = new SimpleDateFormat(DB_fmtDate3339s, Locale.US);
        Date sunDn = dateFormatTime.parse(Sunset);
        Date sunUp =  dateFormatTime.parse(Sunrise);


        if(ConditionIcon.equalsIgnoreCase(DB_CondClear) || ConditionIcon.equalsIgnoreCase(DB_CondSun) ||
           ConditionIcon.equalsIgnoreCase(DB_CondMostCloud) || ConditionIcon.equalsIgnoreCase(DB_CondMostSun) ||
           ConditionIcon.equalsIgnoreCase(DB_CondPartCloud) || ConditionIcon.equalsIgnoreCase(DB_CondPartSun)){
            if(isNight.equalsIgnoreCase(DATA_NA) && (current.after(sunDn) || current.before(sunUp))){
                if(ConditionIcon.equalsIgnoreCase(DB_CondClear) || ConditionIcon.equalsIgnoreCase(DB_CondSun)){
                    ConditionIcon = DB_CondClearN;
                }
                if(ConditionIcon.equalsIgnoreCase(DB_CondMostCloud) || ConditionIcon.equalsIgnoreCase(DB_CondMostSun) ||
                   ConditionIcon.equalsIgnoreCase(DB_CondPartCloud) || ConditionIcon.equalsIgnoreCase(DB_CondPartSun)){
                    ConditionIcon = DB_CondPartCloudN;
                }
            }
            if(isNight.equalsIgnoreCase("1")){  CorrectConditionIconNight(); }
        }
        int daytime = ClimateDB.SQLITE_TRUE;
        if(current.after(sunDn) || current.before(sunUp)){ // Night Time
            daytime = ClimateDB.SQLITE_FALSE;
        }
        return daytime;

    } catch (Exception ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".CorrectConditionIcon"); return ClimateDB.SQLITE_TRUE; }
    }

    private void CorrectConditionIconNight(){
        if(ConditionIcon.equalsIgnoreCase(DB_CondClear) || ConditionIcon.equalsIgnoreCase(DB_CondSun)){
            ConditionIcon = DB_CondClearN;
        }
        if(ConditionIcon.equalsIgnoreCase(DB_CondMostCloud) || ConditionIcon.equalsIgnoreCase(DB_CondMostSun) ||
           ConditionIcon.equalsIgnoreCase(DB_CondPartCloud) || ConditionIcon.equalsIgnoreCase(DB_CondPartSun)){
            ConditionIcon = DB_CondPartCloudN;
        }
    }

    // This is a simple way of getting the output of a stream into a string, assuming the stream ends with a \A.
    private String inputStreamToString(InputStream stream, String encoding) {
        if(encoding.length() == 0)
            encoding = "UTF-8";
        Scanner scanner = new Scanner(stream, encoding);
        scanner.useDelimiter("\\A");
        String holdStream = "";
        if (scanner.hasNext()) holdStream = scanner.next();
        scanner.close();
        return holdStream;
    }

    // Confirm the string conforms to the personal weather station standard.
    private boolean isPwsCode(String src) {
    try{
        return (src.length() != 0 && src.trim().toLowerCase(Locale.US).substring(0, 4).equalsIgnoreCase(PWS_CODEKEY));
    } catch (Exception ex) { /* nothing really to do here. */ return false; }
    }

    // Initializes the Status object with some default data. Helpful upon first use if not data is found in the local db.
    void SetDefault(){
        // Location
        PublicCode = "NZSP";
        PrivateCode = "zmw:00000.1.89009";
        PublicDist = "0";
        PrivateDist = "0";
        City = "Amundsen-Scott";
        Country = "AA";
        State = "AA";
        Zip = "00000";
        LocalTime = "2012-05-07T08:10:08+1200";
        TimeZone = "Pacific/Auckland";
        Latitude = "90.0";
        Longitude = "0.0";
        // Condition
        Temperature = "0"; 
        TempAveMax = "0";
        TempAveMin = "0";
        TempRecMax = "0";
        TempRecMin = "0";
        TempRecMaxYY = "0";
        TempRecMinYY = "0";
        ObservedTime = "2012-05-07T08:10:08+1200";
        Condition = "Clear";   
        ConditionIcon = "clear";
        Visibility = "0";  
        Humidity = "0";    
        WindSpeed = "0";   
        WindGusts = "0";   
        Pressure = String.valueOf(XTR_STD_PRESSURE);    
        DewPoint = "0";    
        Precip24 = "0";    
    }
    
    
}

















