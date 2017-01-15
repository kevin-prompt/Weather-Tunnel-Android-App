package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.mobeta.android.dslv.DragSortListView;

/* The Slate class is a List Window that shows all the selected weather sites being followed.
 * From this window, sites can be added or removed.  One can navigate to the Detail page, too.
 */
public class Slate extends AppCompatActivity implements LocationListener {
    // Database Helper Class
    private ClimateDB climate;

    // List view added manually
    ListView mListView;

    // Location Helper Class
    LocationManager whereAmI;
    String providerCoarse = null; 
    String providerFine = null;
    int locationPermissionCheck;

    // Handler used as a timer to trigger updates.
    private Handler hRefresh = new Handler();
    private Integer hRefreshCntr = 0;
    
    // Shared data for Contact Address Dialog
    ArrayList<ContactAddr> addrs = new ArrayList<>(3);
    int contactPermissionCheck;

    // Temporary Manual Sorting
    private volatile boolean manualSort = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up main view and menu.
        setContentView(R.layout.slate);
        registerForContextMenu(getListView());
        DragSortListView lv = (DragSortListView) getListView();
        lv.setDropListener(onDrop);
        lv.setRemoveListener(onRemove);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long id) { onListItemClick(mListView, view,  position, id); } }
        );
        
        // Initially load any data from the Status table.
        climate = new ClimateDB(this);  // Be sure to close this in the onDestroy
        initWatchItem();
        fillSlate(getWatchItem());
        contactPermissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS);

        // Initialize the Location Service. Need to lookup the names of providers, since they may change over time.
        /* Might use this to do a quick location fix
         * lastLocation = locationManager.getLastKnownLocation(provider); 
         * */
        locationPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if(locationPermissionCheck==PackageManager.PERMISSION_GRANTED) {
            whereAmI = (LocationManager) getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            providerCoarse = whereAmI.getBestProvider(criteria, true);
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            providerFine = whereAmI.getBestProvider(criteria, true);
        }

        // EULA & Welcome Screen
        SharedPreferences eulaOK = getSharedPreferences(SP_EULA_OK, Context.MODE_PRIVATE);
        if(eulaOK.getInt(SP_EULA_VER, 0) < SP_EULA_CURRENT){
            Intent intent = new Intent(this, Welcome.class);
            startActivityForResult(intent, KY_WELCOME);
        } else {
            // Do not want to ask for location unless they have already gotten past the welcome screen.
            if(locationPermissionCheck!=PackageManager.PERMISSION_GRANTED && Settings.getCheckLocation(getApplicationContext())) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, SEC_READ_LOCATION);
            }
        }
    }
    
    /*****************************************************************************************
     * Override Virtual Methods Section
     */

    /* Handle any security related callbacks. */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case SEC_READ_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
                    whereAmI = (LocationManager) getSystemService(LOCATION_SERVICE);
                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                    providerCoarse = whereAmI.getBestProvider(criteria, true);
                    criteria.setAccuracy(Criteria.ACCURACY_FINE);
                    providerFine = whereAmI.getBestProvider(criteria, true);
                }
                break;
            case SEC_READ_CONTACTS:
                // If you get here and things are ok, it continue on to select contacts.
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    contactPermissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS);
                    Intent intentAdd = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
                    if(isThereIntent(this, intentAdd)) startActivityForResult(intentAdd, KY_CONTACT);
                }
                break;
        }
    }

    /* Part of Activity life cycle. Start looking for current location. Refresh the list from DB.*/
    @Override
    public void onResume() {
        super.onResume();
        // Refresh the Location
        if(locationPermissionCheck==PackageManager.PERMISSION_GRANTED && Settings.getCheckLocation(getApplicationContext())) {
            try{
                whereAmI.requestLocationUpdates(providerCoarse, 0, 0, this);
                if(Settings.getUseGPSLocation(getApplicationContext())) {
                    whereAmI.requestLocationUpdates(providerFine, 0, 0, this);
                }
            }catch (SecurityException ex) {
                ExpClass.LogEX(ex, this.getClass().getName() + ".onResumeLocation");
                Toast.makeText(getApplicationContext(), R.string.msgNoLocation, Toast.LENGTH_LONG).show();
            }
        }
        // To make things simpler, lets always turn Drag&Drop off, the user can turn it on later.
        DragSortListView lv = (DragSortListView) getListView();
        lv.setDragEnabled(false);
        manualSort = false;
        // Refresh the Ad
        Status place = new Status();
        place.LoadLocation(this, LOC_CURRENT_ID);

        // Refresh the Database
        Intent intent = new Intent(this, PulseStatus.class);
        startService(intent);
        // Refresh the Screen (in 5 seconds)
        hRefresh.postDelayed(rRefresh, 5000);
    }
    
    /* Part of Activity life cycle. Turn off all the location providers. Stop updating the Slate
     * from the database.  Slow down the UpdateStatus service.*/
    @Override
    public void onPause() {
        super.onPause();
        try {
            if(locationPermissionCheck==PackageManager.PERMISSION_GRANTED) { whereAmI.removeUpdates(this); }
        } catch (SecurityException e) { /* No big deal, just need this in case. */ }
        hRefresh.removeCallbacks(rRefresh);
        Intent intentSV = new Intent(this, PulseStatus.class);                  // Update DB
        PendingIntent pIntentSV = PendingIntent.getService(this, 0, intentSV, 0);
        AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
        if(getWidgetCnt() > 0){    // If widgets on desktop, start AlarmManager to keep them up to date.
            ExpClass.LogIN(KEVIN_SPEAKS, "Turning on Alarm from Slate.");
            alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), UPD_DBASE_TM, pIntentSV);
        }else{  // or kill any lingering Alarms
            ExpClass.LogIN(KEVIN_SPEAKS, "Turning off Alarms.");
            alarmMgr.cancel(pIntentSV);
        }
    }
    
   /* Part of Activity life cycle. Cleanup and stop any lingering objects. */
    @Override
    protected void onDestroy() {
        climate.close();
        super.onDestroy();        
    }
    
    /* When the location changes, we update the database with a new lon/lat and change the special message slightly.
     * This different special message will trigger the Service to update the record using the new lon/lat.  This 
     * keeps us from having to do network calls in the main Activity.
     * To save battery life, we also stop looking for the current location once we get it. */
    @Override
    public void onLocationChanged(Location location) {
        try {
            if(locationPermissionCheck==PackageManager.PERMISSION_GRANTED) { whereAmI.removeUpdates(this); }
        } catch (SecurityException e) { /* No big deal, just need this in case. */ }
        if(locationPermissionCheck!=PackageManager.PERMISSION_GRANTED || !Settings.getCheckLocation(getApplicationContext())) { return; }
        
        // Check if we have moved.
        Status holdFirst = new Status();
        holdFirst.LoadLocation(this, LOC_CURRENT_ID);
        Location amHere = new Location(LOC_APP_KEY);
        amHere.setLatitude(holdFirst.getLatitude());
        amHere.setLongitude(holdFirst.getLongitude());
        if(location.distanceTo(amHere) > LOC_MIN_DIST) {
            chgCurrLocation(LOC_CURRENT_ID, String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()), LOC_SPECIAL_UPDT);
        }
        
        // Sending an intent will force a refresh (and update the database with correct data).
        Intent intentSrv = new Intent(this, PulseStatus.class);
        startService(intentSrv);
        hRefresh.postDelayed(rRefresh, 5000);
    }

    @Override
    public void onProviderDisabled(String provider) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    /*
        These technically are not @Override methods, but since this was a conversion from a
        ListActivity, it seems fair to put them here.
    */
    protected ListView getListView() {
        if (mListView == null) {
            mListView = (ListView) findViewById(android.R.id.list);
        }
        return mListView;
    }

    protected void setListAdapter(ListAdapter adapter) {
        getListView().setAdapter(adapter);
    }

    protected ListAdapter getListAdapter() {
        ListAdapter adapter = getListView().getAdapter();
        if (adapter instanceof HeaderViewListAdapter) {
            return ((HeaderViewListAdapter)adapter).getWrappedAdapter();
        } else {
            return adapter;
        }
    }
    
    /*****************************************************************************************
     * GUI Elements Section
     */

    /* This event handler allows the Details Activity to launch when they touch a specific item. */
    public void onListItemClick(ListView list, View view, int pos, long id){
        // Don't bother searching if the network is down.
        ConnectivityManager net = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo priNet = net.getActiveNetworkInfo();
        if(priNet==null || !priNet.isConnected()) {
            Toast.makeText(getApplicationContext(), R.string.msgNoNet, Toast.LENGTH_LONG).show();
            return;
        }
        TextView rowId = (TextView)view.findViewById(R.id.row_Id);
        ImageView alertf = (ImageView)view.findViewById(R.id.rowWarning);
        Intent localAct = new Intent(this, Weather.class);
        localAct.putExtra(IN_CITY_ID, rowId.getText());
        localAct.putExtra(IN_AM_WIDGET, false);
        localAct.putExtra(IN_ALERT_FLG, (alertf.getVisibility()==View.VISIBLE));
        startActivity(localAct);
    }
    
    /* This method processes any Activity responses.  Generally this is when some page has been navigated to, 
     * and upon its return, if it needs to send back data, it will exercise this call back. You probably want
     * to reload the Slate at that point. */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case KY_CITIES:     // Returning from the city list picker.
                if (resultCode == RESULT_OK) {
                    if (data.getExtras().getString(IN_CITY_TYPE).equalsIgnoreCase(LOC_TYPE_PREPAQ)) {
                        checkCity(data.getExtras().getString(IN_LINK), data.getExtras().getString(IN_CITY), "", LOC_TYPE_EXACT);
                    } else {
                        checkCity(data.getExtras().getString(IN_CITY), "", data.getExtras().getString(IN_LINK), LOC_TYPE_GENERAL);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), R.string.msgNotFound, Toast.LENGTH_SHORT).show();
                }
                break;
            case KY_CONTACT:    // Returning from the contacts list picker which is a Content Provider.
                if (resultCode == RESULT_OK) {
                    Cursor contact = null;
                    try {
                        contact = getContentResolver().query(data.getData(), new String[]{Contacts.DISPLAY_NAME, Contacts._ID}, null, null, null);
                        if (contact!=null && contact.moveToFirst()) { // True if the cursor is not empty
                            fillContactAddr(contact.getString(contact.getColumnIndex(Contacts.DISPLAY_NAME)), contact.getString(contact.getColumnIndex(Contacts._ID)));
                        }
                        if (contact!=null) contact.close();
                    } catch (Exception ex) {
                        ExpClass.LogEX(ex, this.getClass().getName() + ".onActivityResult");
                        Toast.makeText(getApplicationContext(), R.string.msgNotFound, Toast.LENGTH_SHORT).show();
                        if (contact != null) contact.close();
                    }
                }
                break;
            case KY_WELCOME:
                // The EULA value should have been updated, exit if it has not (been agreed to).
                SharedPreferences eulaOK = getSharedPreferences(SP_EULA_OK, Context.MODE_PRIVATE);
                if (eulaOK.getInt(SP_EULA_VER, 0) < SP_EULA_CURRENT) {
                    finish();
                }
                break;
        }
    }
    
    /* This method handles the search dialog results. I guess it needs its own unique call back, instead of 
     * just using the onActivityResult().*/
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        /* 
         * The "Action_Search" means the person typed it in directly.  We handle the prepackaged selections differently. 
         * Zip codes: Add special message.  This will make it part of the display, which helps when looking at the list 
         * because some cities have multiple zip codes, e.g. San Francisco zip codes have different temperatures.
         * Pws codes: Add special message. The person weather station needs to stand out, since it is probably owned by
         * the user.  This also helps get the right data, since later it will be forced into service as the data source.  
         */
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String msg = "";
            String lnk = "";
            String src = intent.getStringExtra(SearchManager.QUERY).trim();
            if(isZipCode(src)) { msg = "zip code: " + src; }
            if(isPwsCode(src)) { msg = "Station " + src.toLowerCase(Locale.US); lnk = src; }
            checkCity(src, msg, lnk, LOC_TYPE_GENERAL);
        }
        /*
         *  The Action View means the person picked it from the prepackaged selection list. In this case the db ID is returned 
         *  and the true text needs to be looked up. Once an item is selected, the selected text becomes the Special message.
         */
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            try{
                Uri suggestUri = Uri.withAppendedPath(Suggestion.CONTENT_URI, intent.getDataString());
                Cursor data = managedQuery(suggestUri, null, null, null, null);
                if (data.moveToFirst()) { 
                    checkCity(data.getString(data.getColumnIndex(SuggestionDB.SUGGEST_SUPPORT)), data.getString(data.getColumnIndex(SuggestionDB.SUGGEST_TEXT)), "", LOC_TYPE_PREPAQ); 
                }
            }catch (Exception ex) {ExpClass.LogEX(ex, this.getClass().getName() + ".onNewIntent");}
        }
    }

    /*****************************************************************************************
     * Dialogs, Options & Context Menus Section
     */

    /* Dialogs can be used for multiple purposes.  I do not like their look much, but easier than creating
     * an activity for some things.  The onCreate is called when the program calls showDialog().  
     * Note: Dialogs are cached and so kind of a pain with dynamic data (unless you removeDialog() each time).*/
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_PICK_SUB_CONTACTS:  // Dialog to show/choose addresses from a single contact
            try {
            CharSequence[] display = new CharSequence[addrs.size()];
            boolean[] checked = new boolean[addrs.size()];
            for(int i = 0; i < addrs.size(); ++i){
                display[i] = addrs.get(i).getItemDisplay();
                if(addrs.get(i).getAddrFull().length() > 0){
                    checked[i] = true;
                }else{
                    addrs.get(i).setSelected(false);    // if it is not selected, lets not use it
                }
            }
            return new AlertDialog.Builder(this)
                .setTitle(R.string.dlgPickAddr)
                .setMultiChoiceItems(display, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
                        addrs.get(whichButton).setSelected(isChecked);
                    }})
                .setPositiveButton(R.string.dlgDone, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* Take the selected items and add them to the Slate */
                        for(int i = 0; i < addrs.size(); ++i){
                            if(addrs.get(i).isSelected()){
                                checkCity(addrs.get(i).getAddressClean(), 
                                          addrs.get(i).getNameDisplay(), 
                                          addrs.get(i).getPostalOrCityLink(), 
                                          LOC_TYPE_GEOCODE);
                            }
                        }
                        removeDialog(DIALOG_PICK_SUB_CONTACTS); // This is a rare dialog, so just kill it.
                    }               
                })
                .create();
            } catch (Exception ex) { 
                ExpClass.LogEX(ex, this.getClass().getName() + ".onCreateDialog"); 
                Toast.makeText(getApplicationContext(), R.string.msgCityNotFound, Toast.LENGTH_LONG).show(); 
            }
        }
        return null;
    }
    
    /* The Context menu for the list of locations is used primarily to allow deletes.  Can also
     * navigate to the Details window.  The system will call onCreate when user does long press. */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_slate, menu);
    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        TextView me = (TextView)info.targetView.findViewById(R.id.row_Id);
        switch (item.getItemId()) {
        case R.id.mnuSlateDelete:
            delWatchItem(me.getText().toString());
            fillSlate(getWatchItem());
            return true;
        case R.id.mnuSlateDetail:
            Intent localAct = new Intent(this, Weather.class);
            localAct.putExtra(IN_CITY_ID, me.getText().toString());
            startActivity(localAct);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }
    
    /* The Options Menu now works closely with the ActionBar.  It can show useful menu items on the bar
     * while hiding less used ones on the traditional menu.  The xml configuration determines how they
     * are shown. The system will call the onCreate when the user presses the menu button. 
     * Note: Android refuses to show icon+text on the ActionBar in portrait, so deal with it. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.properties_slate, menu);    
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection    
        switch (item.getItemId()) {    
        case R.id.mnuSlateAddCities:
        case R.id.mnuSlateAddPlace:
            onSearchRequested();
            return true;
        case R.id.mnuSlateAddContact:
            if (contactPermissionCheck == PackageManager.PERMISSION_GRANTED) {
                Intent intentAdd = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
                if(isThereIntent(this, intentAdd)) startActivityForResult(intentAdd, KY_CONTACT);
            } else {
                // If permission has not been granted, ask for it.  Continue to display what you have.
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_CONTACTS}, SEC_READ_CONTACTS);
            }
            return true;
        case R.id.mnuSlateSort:
            manualSort = !manualSort;
            DragSortListView lv = (DragSortListView) getListView();
            lv.setDragEnabled(manualSort);
            fillSlate(getWatchItem());
            if(manualSort) Toast.makeText(getApplicationContext(), R.string.msgHowToSort, Toast.LENGTH_SHORT).show();
            return true;
        case R.id.mnuSlateSettings:
            startActivity(new Intent(this, Settings.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /*****************************************************************************************
     * Private Helper Functions
     */
    
    // Add a row to the Status Table.  Return -1 if it fails.
    private long newWatchItem(Status item){
    try{
        int endRecCnt = getLastUserCnt() + 1;
        SQLiteDatabase db = climate.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ClimateDB.STATUS_ORDER, endRecCnt);
        values.put(ClimateDB.STATUS_STAT_PUB, item.PublicCode);
        values.put(ClimateDB.STATUS_STAT_PRI, item.PrivateCode);
        values.put(ClimateDB.STATUS_STAT_PRI2, item.PrivateCodeB);
        values.put(ClimateDB.STATUS_DIST_PUB, item.getPublicDist());
        values.put(ClimateDB.STATUS_DIST_PRI, item.getPrivateDist());
        values.put(ClimateDB.STATUS_DIST_PRI2, item.getPrivateDistB());
        values.put(ClimateDB.STATUS_ZIP, item.Zip);
        values.put(ClimateDB.STATUS_CITY, item.City);
        values.put(ClimateDB.STATUS_STATE, item.State.length()==0?item.Country:item.State); // non-US has no state
        values.put(ClimateDB.STATUS_CNTY, item.Country);
        values.put(ClimateDB.STATUS_LATI, item.Latitude);
        values.put(ClimateDB.STATUS_LONG, item.Longitude);
        values.put(ClimateDB.STATUS_TZONE, item.TimeZone);
        values.put(ClimateDB.STATUS_TIME, item.LocalTime);
        values.put(ClimateDB.STATUS_TEMP, item.getTemperature());
        values.put(ClimateDB.STATUS_NOTE, item.Special);
        values.put(ClimateDB.STATUS_COND, item.Condition);
        values.put(ClimateDB.STATUS_CICON, item.ConditionIcon);
        values.put(ClimateDB.STATUS_ALERT, item.IsAlert);
        values.put(ClimateDB.STATUS_ONSLATE, item.IsSlate);
        values.put(ClimateDB.STATUS_ONWIDGET, item.IsWidget);
        return db.insert(ClimateDB.STATUS_TABLE, null, values);
    }catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".newWatchItem"); return -1; }
    }
    
    /* Update the location for an item. */
    private long chgCurrLocation(String id, String longitude, String latitude, String msg) {
    try{
        SQLiteDatabase db = climate.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ClimateDB.STATUS_LATI, longitude);
        values.put(ClimateDB.STATUS_LONG, latitude);
        values.put(ClimateDB.STATUS_NOTE, msg);
        String where = "_id = " + id;
        String[] filler = {};
        return db.update(ClimateDB.STATUS_TABLE, values, where, filler);
    }catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".chgCurrLocation"); return 0; }   
    }
    
    /* Remove item from being visible on Slate. */
    private long chgOffSlate(String id){
    try{
        SQLiteDatabase db = climate.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ClimateDB.STATUS_ONSLATE, 0);
        String where = "_id = " + id;
        String[] filler = {};
        return db.update(ClimateDB.STATUS_TABLE, values, where, filler);
    }catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".chgOffSlate"); return -1; }   
    }

    /* Returns the list of all watch items. Currently used to populate the listview, so we use the cursor manager.
     * There might be a better way to do this, e.g. create a cursor adaptor or use a different manager (since this
     * one is depreciated).*/    
    private Cursor getWatchItem(){
        SQLiteDatabase db = climate.getReadableDatabase();
        String[] filler = {};
        Cursor cursor = db.rawQuery(DB_StatusSlate, filler);
        startManagingCursor(cursor);
        return cursor;
    }

    // Returns a count of how many watch items exist
    private int getWatchCnt() {
    try{
        int cnt;
        SQLiteDatabase db = climate.getReadableDatabase();
        String[] filler = {};
        Cursor cursor = db.rawQuery(DB_StatusSlate, filler);
        try {
            cnt = cursor.getCount();
            cursor.close();
        } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getWatchCnt"); cursor.close(); cnt = 0; }
        return cnt;
    } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getWatchCnt"); return 0; }
    }

    // Returns a count of how many widgets exist
    private int getWidgetCnt() {
    try{
        int cnt;
        SQLiteDatabase db = climate.getReadableDatabase();
        String[] filler = {};
        Cursor cursor = db.rawQuery(DB_StatusAnyWidget, filler);
        try {
            cnt = cursor.getCount();
            cursor.close();
        } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getWidgetCnt"); cursor.close(); cnt = 0; }
        return cnt;
    }catch(Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getWidgetCnt"); return 0; }
    }

    // Returns the highest usersort value in the database.
    private int getLastUserCnt(){
    try{
        int cnt;
        SQLiteDatabase db = climate.getReadableDatabase();
        String[] filler = {};
        Cursor cursor = db.rawQuery(DB_StatusLastUser, filler);
        try {
            cursor.moveToNext();
            cnt = cursor.getInt(cursor.getColumnIndex(ClimateDB.STATUS_ORDER));
            cursor.close();
        } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getLastUserSort"); cursor.close(); cnt = 0; }
        return cnt;
    } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getLastUserCnt"); return 0; }
    }

    // Returns the id of the of the watch item based on the usersort.
    private int getUserSort(Integer ID){
    try{
        int sortID;
        SQLiteDatabase db = climate.getReadableDatabase();
        String[] filler = {};
        String real = DB_StatusID + ID.toString();
        Cursor cursor = db.rawQuery(real, filler);
        try {
            cursor.moveToNext();
            sortID = cursor.getInt(cursor.getColumnIndex(ClimateDB.STATUS_ORDER));
            cursor.close();
        } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getUserSortID"); cursor.close(); sortID = -1; }
        return sortID;
    } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getUserSort"); return -1; }
    }

    // Changes the user sort key.
    private int chgUserSort(int id, int sort){
    try{
        SQLiteDatabase db = climate.getWritableDatabase();
        String[] filler = {};
        ContentValues values = new ContentValues();
        values.put(ClimateDB.STATUS_ORDER, sort);
        String where = "_id = " + id;
        return db.update(ClimateDB.STATUS_TABLE, values, where, filler);
    } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".chgUserSort"); return 0; }
    }

    // Add Contact information to the Slate.
    private void fillContactAddr(String name, String id){
        Cursor postal = null;
        try {
            if (contactPermissionCheck == PackageManager.PERMISSION_GRANTED) {
                postal = getContentResolver().query(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + " = " + id, null, null);
                int postalCode = postal.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE);
                int cityCode = postal.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY);
                int cntyCode = postal.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY);
                int muniCode = postal.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION);
                int typeCode = postal.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE);
                int typeLabel = postal.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.LABEL);
                int streetfull = postal.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS);
                int cnt = postal.getCount();
                if (cnt == 0) {
                    Toast.makeText(getApplicationContext(), R.string.msgNotFound, Toast.LENGTH_SHORT).show();
                    postal.close();
                    return;
                }

                // Fill out the data and let the Dialog add the selected items.
                addrs.clear();
                while (postal.moveToNext()) {
                    ContactAddr ali = new ContactAddr();
                    ali.setName(name);
                    ali.setLabel(postal.getInt(typeCode) > 0 ? CONTACT_TYPES[postal.getInt(typeCode)] : postal.getString(typeLabel));
                    ali.setAddrFull(postal.getString(streetfull));
                    ali.setPostal(postal.getString(postalCode));
                    ali.setCity(postal.getString(cityCode));
                    ali.setContry(postal.getString(cntyCode));
                    ali.setMuni(postal.getString(muniCode));
                    ali.setSelected(true);
                    addrs.add(ali);
                }
                postal.close();

                // If only one address, just add it.
                if (cnt == 1) {
                    checkCity(addrs.get(0).getAddressClean(), addrs.get(0).getNameDisplay(), addrs.get(0).getPostalOrCityLink(), LOC_TYPE_GEOCODE);
                    return;
                }
                // If more than one, this dialog will use the addrs list, since it is global to the Activity.
                showDialog(DIALOG_PICK_SUB_CONTACTS);
            }
            
        } catch (Exception ex) { 
            ExpClass.LogEX(ex, this.getClass().getName() + ".fillContactAddr"); 
            if(postal!=null) postal.close(); addrs.clear(); 
        }
    }
    
    // Handles the movement of data from the database to the layout.
    private void fillSlate(Cursor cursor){
        if(cursor.getCount() == 0) return; // should not happen
        // SQL Data Binding Map
        String[] StatusMapFROM = {ClimateDB.STATUS_ID, ClimateDB.STATUS_CICON, ClimateDB.STATUS_COND, ClimateDB.STATUS_CITY, ClimateDB.STATUS_STATE, ClimateDB.STATUS_NOTE, ClimateDB.STATUS_TIME, ClimateDB.STATUS_TEMP, ClimateDB.STATUS_ALERT, ClimateDB.STATUS_ALERT};
        int[] StatusMapTO = {R.id.row_Id, R.id.rowCondImg, R.id.rowCondition, R.id.rowCity, R.id.rowState, R.id.rowSpecial, R.id.rowTime, R.id.rowTemperature, R.id.rowWarning, R.id.rowMove};
        // The SQL adapter will bind the database to the layout
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.watch_row, cursor, StatusMapFROM, StatusMapTO);
        /* This method overrides the mapping between database and layout.
         * As always seems to happen with database bindings, most of the fields need a little GUI tweaking.  For our purposes, we need
         * to properly format the date, and substitute an image for the text based condition, add some extra space for the italics, 
         * and allow for a celsius temperature. Note that we cheat on the rowMove, as that is not tied to a database field, but needs
         * to look like it is for this to work.  The rowWarning and rowMove are mutually exclusive, so there is a relationship there.
        */ 
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                boolean rtn = false;
                if (view.getId() == R.id.rowTime) {     // Display the desired date format 
                    try {
                        String dtFmt = Settings.getUse24Clock(getApplicationContext())?DB_fmtDateTime24:DB_fmtDateTime;
                        String tzone = cursor.getString(cursor.getColumnIndex(ClimateDB.STATUS_TZONE));
                        ((TextView) view).setText(KTime.ParseToFormat(cursor.getString(columnIndex), DB_inputDate3339, tzone, dtFmt));
                    } catch (ExpParseToCalendar ex) {
                        ExpClass.LogEX(ex, this.getClass().getName() + ".fillSlate");
                        ((TextView) view).setText(BAD_TIME);
                    }
                    rtn = true;
                }
                if(view.getId() == R.id.rowCondition) { // Sometimes they get excited and put way too much detail in condition text
                    String condDesc = cursor.getString(columnIndex);
                    if(condDesc.length() > 18){
                        condDesc = condDesc.substring(0, 18) + "...";
                    }
                    ((TextView) view).setText(condDesc);
                    rtn = true;
                }
                if(view.getId() == R.id.rowCondImg) {   // Populate the condition graphic based on the condition icon text
                    String cond = cursor.getString(columnIndex);
                    Resources res = getResources();
                    boolean imageOK = false;

                    if(cond.equals(DB_CondClear))    { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.clear)); imageOK = true; }
                    if(cond.equals(DB_CondClearN))   { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.nt_clear)); imageOK = true; }
                    if(cond.equals(DB_CondCloud))    { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.cloudy)); imageOK = true; }
                    if(cond.equals(DB_CondFlurry))   { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.flurries)); imageOK = true; }
                    if(cond.equals(DB_CondFog))      { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.fog)); imageOK = true; }
                    if(cond.equals(DB_CondHazy))     { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.fog)); imageOK = true; }
                    if(cond.equals(DB_CondMostCloud)){ ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.partlycloudy)); imageOK = true; }
                    if(cond.equals(DB_CondMostSun))  { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.partlycloudy)); imageOK = true; }
                    if(cond.equals(DB_CondPartCloud)){ ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.partlycloudy)); imageOK = true; }
                    if(cond.equals(DB_CondPartCloudN)){ ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.nt_partlycloudy)); imageOK = true; }
                    if(cond.equals(DB_CondPartSun))  { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.partlycloudy)); imageOK = true; }
                    if(cond.equals(DB_CondRain))     { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.rain)); imageOK = true; }
                    if(cond.equals(DB_CondSleet))    { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.rain)); imageOK = true; }
                    if(cond.equals(DB_CondSnow))     { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.flurries)); imageOK = true; }
                    if(cond.equals(DB_CondSun))      { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.clear)); imageOK = true; }
                    if(cond.equals(DB_CondThunder))  { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.tstorms)); imageOK = true; }
                    if(cond.equals(DB_CondUnknown))  { ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.partlycloudy)); imageOK = true; }
                    if(!imageOK){ ((ImageView) view).setImageDrawable(res.getDrawable(R.drawable.partlycloudy)); }

                    rtn = true;
                }
                if(view.getId() == R.id.rowCity) {      // The city displayed in italic seems to loose some pixels, so add a space.
                    String cityPlus = cursor.getString(columnIndex) + ", ";
                    ((TextView) view).setText(cityPlus);
                    rtn = true;
                }
                if(view.getId() == R.id.rowState) {      // The state displayed in italic seems to loose some pixels, so add a space.
                    String statePlus = cursor.getString(columnIndex) + " ";
                    ((TextView) view).setText(statePlus);
                    rtn = true;
                }
                if(view.getId() == R.id.rowTemperature) {// The temperature should be formatted like all the other temperatures.
                    ((TextView) view).setText(xtrTemperature(cursor.getString(columnIndex), TMP_ROUND_WHOLE));
                    rtn = true;
                }
                if(view.getId() == R.id.rowWarning) {   //  The alert icon is displayed if alerts exist for this location.
                    if(cursor.getInt(columnIndex) == 1 && !manualSort) {
                        view.setVisibility(View.VISIBLE);
                    }else{
                        view.setVisibility(View.GONE);
                    }
                    rtn = true;
                }
                if(view.getId() == R.id.rowMove) {      // The move icon is displayed if manual drag and drop is enabled.
                    if(manualSort) {
                        view.setVisibility(View.VISIBLE);
                    }else{
                        view.setVisibility(View.GONE);
                    }
                    rtn = true;
                }
               return rtn;
            }
            private String xtrTemperature(String tmp, int precision){
                String outFrmt = "";
                int precisionNbr = 0;
                double holdTemp;
                
                try { holdTemp = Double.parseDouble(tmp); } 
                catch (NumberFormatException ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".fillSlate_xtrTemperature"); holdTemp = 0; }
                if(Settings.getUseCelsius(getApplicationContext())) { 
                    // Rounding trick: Math.floor(value * X + .5) X = power of 10 (precision where 10=.1, 100=.01, etc.).
                    holdTemp = Math.floor(((holdTemp - 32) * 5 / 9) * precision + 0.5) / precision;
                } else {
                    holdTemp = Math.floor(holdTemp * precision + 0.5) / precision;
                }
                for(int place = precision;  place > 1; place /= 10) { precisionNbr++;  }
                outFrmt += "%1." + precisionNbr + "f°";
                return String.format(outFrmt, holdTemp);
            }
            
        });
        // Remember where top of list is located.
        int topNdx = getListView().getFirstVisiblePosition();
        View firstLine = getListView().getChildAt(0);
        int topPos = 0;
        if (firstLine != null) topPos = firstLine.getTop();
        
        // Apply the mapping and reposition the list.
        setListAdapter(adapter);
        getListView().setSelectionFromTop(topNdx, topPos);
        
        // Update all the Widgets too.
        try{
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            SharedPreferences widgetMap = getSharedPreferences(SP_WIDGET_MAP, Context.MODE_PRIVATE);
            Map<String, ?> prefList = widgetMap.getAll();
            for (Entry<String, ?> entry : prefList.entrySet()) {
                int widgetId = Integer.parseInt(entry.getKey());
                CharSequence dbid = (String)entry.getValue();
                QuickView.updateAppWidget(this, appWidgetManager, widgetId, dbid);
            } 
        } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".fillSlate_UpdWidgets"); }
    }

    /*
     * The DragSortListView (DSLV) is used to allow drag & drop on a listbox.  While it will move
     * around the visual elements, the underlying database must also be changed for this to work.
     * There is a dedicated field (usersort) in the database used to track sort order, it is that
     * value that needs to change when a drag & drop occurs.  Since the listbox is small (max 25 
     * rows), we will take the simple approach of doing a "bubble" reorder of the item.  Not very
     * efficient but very simple in code and quit fast enough in this scenrio. 
     */
    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            try {
                if(from == to) return;
                int fromID = getIDbyListNbr(from);
                
                if(from < to){ // bubble-up
                    for(int i = from + 1; i <= to; ++i) {
                        int nextID = getIDbyListNbr(i);
                        if(!swapUserSort(fromID, nextID)) break;
                    }
                } else { // bubble-down
                    for(int i = from - 1; i >= to; --i) {
                        int nextID = getIDbyListNbr(i);
                        if(!swapUserSort(fromID, nextID)) break;
                    }
                }
                fillSlate(getWatchItem());
            } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".onDrop"); }
        }
    };
    
    /*
     * The delete of a row is considerably easier than moving it.  Just call delete.
     */
    private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
        @Override
        public void remove(int which) {
            Integer whichID = getIDbyListNbr(which);
            delWatchItem(whichID.toString());
            fillSlate(getWatchItem());
        }
    };

    /*
     * This is the only reliable way to know what db id is associated with an watch row.
     */
    private int getIDbyListNbr(int listNbr) {
        SQLiteDatabase db = climate.getReadableDatabase();
        String[] filler = {};
        int rowID;
        Cursor cursor = db.rawQuery(DB_StatusSlate, filler);
        try {
            cursor.moveToPosition(listNbr);
            rowID = cursor.getInt(cursor.getColumnIndexOrThrow(ClimateDB.STATUS_ID));
            cursor.close();
        } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".getIDbyListNbr"); cursor.close(); rowID = -1; }
        return rowID;
    }
    
    /*
     * This will swap the order in the listview between the two rows, based on the usersort value.
     */
    private boolean swapUserSort(Integer from, Integer to) {
        int fromSort = getUserSort(from);
        int toSort = getUserSort(to);
        return !(fromSort < 0 || toSort <0) && !(chgUserSort(from, toSort) == 0 || chgUserSort(to, fromSort) == 0);
    }
            
    /* Delete one watch item based on id.  The special id=1 cannot be deleted, but can be hidden.
     * If an item on the Slate is currently being used as a Widget, we do not want
     * to delete it, just change the isSlate status to false. 
     * While we are hear, lets also see if any orphaned records need to be deleted. */
    private void delWatchItem(String id) {
    try{
        boolean okToDelete = true;
        
        // Some delete requests really should only remove the item from the Slate list, not really delete.
        // Special Case: The first item should never be deleted.
        Status holdFirst = new Status();
        holdFirst.LoadLocation(this, id);
        if(id.equalsIgnoreCase(LOC_CURRENT_ID) || holdFirst.IsWidget == ClimateDB.SQLITE_TRUE){  // Widget exists, just change onSlate status.
            chgOffSlate(id);
            okToDelete = false;
        }
        
        // To allow the first item (current location) to return, the Current Location checkbox in Settings can be checked.
        // For consistency then, we need to uncheck it here.
        if(id.equalsIgnoreCase(LOC_CURRENT_ID)) {
            Settings.setCheckLocation(this, false);
        }
        
        // Got any orphans records we might want to delete?
        ArrayList<String> deletelist = new ArrayList<>();
        SQLiteDatabase dbOrphans = climate.getReadableDatabase();
        String[] filler = {};
        Cursor orphanCursor = dbOrphans.rawQuery(DB_StatusOrphans, filler);
        try{
            while(orphanCursor.moveToNext()){
                deletelist.add(String.valueOf(orphanCursor.getInt(orphanCursor.getColumnIndex(ClimateDB.STATUS_ID))));
            }
            orphanCursor.close();
        } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".delWatchItemB"); orphanCursor.close(); }
        
        // Add the one we originally wanted to delete.
        if(okToDelete) deletelist.add(id);
         
        // Delete the records
        SQLiteDatabase dbDelete = climate.getWritableDatabase();
        for(int i = 0; i < deletelist.size(); ++i){
            String where = "_ID=" + deletelist.get(i);
            dbDelete.delete(ClimateDB.STATUS_TABLE, where, null);
        }
    } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".delWatchItemC"); }
    }     

    /**
     * This will initialize the database upon first run.  The first record is the current location.
     * Note: If the database becomes large, this should be moved to an ansync task to avoid possible
     * long waits on the main thread.
     */
    private void initWatchItem() {
        Cursor cursor = null;
        try{
            SQLiteDatabase db = climate.getReadableDatabase();
            String[] filler = {};
            cursor = db.rawQuery(DB_StatusAll, filler);
            if(cursor.getCount() == 0){ // when db empty, add in the current location database record.
                Status seed = new Status();
                seed.SetDefault();
                newWatchItem(seed);
            }
            cursor.close();
        } catch (Exception ex){ ExpClass.LogEX(ex, this.getClass().getName() + ".initWatchItem"); if (cursor!=null) cursor.close(); }
    }
    
    // Confirm the string conforms to zip code standards. 
    // Since leading zeros break the string-number-string conversion, convert them to 1 first,
    // not a big deal, since we just want to verify this is a 5 digit number.
    private boolean isZipCode(String src) {
    try {
        src = src.replace('0', '1');
        String src2 = String.valueOf(Integer.parseInt(src));
        return src.length()==5 && src.equalsIgnoreCase(src2);
    } catch (Exception ex) { 
        /* Not logging this because both numeric and non-numeric data is sent as an expected condition.
         * ExpClass.LogEX(ex, this.getClass().getName() + ".isZipCode");*/
         return false; }
    }

    // Confirm the string conforms to the personal weather station standard.
    private boolean isPwsCode(String src) {
    try{
        return src.length()!=0 && src.trim().toLowerCase(Locale.US).substring(0, 4).equalsIgnoreCase(PWS_CODEKEY);
    } catch (Exception ex) { /* nothing really to do here. */ return false; }
    }

    // Check if the external application of the targeted intent exists.
    public static boolean isThereIntent(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> listInfo = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return listInfo.size() > 0;
    }    
    
    /* This will perform a search on the desired query.  If the search returns more than one result, 
     * another Activity will launch to allow selection from the list.  If nothing is returned, then 
     * the query could not be resolved.  If a single city is returned, it is added to the database.
     * 
     * NOTE: All of the work is done in the AsyncTask, which off loads the network calls to a thread.
     */
    private void checkCity(String query, String msg, String link, String type){
    try {
   
        new StatusSearchTask(this, getResources().getString(R.string.app_name)).execute(query, link, type, msg);
        
    } catch (Exception ex) {
        ExpClass.LogEX(ex, this.getClass().getName() + ".checkCity");
        Toast.makeText(getApplicationContext(), R.string.msgCityNotFound, Toast.LENGTH_LONG).show(); }
    }

    /*
     * The nested AsyncTask class is used to off-load the network call to a separate thread.
     */
    private class StatusSearchTask extends AsyncTask<String, Boolean, Status> {
        private ProgressDialog progressDialog;
        private Context context;
        private String title;

        private StatusSearchTask(Activity activity, String name){
            context = activity;
            title = name;
        }

        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context, title, "Searching...", true, true);
            progressDialog.setOnDismissListener(new OnDismissListener() { @Override public void onDismiss(DialogInterface dialog) { cancel(true); } });
        }

        protected void onPostExecute(coolftc.weathertunnel.Status result) {
        try {
            progressDialog.dismiss();
            
            switch(result.getCityListCnt()) {
            case 0:
                Toast.makeText(getApplicationContext(), R.string.msgCityNotFound, Toast.LENGTH_LONG).show();
                break;
            case 1:
                if(getWatchCnt() > DB_MAX_ONSLATE) {    // too many on slate
                    Toast.makeText(getApplicationContext(), R.string.msgMaxSlate, Toast.LENGTH_LONG).show();
                }else{
                    if(newWatchItem(result) >= 0){
                        Toast.makeText(getApplicationContext(), R.string.msgFound, Toast.LENGTH_SHORT).show();
                        fillSlate(getWatchItem());
                    }
                }
                break;
            default:
                Intent chooseCity = new Intent(context, coolftc.weathertunnel.Cities.class);
                chooseCity.putExtra(IN_CITY_TYPE, result.getCityListType());
                chooseCity.putStringArrayListExtra(IN_CITIES, result.getCityList());
                startActivityForResult(chooseCity, KY_CITIES);
                break;
            }
        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".onPostExecute");
            Toast.makeText(getApplicationContext(), R.string.msgCityNotFound, Toast.LENGTH_LONG).show(); }
        }

        protected void onProgressUpdate(Boolean... values) {
            if(!values[0]) Toast.makeText(getApplicationContext(), R.string.msgNoNet, Toast.LENGTH_LONG).show();
        }

        protected void onCancelled() {
            Toast.makeText(context, R.string.msgUserCancel, Toast.LENGTH_LONG).show();
        }

        protected coolftc.weathertunnel.Status doInBackground(String... criteria) {
        try {
            coolftc.weathertunnel.Status place = new coolftc.weathertunnel.Status();
            String where = criteria[0]; String link = criteria[1]; String type = criteria[2]; String msg = criteria[3];

            ConnectivityManager net = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo priNet = net.getActiveNetworkInfo();
            if(priNet==null || !priNet.isConnected()) {
                publishProgress(false);
            } else {
                place.Search(getApplicationContext(), where, link, type);
                if(msg.length() > 0) place.Special = msg;
            }
            return place;
            
        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".doInBackground");
            publishProgress(false);
            return new coolftc.weathertunnel.Status();}
        }
    }

    // Non-Thread Timer used to periodically refresh list. The PulseStatus Service actually updates the DB
    // with new weather data every n minutes. This just updates the screen with data from the DB.
    // For the first 5 iterations we want to use the faster refresh rate of TQ.  This should cover
    // the time a person would actually be looking at the screen, then back off to save battery.
    private Runnable rRefresh = new Runnable() {
        public void run() {

            fillSlate(getWatchItem());
            hRefreshCntr += UPD_SCREEN_TQ;
            if(hRefreshCntr < UPD_SCREEN_TQ*5){
                hRefresh.postDelayed(this, UPD_SCREEN_TQ);
            } else {
                hRefresh.postDelayed(this, UPD_SCREEN_TM);
            }
        }

    };    
    
}
