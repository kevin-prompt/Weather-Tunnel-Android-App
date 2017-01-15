package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.text.format.DateFormat;

/* >>> If you have a Date/Calendar object and just need formatting, use DateFormat directly!!!
 * 
 * At one time the SimpleDateFormat just started acting bad, and others complained too
 * (search "Loaded time zone name for "). It got real slow (min .5-2sec). Of course, 
 * after I wrote this, it all started working again.  I'll use this for a time, but
 * eventually go back to the system supplied class because parsing can be tricky. I should
 * be able to just plug SimpleDateFormat into this class and not change much code.
 
 * The primary customization offered is for parsing some string formatted dates.
   The three expected formats are:
    RFC 822 = "EEE, dd MMM yyyy HH:mm:ss Z";
    RFC 3339 = "yyyy-MM-ddTHH:mm:ssZ";
    RFC 8601 = "yyyy-MM-dd HH:mm:ss.SSSZ";
   Others will work if they use the same general formatting characters. Note this is
   a POSITIONAL parse.  The date string has to match the template EXACTLY. For example,
   date strings without zero padding will not work.
   
 * Time zone is the tricking item.  When explicitly passed in with the Parse methods, it
   is just applied to the information. If you want to convert a time in one zone to what 
   it will be in another zone, use the ConvertTimezone method. All time zones explicitly 
   passed in are expected to be a long time zone id, e.g. "America/Los_Angeles". In the  
   date strings themselves time zones are generally found as offsets to UTC or the literal "Z". 
   
   -------------------------------------------------------------------------------
 * Output Formatting template for Android's DateFormat:
    AM_PM               'a' :: corresponds to the AM_PM field.
    DATE                'd' :: corresponds to the DATE field.
    DAY_OF_WEEK         'E' :: corresponds to the DAY_OF_WEEK field.
    DAY_OF_WEEK_IN_MONTH'F' :: corresponds to the DAY_OF_WEEK_IN_MONTH field.
    DAY_OF_YEAR         'D' :: corresponds to the DAY_OF_YEAR field.
    ERA                 'G' :: corresponds to the ERA field.
    HOUR0               'K' :: corresponding to the 24 HOUR field.
    HOUR1               'h' :: corresponding to the 12 HOUR field.
    HOUR_OF_DAY0        'H' :: corresponds to the 12 HOUR_OF_DAY field.
    HOUR_OF_DAY1        'k' :: corresponds to the 24 HOUR_OF_DAY field.
    MILLISECOND         'S' :: corresponds to the MILLISECOND field.
    MINUTE              'm' :: corresponds to the MINUTE field.
    MONTH               'M' :: corresponds to the MONTH field.
    SECOND              's' :: corresponds to the SECOND field.
    TIMEZONE            'z' :: corresponds to the ZONE_OFFSET and DST_OFFSET fields.
    WEEK_OF_MONTH       'W' :: corresponds to the WEEK_OF_MONTH field.
    WEEK_OF_YEAR        'w' :: corresponds to the WEEK_OF_YEAR field.
    YEAR                'y' :: corresponds to the YEAR field.   
 */
class KTime {
    private static final int MISSING = -1;
    private static final String DAY_dd = "dd";      // Day of the week - numeric
    private static final String DAY_MM = "MM";      // Month of the year - numeric
    private static final String DAY_MMM = "MMM";    // Month of the year - string
    private static final String DAY_yyyy = "yyyy";  // Year - numeric
    private static final String DAY_HH = "HH";      // 24 hour of day - numeric
    private static final String DAY_hh = "hh";      // 12 hour of day - numeric
    private static final String DAY_aa = "aa";      // symbol for AM/PM - string
    private static final String DAY_mm = "mm";      // minute of hour - numeric
    private static final String DAY_ss = "ss";      // second of minute - numeric
    private static final String DAY_SSS = "SSS";    // milliseconds past last second - numeric
    private static final String DAY_Z = "Z";        // time zone
    private static final String MONTH_FIXED = "NUL JAN FEB MAR APR MAY JUN JUL AUG SEP OCT NOV DEC";
    
    // Quick way to get the current date formatted.  Also can change it to local time in another time zone.
    static CharSequence ParseNow(String outFormat, String timezone){
        Calendar work = Calendar.getInstance();
        if(!timezone.equalsIgnoreCase(DATA_NA)) work.setTimeZone(TimeZone.getTimeZone(timezone));
        return DateFormat.format(outFormat, work);
    }
    
    // What is the difference in msec between the two dates
    static long CalcDateDifference(String tndxOne, String tndxTwo, String inFormat) throws ExpParseToCalendar{
        Calendar time1 = ParseToCalendar(tndxOne, inFormat, DATA_NA);
        Calendar time2 = ParseToCalendar(tndxTwo, inFormat, DATA_NA);
        return Math.abs(time1.getTimeInMillis() - time2.getTimeInMillis());
    }
    // This will convert the time from one time zone to anther time zone.
    static Calendar ConvertTimezone(Calendar inTime, String timezone){
        Date holdDate = inTime.getTime();
        Calendar outTime = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        outTime.setTime(holdDate);
        return outTime;
    }
    
    // This will take an Epoch (Unix) time and output it in the desired format.  Epoch does assumes UTC as
    // the time zone, so if you want something different you need to specify.
    static CharSequence ParseEpochToFormat(long secs, String timezone, String outFormat){
        long msecs = secs * 1000;  // Calendar like msecs
        if(timezone.equalsIgnoreCase(DATA_NA) || timezone.length() == 0) timezone = "UTC";
        Calendar newday = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        newday.setTimeInMillis(msecs);
        return DateFormat.format(DB_fmtDate3339k, newday);
    }

    // Note that the DateFormat uses slightly different characters to define output than the RFC formats. :(
    static CharSequence ParseToFormat(String inTime, String inFormat, String timezone, String outFormat) throws ExpParseToCalendar {
        Calendar work = ParseToCalendar(inTime, inFormat, timezone);
        return DateFormat.format(outFormat, work);
    }

    // 
    static Calendar ParseToCalendar(String inTime, String inFormat, String timezone) throws ExpParseToCalendar{
    try {
        int ndx;
        boolean monthFound = false; // if the MMM is found, don't bother looking for the MM
        Calendar work = Calendar.getInstance();
        
        ndx=inFormat.indexOf(DAY_dd);
        if(ndx != MISSING){
            work.set(Calendar.DAY_OF_MONTH, Integer.parseInt(inTime.substring(ndx, ndx+2)));
        }
            
        ndx=inFormat.indexOf(DAY_MMM);
        if(ndx != MISSING){
            int holdMonth = MONTH_FIXED.indexOf(inTime.substring(ndx, ndx+3).toUpperCase()) / 4;
            if(holdMonth > 0){
                work.set(Calendar.MONTH, holdMonth - 1); // Note Calendar month fields are zero indexed.
                monthFound = true;
            }
        }
        
        ndx=inFormat.indexOf(DAY_MM);
        if(ndx != MISSING && !monthFound){
            work.set(Calendar.MONTH, Integer.parseInt(inTime.substring(ndx, ndx+2)) - 1);   // Note Calendar month fields are zero indexed.
        }
        
        ndx=inFormat.indexOf(DAY_yyyy);
        if(ndx != MISSING){
            work.set(Calendar.YEAR, Integer.parseInt(inTime.substring(ndx, ndx+4)));
        }
            
        ndx=inFormat.indexOf(DAY_HH);
        if(ndx != MISSING){
            work.set(Calendar.HOUR_OF_DAY, Integer.parseInt(inTime.substring(ndx, ndx+2)));
        }
        
        ndx=inFormat.indexOf(DAY_hh);
        if(ndx != MISSING){
            work.set(Calendar.HOUR_OF_DAY, Integer.parseInt(inTime.substring(ndx, ndx+2)));
            ndx=inFormat.indexOf(DAY_aa);
            if(ndx != MISSING){
                work.set(Calendar.AM_PM, inTime.substring(ndx, ndx+2).toLowerCase().equals("am")?Calendar.AM:Calendar.PM);
            }
        }
        
        ndx=inFormat.indexOf(DAY_mm);
        if(ndx != MISSING){
            work.set(Calendar.MINUTE, Integer.parseInt(inTime.substring(ndx, ndx+2)));
        }
        
        ndx=inFormat.indexOf(DAY_ss);
        if(ndx != MISSING){
            work.set(Calendar.SECOND, Integer.parseInt(inTime.substring(ndx, ndx+2)));
        }
        
        ndx=inFormat.indexOf(DAY_SSS);
        if(ndx != MISSING){
            work.set(Calendar.MILLISECOND, Integer.parseInt(inTime.substring(ndx, ndx+3)));
        }
        
        ndx=inFormat.indexOf(DAY_Z);
        if(ndx != MISSING){
            int tmzoff = 0;
            int tmzsign = 1;
            String tmz = inTime.substring(ndx).trim(); // time zone is the final element in these formats
            if(!(tmz.equalsIgnoreCase("Z") || tmz.equalsIgnoreCase("GMT") || tmz.equalsIgnoreCase("UTC"))){
                tmz = tmz.replace(":", "");
                if(tmz.contains("-")) tmzsign = -1;
                tmz = tmz.replace("-", ""); tmz = tmz.replace("+", "");
                int tmzHours = Integer.parseInt(tmz.substring(0, 2));
                int tmzMins = Integer.parseInt(tmz.substring(2));
                tmzoff = (tmzHours * 60 * 60) + (tmzMins * 60); 
                tmzoff = tmzoff * 1000 * tmzsign;
            }
            work.set(Calendar.ZONE_OFFSET, tmzoff);    
        }
        
        // Sometimes the time zone is passed in directly, which should override the input string, so it is done last.
        if(!timezone.equalsIgnoreCase(DATA_NA)) work.setTimeZone(TimeZone.getTimeZone(timezone));
        
        return work;
    }catch(Exception ex){
        throw new ExpParseToCalendar(ex.getMessage(), ex);
    }
    }

}
