package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;

import java.util.ArrayList;
import java.util.Calendar;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


public class Alert extends AppCompatActivity {
    private String location;
    private String special;
    private String timezone;
    private ArrayList<AlertData> data = new ArrayList<>();
    private int current = 0;
    
    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            location = extras.getString(IN_ALERT_LOC);
            special = extras.getString(IN_ALERT_SPC);
            timezone = extras.getString(IN_ALERT_TZ);
            data = (ArrayList<AlertData>) extras.getSerializable(IN_ALERT_ID);
        }
        ActionBar bar = getSupportActionBar();
        //bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE); 
        if(bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowTitleEnabled(true);
        }
        setContentView(R.layout.alert);
        showDetails(current);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) { 
        case android.R.id.home: 
            finish(); 
            return true; 
        default: 
            return super.onOptionsItemSelected(item); 
        } 
    }

    // Allow to dismiss with a touch.
    public void frmCloseOnClick(View view){
        finish();
    }
    
    // Display Additional Alerts
    public void displayMoreOnClick(View view){
        switch (view.getId()) {
        case R.id.imgWarningNext:
            current++;
            current = current % (data.size());
            showDetails(current);
            break;
        case R.id.imgWarningBack:
            current--;
            if(current == -1) current = data.size() - 1;
            showDetails(current);
            break;
        }
    }
    
    private void showDetails(int ndx){
        TextView holdView;
        ImageView holdImg;
        getResources(); 
        
        holdView = (TextView)this.findViewById(R.id.txtCityAlr);
        int cutit = Math.min(location.length(), 23); 
        String holdLoc = location.substring(0,cutit); 
        if(location.length() > 23) holdLoc += "...";
        holdView.setText(holdLoc);
       
        holdView = (TextView)this.findViewById(R.id.txtSpecialAlr);
        if(special.length() == 0){
            holdView.setVisibility(View.INVISIBLE);
        } else {
            holdView.setText(special);
        }
        
        if(data.size() < 2){    // Hide the next/back buttons if only one alert
            holdImg = (ImageView)this.findViewById(R.id.imgWarningNext);
            holdImg.setVisibility(View.INVISIBLE);
            holdImg = (ImageView)this.findViewById(R.id.imgWarningBack);
            holdImg.setVisibility(View.INVISIBLE);
        }

        holdView = (TextView)this.findViewById(R.id.lblCondBarAlr);
        holdView.setText(data.get(ndx).getTitleCut(23, "..."));
         
        holdView = (TextView)this.findViewById(R.id.txtExpireAlr);
        holdView.setText(alertTimeLeft(ndx));
        
        holdView = (TextView)this.findViewById(R.id.txtWarningCnt);
        holdView.setText("(" + String.valueOf(current+1) + " of " + String.valueOf(data.size()) + ") "); // extra space for italics
         
        holdView = (TextView)this.findViewById(R.id.txtExpireTimeAlr);
        Calendar exp_adjusted = adjustExpired(ndx);
        holdView.setText(DateFormat.format(Settings.getDateDisplayFormat(getApplicationContext(), DATE_FMT_ALERT_EXP), exp_adjusted));
         
        holdView = (TextView)this.findViewById(R.id.txtMsgAlr);
        holdView.setText(data.get(ndx).getBody());
    }
    
    private String alertTimeLeft(int ndx){
        String hoursToGo = "Expires in ";
        Calendar now = Calendar.getInstance();
        Calendar exp_adjusted = adjustExpired(ndx);
        
        long diffmsecs = exp_adjusted.getTimeInMillis() - now.getTimeInMillis();
        long oneHour = 3600000;// 1 hour of msecs
        if(diffmsecs > oneHour){
            long hours = diffmsecs / oneHour; 
            hoursToGo += String.valueOf(hours) + (hours>1?" hours":" hour"); 
        }else{
            hoursToGo += "under an hour";
        }
        return hoursToGo;
    }
    
    private Calendar adjustExpired(int ndx){
        Calendar exp;
        try { exp = KTime.ParseToCalendar(data.get(ndx).getExpire(), DB_inputDate3339, DATA_NA); } 
        catch (ExpParseToCalendar ex) { ExpClass.LogEX(ex, this.getClass().getName() + ".AlertTimeLeft"); exp = Calendar.getInstance();}
        return KTime.ConvertTimezone(exp, timezone);
    }

}
