package coolftc.weathertunnel;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class About extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_dlg);
    }

    // Allow to dismiss with a touch.
    public void frmCloseOnClick(View view){
        finish();
    }
}
