package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

public class Welcome extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_dlg);
        
   }

    @Override
    protected void onStart() {
        /**
         * To get the hyperlinks to work, we have to apply this setting to the
         * textview after it has been created.  That is why it is here.
         */
        TextView holdView;
        holdView = (TextView) findViewById(R.id.welWTeula);
        holdView.setMovementMethod(LinkMovementMethod.getInstance());
        super.onStart();
    }

    /**
     * We verify that the checkbox has been clicked and update the shared database.
     * In either case we return notice back to the Slate that we are done, at which
     * point it will check the shared database for the result.
     */
    public void onClickAccept(View view){
        switch (view.getId()){
        case R.id.welBtnAccept:
            CheckBox checkBox = (CheckBox)findViewById(R.id.welChkRead);
            if(checkBox.isChecked()) { 
                SharedPreferences eulaOK = getSharedPreferences(SP_EULA_OK, Context.MODE_PRIVATE);
                SharedPreferences.Editor ali = eulaOK.edit();
                ali.putInt(SP_EULA_VER, SP_EULA_CURRENT);
                ali.apply();
            }
            Intent rtn = new Intent(IN_WELCOME_BACK);
            setResult(RESULT_OK, rtn);
            finish();
        }
    }
    
    /**
     * Same as when onClickAccept is hit.
     */
    @Override
    public void onBackPressed() {
        CheckBox checkBox = (CheckBox)findViewById(R.id.welChkRead);
        if(checkBox.isChecked()) { 
            SharedPreferences eulaOK = getSharedPreferences(SP_EULA_OK, Context.MODE_PRIVATE);
            SharedPreferences.Editor ali = eulaOK.edit();
            ali.putInt(SP_EULA_VER, SP_EULA_CURRENT);
            ali.apply();
        }
        Intent rtn = new Intent(IN_WELCOME_BACK);
        setResult(RESULT_OK, rtn);
        finish();
    }

    

}
