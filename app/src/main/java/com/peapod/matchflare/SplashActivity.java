package com.peapod.matchflare;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class SplashActivity extends Activity implements Callback<Person> {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //Check if access token exists and is valid
        String accessToken = ((Global) getApplication()).getAccessToken();
        if (accessToken != null) {
            Map options = new HashMap<String, String>();
            options.put("access_token",accessToken);
            ((Global)getApplication()).ui.verifyAccessToken(options, this);
        }
        else {
            ProcessContactsTask task = new ProcessContactsTask();
            task.execute();
        }
    }

    public class MatchesAndPairs {
        ArrayList<Match> matches;
        Set<Person> contact_objects;
    }

    class ProcessContactsTask extends AsyncTask<Integer, Void, Boolean>{

        MatchesAndPairs matchesAndPairs;
        Integer contactId;

        @Override
        protected Boolean doInBackground(Integer... thisUserContactId) {
            if (thisUserContactId.length > 0) {
                contactId = thisUserContactId[0];
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
            Cursor contactsCursor = cr.query(ContactsContract.Data.CONTENT_URI, projection, filter, null, ContactsContract.Contacts.TIMES_CONTACTED + " DESC");

            Set<Person> people = new HashSet<Person>(contactsCursor.getCount());

            while (contactsCursor.moveToNext()) {
                String full_name = contactsCursor.getString(1); //Get name
                String raw_phone_number = contactsCursor.getString(3);  //Get raw phone number
                Person toAdd = new Person(full_name,raw_phone_number);
                people.add(toAdd);
            }

            Map<String,Integer> options = new HashMap<String,Integer>();

            if (thisUserContactId.length > 0) {
                //This user has a contact id already assigned (valid access token)
                options.put("contact_id",contactId.intValue());
            }

            try {
                matchesAndPairs = ((Global) getApplication()).ui.processContacts(new Contacts(people), options);
                Log.e("Successfully processed contacts", "woo");
            }
            catch (RetrofitError e) {
                Log.e("Error processing contacts", e.toString());
                return new Boolean(false);
            }

            return new Boolean(true);

        };

        @Override
        protected void onPostExecute(Boolean result) {
            if (result.booleanValue()) { //If successful...

                if (contactId == null) {
                    // if the current user does not have a valid access token, pass in the pairs and start matching
                    ((Global) getApplication()).thisUser.contact_objects = matchesAndPairs.contact_objects;
                    Intent i = new Intent(SplashActivity.this, PresentMatchesActivity.class);
                    i.putExtra("matches", (Serializable) matchesAndPairs.matches);
                    startActivity(i);
                } else {
                    //Do nothing
                    Style.makeToast(SplashActivity.this,"Successfully processed contacts");
                }
            } else { //If processing contacts failed...
                if (contactId == null) {
                    //Close the application and inform user to try again
                    Style.makeToast(SplashActivity.this, "Could not process your contacts. Try again later");
                } else {
                    //Accept the failure and do nothing
                }
            }
        }
    }

    @Override
    public void success(Person person, Response response) {
        //Successfully verified access token
        ((Global) getApplication()).thisUser = person;
        ProcessContactsTask task = new ProcessContactsTask();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, person.contact_id);
        else
            task.execute(person.contact_id);
        Intent i = new Intent(this,PresentMatchesActivity.class);
        startActivity(i);
    }

    @Override
    public void failure(RetrofitError error) {
        //Failed to verify access token
        ProcessContactsTask task = new ProcessContactsTask();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            task.execute();
    }

    public void proceedWithNoAccessToken() {
        Intent i = new Intent(this, VerificationActivity.class);
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
