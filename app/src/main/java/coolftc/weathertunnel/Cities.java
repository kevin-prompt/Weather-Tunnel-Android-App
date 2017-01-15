package coolftc.weathertunnel;

import static coolftc.weathertunnel.Constants.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/*********************************************************************************
 * This Activity is used when there is some ambiguity in the search.  For example,
 * if the city or country name entered might apply to more than one location.  Typical
 * examples would be the name of a state or country, which would display a list of 
 * possible cities.
 * Some items require more than the display name to determine what they are, so a link
 * is also provided as part of the input bundle.  If a person selects an item, the link
 * is also transfered back to help the search class find it. Using an intent to pass the
 * data requires the display/link information be compacted, then expanded here for ease
 * of use.  That is the reason for the creation of the special ArrayList and Map.
 */
public class Cities extends ListActivity {
    private String choice = "";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            final String listType = extras.getString(IN_CITY_TYPE);
            // Parse the incoming DisplayName?LinkName into a ArraryList<DisplayName> and Map<DisplayName,LinkName>.
            ArrayList<String> cityList = new ArrayList<>();
            final Map<String, String> cityMap = new TreeMap<>();
            try {
                for (int i = 0; i < extras.getStringArrayList(IN_CITIES).size(); ++i) {
                    String display = extras.getStringArrayList(IN_CITIES).get(i).substring(0, extras.getStringArrayList(IN_CITIES).get(i).indexOf(DELIMIT_SPLIT));
                    String link = extras.getStringArrayList(IN_CITIES).get(i).substring(extras.getStringArrayList(IN_CITIES).get(i).indexOf(DELIMIT_SPLIT) + 1);
                    cityList.add(display);
                    cityMap.put(display, link);
                }
            } catch (NullPointerException ex) { /* live with it */ }
            setListAdapter(new ArrayAdapter<String>(this, R.layout.city_row, cityList));
            ListView lv = getListView();
            lv.setTextFilterEnabled(true);
            lv.setOnItemClickListener(new OnItemClickListener() {
                // When someone selects an item, return it back to the caller.
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    choice = (String) ((TextView) view).getText();
                    if(choice.length() > 0){
                        Intent rtn = new Intent(IN_CITIES);
                        rtn.putExtra(IN_CITY, choice);
                        rtn.putExtra(IN_LINK, cityMap.get(choice));
                        rtn.putExtra(IN_CITY_TYPE, listType);
                        setResult(RESULT_OK, rtn);
                    }
                    finish();
                }  
            });
        }
    }    
 }
