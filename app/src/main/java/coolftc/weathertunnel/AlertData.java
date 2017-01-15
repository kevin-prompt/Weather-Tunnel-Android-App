package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;

import java.io.Serializable;

class AlertData implements Serializable {
    
    /*
     * There is a bunch of processing that has to take place with the alerts data, so forcing the use of "get" methods.
     */
    private static final long serialVersionUID = 5264124489360369638L; // Required for serialization.
    private String Title = "";       // The category of the alert which can also be used as a short description, e.g. "Flood Warning".
    private String TitleEU = "";     // The title for European alerts will override the default Title if present.
    private String Starts = "";      // The effective time of the alert.
    private String StartEpoch = "";  // Native format of start in Unix time.
    private String Expire = "";      // The expiration time of the alert.
    private String ExpireEpoch = ""; // Native format of expiration in Unix time.
    private String Body = "";        // The full text of the Alert Message.

    AlertData() { }
    
    AlertData(AlertData other){
        Title = other.Title;
        TitleEU = other.TitleEU;
        Starts = other.Starts;
        StartEpoch = other.StartEpoch;
        Expire = other.Expire;
        ExpireEpoch = other.ExpireEpoch;
        Body = other.Body;
    }

    public void setTitle(String title) {
        Title = title;
    }

    void setTitleEU(String titleEU) {
        TitleEU = titleEU;
    }

    void setStarts(String starts) {
        Starts = starts;
    }

    void setStartEpoch(String startEpoch) {
        StartEpoch = startEpoch;
    }

    void setExpire(String expire) {
        Expire = expire;
    }

    void setExpireEpoch(String expireEpoch) {
        ExpireEpoch = expireEpoch;
    }

    void setBody(String body) {
        Body = body;
    }

//    public String getStarts() { return setTimeStampEpoch(StartEpoch, Starts); }

    String getExpire() {
        return setTimeStampEpoch(ExpireEpoch, Expire);
    }

    String getBody() {
        return reformatMsg();
    }
    
    // If the EU Title exists, then it must be the EU Alert.  The regular Title gets filled in for both US and EU.
    String getTitleCut(int sizeMax, String extra){
        String primTitle = TitleEU.length() > 0 ? TitleEU : Title;
        int cutit = Math.min(primTitle.length(), sizeMax); 
        String holdTitle = primTitle.substring(0,cutit); 
        if(primTitle.length() > sizeMax) holdTitle += extra;
        return holdTitle;
    }

    private String reformatMsg(){
        if(TitleEU.length() > 0) return Body;   // EU does not have as many breaks 
        String holdMsg = Body.replace("\n\n", "zz");
        holdMsg = holdMsg.replace("\n", " ");
        holdMsg = holdMsg.replace("zz", "\n\n");
        return holdMsg + "\n\n";
    }
    
    // The Epoch (Unix) time is in seconds, while the Date class likes milliseconds, so do the 
    // math and convert to a date that we like.  We do not know the time zone, so do it later.
    private String setTimeStampEpoch(String epoch, String f8601){
        try { 
            long holdEpoch;
            try{ holdEpoch = Long.parseLong(epoch); }
            catch(Exception ex) { holdEpoch = 0; } // if the epoch fails, try the formatted string
            if(holdEpoch > 0){
                return (String) KTime.ParseEpochToFormat(holdEpoch, DATA_NA, DB_fmtDate3339k);
            }else{
                return (String) KTime.ParseToFormat(f8601, DB_fmtDateEUAlert, DATA_NA, DB_fmtDate3339k);
            }
        } catch (Exception ex){
            ExpClass.LogEX(ex, this.getClass().getName() + ".setTimeStampEpoch");
            return (String) KTime.ParseNow(DB_fmtDate3339k, DATA_NA);
        }
    }
    
    public void clear(){
        Title = "";
        TitleEU = "";
        Starts = "";
        StartEpoch = "";
        Expire = "";
        ExpireEpoch = "";
        Body = "";
    }
    
}


