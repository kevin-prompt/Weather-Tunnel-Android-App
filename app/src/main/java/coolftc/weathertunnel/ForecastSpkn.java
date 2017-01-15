package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;

class ForecastSpkn {

    String ConditionIcon = "";
    String Title = "";
    String Description = "";
    String DescriptionAlt = "";
    Boolean Hide = false;
    
    ForecastSpkn() { }
    
    ForecastSpkn(ForecastSpkn other){
        ConditionIcon = other.ConditionIcon;
        Title = other.Title;
        Description = other.Description;
        DescriptionAlt = other.DescriptionAlt;
        Hide = other.Hide;
    }
    
    // The &deg; replacement has flip/flopped a bit with the APIs.  I have coded to deal
    // with it in the XPP if it is an Entity and if it makes it through as a literal.
    String getDescription(boolean useCelsius){
        
        if(useCelsius) {
            if(DescriptionAlt.length()>0) // Not all forecasts seem to have Alt text.
                return DescriptionAlt.replace("&deg;", "°" );
        }
        
        return Description.replace("&deg;", "°" );
    }
    
    void setCondition(String cond){
        if(cond.equalsIgnoreCase("nt_clear") || cond.equalsIgnoreCase("nt_sunny")){
            cond = DB_CondClearN;
        }
        if(cond.equalsIgnoreCase("nt_mostlycloudy") || 
           cond.equalsIgnoreCase("nt_mostlysunny") || 
           cond.equalsIgnoreCase("nt_partlycloudy") || 
           cond.equalsIgnoreCase("nt_partlysunny")){
            cond = DB_CondPartCloudN;
        }
        CharSequence night = "nt_"; 
        ConditionIcon = cond.replace(night, "");
    }
    
   
    /* In the US the names Today and Tonight are used, but non-US forecasts just use the name of
     * day.  Check for either the Today/Tonight or that today matches the day name supplied.  */
    boolean isCurrent(String todayName){
        return ((TODAY_TONIGHT.contains(Title.toLowerCase())) || (Title.contains(todayName)));
    }
    
    // You do not get a nice condition text with the forecast, so we need to do it here.
    String conditonFull(){
        String holdCond = "Partly \n Cloudy";
        String cond = ConditionIcon;
        if(cond.equals(DB_CondClear))       { holdCond = "Clear"; }
        if(cond.equals(DB_CondClearN))      { holdCond = "Clear"; }
        if(cond.equals(DB_CondCloud))       { holdCond = "Cloudy"; }
        if(cond.equals(DB_CondFlurry))      { holdCond = "Flurries"; }
        if(cond.equals(DB_CondFlurryP))     { holdCond = "Flurries"; }
        if(cond.equals(DB_CondFog))         { holdCond = "Fog"; }
        if(cond.equals(DB_CondHazy))        { holdCond = "Hazy"; }
        if(cond.equals(DB_CondMostCloud))   { holdCond = "Mostly\nCloudy"; }
        if(cond.equals(DB_CondMostSun))     { holdCond = "Mostly\nSunny"; }
        if(cond.equals(DB_CondPartCloud))   { holdCond = "Partly\nCloudy"; }
        if(cond.equals(DB_CondPartCloudN))  { holdCond = "Partly\nCloudy"; }
        if(cond.equals(DB_CondPartSun))     { holdCond = "Partly\nSunny"; }
        if(cond.equals(DB_CondRain))        { holdCond = "Rain"; }
        if(cond.equals(DB_CondRainP))       { holdCond = "Rain"; }
        if(cond.equals(DB_CondSleet))       { holdCond = "Sleet"; }
        if(cond.equals(DB_CondSleetP))      { holdCond = "Sleet"; }
        if(cond.equals(DB_CondSnow))        { holdCond = "Snow"; }
        if(cond.equals(DB_CondSnowP))       { holdCond = "Snow"; }
        if(cond.equals(DB_CondSun))         { holdCond = "Sunny"; }
        if(cond.equals(DB_CondThunder))     { holdCond = "Thunder\nStorms"; }
        if(cond.equals(DB_CondThunderP))    { holdCond = "Thunder\nStorms"; }
        if(cond.equals(DB_CondUnknown))     { holdCond = "Partly\nCloudy"; }
        return holdCond;
    }
    
    void clear(){
        ConditionIcon = "";
        Title = "";
        Description = "";
        DescriptionAlt = "";
        Hide = false;
    }
        
}

