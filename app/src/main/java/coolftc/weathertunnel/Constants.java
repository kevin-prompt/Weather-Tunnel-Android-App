package coolftc.weathertunnel;

import android.app.AlarmManager;
import android.app.SearchManager;
import android.provider.BaseColumns;

public interface Constants {

    /* The Time Formats. The WU API supplies an RFC822 style date. We prefer a RFC3339 style.
     * In general, because most of the time I have a long form time zone supplied by the API,
     * I try to use that when dealing with the time.  Sometimes the time zone attached to the 
     * formatted date string will be used. The 8601 is an ISO format.  Most of the output is
     * done by the android.DateFormat, which is a slightly different template than the old ctime
     * used by other formatters like SimpleDateFormat.  */
    // Input formats
    String DB_inputDate822 = "EEE, dd MMM yyyy HH:mm:ss Z";
    String DB_inputDate3339 = "yyyy-MM-ddTHH:mm:ssZ";
    String DB_fmtDate822k = "EEE, dd MMM yyyy kk:mm:ss z";  // used by android.DateFormat
    String DB_fmtDate3339k = "yyyy-MM-ddTkk:mm:ssz";        // used by android.DateFormat
    String DB_fmtDate3339s = "yyyy-MM-dd'T'HH:mm:ssZ";      // used by SimpleDateFormat
    String DB_fmtDate8601k = "yyyy-MM-dd kk:mm:ss.SSSz";    // used by android.DateFormat
    String DB_fmtDate8601 = "yyyy-MM-dd HH:mm:ss.SSSZ";
    String DB_fmtDateEUAlert = "yyyy-MM-dd HH:mm:ss Z";     // format used by Alerts from Europe
    /*
     * The various display date format templates.  The time has two variations, 24 hour clock and AM/PM.
     * The dates have three major variations based on cultural ordering of elements, and some additional
     * play based on separators and how Months are represented.
     */
    String DB_fmtDateShrtMiddle = "MM/dd/yyyy";             // on wikipedia they call these 3 date formats,
    String DB_fmtDateShrtLittle = "dd/MM/yyyy";             // the ones are most commonly used across countries, 
    String DB_fmtDateShrtBig = "yyyy/MM/dd";                // big/little/middle Endian
    String DB_fmtDateMonthMiddle = "MMM dd, yyyy";          // Middle = m/d/y used in USA
    String DB_fmtDateMonthLittle = "dd MMM, yyyy";          // Little = d/m/y used in India, Russia, South America
    String DB_fmtDateMonthBig = "yyyy, MMM dd";             // Big = y/m/d used in China
    String DB_fmtLongMonthMiddle = "MMMM dd, yyyy";         // Full Month Name with MMMM
    String DB_fmtLongMonthLittle = "dd MMMM, yyyy";         // 
    String DB_fmtLongMonthBig = "yyyy, MMMM dd";            // 
    String DB_fmtDateNoYearBigMid = "MMM dd";               // Without the year, big & middle the same
    String DB_fmtDateNoYearLittle = "dd MMM";               // 
    String DB_fmtDateTime = "h:mmaa";
    String DB_fmtDateTime24 = "kk:mm  ";                    
    String DB_fmtDateTimeZone = "h:mmaa z";
    String DB_fmtDayOfWeek = "EEEE";
    String TITLE_TODAY = "Today";
    String TITLE_TONIGHT = "Tonight";
    String TITLE_NEXTDAY = "Tomorrow";
    String TODAY_TONIGHT = "today tonight";
    String BAD_TIME = "99:99";
    // Date Formatting Constants
    int DATE_FMT_SHORT = 0;             // Short Date Display format
    int DATE_FMT_OBSERVED = 1;          // Observed Date Display format
    int DATE_FMT_ALERT_CURR = 2;        // Alert Date Display on Weather Current
    int DATE_FMT_ALERT_EXP = 3;         // Alert Date Display on Alert
    // This regex allows for checking if any day name is in a string (case ignored)
    String REGEX_DAYNAMES = ".*((?i)today|(?i)tonight|(?i)monday|(?i)tuesday|(?i)wednesday|(?i)thursday|(?i)friday|(?i)saturday|(?i)sunday).*";
    
    /* Mapping constants used with the forecast list */
    String FC_LOC_ID = "location";
    String FC_CONDICON = "icon";
    String FC_COND = "condition";
    String FC_TITLE = "title";
    String FC_DESC = "description";
    String FC_TEMP_HIGH = "high";
    String FC_TEMP_LOW = "low";
    String FC_WIND = "wind";
    String FC_RAIN = "rain";
    String FC_RAIN_LBL = "rainorsnow";
    String FC_RAIN_TXT = "Rain - ";
    String FC_SNOW_TXT = "Snow - ";
    
    /* The condition values returned by the API */
    String DB_CondClear = "clear";
    String DB_CondClearN = "clearnight";
    String DB_CondSun = "sunny";
    String DB_CondCloud = "cloudy";
    String DB_CondMostCloud = "mostlycloudy";
    String DB_CondMostSun = "mostlysunny";
    String DB_CondPartCloud = "partlycloudy";
    String DB_CondPartSun = "partlysunny";
    String DB_CondPartCloudN = "partlycloudynight";
    String DB_CondFog = "fog";
    String DB_CondHazy = "hazy";
    String DB_CondRain = "rain";
    String DB_CondRainP = "chancerain";
    String DB_CondSleet = "sleet";
    String DB_CondSleetP = "chancesleet";
    String DB_CondThunder = "tstorms";
    String DB_CondThunderP = "chancetstorms";
    String DB_CondSnow = "snow";
    String DB_CondSnowP = "chancesnow";
    String DB_CondFlurry = "flurries";
    String DB_CondFlurryP = "chanceflurries";
    String DB_CondUnknown = "unknown";
    
    /* The possible Moon Phase values */
    String DB_MoonNew = "New Moon";
    String DB_MoonWaxCrescent = "Waxing Crescent";
    String DB_MoonFirstQtr = "First Qtr.";
    String DB_MoonWaxGibbous = "Waxing Gibbous";
    String DB_MoonFull = "Full Moon";
    String DB_MoonWanGibbous = "Waning Gibbous";
    String DB_MoonLastQtr = "Last Qtr.";
    String DB_MoonWanCrescent = "Waning Crescent";
    
    /* Descriptive Climate phrases. */
    String DEW_DRY = "Dry";
    String DEW_NORMAL = "Normal";
    String DEW_MUGGY = "Muggy";
    String DEW_STICKY = "Sticky";
    String DEW_OPPRESIVE = "Oppresive";
    String WIND_LABEL_FORE = "Expectation - ";
    String WIND_NONE = "No Wind";
    String WIND_CALM = "Calm";
    String WIND_BREEZY = "Breezy";
    String WIND_WINDY = "Windy";
    String WIND_BLUSTERY = "Blustery";
    String WIND_GUSTY = "Gusty";
    String WIND_GALE = "Gale Winds";
    String WIND_STORM = "Storm Winds";
    String WIND_DESTROY = "Destructive";
    String SEASON_WINTER = "Winter";
    String SEASON_SPRING = "Spring";
    String SEASON_SUMMER = "Summer";
    String SEASON_AUTUMN = "Autumn";
    String TEMP_AVE_NORM = "Average";
    String TEMP_AVE_HIGH = "Above";
    String TEMP_AVE_LOW = "Below";
    String PRESSURE_STEADY = "Press. Steady";
    String PRESSURE_DOWN = "Press. Falling";
    String PRESSURE_RISE = "Press. Rising";
    String COND_TSTORM_LONG = "Thunderstorms and Rain";
    String COND_TSTORM_SHRT = "Thunder Storms";
    
    /* Constants used as codes for cross Activity communications. */
    String IN_CITIES = "ChooseCity";
    String IN_CITY_TYPE = "CityType";
    String IN_CITY = "ChoosenCity";
    String IN_LINK = "ChoosenLink";
    String IN_CITY_ID = "coolftc.android.weather.tunnel.CityItem";
    String IN_AM_WIDGET = "iAmWidget";
    String IN_FORCE_UPDT = "ForceUpdate";
    String IN_ALERT_ID = "ShowAlert";
    String IN_ALERT_LOC = "Location";
    String IN_ALERT_SPC = "Special";
    String IN_ALERT_TZ = "Timezone";
    String IN_ALERT_FLG = "AlertFlag";
    String IN_FORED_TITLE = "Title";
    String IN_FORED_DESC = "Detail";
    String IN_WELCOME_BACK = "WelcomeBack";
    String TAB_ID_CURRENT = "CurrentTab";
    String TAB_ID_FORECAST = "ForecastTab";
    String TAB_ID_SHOWING = "TabNumber";
    String DELIMIT_SPLIT = "?";
    int KY_CITIES = 10001;
    int KY_CONTACT = 10002;
    int KY_WELCOME = 10003;
    int DIALOG_PICK_SUB_CONTACTS = 11001;
    int SEC_READ_LOCATION = 10004;
    int SEC_READ_CONTACTS = 10005;
    String KEVIN_SPEAKS = "Kevin Speaks";
    
    /* Constants used by the Location services. */
    String LOC_TYPE_GENERAL = "gen";            // General = random data
    String LOC_TYPE_EXACT = "ext";              // Exact = coordinate pair
    String LOC_TYPE_GEOCODE = "geo";            // Geocode = address lookup
    String LOC_TYPE_PREPAQ = "paq";             // Prepaq = prepaq group name 
    String LOC_CURRENT_ID = "1";
    String LOC_APP_KEY = "TUNNEL";
    float LOC_MIN_DIST = 2000;                  // Meters from last location required before change location
    String LOC_SPECIAL = "(Current Location)";  // Label placed on the current location temperature
    String LOC_SPECIAL_UPDT = "(*Current Location)"; // Label placed on current location when it is out of date
    int UPD_SCREEN_TM = 30000;                  // How often in msec to update the screen from data base
    int UPD_SCREEN_TQ = 10000;                  // How often in msec to update the screen from data base (first minute)
    long UPD_DBASE_TM = AlarmManager.INTERVAL_FIFTEEN_MINUTES; // How often in msec the AlarmManager will call the PulseStatus to update DB.
    String REGEX_NUMERIC = "[^0-9,.,-]";        // Regular expression to use with replace to get only a number
    String REGEX_TIME = "[^0-9,:]";             // Regular expression to use with replace to get time only
    String DATA_NA = "-9999";                   // API response data that is invalid can appear as this string.  Missing data is just empty string.
    String DATA_NA_ALT = "-999.0";              // Undocumented API response data of invalid data.  Probably for floating point numbers, e.g. Temperature.
    int NMBR_NA = -9999;                        // If no data or invalid data is found in the API response, it will appear as this in numeric form.
    String DISP_NA = "Not Available";           // Use this when displaying information that is none available.
    
    /* Random weather information constants */
    enum MEASURE_DIST {INCHES, FEET, MILES}
    int UPD_STATIONS_ALL = 1;
    int UPD_STATIONS_CLOSEST = 2;
    int UPD_STATIONS_ALL_SL = 3;
    int UPD_STATIONS_CLOSEST_SL = 4;
    int UPD_STATIONS_FORCE = 5;
    double XTR_STD_PRESSURE = 1013.25;
    double XTR_STD_PRESSURE_IN = 29.92;
    int TMP_ROUND_WHOLE = 1;
    int TMP_ROUND_TENTH = 10;
    int TMP_ROUND_HUNDRED = 100;
    int PRESS_DELTA_LIMIT = 2;
    String PWS_CODEKEY = "pws:";
    String[] CONTACT_TYPES = {"Custom", "Home", "Work", "Other" };
    
    /* Constants used in dealing with the SQLite Database and Shared Preference Database. */ 
    long DB_PULSE_DEBOUNCE = 40000;     // msec
    long DB_APICACHE_STALE = 60000;     // msec
    long DB_MAX_ONSLATE = 24;
    String SP_WIDGET_MAP = "widgetMap";
    String SP_EULA_OK = "eulastatus";
    String SP_EULA_VER = "eulaversion";
    int SP_EULA_CURRENT = 1;
    String SP_REG_OK = "registration";
    String SP_REG_KEY = "registeredkey";
    String APICACHE_TYPE_PUB = "public";
    String APICACHE_TYPE_PRI = "private";
    String DB_StatusAll = "select * from status";
    String DB_StatusLastUser = "select usersort from status order by usersort desc";
    String DB_APICACHEHIT = "select *  from apicache where apitype like 'ZZZ'";
    String DB_StatusSlate = "select * from status where onslate = 1 order by usersort";
    String DB_StatusForWidget = "select * from status where onslate = 1 and onwidget = 0 order by usersort";
    String DB_StatusAnyWidget = "select * from status where onwidget = 1";
    String DB_StatusOrphans = "select * from status where onslate = 0 and onwidget = 0 and " + BaseColumns._ID + " > 1";
    String DB_StatusID = "select * from status where _ID = ";
    String DB_SuggestID = "select * from suggest where _ID = ";
    String DB_UserSortByID = "select " + BaseColumns._ID +" from status where usersort = ZZZ";
    // The DB_SuggestLike is built to return the data just as the Android Search dialog wants it.
    // The first two values help build the selection list, while the third is sent with the intent
    // if an item on the select list is chosen.  It can then be used to query for further info.
    // The ZZZ is replaced in code with what ever the user has typed in.
    String DB_SuggestLike = 
        "select " + BaseColumns._ID + ", " + 
                    SearchManager.SUGGEST_COLUMN_TEXT_1 + ", " + 
                    BaseColumns._ID + " as " + SearchManager.SUGGEST_COLUMN_INTENT_DATA + 
        " from suggest where " + SearchManager.SUGGEST_COLUMN_TEXT_1 + " like 'ZZZ%' or " + SearchManager.SUGGEST_COLUMN_TEXT_1 + " like '% ZZZ%'";

    /* Constants used in dealing with the WU API and other web sites
     * Home 192.168.69.80 */
    String WTI_Prepaq = "http://wtunnelapi.cloudapp.net/Climate.svc/rest/v1/location/prepaq/";
    String WUI_Search = "http://autocomplete.wunderground.com/aq?query=";
    String WUI_SearchLink = "http://wtunnelapi.cloudapp.net/Climate.svc/rest/v1/location/search/";
    String WUI_Station = "http://wtunnelapi.cloudapp.net/Climate.svc/rest/v1/weather/";
    String WUI_StationSL = "http://wtunnelapi.cloudapp.net/Climate.svc/rest/v1/weather/slim/";
    String WUI_StationDT = "http://wtunnelapi.cloudapp.net/Climate.svc/rest/v1/weather/detail/";
    String WUI_StationFR = "http://wtunnelapi.cloudapp.net/Climate.svc/rest/v1/weather/force/";
    String WTI_Register = "http://wtunnelapi.cloudapp.net/Climate.svc/rest/v1/weather/register/";
    String WUI_SearchFmtXml = "&format=xml";
    String WUI_GeneralFmtXml = ".xml";
    String WTI_TicketVer = "AndroidWTv";
    String GMI_Lookup = "http://maps.googleapis.com/maps/api/geocode/xml?sensor=true&address=";
    String WEB_EULA = "http://www.coolftc.org/wtunnel/wt_eula.html";
    String WEB_HELP = "http://www.coolftc.org/wtunnel/wt_app.html";
    String WU_EULA = "http://www.wunderground.com/weather/api/d/terms.html";
    Boolean ADS_HIDDEN = true;  // set to true to hide ads, e.g. for screen shots
    int FTI_TIMEOUT = 30000;


    /* Database initialization values */
    // Populate the Slate
    String[] LOC_HongKong = new String[] {"VHHH", "pws:IROYALOB10", "pws:IROYALOB10", "18", "1", "1", "00000",
        "Causeway Bay", "HK", "CH", "22.28000069", "114.18000031", "Asia/Shanghai", "2017-01-08T12:09:03+0800" };
    String[] LOC_SanFrancico = new String[] {"KSFO", "pws:KCASANFR506", "pws:KCASANFR375", "11", "0", "1", "94118",
        "San Francisco", "CA", "US", "37.77999878", "-122.45999908", "America/Los_Angeles", "2017-01-08T12:09:03-0700" };
    String[] LOC_MexicoCity = new String[] { "MMMX", "pws:ICIUDADD45", "pws:ICIUDADD150", "3", "0", "1", "00000",
        "Ciudad De México", "", "MX", "19.43000031", "-99.13999939", "America/Mexico_City", "2017-01-08T12:09:03-0600" };
    String[] LOC_Portland = new String[] { "KPDX", "pws:KORPORTL396", "pws:KORPORTL289", "12", "0", "0", "97201",
        "Portland", "OR", "US", "45.50999832", "-122.69000244", "America/Los_Angeles", "2017-01-08T12:09:03-0700" };
    
    /* Prepaqs have a display name seen by user, and a more concise database name used to categorize the destinations.  */
    String[] PQ_BALLPARKS = new String[] { "Ballparks - North America Major Leauge", "testingPrePaq" };
    String[] PQ_AIRPORTS = new String[] { "Airports - North America A - M", "testingPrePaq" };

}