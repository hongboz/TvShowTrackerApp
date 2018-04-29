package com.hongbo.projectethereal;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class ShowInformationActivity extends AppCompatActivity {

    TextView titleTextView, genreTextView, nextEpisodeTextView, nextEpisodeDateTextView, descriptionTextView;
    Episode nextEpisode = new Episode();

    XmlPullParserFactory xmlPullParserFactory;
    XmlPullParser parser;

    FirebaseDatabase mDatabase;
    DatabaseReference mRef;
    FirebaseUser user;

    static String showName, seriesId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_information);

        titleTextView = (TextView) findViewById(R.id.titleTextView);
        genreTextView = (TextView) findViewById(R.id.genreTextView);
        nextEpisodeTextView = (TextView) findViewById(R.id.nextEpisodeTextView);
        nextEpisodeDateTextView = (TextView) findViewById(R.id.nextEpisodeDateTextView);
        descriptionTextView = (TextView) findViewById(R.id.descriptionTextView);

        Intent intent = getIntent();
        showName = intent.getStringExtra("showName");
        user = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference().child("users").child(user.getUid()).child("favourites")
                .child(showName.toLowerCase());
        mRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                seriesId = dataSnapshot.getValue(String.class);
                Log.i("series", seriesId);
                new LoadSeriesInfo().execute();
                new LoadEpisodeInfo().execute();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        try {
            xmlPullParserFactory = XmlPullParserFactory.newInstance();
            xmlPullParserFactory.setNamespaceAware(false);
            parser = xmlPullParserFactory.newPullParser();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

    }

    public class LoadSeriesInfo extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... aVoid) {

            URL url = null;
            String returnedResult = "";
            try {
                url = new URL("http://thetvdb.com/api/D78CF5ED8AD3A4AD/series/" + seriesId + "/all/en.xml");
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
                returnedResult = getLoadedXmlValues(parser);
            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
            }

            Log.i("Returned Result", returnedResult);
            return null;
        }

        private String getLoadedXmlValues(XmlPullParser parser) throws XmlPullParserException, IOException {
            int eventType = parser.getEventType();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            Date now = null;
            try {
                now = sdf.parse(sdf.format(new Date()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            String name, airDate;
            while (eventType != XmlPullParser.END_DOCUMENT){
                if(eventType == XmlPullParser.START_TAG){
                    name = parser.getName();
                    if (name.equalsIgnoreCase("id")) {
                        nextEpisode.setEpisodeId(parser.nextText());
                    } else if (name.equalsIgnoreCase("FirstAired")) {
                        try {
                            airDate = parser.nextText();
                            airDate = airDate.replaceAll("-","/");
                            if(now.before(sdf.parse(airDate))){
                                break;
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else if(name.equalsIgnoreCase("genre")){
                        nextEpisode.setGenre(parser.nextText());
                    }
                }
                eventType = parser.next();
            }
            return nextEpisode.getEpisodeId();
        }

    }

    public class LoadEpisodeInfo extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            URL url = null;
            try {
                url = new URL("http://thetvdb.com/api/D78CF5ED8AD3A4AD/episodes/"+ nextEpisode.getEpisodeId() +"/en.xml");
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
                getLoadedEpisodeXmlValues(parser);
            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
            }
            return null;
        }

        private Void getLoadedEpisodeXmlValues(XmlPullParser parser) throws XmlPullParserException, IOException {
            int eventType = parser.getEventType();
            String name;
            while (eventType != XmlPullParser.END_DOCUMENT){
                if(eventType == XmlPullParser.START_TAG){
                    name = parser.getName();
                    if (name.equalsIgnoreCase("id")) {
                        nextEpisode.setEpisodeId(parser.nextText());
                    } else if (name.equalsIgnoreCase("FirstAired")) {
                        nextEpisode.setAirDate(parser.nextText());

                    } else if (name.equalsIgnoreCase("EpisodeName")) {
                        nextEpisode.setEpisodeName(parser.nextText());

                    } else if (name.equalsIgnoreCase("seasonid")) {
                        nextEpisode.setSeasonId(parser.nextText());

                    } else if (name.equalsIgnoreCase("SeasonNumber")) {
                        nextEpisode.setSeasonNumber(parser.nextText());

                    } else if (name.equalsIgnoreCase("EpisodeNumber")) {
                        nextEpisode.setEpisodeNumber(parser.nextText());

                    } else if (name.equalsIgnoreCase("Overview")) {
                        nextEpisode.setOverview(parser.nextText());

                    } else if (name.equalsIgnoreCase("Rating")) {
                        nextEpisode.setRating(parser.nextText());

                    }
                }
                eventType = parser.next();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Intent intent = getIntent();

            titleTextView.setText(intent.getStringExtra("showName"));
            genreTextView.setText(nextEpisode.getGenre());
            nextEpisodeTextView.setText("Episode Name: " + nextEpisode.getEpisodeName() + " " + nextEpisode.getSeasonNumber()
                                        + " " + nextEpisode.getEpisodeNumber()
                                        + " " + nextEpisode.getRating());
            nextEpisodeDateTextView.setText("Airdate: " + nextEpisode.getAirDate());
            descriptionTextView.setText(nextEpisode.getOverview());
        }
    }

}
