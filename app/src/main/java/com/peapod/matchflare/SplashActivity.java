package com.peapod.matchflare;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;
import com.peapod.matchflare.Objects.Contacts;
import com.peapod.matchflare.Objects.Notification;
import com.peapod.matchflare.Objects.Person;
import com.peapod.matchflare.Objects.StringResponse;
import com.viewpagerindicator.CirclePageIndicator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import com.viewpagerindicator.PageIndicator;

/*
 * The Splash Screen Activity where the user is presented with instructions if not registered,
 * or taken directly to PresentMatchesActivity, if registered.
 */
public class SplashActivity extends FragmentActivity implements Callback<Person> {

    //Activity Components
    FragmentAdapter mAdapter;
    ViewPager mPager;
    PageIndicator mIndicator;
    RelativeLayout splashLogo;
    RelativeLayout instructionPager;
    View progressIndicator;
    Button registerButton;
    Button nextButton;

    //Activity Variables
    boolean reachedFinalInstruction = false;
    boolean toPresentMatches = false;
    boolean toRegister = false;
    boolean finishedProcessingContacts = false;
    MatchesAndPairs matchesAndPairs;
    ArrayList<Intent> nextIntents = new ArrayList<Intent>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Intent backIntent = new Intent(this, PresentMatchesActivity.class); //Always add PresentMatches first so that it is on the backstack
        nextIntents.add(backIntent);

        // If splash was called from a tapped notificaton (from GCM),
        // then find out where the notification was leading to and add to intent[] for pendingintent
        Notification receivedNotification = (Notification) getIntent().getSerializableExtra("notification");
        if (receivedNotification != null) {
            Map<String,Integer> options = new HashMap<String,Integer>();
            options.put("notification_id",receivedNotification.notification_id);
            ((Global) getApplication()).ui.seeNotification(options, new Callback<StringResponse>() { //Mark tapped notification as seen
                @Override
                public void success(StringResponse stringResponse, Response response) {
                    Log.e("Saw notification", stringResponse.response);
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e("Cant mark as seen", error.toString());

                    //Google Analytics
                    Tracker t = ((Global) getApplication()).getTracker();
                    t.send(new HitBuilders.ExceptionBuilder()
                            .setDescription("(Splash) Failure to mark tapped notification as seen: " +
                                    new StandardExceptionParser(SplashActivity.this, null)
                                            .getDescription(Thread.currentThread().getName(), error))
                            .setFatal(false)
                            .build());

                }
            });

            Intent nextIntent = Notification.makeIntent(SplashActivity.this,receivedNotification);
            if (nextIntent != null) {
                nextIntents.add(nextIntent); //Add the appropriate activity to the top of the backstack
            }
        };

        //Retrieve components
        splashLogo = (RelativeLayout) findViewById(R.id.splash_logo);
        instructionPager = (RelativeLayout) findViewById(R.id.instruction_pager);
        progressIndicator = findViewById(R.id.progress_indicator);
        mPager = (ViewPager)findViewById(R.id.pager);
        mIndicator = (CirclePageIndicator)findViewById(R.id.indicator);
        registerButton = (Button) findViewById(R.id.register_button);
        nextButton = (Button) findViewById(R.id.start_matching);

        //Stylize components
        progressIndicator.bringToFront();
        progressIndicator.setVisibility(View.GONE);
        mAdapter = new FragmentAdapter(getSupportFragmentManager());
        mPager.setAdapter(mAdapter);
        mIndicator.setViewPager(mPager);
        mPager.setVisibility(View.VISIBLE); //Show the instructions
        Style.toOpenSans(this,registerButton,"light");
        Style.toOpenSans(this,nextButton,"light");

        //Add listeners
        nextButton.setOnClickListener(new View.OnClickListener() {  //Configure 'Next' buttons and 'Register' buttons
            @Override
            public void onClick(View view) {
                if (reachedFinalInstruction){ //If reached last instruction, then...
                    toPresentMatches = true;
                    if (finishedProcessingContacts) { //If contacts finished processing, then go to PresentMatches
                        Intent i = nextIntents.get(0);
                        i.putExtra("matches", (Serializable) matchesAndPairs.matches);
                        PendingIntent pendingIntent = PendingIntent.getActivities(SplashActivity.this,0,nextIntents.toArray(new Intent[nextIntents.size()]),0);
                        try {
                            pendingIntent.send();
                        }
                        catch (PendingIntent.CanceledException e) {
                            Log.e("PendingIntent cancelled",e.toString());
                        }
                    }
                    else { //If not yet done processing, then wait and disable buttons
                        nextButton.setEnabled(false);
                        registerButton.setEnabled(false);
                        progressIndicator.setVisibility(View.VISIBLE);
                        mPager.setVisibility(View.INVISIBLE);
                    }

                    //Google Analytics
                    Tracker t = ((Global) getApplication()).getTracker();
                    t.send(new HitBuilders.EventBuilder()
                            .setCategory("ui_action")
                            .setAction("button_press")
                            .setLabel("SplashToPresent")
                            .build());
                }
                else { //If not yet at last page, then just move to next page
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

                //Go to register activity with the current state of contact processing
                Intent i = new Intent(SplashActivity.this,VerificationActivity.class);
                i.putExtra("finished_processing_contacts",finishedProcessingContacts);
                startActivity(i);

                //Google Analytics
                Tracker t = ((Global) getApplication()).getTracker();
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("ui_action")
                        .setAction("button_press")
                        .setLabel("SplashToRegister")
                        .build());
            }
        });

        //Listen for eaching the last page and change button text on last page.
        ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //Do nothing on scroll
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
                //Do nothing
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
        else { //If no access token, then begin processing contacts and show instructions
            splashLogo.setVisibility(View.GONE);
            instructionPager.setVisibility(View.VISIBLE);
            ProcessContactsTask task = new ProcessContactsTask();
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                task.execute();
        }

        //Google Analytics
        Tracker t = ((Global) this.getApplication()).getTracker();
        t.setScreenName("SplashActivity");
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    class ProcessContactsTask extends AsyncTask<Integer, Void, Boolean>{

        Integer contactId;

        @Override
        protected Boolean doInBackground(Integer... thisUserContactId) {
            if (thisUserContactId.length > 0) {
                contactId = thisUserContactId[0];
            }

            final String[] projection = new String[] { //Columns that will be retrieved
                    ContactsContract.Contacts.Data._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.TIMES_CONTACTED,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
            };

            //Query filter contacts for having a mobile phone number, minimum length, not from GroupMe
            String filter = ContactsContract.Contacts.HAS_PHONE_NUMBER + "==1 AND " + ContactsContract.CommonDataKinds.Phone.TYPE + " = " + ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE + " AND LENGTH(" + ContactsContract.Contacts.DISPLAY_NAME + ") > 6" + " AND LENGTH(" + ContactsContract.CommonDataKinds.Phone.NUMBER + ") >= 7 AND SUBSTR(" + ContactsContract.Contacts.DISPLAY_NAME + ",1,1) != '#' AND SUBSTR(" + ContactsContract.Contacts.DISPLAY_NAME + ",1,2) != 'GM GROUP BY' ";
            ContentResolver cr = getApplicationContext().getContentResolver();
            Cursor contactsCursor = cr.query(ContactsContract.Data.CONTENT_URI, projection, filter, null, ContactsContract.Contacts.TIMES_CONTACTED + " DESC"); //Make query sorted by contact time

            Set<Person> people = new HashSet<Person>(contactsCursor.getCount()); //Move results of query to the set

            while (contactsCursor.moveToNext()) {  //Traverse the list and create a Person object for each contact
                String full_name = contactsCursor.getString(1); //Get name
                String raw_phone_number = contactsCursor.getString(3);  //Get raw phone number
                Person toAdd = new Person(full_name,raw_phone_number);
                people.add(toAdd);
            }
            contactsCursor.close();

            Map<String,Integer> options = new HashMap<String,Integer>();
            if (thisUserContactId.length > 0) {
                options.put("contact_id",contactId.intValue()); //Add contact id if user has a contact id already assigned (valid access token)
            }

            try {
                //Sync call is OK since already in background thread
                matchesAndPairs = ((Global) getApplication()).ui.processContacts(new Contacts(people), options); //Post and update contacts
                Log.e("Processed contacts", "woo");
            }
            catch (RetrofitError e) {
                Log.e("Cant process contacts", e.toString());

                //Google Analytics
                Tracker t = ((Global) getApplication()).getTracker();
                t.send(new HitBuilders.ExceptionBuilder()
                        .setDescription("(Splash) Error processing contacts: " +
                                new StandardExceptionParser(SplashActivity.this, null)
                                        .getDescription(Thread.currentThread().getName(), e))
                        .setFatal(false)
                        .build());

                return new Boolean(false);
            }

            return new Boolean(true);
        };

        @Override
        protected void onPostExecute(Boolean result) {
            finishedProcessingContacts = true;

            if (result.booleanValue()) { //If successfully processed contacts...

                if (contactId == null) {
                    // If the current user does not have a valid access token, pass in the pairs to PresentMatches and start matching
                    ((Global) getApplication()).thisUser.contact_objects = matchesAndPairs.contact_objects;

                    if (toPresentMatches) {  //If the unregistered user pressed the 'start matching' button, go to PresentMatches
                        Intent i = new Intent(SplashActivity.this, PresentMatchesActivity.class);
                        i.putExtra("matches", (Serializable) matchesAndPairs.matches);
                        startActivity(i);
                    }
                    else if (toRegister) {  //If the user pressed the register button, then send a broadcast to be received by VerificationActivity
                        Intent intent = new Intent("com.peapod.matchflare.FINISHED_PROCESSING_CONTACTS");
                        LocalBroadcastManager.getInstance(SplashActivity.this).sendBroadcast(intent);
                    }

                } else {
                    //Do nothing if already registered
                }

            } else { //If processing contacts failed...
                if (contactId == null) {
                    //Inform user to try again
                    Style.makeToast(SplashActivity.this, "Could not process your contacts. Try again later");
                } else {
                    //Accept the failure and do nothing
                    Style.makeToast(SplashActivity.this, "Could not process your new contacts");
                }
            }

            if (contactId != null && contactId > 0) { //If the user is registerd, update the GCM Registration ID if necessary
                String SENDER_ID="614720100487";
                GCMRegistrarCompat.checkDevice(SplashActivity.this);
                if (BuildConfig.DEBUG) {
                    GCMRegistrarCompat.checkManifest(SplashActivity.this);
                }

                final String regId=GCMRegistrarCompat.getRegistrationId(SplashActivity.this);

                if (regId.length() == 0) {
                    new RegisterTask(SplashActivity.this).execute(SENDER_ID, contactId + "");
                } else
                {
                    Log.d(getClass().getSimpleName(), "Existing registration: " + regId);
                }
            }

            nextButton.setEnabled(true);
            registerButton.setEnabled(true);
            progressIndicator.setVisibility(View.GONE);
            mPager.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void success(Person person, Response response) { //If the access token was valid...

        ((Global) getApplication()).thisUser = person;

        //Google Analytics
        Tracker t = ((Global) getApplication()).getTracker();
        t.set("&uid", person.contact_id + ""); //Put User ID into Google Analytics

        //Start processing contacts in the background
        ProcessContactsTask task = new ProcessContactsTask();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, person.contact_id);
        else
            task.execute(person.contact_id);

        //Go to the next activities
        PendingIntent pendingIntent = PendingIntent.getActivities(SplashActivity.this,0,nextIntents.toArray(new Intent[nextIntents.size()]),0);
        try {
            pendingIntent.send();
        }
        catch (PendingIntent.CanceledException e) {
            Log.e("PendingIntent cancelled",e.toString());
        }
    }

    @Override
    public void failure(RetrofitError error) { //Failed to verify access token, so go through standard Instruction pager flow

        splashLogo.setVisibility(View.GONE);
        instructionPager.setVisibility(View.VISIBLE);
        ProcessContactsTask task = new ProcessContactsTask();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            task.execute();

        //Google Analytics
        Tracker t = ((Global) getApplication()).getTracker();
        t.send(new HitBuilders.ExceptionBuilder()
                .setDescription("(Splash) Failed to verify access token: " +
                        new StandardExceptionParser(SplashActivity.this, null)
                                .getDescription(Thread.currentThread().getName(), error))
                .setFatal(false)
                .build());

    }

    //Inner class for Retrofit return object containing initial set of matches and the user's contacts
    public class MatchesAndPairs {
        ArrayList<Match> matches;
        Set<Person> contact_objects;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
