package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;

 class ForecastData {
    
    String HighTemperature = "";
    String LowTemperature = "";
    String PercipitationChance = "";
    String AverageHumidity = "";
    String AverageWind = "";
    String MaximumWind = "";
    String AverageWindDir = "";
    String TimeStamp = "";
    String Timezone = "";
    String Epoch = "";
    String Condition = "";
    String ConditionIcon = "";
    String PercipitationAmount = "";
    String SnowAmount = "";

    ForecastData() { }
    
    ForecastData(ForecastData other){
        HighTemperature = other.HighTemperature;
        LowTemperature = other.LowTemperature;
        PercipitationChance = other.PercipitationChance;
        AverageHumidity = other.AverageHumidity;
        AverageWind = other.AverageWind;
        MaximumWind = other.MaximumWind;
        AverageWindDir = other.AverageWindDir;
        TimeStamp = other.TimeStamp;
        Epoch = other.Epoch;
        Timezone = other.Timezone;
        Condition = other.Condition;
        ConditionIcon = other.ConditionIcon;
        PercipitationAmount = other.PercipitationAmount;
        SnowAmount = other.SnowAmount;
    }
    
    double getLowTemperature(){
        try { return Double.parseDouble(LowTemperature); } 
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getLowTemperature"); return NMBR_NA; }
    }
    double getHighTemperature(){
        try { return Double.parseDouble(HighTemperature); } 
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getHighTemperature"); return NMBR_NA; }
    }
    double getAverageHumidity(){
        try { return Double.parseDouble(AverageHumidity); }
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getAverageHumidity"); return NMBR_NA; }
    }
    private double getAverageWind(){
        try { return Double.parseDouble(AverageWind); } 
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getAverageWind"); return NMBR_NA; }
    }
    private double getMaximumWind(){
        try { return Double.parseDouble(MaximumWind); } 
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getMaximumWind"); return NMBR_NA; }
    }
    
    // WU breaks out rain and snow, but I prefer to display one or the other.  Simpler to understand and correct most of time.
    double getPercipitationAmt(){
        double amountRain; double amountSnow;
        try { amountRain = Double.parseDouble(PercipitationAmount); } 
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getPercipitationAmount"); amountRain = NMBR_NA; }
        try { amountSnow = Double.parseDouble(SnowAmount); } 
        catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getSnowAmount"); amountSnow = NMBR_NA; }
        return Math.max(amountRain, amountSnow);
    }
    
    String getCondition(){
        // The "Probability" labels are dumb and inconsistent, just use the base names. 
        if(ConditionIcon.equalsIgnoreCase(DB_CondRainP)){ return "Rain"; }
        if(ConditionIcon.equalsIgnoreCase(DB_CondSleetP)){ return "Sleet"; }
        if(ConditionIcon.equalsIgnoreCase(DB_CondThunderP)){ return "Thunder Storms"; }
        if(ConditionIcon.equalsIgnoreCase(DB_CondThunder)){ return "Thunder Storms"; }  // little off topic (not a P), but lets split this word for readability.
        if(ConditionIcon.equalsIgnoreCase(DB_CondFlurryP)){ return "Snow Flurries"; }
        return Condition;
    }
    
    String getExpectedWind(boolean useMetric){
        String descriptive = "";//WIND_LABEL_FORE; 
        String speed = "  (";
        Double holdWind = getAverageWind() == NMBR_NA ? 0 : getAverageWind();
        Double holdMax = getMaximumWind() == NMBR_NA ? 0 : getMaximumWind();
        if(holdWind == 0) holdWind = holdMax;
        
        if(useMetric){
            holdWind = Math.floor((holdWind * 1.609344) * TMP_ROUND_WHOLE + 0.5) / TMP_ROUND_WHOLE; 
            speed += String.format("%1.0f", holdWind) + " kph)";
        }else{
            speed += holdWind.toString() + " mph)";
        }
        
        if(holdWind == 0) speed = " (" + WIND_NONE + ")";
        if(holdWind < 4.0) { return descriptive + WIND_CALM + speed; }
        if(holdWind >= 4.0 && holdWind < 8.0) { return descriptive + WIND_BREEZY + speed; }
        if(holdWind >= 8.0 && holdWind < 19.0) { return descriptive + WIND_WINDY + speed; }
        if(holdWind >= 19.0 && holdWind < 32.0) { return descriptive + WIND_BLUSTERY + speed; }
        if(holdWind >= 32.0 && holdWind < 39.0) { return descriptive + WIND_GUSTY + speed; }
        if(holdWind >= 39.0 && holdWind < 55.0) { return descriptive + WIND_GALE + speed; }
        if(holdWind >= 55.0 && holdWind < 65.0) { return descriptive + WIND_STORM + speed; }
        if(holdWind >= 65.0) { return descriptive + WIND_DESTROY + speed; }
        return descriptive + WIND_CALM + speed; // Failsafe
    }
    
    // The Epoch (Unix) time is in seconds, while the Date class likes milliseconds, so do the 
    // math and convert to a date that we like.  Then build out a time with a proper time zone.
    void setTimeStampEpoch(){
        try { 
            TimeStamp = (String) KTime.ParseEpochToFormat(Long.parseLong(Epoch), Timezone, DB_fmtDate3339k);
        } catch (Exception ex){
            ExpClass.LogEX(ex, this.getClass().getName() + ".setTimeStampEpoch");
            TimeStamp = (String) KTime.ParseNow(DB_fmtDate3339k, Timezone);
        }
    }
    
    void setConditionIcon(String cond){
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

    void clear(){
        HighTemperature = "";
        LowTemperature = "";
        PercipitationChance = "";
        AverageHumidity = "";
        AverageWind = "";
        MaximumWind = "";
        AverageWindDir = "";
        TimeStamp = "";
        Epoch = "";
        Timezone = "";
        Condition = "";
        ConditionIcon = "";
        PercipitationAmount = "";
        SnowAmount = "";
    }    
}
