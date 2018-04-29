package com.hongbo.projectethereal;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public String uid,email,name;
    ArrayList<String> movies;
    ArrayAdapter<String> arrayAdapter;

    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseAuth auth;
    private FirebaseDatabase mDatabase;
    private DatabaseReference rootRef, favouritesRef;

    public static final int RC_SIGN_IN = 1;

    Button addShowsButton, signOutButton;

    int moviesId;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get firebase auth instance
        auth = FirebaseAuth.getInstance();

        //get current user
        final FirebaseUser user = auth.getCurrentUser();
        // getting a reference to the database
        mDatabase = FirebaseDatabase.getInstance();
        rootRef = mDatabase.getReference();

        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                final FirebaseUser user = auth.getCurrentUser();
                if (user == null) {
                    Log.i("User", "Not logged in!");
                    // user auth state is changed - user is null
                    // Use Firebase UI Sign In
                    startActivityForResult(
                            // Get an instance of AuthUI based on the default app
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .build(),

                            RC_SIGN_IN);
                } else{
                    Log.i("User", "Is Logged in");

                    //
                    final Map userInfo = new HashMap();

                    userInfo.put("email", user.getEmail());
                    userInfo.put("name", user.getDisplayName());

                    rootRef.child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.getValue() != null){ // !== compares without having to get value != you have to use getvalue

                                // user exists, do nothing
                                Log.i("Data", dataSnapshot.getValue().toString());
                            } else{
                                // user doesn't exist, add to database
                                rootRef.child("users").child(user.getUid()).setValue(userInfo);

                                Log.i("User", "Added + " + dataSnapshot.getValue());

                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }
        };




        addShowsButton = (Button) findViewById(R.id.addShowsButton);
        addShowsButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddShowsActivity.class));
            }

        });
        signOutButton = (Button) findViewById(R.id.signOutButton);
        signOutButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                AuthUI.getInstance().signOut(MainActivity.this);
            }

        });

        ListView myListView = (ListView) findViewById(R.id.myListView);
        movies = new ArrayList<>();

        arrayAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, movies);
        myListView.setAdapter(arrayAdapter);

        favouritesRef = rootRef.getRef().child("users").child(user.getUid()).child("favourites");
        favouritesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // clearing the list if it is not empty to avoid duplicates when deleting
                if(movies.size() != 0){ movies.clear();}
                for(DataSnapshot showSnapshot: dataSnapshot.getChildren()){
                    movies.add(WordUtils.capitalize(showSnapshot.getKey()));

                }
                arrayAdapter.notifyDataSetChanged();
                Log.i("Shows", "Showing");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i("Error: ", databaseError.getDetails());
            }
        });


        registerForContextMenu(myListView);

        myListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){


            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(MainActivity.this, ShowInformationActivity.class);
                intent.putExtra("showName", movies.get(position));

                startActivity(intent);
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.setHeaderTitle("Select The Action");
        menu.add(0, v.getId(), 0, "Delete");//groupId, itemId, order, title
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            favouritesRef = rootRef.getRef().child("users").child(user.getUid()).child("favourites");
        }
        if(item.getTitle() == "Delete"){
            // Delete show from firebase favourites and updating arrayadapter
            favouritesRef.child(arrayAdapter.getItem(info.position).toLowerCase()).removeValue();
            arrayAdapter.notifyDataSetChanged();
        }
        else{
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN){
            if(resultCode == RESULT_CANCELED){
                // finish calls the counter part of the methods, eg: oncreate - ondestroy
                // in this case it gets rid of the bug of not exiting app when back button is pressed
                finish();
            }

        }
    }

    // Udacity tutorial said to add on resume and remove on pause, the authstatelistener; where as
    // currently it's add on start and remove on stop
    @Override
    public void onResume() {
        super.onResume();
        auth.addAuthStateListener(authListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (authListener != null) {
            auth.removeAuthStateListener(authListener);
        }
    }

}
