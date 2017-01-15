package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ForecastDay extends Activity {
    String day = "";
    String forecast = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            day = extras.getString(IN_FORED_TITLE);
            forecast = extras.getString(IN_FORED_DESC);
        }
        setContentView(R.layout.forecastday);
        
        showDetails();
    }

    // Allow to dismiss with a touch.
    public void frmCloseOnClick(View view){
        finish();
    }

    private void showDetails() {
        TextView holdView;

        setTitle(day);
        
        holdView = (TextView)this.findViewById(R.id.fcdFullText);
        holdView.setText(forecast);
       

    }

}
