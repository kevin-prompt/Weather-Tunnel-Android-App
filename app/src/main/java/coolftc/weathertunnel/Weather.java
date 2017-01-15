package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class Weather extends AppCompatActivity {
    private Status data = new Status();
    private String recID = LOC_CURRENT_ID;
    private boolean alert = false;    
    private int lastTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState); 
        if(savedInstanceState != null){
            lastTab = savedInstanceState.getInt(TAB_ID_SHOWING);
        }
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            alert = extras.getBoolean(IN_ALERT_FLG);
            recID = extras.getString(IN_CITY_ID);
        }

        Resources res = getResources();
        ViewPager viewPager = new ViewPager(this);
        viewPager.setId(R.id.pager);
        setContentView(viewPager);
        final ActionBar bar =  getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayShowTitleEnabled(true);

        TabsView tabsView = new TabsView(this, viewPager);
        tabsView.addTab(bar.newTab().setText(res.getString(R.string.cur_name)), WeatherCurrent.class, null);
        tabsView.addTab(bar.newTab().setText(res.getString(R.string.for_name)), WeatherForecast.class, null);
        
     /**
      * To talk to the specific tabs created use the following line of code as an example:
      *   WeatherCurrent tabCurrent = (WeatherCurrent) getSupportFragmentManager().findFragmentByTag(TAB_ID_CURRENT);
      * To have the tab talk to the Activity, simply cast to the Activity's class:
      *   ((Weather)getActivity()).getData()
      */
        // The Current Conditions tab does work required by the Forecast, so always default to run it.
        bar.setSelectedNavigationItem(lastTab); 
    } 
     
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.properties_detail, menu);
        menu.findItem(R.id.mnuDetailAlert).setVisible(alert);
        return true;
    }

    @Override 
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) { 
        case android.R.id.home: // Back button
            Intent intent = new Intent(this, Slate.class); 
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
            startActivity(intent);
            finish();
            return true;
        case R.id.mnuDetailAlert:
            if(data.ALData.size()==0) return true; // if menu item accidentally displayed.
            Intent localAct = new Intent(this, Alert.class);
            localAct.putExtra(IN_ALERT_ID, data.ALData);
            localAct.putExtra(IN_ALERT_LOC, data.City + ", " + data.State);
            localAct.putExtra(IN_ALERT_SPC, data.Special);
            localAct.putExtra(IN_ALERT_TZ, data.TimeZone);
            startActivity(localAct);
            return true;
        case R.id.mnuDetailMap:
            String loc = data.Special;
            String tLat = data.Latitude;
            String tLon = data.Longitude;
            if(loc.length() == 0){ loc = "Location"; }  // map does not like empty marker
            loc = loc.replace("(", ""); // map does not like extra parenthesis
            loc = loc.replace(")", ""); // map does not like extra parenthesis
            String query = "geo:0,0?q=" + tLat + "," + tLon + " (" + loc + ")";
            Intent intentMap = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(query));
            if(isThereIntent(this, intentMap)) startActivity(intentMap);
            return true;
        case R.id.mnuDetailRefresh:
            Intent localRef = new Intent(this, Weather.class);
            localRef.putExtra(IN_CITY_ID, recID);
            localRef.putExtra(IN_AM_WIDGET, false);
            localRef.putExtra(IN_ALERT_FLG, alert);
            localRef.putExtra(IN_FORCE_UPDT, true);
            startActivity(localRef);
            this.finish();
            return true;
        default: 
            return super.onOptionsItemSelected(item); 
        } 
    } 
     
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, Slate.class); 
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
        startActivity(intent);
        finish();
    }

    @Override 
    protected void onSaveInstanceState(Bundle outState) { 
        super.onSaveInstanceState(outState);
        // If you need to remember anything, put it in the outState
        outState.putInt(TAB_ID_SHOWING, getSupportActionBar().getSelectedNavigationIndex()); 
    } 
     
    public Status getData() {
        return data;
    }

    public void setData(Status data) {
        this.data = data;
    }

    /**
     * One little hack is that the title and description are lifted off the row directly.  Building the rows
     * is a little bit of a pain, so this is simple way to avoid redoing that logic.  I say a small hack 
     * because the rowDescDay is a hidden field on the GUI.
     */
    public void showVerboseOnClick(View view){
        switch (view.getId()){
        case R.id.rowfItem:
            TextView rowDay = (TextView)view.findViewById(R.id.rowfTitleDay);
            TextView rowDesc = (TextView)view.findViewById(R.id.rowfDescDay);
            Intent localAct = new Intent(this, ForecastDay.class);
            localAct.putExtra(IN_FORED_TITLE, rowDay.getText());
            localAct.putExtra(IN_FORED_DESC, rowDesc.getText());
            startActivity(localAct);
        }
    }

    /**
     * This will try to launch a map app to display where the WT thinks the weather is coming from.
     * I have really only tested this against Google Maps.
     */
    public void mapLocationOnClick(View view){
        switch (view.getId()) {
        case R.id.txtCity:
            String loc = data.Special;
            String tLat = data.Latitude;
            String tLon = data.Longitude;
            if(loc.length() == 0){ loc = "Location"; }  // map does not like empty marker
            loc = loc.replace("(", ""); // map does not like extra parenthesis
            loc = loc.replace(")", ""); // map does not like extra parenthesis
            
            String query = "geo:0,0?q=" + tLat + "," + tLon + " (" + loc + ")";
            Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(query));
            if(isThereIntent(this, intent)) startActivity(intent);
            break;
        }
    }
    
    /**
     * Check if the external application of the targeted intent exists.
     */
    public static boolean isThereIntent(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> listInfo = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return listInfo.size() > 0;
    }    
    
    public static class TabsView extends FragmentPagerAdapter implements ActionBar.TabListener, ViewPager.OnPageChangeListener {

        private final Context context;
        private final ActionBar bar;
        private final ViewPager viewPager;
        private final ArrayList<TabInfo> tabs = new ArrayList<>();

        static final class TabInfo {
            private final Class<?> cIn;
            private final Bundle parms;

            TabInfo(Class<?> name, Bundle data) {
                cIn = name;
                parms = data;
            }
        }

        TabsView(AppCompatActivity activity, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            context = activity;
            bar = activity.getSupportActionBar();
            viewPager = pager;
            viewPager.setAdapter(this);
            viewPager.setOnPageChangeListener(this);
        }

        void addTab(ActionBar.Tab tab, Class<?> cIn, Bundle args) {
            TabInfo info = new TabInfo(cIn, args);
            tab.setTag(info);
            tab.setTabListener(this);
            tabs.add(info);
            bar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            bar.setSelectedNavigationItem(position);
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            viewPager.setCurrentItem(tab.getPosition());
            Object tag = tab.getTag();
            for (int i=0; i<tabs.size(); i++) {
                if (tabs.get(i) == tag) {
                    viewPager.setCurrentItem(i);
                }
            }
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }

        @Override
        public Fragment getItem(int ali) {
            TabInfo info = tabs.get(ali);
            return Fragment.instantiate(context, info.cIn.getName(), info.parms);
        }

        @Override
        public int getCount() {
            return tabs.size();
        }        
    }
}

