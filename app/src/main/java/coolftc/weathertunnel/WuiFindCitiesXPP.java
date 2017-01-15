package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

class WuiFindCitiesXPP {
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
    
    // This class can handle a list of possible cities. It will have 0 to n cities
    // listed. The city list contains both the display name of the city and the link
    // needed to access the city location data.
    // The data returned from the API is not put together in a particularly useful
    // way.  Each returned location should be surrounded by some type of location
    // tag, but instead they are all just sitting there at the same level.  The
    // assumption is that their proximity/order defines their relationship, but that
    // was is something good xml could let us avoid.  Also the data includes states
    // and countries that match the search criteria, but are not useful in the API,
    // as far as I can tell.  Just ignore them.
    // 0 - Some problem, most likely could not find anything.
    // 1 - One city was found, so the result can be used in another API call
    //     to populate a Status object.
    // n - Multiple cities were found, need to be more selective.
    private Map<String, String> cities = new TreeMap<>();
    private String currCity = "";
    private String currType = "";
    private String currLink = "";

    // Constructor - create a XPP
    WuiFindCitiesXPP() throws XmlPullParserException {
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
        ExpClass.LogIN(KEVIN_SPEAKS, "The WuiFindCitiesXPP has been called.");
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
    
    // Return the link from the first item.
    String link(){
        Iterator<Map.Entry<String, String>> ali = cities.entrySet().iterator();
        if (ali.hasNext()) {
            Map.Entry<String, String> city = ali.next();
            return city.getValue();
        }
        return "";  // If no data found.
    }
    
    /******************************************************************
     * TAG handling logic.
     */
    // Called when the xml document is first opened
    private void startDocument() {
        initializeTAGs();
    }
    
    // Clear out the map and initialize the tags we care about.
    private void initializeTAGs() {
        tag.clear();
        tag.put("name", false);
        tag.put("type", false);
        tag.put("c", false);
        tag.put("zmw", false);
        tag.put("place", false);
        tag.put("link", false);
        tag.put("dest", false);
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

        /*
         * The search data from WU-API comes in as a flat, ordered set of data.
         * We assume "zmw" will come after the name & type, so use that do trigger a save.
         */
        if(tag.get("name")) { currCity = chars; }
        if(tag.get("type")) { currType = chars; }
        if(tag.get("zmw")) {
            if(currType.equalsIgnoreCase("city")) {
                if(currCity.length() > 0){
                    currLink = "zmw:" + chars;      // need that extra prefix or will be treated as a zip code in lookup.
                    cities.put(currCity, currLink);   
                }
            }
           currCity = "";
        }
        
        /* The prepaq data from WT-API comes in as a set of places with a destination and a link.
         * We just look for the "place" end tag to trigger a save.
         */
        if(tag.get("place") && tag.get("dest")) { currCity = chars; }
        if(tag.get("place") && tag.get("link")) { currLink = chars; }
        if(endTag.equalsIgnoreCase("place")) { cities.put(currCity, currLink); }
        
    }
}
