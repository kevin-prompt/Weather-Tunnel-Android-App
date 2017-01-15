package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

class GmiGeocodeXPP {
    /**********************************************************************
     * Every technology starts out simple, and either dies or becomes popular.  When it
     * becomes popular, it starts to add features to meet the demands of a complex world.
     * As it becomes every more complex, newer simple technologies are introduced, and 
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
     * in the "cuts", both the EMPL and NAME keys of the TAG map will be true. Just wait for 
     * that condition and than grab the cut characters.
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
    
    /* This class can handle Google Maps Geocoding API.  It will take a formatted address and parse it
     * into it component parts, e.g. city, state, zip & country. It will also find the GPS coordinates
     * of the address to the best of its ability.  
     */
    private ContactAddr data;
    private String currLong = "";
    private String currShrt = "";
    private String currTyp1 = "";
    private String currTyp2 = "";
    
    // Constructor - create a XPP
    GmiGeocodeXPP() throws XmlPullParserException {
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
        //try {
        //    xpp.defineEntityReplacementText("deg", "°");
        //} catch (XmlPullParserException e) {   }
        ExpClass.LogIN(KEVIN_SPEAKS, "The GmiGeocodeXPP has been called.");
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
    ContactAddr getStatus() {
        return data;   
    }
    
    /******************************************************************
     * TAG handling logic.
     */
    // Called when the xml document is first opened
    private void startDocument() {
        data = new ContactAddr();
        initializeTAGs();
    }
    
    // Clear out the map and initialize the tags we care about.
    private void initializeTAGs() {
        tag.clear();
        tag.put("address_component", false);
        tag.put("type", false);
        tag.put("long_name", false);
        tag.put("short_name", false);
        tag.put("geometry", false);
        tag.put("location", false);
        tag.put("lat", false);
        tag.put("lng", false);
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
        
        if(tag.get("address_component")){
            if(tag.get("long_name")) { currLong = chars; }
            if(tag.get("short_name")) { currShrt = chars; }
            if(tag.get("type")) { 
                if(currTyp1.length() == 0)
                    currTyp1 = chars; 
                else
                    currTyp2 = chars;
            }
        }
        
        if(tag.get("geometry") && tag.get("location") && tag.get("lat")) data.setLatitude(chars);
        if(tag.get("geometry") && tag.get("location") && tag.get("lng")) data.setLongitude(chars);
        
        
        if(endTag.equalsIgnoreCase("address_component")){
            
            if(currTyp1.equalsIgnoreCase("postal_code") || currTyp2.equalsIgnoreCase("postal_code")){ data.setPostal(currLong); }
            
            if(currTyp1.equalsIgnoreCase("locality") || currTyp2.equalsIgnoreCase("locality")){ data.setCity(currLong); }
            
            if(currTyp1.equalsIgnoreCase("administrative_area_level_1") || currTyp2.equalsIgnoreCase("administrative_area_level_1")){ data.setMuni(currShrt); }
            
            if(currTyp1.equalsIgnoreCase("country") || currTyp2.equalsIgnoreCase("country")){ data.setContry(currShrt); }
                            
            currTyp1 = ""; currTyp2 = ""; currLong = ""; 
        }
        
    }

}
