package com.peapod.matchflare;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.TitlePageIndicator;

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

import com.viewpagerindicator.PageIndicator;

public class SplashActivity extends FragmentActivity implements Callback<Person> {

    FragmentAdapter mAdapter;
    ViewPager mPager;
    PageIndicator mIndicator;
    RelativeLayout splashLogo;
    RelativeLayout instructionPager;

    Button registerButton;
    Button nextButton;

    int Number = 0;
    boolean reachedFinalInstruction = false;
    boolean toPresentMatches = false;
    boolean toRegister = false;
    boolean finishedProcessingContacts = false;

    MatchesAndPairs matchesAndPairs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        splashLogo = (RelativeLayout) findViewById(R.id.splash_logo);
        instructionPager = (RelativeLayout) findViewById(R.id.instruction_pager);

        mAdapter = new FragmentAdapter(getSupportFragmentManager());
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        mIndicator = (CirclePageIndicator)findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);

        registerButton = (Button) findViewById(R.id.register_button);
        nextButton = (Button) findViewById(R.id.start_matching);
        Style.toOpenSans(this,registerButton,"light");
        Style.toOpenSans(this,nextButton,"light");

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (reachedFinalInstruction){
                    Style.makeToast(SplashActivity.this,"Presenting Matches");
                    toPresentMatches = true;
                    if (finishedProcessingContacts) {
                        Intent i = new Intent(SplashActivity.this, PresentMatchesActivity.class);
                        i.putExtra("matches", (Serializable) matchesAndPairs.matches);
                        startActivity(i);
                    }
                }
                else {
                    if (mPager.getCurrentItem() < 2) {
                        mPager.setCurrentItem(mPager.getCurrentItem() + 1);
                    }
                }
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toRegister = true;
                registerButton.setEnabled(false);

                Intent i = new Intent(SplashActivity.this,VerificationActivity.class);
                i.putExtra("finished_processing_contacts",finishedProcessingContacts);
                startActivity(i);
            }
        });

        ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 2) {
                    reachedFinalInstruction = true;
                    registerButton.setEnabled(true);
                    nextButton.setText("Start Matching");
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        };
        mIndicator.setOnPageChangeListener(pageChangeListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        //Check if access token exists and is valid
        String accessToken = ((Global) getApplication()).getAccessToken();
        if (accessToken != null) {
            Map options = new HashMap<String, String>();
            options.put("access_token",accessToken);
            ((Global)getApplication()).ui.verifyAccessToken(options, this);
        }
        else {
            splashLogo.setVisibility(View.GONE);
            instructionPager.setVisibility(View.VISIBLE);
            ProcessContactsTask task = new ProcessContactsTask();
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                task.execute();
        }
    }
    public class MatchesAndPairs {
        ArrayList<Match> matches;
        Set<Person> contact_objects;
    }

    class ProcessContactsTask extends AsyncTask<Integer, Void, Boolean>{

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
            contactsCursor.close();
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
            finishedProcessingContacts = true;

            if (result.booleanValue()) { //If successful...
                Style.makeToast(SplashActivity.this,"Successfully processed contacts");
                if (contactId == null) {
                    // if the current user does not have a valid access token, pass in the pairs and start matching
                    ((Global) getApplication()).thisUser.contact_objects = matchesAndPairs.contact_objects;

                    if (toPresentMatches) {  //If the unregistered user pressed the 'start matching' button
                        Intent i = new Intent(SplashActivity.this, PresentMatchesActivity.class);
                        i.putExtra("matches", (Serializable) matchesAndPairs.matches);
                        startActivity(i);
                    }
                    else if (toRegister) {  //If the user pressed the register button, then send a broadcast to be received by VerificationActivity
                        Intent intent = new Intent("com.peapod.matchflare.FINISHED_PROCESSING_CONTACTS");
                        LocalBroadcastManager.getInstance(SplashActivity.this).sendBroadcast(intent);
                    }

                } else {
                    //Do nothing

                }
            } else { //If processing contacts failed...
                if (contactId == null) {
                    //Close the application and inform user to try again
                    Style.makeToast(SplashActivity.this, "Could not process your contacts. Try again later");
                } else {
                    //Accept the failure and do nothing
                    Style.makeToast(SplashActivity.this, "Could not process your new contacts");
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
        //Intent i = new Intent(this,VerificationActivity.class);
        startActivity(i);
    }

    @Override
    public void failure(RetrofitError error) {
        //Failed to verify access token
        splashLogo.setVisibility(View.GONE);
        instructionPager.setVisibility(View.VISIBLE);
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

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        Intent i = new Intent(this,SplashActivity.class);
        startActivity(i);
        finish();
    }
}
