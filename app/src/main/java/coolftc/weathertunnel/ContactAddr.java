package coolftc.weathertunnel;

/*************************************************************************
 * Data structure representing a Contact's location. 
 */
class ContactAddr {
    
    private String name = "";    // Name on contact
    private String label = "";   // Typically Home/Work/Other
    private String street = "";  // Street address (123 ABC Street)
    private String city = "";    // City
    private String muni = "";    // Municipality, e.g. State, Province, County
    private String postal = "";  // Postal code
    private String contry = "";  // Country
    private String addrFull = "";// The full address
    private String latitude = "";// The latitude of the address
    private String longitude = "";//The longitude of the address
    
    private boolean selected = false;    // Useful for Dialog

    String getName() {
        return name;
    }
    void setName(String name) {
        if(name!=null) this.name = name;
    }
    String getLabel() {
        return label;
    }
    void setLabel(String label) {
        if(label!=null) this.label = label;
    }
    String getStreet() {
        return street;
    }
    void setStreet(String street) {
        if(street!=null) this.street = street;
    }
    String getCity() {
        return city;
    }
    void setCity(String city) {
        if(city!=null) this.city = city;
    }
    String getMuni() {
        return muni;
    }
    void setMuni(String muni) {
        if(muni!=null) this.muni = muni;
    }
    String getPostal() {
        return postal;
    }
    void setPostal(String postal) {
        if(postal!=null) this.postal = postal;
    }
    String getContry() {
        return contry;
    }
    void setContry(String contry) {
        if(contry!=null) this.contry = contry;
    }
    String getAddrFull() {
        return addrFull;
    }
    void setAddrFull(String addrFull) {
        if(addrFull!=null) this.addrFull = addrFull;
    }
    String getLatitude() {
        return latitude;
    }
    void setLatitude(String latitude) {
        this.latitude = latitude;
    }
    String getLongitude() {
        return longitude;
    }
    void setLongitude(String longitude) {
        this.longitude = longitude;
    }
    boolean isSelected() {
        return selected;
    }
    void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    // Methods
    String getItemDisplay(){
        return "(" + label + ")\n" + addrFull;
    }
    String getNameDisplay(){
        return name + " (" + label + ")";
    }
    String getAddressClean(){
        // Remove any non-alphanumeric characters, except the dash "-".
        return addrFull.replaceAll("[^\\p{L}\\p{N}-]", " ");
    }
    String getPostalOrCityLink(){
        if(postal.length() > 0)
            return postal;
        
        if(muni.length() > 0)
            return muni + "/" + city;
        else
            return  contry + "/" + city;
    }
    String getGPSLink(){
        if(longitude.length() > 0 && latitude.length() > 0)
            return latitude + "," + longitude;
        return "";
    }
}
