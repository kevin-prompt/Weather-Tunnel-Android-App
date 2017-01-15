package coolftc.weathertunnel;

import android.util.Log;


public class ExpClass extends Exception {
    public static final long serialVersionUID = -6463927990362971950L;  // required by superclass
    private static final String ERR_TAG = "Exception Sink";
    private static final boolean DEBUG_RUN = false;

    private int Number;
    private String Name;
    
    public ExpClass(){
        super("General Exception Thrown.");
        Number = 18000;
        Name = "GeneralException";
    }
    
    public ExpClass(int number, String name, String desc, Throwable source){
        super(desc, source);
        Number = number;
        Name = name;
    }
    
    public static void LogEX(Exception ex, String key){
        Log.e(ERR_TAG, ex.getStackTrace()[0].getClassName() + "." + ex.getStackTrace()[0].getMethodName() + "() :: (key=" + key + ") " + ex.getMessage());
    }
    
    public static void LogIN(String tag, String message){
        if(DEBUG_RUN) Log.i(tag, message);
    }

}
