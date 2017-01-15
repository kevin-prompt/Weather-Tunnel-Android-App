package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

class WuiLocationXPP {
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
    
    // This class can handle two types of xml.  The first type is the specific
    // location data for a city.  The other is a list of possible cities. The
    // city list is always populated.  It will have 0, 1 or n+1 cities listed.
    // The city list contains both the display name of the city and the link
    // needed to access the city location data (or get another list of cities).
    // ***NOTE: Getting 0 or n is not expected, but is possible, so the
    //          code will be left in. For example, using just "Portland" as the 
    //          search term will return multiple results. We want an exact match, 
    //          so we will rely on the WuiFindCitiesXPP to be the real way cities
    //          are found.  In general, only links will be used for this API call, 
    //          so exactly 1 item should be found.
    // 0 - Some problem, most likely could not find anything.
    // 1 - A single city was found and the Status data is populated.
    // n - Multiple cities were found, need to be more selective.
    private Status data;
    private Map<String, String> cities = new TreeMap<>();
    private String currCity = "";

    // Constructor - create a XPP
    WuiLocationXPP() throws XmlPullParserException {
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
        ExpClass.LogIN(KEVIN_SPEAKS, "The WuiLocationXPP has been called.");
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
    
    // Return a list to choose from.
    Map<String, String> pickCity(){
        return cities;
    }
    
    // This gives a hint as to how things went.  
    // 0   = problem.
    // 1   = city was found.
    // n+1 = cities to choose from.
    int result(){
        return cities.size();
    }
    
    /******************************************************************
     * TAG handling logic.
     */
    // Called when the xml document is first opened
    private void startDocument() {
        data = new Status();
        initializeTAGs();
    }
    
    // Clear out the map and initialize the tags we care about.
    private void initializeTAGs() {
        tag.clear();
        // Valid Result
        tag.put("response", false);
        
        // Multiple Results
        tag.put("result", false);
        tag.put("zmw", false);
        tag.put("name", false);
        
        // Single Result
        tag.put("location", false);
        tag.put("nearby_weather_stations", false);
        tag.put("airport", false);
        tag.put("pws", false);
        tag.put("station", false);
        tag.put("icao", false);
        tag.put("id", false);
        tag.put("country", false);
        tag.put("state", false);
        tag.put("city", false);
        tag.put("zip", false);
        tag.put("tz_long", false);
        tag.put("lat", false);
        tag.put("lon", false);
        tag.put("l", false);
        tag.put("distance_mi", false);
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
        
        // Lots of cities found.  Search needs to be refined, so collect a list.
        if(tag.get("result")) {
            if(tag.get("name")) { currCity = chars; }
            if(tag.get("zmw")) {
                if(currCity.length() > 0){
                    cities.put(currCity, "zmw:" + chars);   // need that extra prefix or will be treated as a zip code in lookup.
                }
                currCity = "";
            }
        }
        
        // City found so get basic location stuff that is at the top of the hierarchy.
        if(tag.get("location")){
            if(!tag.get("nearby_weather_stations")){
                if(tag.get("country")){ data.Country = chars; }
                if(tag.get("state")){ data.State = chars; }
                if(tag.get("zip")){ data.Zip = chars; }
                if(tag.get("tz_long")){ data.TimeZone = chars; }
                if(tag.get("lat")){ data.Latitude = chars; }
                if(tag.get("lon")){ data.Longitude = chars; }
                if(tag.get("city")){ 
                    data.City = chars;
                    cities.put(chars, chars);  // Add it as a signal we have one.
                }
            }
        }

        // Public weather station. Just get the first one found.
        if(tag.get("nearby_weather_stations") && tag.get("airport") && tag.get("station")) { 
            if(tag.get("icao") && (data.PublicCode.length()==0)) {
                if(!chars.equalsIgnoreCase("----"))    // if an airport code is not there, may get dashes.
                    data.PublicCode = chars;
            }
            if(tag.get("country") && (data.PublicCntry.length()==0)) {
                data.PublicCntry = chars;
            }
            // note bug:if the "----" happens we are getting wrong lat/lon. Fix is to recalculate from station detail data.
            if(tag.get("lat") && (data.PublicLati.length()==0)) { 
                data.PublicLati = chars;
            }
            if(tag.get("lon") && (data.PublicLong.length()==0)) {
                data.PublicLong = chars;
            }
        }
        
        // Private weather station & distance. Get first one, which is the closest.  Then get the
        // second one as a backup.
        if(tag.get("nearby_weather_stations") && tag.get("pws") && tag.get("station")) { 
            
            if(tag.get("id") && (data.PrivateCode.length()==0)){
                data.PrivateCode = "pws:" + chars;
            }
            
            if(tag.get("distance_mi") && (data.PrivateDist.length()==0)){
                data.PrivateDist = chars;
            }
            
            if(tag.get("id") && (data.PrivateCode.length()!=0) && (data.PrivateCodeB.length()==0)){
                if(!chars.equalsIgnoreCase(data.PrivateCode.substring(4)))  // remember we added a prefix
                    data.PrivateCodeB = "pws:" + chars;
            }
            // Little tricky, but if the PrivateCodeB exists, this is most likely its distance.
            if(tag.get("distance_mi") && (data.PrivateCodeB.length()!=0) && (data.PrivateDistB.length()==0)){
                    data.PrivateDistB = chars;
            }

        }
    }
       
}
