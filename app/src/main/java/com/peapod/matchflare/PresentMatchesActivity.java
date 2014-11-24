package com.peapod.matchflare;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class PresentMatchesActivity extends Activity implements Callback<StringResponse> {


    Cursor contactsCursor;
    Person myContactId;
    RestService ui;
    Queue<Match> matches;
    Match currentMatch;
    TextView firstContactTextview;
    TextView secondContactTextview;
    Button passButton;
    Button matchButton;
    TextView eloInstructions;
    static final String SENDER_ID="614720100487";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_present_matches);

        //Retrieve UI elements
        firstContactTextview = (TextView) findViewById(R.id.first_contact_textview);
        secondContactTextview = (TextView) findViewById(R.id.second_contact_textview);
        eloInstructions = (TextView) findViewById(R.id.elo_instructions);

        passButton = (Button) findViewById(R.id.pass_button);
        matchButton = (Button) findViewById(R.id.match_button);

        //Attach listeners
        ButtonListener buttonListener = new ButtonListener();
        passButton.setOnClickListener(buttonListener);
        matchButton.setOnClickListener(buttonListener);
        firstContactTextview.setOnClickListener(buttonListener);
        secondContactTextview.setOnClickListener(buttonListener);

        //Set current user
        ((Global) getApplication()).thisUser.contact_id = 90;

        //Register GCM for Push Notifications

        GCMRegistrarCompat.checkDevice(this);
        if (BuildConfig.DEBUG) {
            GCMRegistrarCompat.checkManifest(this);
        }

        final String regId=GCMRegistrarCompat.getRegistrationId(this);

        if (regId.length() == 0) {
            new RegisterTask(this).execute(SENDER_ID, ((Global) getApplication()).thisUser.contact_id + "");
        } else
        {
            Log.d(getClass().getSimpleName(), "Existing registration: "
                    + regId);
            //Toast.makeText(this, regId, Toast.LENGTH_LONG).show();
        }



        //Retrieve contacts for upsert with Contacts table
        String[] contacts = {};

        final String[] projection = new String[] {
                ContactsContract.Contacts.Data._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.TIMES_CONTACTED,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Photo.PHOTO_URI
        };

        String filter = ContactsContract.Contacts.HAS_PHONE_NUMBER + "==1 AND " + ContactsContract.CommonDataKinds.Phone.TYPE + " = " + ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE + " AND LENGTH(" + ContactsContract.Contacts.DISPLAY_NAME + ") > 6" + " AND LENGTH(" + ContactsContract.CommonDataKinds.Phone.NUMBER + ") >= 7 AND SUBSTR(" + ContactsContract.Contacts.DISPLAY_NAME + ",1,1) != '#' AND SUBSTR(" + ContactsContract.Contacts.DISPLAY_NAME + ",1,2) != 'GM GROUP BY' ";
        Log.e("Filter", filter);

        ContentResolver cr = getApplicationContext().getContentResolver(); //Activity/Application android.content.Context
//        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        contactsCursor = cr.query(ContactsContract.Data.CONTENT_URI, projection, filter, null, ContactsContract.Contacts.TIMES_CONTACTED + " DESC");
//        if(cursor.moveToFirst())
//        {
//
//            ArrayList<String> alContacts = new ArrayList<String>();
//            do
//            {
//                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
//
//                if(Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0)
//                {
//                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",new String[]{ id }, null);
//                    while (pCur.moveToNext())
//                    {
//                        String contactNumber = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
//                        alContacts.add(contactNumber);
//                        break;
//                    }
//                    pCur.close();
//                }
//
//            } while (cursor.moveToNext()) ;
//
//
//            contacts = (String[]) alContacts.toArray(new String[alContacts.size()]);
//            ArrayAdapter<String> contactsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, contacts);
//            ListView contactsListView = (ListView) findViewById(R.id.contact_list_view);
//            contactsListView.setAdapter(contactsAdapter);
//        }

//        SimpleCursorAdapter contactsCursorAdapter = new SimpleCursorAdapter(this, R.layout.contacts_cursor_row, contactsCursor, new String[] {ContactsContract.Contacts.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER}, new int[] {R.id.name, R.id.phone_number},0);
//        ListView contactsListView = (ListView) findViewById(R.id.contact_list_view);
//        contactsListView.setAdapter(contactsCursorAdapter);

        Set<Person> people = new HashSet<Person>(contactsCursor.getCount());

        while (contactsCursor.moveToNext()) {
            String full_name = contactsCursor.getString(1); //Get name
            String raw_phone_number = contactsCursor.getString(3);  //Get raw phone number
            Person toAdd = new Person(full_name,raw_phone_number);
            people.add(toAdd);
        }


        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint("https://matchflare.herokuapp.com")
                .build();
        ui = restAdapter.create(RestService.class);
        Map<String,Integer> options = new HashMap<String,Integer>();
        options.put("contact_id",((Global) getApplication()).thisUser.contact_id);
        ui.processContacts(new Contacts(people), options, this);

    }

    private class ButtonListener implements View.OnClickListener {
        public void onClick(View v) {

            currentMatch.matcher_contact_id = ((Global) getApplication()).thisUser.contact_id;
            
            if (v.getId() == R.id.match_button) {
                currentMatch.match_status = "MATCHED";
                ui.addMatch(currentMatch,null,new postMatchCallback());
                presentNextMatch();
            }
            else if (v.getId() == R.id.pass_button) {
                passButton.setVisibility(View.INVISIBLE);
                matchButton.setVisibility(View.INVISIBLE);
                eloInstructions.setVisibility(View.VISIBLE);
            }
            else if (v.getId() == R.id.first_contact_textview) {
                currentMatch.match_status = "FIRST_CONTACT_WINS";
                ui.addMatch(currentMatch, null, new postMatchCallback());
                presentNextMatch();
            }
            else if (v.getId() == R.id.second_contact_textview) {
                currentMatch.match_status = "SECOND_CONTACT_WINS";
                ui.addMatch(currentMatch, null, new postMatchCallback());
                presentNextMatch();
            }
            //Post result of currently presented match
            //NEED TO IMPLEMENT


        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        contactsCursor.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.present_matches, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Retrofit callback for process contacts
    @Override
    public void failure(RetrofitError err)
    {
        Log.e("Error Processing Contacts:", err.toString());
    }

    @Override
    public void success(StringResponse response, Response arg1)
    {
        Log.e("Contacts successfully processed:", response.response);
        Map<String,Integer> options = new HashMap<String,Integer>();
        options.put("contact_id",((Global) getApplication()).thisUser.contact_id);
        ui.getMatches(options, new MatchesCallback());
    }


    //Retrofit callback for getting matches
    private class MatchesCallback implements Callback<Matches> {
        //Retrofit callback for process contacts
        @Override
        public void failure(RetrofitError err)
        {
            Log.e("Error Getting Matches:", err.toString());
        }

        @Override
        public void success(Matches response, Response arg1)
        {
            Log.e("Matches successfully returned:", response.toString());
            matches = response.matches;
            PresentMatchesActivity.this.presentNextMatch();
        }
    }

    //Retrofit callback for getting matches
    private class postMatchCallback implements Callback<StringResponse> {
        //Retrofit callback for process contacts
        @Override
        public void failure(RetrofitError err)
        {
            Log.e("Error Posting Match:", err.toString());
        }

        @Override
        public void success(StringResponse response, Response arg1)
        {
            Log.e("Match successfully posted:", response.response.toString());
        }
    }

    public void presentNextMatch()
    {
        passButton.setVisibility(View.VISIBLE);
        matchButton.setVisibility(View.VISIBLE);
        eloInstructions.setVisibility(View.INVISIBLE);

        if (matches.size() > 0) {
            currentMatch = matches.poll();
            firstContactTextview.setText(currentMatch.first_contact_name);
            secondContactTextview.setText(currentMatch.second_contact_name);
        }

        if (matches.size() < 10) {
            Log.e("Getting more matches", matches.size() + "");
            //NEED TO IMPLEMENT
        }
    }

    //Register device for GCM (if not already registered)
    private static class RegisterTask extends GCMRegistrarCompat.BaseRegisterTask {
        RegisterTask(Context context) {
            super(context);
        }
        @Override
        public void onPostExecute(String regid) {
            Log.d(getClass().getSimpleName(), "registered as: " + regid); Toast.makeText(context, regid, Toast.LENGTH_LONG).show();
        } }
}
