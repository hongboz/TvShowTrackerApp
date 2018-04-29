package com.hongbo.projectethereal;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.commons.lang3.text.WordUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;


public class AddShowsActivity extends AppCompatActivity {

    ArrayAdapter<String> adapter;
    static ArrayList<String> showArray, toBeAdded, seriesIds;
    SparseBooleanArray clickedItemPositions;
    HashMap<String, String> seriesToId;

    FirebaseDatabase mDatabase;
    DatabaseReference mDataRefFavourites;
    FirebaseAuth auth;

    String uid, seriesName;

    XmlPullParserFactory xmlPullParserFactory;
    XmlPullParser parser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_shows);

        try {
            xmlPullParserFactory = XmlPullParserFactory.newInstance();
            xmlPullParserFactory.setNamespaceAware(false);
            parser = xmlPullParserFactory.newPullParser();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

        final ListView myListView = (ListView) findViewById(R.id.showList);
        Button addButton = (Button) findViewById(R.id.addButton);

        auth = FirebaseAuth.getInstance();

        showArray = new ArrayList<>();
        seriesIds = new ArrayList<>();
        seriesToId = new HashMap<>();
        // Add shows to arraylist
        mDatabase = FirebaseDatabase.getInstance();
        /* mDataRefShows = mDatabase.getReference().child("shows");
        // Displaying all the shows we have in Firebase
        mDataRefShows.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot showSnapshot : dataSnapshot.getChildren()){
                    showArray.add(WordUtils.capitalize(showSnapshot.getKey()));


                }
                adapter.notifyDataSetChanged();
                System.out.println("Successful!");

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Still Error: " + databaseError.getCode());

            }
        }); */

        adapter = new ArrayAdapter<>(AddShowsActivity.this, android.R.layout.simple_list_item_multiple_choice, showArray);

        myListView.setAdapter(adapter);

        // use this array to keep track of what the user wants to add from the entire list
        toBeAdded = new ArrayList<>();

        myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                clickedItemPositions = myListView.getCheckedItemPositions();

                for(int index = 0; index < clickedItemPositions.size(); index++){
                    // Get the checked status of the current item
                    boolean checked = clickedItemPositions.valueAt(index);

                    if(checked){
                        // If the current item is checked
                        int key = clickedItemPositions.keyAt(index);
                        String item = (String) myListView.getItemAtPosition(key);

                        // Adding the selected shows to toBeAdded list if it doesn't already exist
                        if(!toBeAdded.contains(item)){
                            toBeAdded.add(item);
                            Log.i("Added ", item);
                        }
                    }
                    else{
                        // If the current item is unchecked
                        int key = clickedItemPositions.keyAt(index);
                        String item = (String) myListView.getItemAtPosition(key);

                        // Removing the show from toBeAdded if it already exists, this means
                        // the user changed their mind about wanting to add this show
                        if(toBeAdded.contains(item)){
                            toBeAdded.remove(item);
                            Log.i("Removed ", item);
                        }

                    }
                }
            }

        });

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            uid = user.getUid();
            mDataRefFavourites = mDatabase.getReference().child("users").child(uid).child("favourites");
        }

        // when button is click update user's favourites section in firebase and return to main activity
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                for(String show: toBeAdded){
                    // make set value to hashmap with more values later
                    // also if show is already in there we're currently just overwriting it
                    // Making the show names all lowercase before adding to firebase database
                    Log.i("Yes", show + " " + Integer.toString(seriesToId.size()));
                    mDataRefFavourites.child(show.toLowerCase()).setValue(seriesToId.get(show));
                }

                Intent intent = new Intent(AddShowsActivity.this, MainActivity.class);

                startActivity(intent);

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);

        MenuItem item = menu.findItem(R.id.menuSearch);
        SearchView searchView = (SearchView)item.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (showArray.size() != 0 || seriesIds.size() != 0){
                    showArray.clear();
                    seriesIds.clear();
                    adapter.notifyDataSetChanged();
                }
                query = query.replaceAll(" ", "_");
                seriesName = query;
                new SearchSeries().execute();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {return false;}
        });

        return super.onCreateOptionsMenu(menu);
    }

    public class SearchSeries extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... aVoid) {

            URL url = null;
            try {
                url = new URL("http://thetvdb.com/api/GetSeries.php?seriesname=" + seriesName);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection)url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(20000);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.connect();
                InputStream is = conn.getInputStream();
                parser.setInput(is, null);
                getLoadedXmlValues(parser);
            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
            }

            return null;
        }

        private void getLoadedXmlValues(XmlPullParser parser) throws XmlPullParserException, IOException {
            int eventType = parser.getEventType();
            String name;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    name = parser.getName();
                    if (name.equalsIgnoreCase("SeriesName")) {
                        showArray.add(parser.nextText());
                    } else if(name.equalsIgnoreCase("seriesid")){
                        seriesIds.add(parser.nextText());
                    }
                }
                eventType = parser.next();
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            runOnUiThread(new Runnable(){
                public void run(){
                    adapter.notifyDataSetChanged();

                    // Mapping the series to its Id for future use
                    if (seriesToId.size() != 0) {
                        seriesToId.clear();
                    }
                    for(int i = 0; i < showArray.size(); i++){
                        seriesToId.put(showArray.get(i), seriesIds.get(i));
                    }
                }
            });
        }
    }
}
