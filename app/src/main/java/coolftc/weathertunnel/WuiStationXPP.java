package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.text.TextUtils;

class WuiStationXPP {
    /**********************************************************************
     * Every technology starts out simple, and either dies or becomes popular.  When it
     * becomes popular, it starts to add features to meet the demands of a complex world.
     * As it becomes ever more complex, newer simple technologies are introduced, and 
     * they either die out or become popular... So we have moved from DOM to SAX to "Pull".
     *  
     * This is a xml pull processor (XPP) handler class.  It reads over an xml file and as 
     * every TAG is opened, the startElement() fires.  Every time a TAG is closed, the 
     * endElement() fires. When these start/end methods fire, they flip the true/false switch
     * on that an element in the TAG map. Between the start/end methods getting fired, the 
     * characters() method is fired. The characters() method allows for the collection of 
     * data between the TAGs. Before and after the start/end methods, the startDocument()
     * and endDocument() methods fire.  
     *  
     * The tag map can be used to figure out where in the xml document the data is located, 
     * and allow it to be properly processed. The processing occurs whenever a closing TAG
     * is found.  Using conditional statement based on the TAG map and temporary data holds,
     * the expected data can generally be extracted.
     * 
     * For example, when the data "JOE" from the xml <EMPL><NAME>JOE</NAME></EMPL> is stored
     * in the "cuts" variable, both the EMPL and NAME keys of the TAG map will be true. Just 
     * wait for that condition and then grab the cut characters.
     *   
     * Keep in mind:  This does not support embedded TAGs of the same name.  It can also only
     * deal with data directly bounded by a tag, as all the data is cleared after each end
     * tag is processed.
     *   
     * It is easier to write the code if you initialize all the tags you care about, 
     * so you do not have to code around a possible null value condition in the map.
     * 
     */
    // Variables used by the parser.
    private XmlPullParser xpp;
    private Map<String, Boolean> tag = new HashMap<>(100);
    private StringBuilder cuts = new StringBuilder(100);    // better than string for concatenation.
    
    // This class processes the detailed weather data.  It looks like they combine some of
    // the public data into the private stations (which we had to do locally before). The
    // elevation comes in two place, so not sure which is correct, but will go with "display_location".
    private Status data;
    private ForecastSpkn fcSpkn;
    private ForecastData fcData;
    private AlertData alData;
    
    // Constructor - create a XPP
    WuiStationXPP() throws XmlPullParserException {
        XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
        xppf.setNamespaceAware(true);
        xpp = xppf.newPullParser();
    }
    
    /*********************************************************************
     * Public Methods
     */
  
    // Assign the input data.
    void setInput(Reader data) throws XmlPullParserException {
        xpp.setInput(data);
        
        // If there is no DTD, but Entity data (e.g. &deg;) is included, this simple
        // substitution method will keep the xml parser from failing. It needs to 
        // happen after setInput() and will throw an exception if the DTD is present.
        try {
            xpp.defineEntityReplacementText("deg", "°");
        } catch (XmlPullParserException ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".setInput"); }
        ExpClass.LogIN(KEVIN_SPEAKS, "The WuiStationXPP has been called.");
    }
    
    // Process the xml.
    void parse() throws XmlPullParserException, IOException {
        int eventType = xpp.getEventType();
        
        while (eventType != XmlPullParser.END_DOCUMENT) {
            
            if(eventType == XmlPullParser.START_DOCUMENT) {
                startDocument();
            }
            else if(eventType == XmlPullParser.START_TAG) {
                startElement(xpp.getName());
            }
            else if(eventType == XmlPullParser.TEXT) {
                characters(xpp.getText());
            }
            else if(eventType == XmlPullParser.END_TAG) {
                endElement(xpp.getName());
            }
            eventType = xpp.next();        
        }
        endDocument();
    }

    // Return the search data.
    Status getStatus() {
        return data;   
    }
    
    
    /******************************************************************
     * TAG handling logic.
     */
    
    // Called when the xml document is first opened
    private void startDocument() {
        data = new Status();
        fcSpkn = new ForecastSpkn();
        fcData = new ForecastData();
        alData = new AlertData();
        initializeTAGs();
    }
    
    // Clear out the map and initialize the tags we care about.
    private void initializeTAGs() {
        tag.clear();
        
        // Valid Result
        tag.put("response", false);
        
        // Registration
        tag.put("wt_id", false);

        // High Level Features
        tag.put("current_observation", false);
        tag.put("forecast", false);
        tag.put("moon_phase", false);
        tag.put("almanac", false);
        tag.put("alerts", false);
        tag.put("sl", false);
        
        // Shared
        tag.put("forecastday", false);
        tag.put("icon", false);
        tag.put("date", false);
       
        // Conditions
        tag.put("temp_f", false);
        tag.put("relative_humidity", false);
        tag.put("wind_dir", false);
        tag.put("wind_mph", false);
        tag.put("wind_gust_mph", false);
        tag.put("pressure_mb", false);
        tag.put("pressure_trend", false);
        tag.put("dewpoint_f", false);
        tag.put("weather", false);
        tag.put("observation_time_rfc822", false);
        tag.put("elevation", false);
        tag.put("local_time_rfc822", false);
        tag.put("visibility_mi", false);
        tag.put("precip_today_in", false);
        tag.put("display_location", false);
        
        // Spoken Forecast Info
        tag.put("txt_forecast", false);
        tag.put("title", false);
        tag.put("fcttext", false);
        tag.put("fcttext_metric", false);
        
        // Numbers Forecast Info
        tag.put("simpleforecast", false);
        tag.put("epoch", false);
        tag.put("tz_long", false);
        tag.put("high", false);
        tag.put("low", false);
        tag.put("fahrenheit", false);
        tag.put("pop", false);
        tag.put("conditions", false);
        tag.put("avewind", false);
        tag.put("maxwind", false);
        tag.put("mph", false);
        tag.put("dir", false);
        tag.put("avehumidity", false);
        tag.put("qpf_allday", false);   // quantitative precipitation forecast
        tag.put("snow_allday", false);
        
        // Celestial Info
        tag.put("sunset", false);
        tag.put("sunrise", false);
        tag.put("percentIlluminated", false);
        tag.put("ageOfMoon", false);
        tag.put("hour", false);
        tag.put("minute", false);
        
        // Alerts
        tag.put("alert", false);
        tag.put("description", false);
        tag.put("date_epoch", false);
        tag.put("expires", false);
        tag.put("expires_epoch", false);
        tag.put("message", false);
        tag.put("wtype_meteoalarm_name", false);
        
        // Almanac Info
        tag.put("temp_high", false);
        tag.put("temp_low", false);
        tag.put("normal", false);
        tag.put("record", false);
        tag.put("recordyear", false);
        tag.put("F", false);
    }

    private void startElement(String tagName) {
        tag.put(tagName, true);
    }
    
    private void characters(String text) {
        if(text != null) cuts.append(text);
    }
    
    private void endElement(String tagName) {
        processChars(tagName);
        cuts.delete(0, cuts.length());
        tag.put(tagName, false);
    }
    
    private void endDocument() {    }

    // The map can be used to determine where in the xml hierarchy you are,
    // and allow you to process it accordingly. 
    private void processChars(String endTag) {
        String chars = cuts.toString().trim();
        
        // Mark as valid
        if(tag.get("response")){
            data.isDataLoaded = true;
        }
        
        // Get the Registration id
        if(tag.get("wt_id")){ data.registration = chars; }
        
        // Special Slim Response
        // Only add new parameter data to the end of the string for backward compatibility.
        // Example: <sl>54?Mostly Cloudy?mostlycloudy?0?1?0?America/Los_Angeles?69?55</sl>
        if(tag.get("sl")){
            TextUtils.SimpleStringSplitter values = new TextUtils.SimpleStringSplitter('?');
            values.setString(chars);
            if(values.hasNext()) { data.Temperature = values.next();}
            if(values.hasNext()) { data.Condition = values.next(); }
            if(values.hasNext()) { data.ConditionIcon = values.next(); }
            if(values.hasNext()) { data.NightTime = values.next(); }
            if(values.hasNext()) { data.isDataStale = values.next().equalsIgnoreCase("1"); }
            if(values.hasNext()) { data.IsAlert = values.next().equalsIgnoreCase("1") ? ClimateDB.SQLITE_TRUE : ClimateDB.SQLITE_FALSE; }
            // The cached value for time is old, so lets just use the phone's time and adjust for time zone.
            if(values.hasNext()) {
                data.LocalTime = (String) KTime.ParseNow(DB_fmtDate822k, values.next());
            }
            if(values.hasNext()) { data.Humidity =  values.next(); }
            if(values.hasNext()) { data.DewPoint =  values.next(); }
            if(values.hasNext()) { data.Precip24 =  values.next(); }
        }

        // Conditions
        if(tag.get("current_observation")){
            if(tag.get("temp_f")){ data.Temperature = chars; }
            if(tag.get("relative_humidity")){ data.Humidity = chars; }
            if(tag.get("wind_dir")){ data.WindDir = chars; }
            if(tag.get("wind_mph")){ data.WindSpeed = chars; }
            if(tag.get("wind_gust_mph")){ data.WindGusts = chars; }
            if(tag.get("pressure_mb")){ data.Pressure = chars; }
            if(tag.get("pressure_trend")){ data.PressureDelta = chars; }
            if(tag.get("dewpoint_f")){ data.DewPoint = chars; }
            if(tag.get("weather")){ data.Condition = chars; }
            if(tag.get("observation_time_rfc822")){ data.ObservedTime = chars; }
            if(tag.get("display_location") && tag.get("elevation")){ data.Elevation = chars; }  // reported in meters here
            if(tag.get("local_time_rfc822")){ data.LocalTime = chars; }
            if(tag.get("visibility_mi")){ data.Visibility = chars; }
            if(tag.get("icon")){ data.ConditionIcon = chars; }
            if(tag.get("precip_today_in")){ data.Precip24 = chars; }
        }
        
        if(tag.get("forecast")){
            // Spoken Forecast Info
            if(tag.get("txt_forecast")) {
                if(tag.get("date")){ data.ForecastUpdt = chars; }
                if(tag.get("forecastday")) {
                    if(tag.get("icon")){ fcSpkn.setCondition(chars); }
                    if(tag.get("title")){ fcSpkn.Title = chars; }
                    if(tag.get("fcttext")){ fcSpkn.Description = chars; }
                    if(tag.get("fcttext_metric")){ fcSpkn.DescriptionAlt = chars; }
                    if(endTag.equalsIgnoreCase("forecastday")){ data.FCText.add(new ForecastSpkn(fcSpkn)); fcSpkn.clear(); }
                }
            }
            
            // Numbers Forecast Info
            if(tag.get("simpleforecast") && tag.get("forecastday")) {
                if(tag.get("epoch")){ fcData.Epoch = chars; }
                if(tag.get("tz_long")){ fcData.Timezone = chars; }
                if(tag.get("high") && tag.get("fahrenheit")){ fcData.HighTemperature = chars; }
                if(tag.get("low") && tag.get("fahrenheit")){ fcData.LowTemperature = chars; }
                if(tag.get("conditions")){ fcData.Condition = chars; }
                if(tag.get("icon")){ fcData.setConditionIcon(chars); }
                if(tag.get("pop")){ fcData.PercipitationChance = chars; }
                if(tag.get("avehumidity")){ fcData.AverageHumidity = chars; }
                if(tag.get("avewind") && tag.get("mph")){ fcData.AverageWind = chars; }
                if(tag.get("avewind") && tag.get("dir")){ fcData.AverageWindDir = chars; }
                if(tag.get("maxwind") && tag.get("mph")){ fcData.MaximumWind = chars; }
                if(tag.get("qpf_allday") && tag.get("in")){ fcData.PercipitationAmount = chars; }
                if(tag.get("snow_allday") && tag.get("in")){ fcData.SnowAmount = chars; }
                if(endTag.equalsIgnoreCase("forecastday")){ 
                    fcData.setTimeStampEpoch();
                    data.FCData.add(new ForecastData(fcData)); fcData.clear(); 
                }
            }
            
        }
        
        // Celestial Info
        if(tag.get("moon_phase")) {
            if(tag.get("sunrise") && tag.get("hour")){ data.Sunrise = chars; }
            if(tag.get("sunrise") && tag.get("minute")){ data.Sunrise += ":" + chars; }
            if(tag.get("sunset") && tag.get("hour")){ data.Sunset = chars; }
            if(tag.get("sunset") && tag.get("minute")){ data.Sunset += ":" + chars; }
            if(tag.get("percentIlluminated")) { data.MoonVisible = chars; }
            if(tag.get("ageOfMoon")) { data.MoonAge = chars; }
            data.NightTime = DATA_NA; // since we have sunrise & sunset, NightTime is not valid.
        }
        
        // Alerts Info
        if(tag.get("alerts")){
            if(tag.get("date_epoch")){ alData.setStartEpoch(chars); }
            if(tag.get("date")){ alData.setStarts(chars); }
            if(tag.get("expires_epoch")){ alData.setExpireEpoch(chars); }
            if(tag.get("expires")){ alData.setExpire(chars); }
            if(tag.get("description")){ alData.setTitle(chars); }
            if(tag.get("wtype_meteoalarm_name")){ alData.setTitleEU(chars); }
            if(tag.get("message")){ alData.setBody(chars); }
            if(endTag.equalsIgnoreCase("alert")){
                data.IsAlert = ClimateDB.SQLITE_TRUE;
                data.ALData.add(new AlertData(alData)); alData.clear();
            }
        }
        
        // Almanac Info
        if(tag.get("almanac")){
            if(tag.get("temp_high")){
                if(tag.get("normal") && tag.get("F")){ data.TempAveMax = chars; }
                if(tag.get("record") && tag.get("F")){ data.TempRecMax = chars; }
                if(tag.get("recordyear")){ data.TempRecMaxYY = chars; }
            }
            if(tag.get("temp_low")){
                if(tag.get("normal") && tag.get("F")){ data.TempAveMin = chars; }
                if(tag.get("record") && tag.get("F")){ data.TempRecMin = chars; }
                if(tag.get("recordyear")){ data.TempRecMinYY = chars; }
            }
        }
    }    

}
