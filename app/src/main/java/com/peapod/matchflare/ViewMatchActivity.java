package com.peapod.matchflare;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;
import com.peapod.matchflare.Objects.Notification;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/*
 * The Activity used to show the status of a Match that a Matcher has made
 */
public class ViewMatchActivity extends Activity implements Callback<Match>, View.OnClickListener {

    //Activity components
    ImageView firstImage;
    ImageView secondImage;
    ImageView firstChatButton;
    ImageView secondChatButton;
    TextView viewStatusText;
    TextView firstMatcheeName;
    TextView secondMatcheeName;
    View progressIndicator;
    View root;

    //Activity variables
    Match thisMatch;
    RestService ui = ((Global) getApplication()).ui;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_match);

        //Retrieve components
        progressIndicator = findViewById(R.id.progress_indicator);
        root = findViewById(R.id.root_match_view);
        firstImage = (ImageView) findViewById(R.id.first_matchee_image_view);
        secondImage = (ImageView) findViewById(R.id.second_matchee_image_view);
        firstMatcheeName = (TextView) findViewById(R.id.first_contact_textview);
        secondMatcheeName = (TextView) findViewById(R.id.second_contact_textview);
        viewStatusText = (TextView) findViewById(R.id.view_status_text);
        firstChatButton = (ImageView) findViewById(R.id.first_chat_button);
        secondChatButton = (ImageView) findViewById(R.id.second_chat_button);

        //Style components
        progressIndicator.setVisibility(View.VISIBLE);
        root.setVisibility(View.GONE);

        Style.toOpenSans(this,firstMatcheeName,"light");
        Style.toOpenSans(this,secondMatcheeName,"light");
        Style.toOpenSans(this,viewStatusText,"light");

        //Set listeners
        firstChatButton.setOnClickListener(this);
        secondChatButton.setOnClickListener(this);

        //Get intent data
        Bundle extras = this.getIntent().getExtras();
        thisMatch = (Match) extras.getSerializable("pair");

        if (thisMatch == null) { //If no pair was passed in, get the pair object from the ID
            int pair_id = extras.getInt("pair_id");
            Map<String,Integer> options = new HashMap<String,Integer>();
            options.put("pair_id",pair_id);
            ui.getMatch(options, this);
        }
        else {
            setMatcheeName();
            progressIndicator.setVisibility(View.GONE);
            root.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        //Register for remote notifications
        IntentFilter filter=new IntentFilter("com.peapod.matchflare.push_notification");
        LocalBroadcastManager.getInstance(this).registerReceiver(onEvent, filter);
        checkChatsForUnreadMessages();  //Checks if either chat has an unread message

        //Google Analytics
        Tracker t = ((Global) this.getApplication()).getTracker();
        t.setScreenName("ViewMatchActivity");
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onEvent); //Unregister for remote notifications
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        Tracker t = ((Global) getApplication()).getTracker();

        //Go to corresponding chat activity
        if (view.getId() == R.id.first_chat_button) {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("chat_id", thisMatch.first_matchee.matcher_chat_id);
            i.putExtra("pair_id", thisMatch.pair_id);
            startActivity(i);

            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("ViewMatchFirstMatcheeChatButtonPressed")
                    .build());
        }
        else if (view.getId() == R.id.second_chat_button) {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("chat_id", thisMatch.second_matchee.matcher_chat_id);
            i.putExtra("pair_id", thisMatch.pair_id);
            startActivity(i);

            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("ViewMatchSecondMatcheeChatButtonPressed")
                    .build());
        }
    }

    public void checkChatsForUnreadMessages() { //Checks both chats if there are unread messages

        int contact_id = ((Global) getApplication()).thisUser.contact_id;

        if (contact_id > 0 && thisMatch != null) {
            Map<String, Integer> options = new HashMap<String, Integer>();
            options.put("contact_id",contact_id);
            options.put("chat_id",thisMatch.first_matchee.matcher_chat_id);
            ((Global) getApplication()).ui.hasUnread(options, new Callback<EvaluateActivity.UnseenObject> () { //Check first matchee chat

                @Override
                public void success(EvaluateActivity.UnseenObject aBoolean, Response response) {
                    if (aBoolean != null && aBoolean.has_unseen.booleanValue() == true) { //If has unseen, then set appropriate drawable
                        firstChatButton.setImageDrawable(ViewMatchActivity.this.getResources().getDrawable(R.drawable.new_message_chat_button));
                    }
                    else {
                        firstChatButton.setImageDrawable(ViewMatchActivity.this.getResources().getDrawable(R.drawable.chat_button));

                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e("Cant check unread",error.toString());

                    //Google analytics
                    Tracker t = ((Global) getApplication()).getTracker();
                    t.send(new HitBuilders.ExceptionBuilder()
                            .setDescription("(ViewMatch) Failed to check for first matchee unread: " +
                                    new StandardExceptionParser(ViewMatchActivity.this, null)
                                            .getDescription(Thread.currentThread().getName(), error))
                            .setFatal(false)
                            .build());
                }
            });

            Map<String, Integer> options2 = new HashMap<String, Integer>();
            options2.put("contact_id",((Global) getApplication()).thisUser.contact_id);
            options2.put("chat_id",thisMatch.second_matchee.matcher_chat_id);
            ((Global) getApplication()).ui.hasUnread(options2, new Callback<EvaluateActivity.UnseenObject> () { //Check for unread in second matchee chat

                @Override
                public void success(EvaluateActivity.UnseenObject aBoolean, Response response) {
                    if (aBoolean != null && aBoolean.has_unseen.booleanValue() == true) {
                        secondChatButton.setImageDrawable(ViewMatchActivity.this.getResources().getDrawable(R.drawable.new_message_chat_button));
                    }
                    else {
                        secondChatButton.setImageDrawable(ViewMatchActivity.this.getResources().getDrawable(R.drawable.chat_button));
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e("Cant check unread",error.toString());

                    //Google Analytics
                    Tracker t = ((Global) getApplication()).getTracker();
                    t.send(new HitBuilders.ExceptionBuilder()
                            .setDescription("(ViewMatch) Failed to check for second matchee unread: " +
                                    new StandardExceptionParser(ViewMatchActivity.this, null)
                                            .getDescription(Thread.currentThread().getName(), error))
                            .setFatal(false)
                            .build());
                }
            });
        }
    }

    //Sets the status and images for the current match
    public void setMatcheeName() {

        Picasso.with(this).load(thisMatch.first_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(firstImage);
        Picasso.with(this).load(thisMatch.second_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(secondImage);
        firstMatcheeName.setText(thisMatch.first_matchee.guessed_full_name);
        secondMatcheeName.setText(thisMatch.second_matchee.guessed_full_name);
        String statusText = "";

        if (thisMatch.first_matchee.contact_status.equals("NOTIFIED") && thisMatch.second_matchee.contact_status.equals("NOT_SENT")) {
            statusText = "waiting for " + thisMatch.first_matchee.guessed_full_name + "...";
            secondChatButton.setVisibility(View.GONE);
            firstChatButton.setVisibility(View.VISIBLE);
        }
        else if (thisMatch.first_matchee.contact_status.equals("NOT_SENT") && thisMatch.second_matchee.contact_status.equals("NOTIFIED")) {
            statusText = "waiting for " + thisMatch.second_matchee.guessed_full_name + "...";
            firstChatButton.setVisibility(View.GONE);
            secondChatButton.setVisibility(View.VISIBLE);
        }
        else if (thisMatch.first_matchee.contact_status.equals("ACCEPT") && thisMatch.second_matchee.contact_status.equals("NOTIFIED")) {
            statusText = "waiting for " + thisMatch.second_matchee.guessed_full_name + "...";
            firstChatButton.setVisibility(View.GONE);
            secondChatButton.setVisibility(View.VISIBLE);
        }
        else if (thisMatch.first_matchee.contact_status.equals("NOTIFIED") && thisMatch.second_matchee.contact_status.equals("ACCEPT")) {
            statusText = "waiting for " + thisMatch.first_matchee.guessed_full_name + "...";
            firstChatButton.setVisibility(View.VISIBLE);
            secondChatButton.setVisibility(View.GONE);
        }
        else if (thisMatch.first_matchee.contact_status.equals("ACCEPT") && thisMatch.second_matchee.contact_status.equals("ACCEPT")) {
            statusText = "they both accepted!";
            firstChatButton.setVisibility(View.VISIBLE);
            secondChatButton.setVisibility(View.VISIBLE);
        }
        else {
            statusText = "waiting...";
        }
        viewStatusText.setText(statusText);
        checkChatsForUnreadMessages();
    }

    @Override
    public void success(Match match, Response response) { //Successfully retrieved the match object
        thisMatch = match;
        setMatcheeName();
        progressIndicator.setVisibility(View.GONE);
        root.setVisibility(View.VISIBLE);
    }

    @Override
    public void failure(RetrofitError error) { //Failed to get match
        Style.makeToast(this, "Failed to get your match. Try again later!");
        Log.e("Cant get match to view", error.toString());

        //Google Analytics
        Tracker t = ((Global) getApplication()).getTracker();
        t.send(new HitBuilders.ExceptionBuilder()
                .setDescription("(ViewMatch) Failed to get match for viewing: " +
                        new StandardExceptionParser(ViewMatchActivity.this, null)
                                .getDescription(Thread.currentThread().getName(), error))
                .setFatal(false)
                .build());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_button_only, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.home_icon:
                //Do stuff
                intent = new Intent(this, PresentMatchesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        Intent i = new Intent(this,SplashActivity.class);
        startActivity(i);
        finish();
    }

    /*
     * Listens for remote notifications
     */
    private BroadcastReceiver onEvent=new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Notification notification = (Notification) intent.getSerializableExtra("notification");
            if (notification != null) {

                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = {0,100};
                v.vibrate(pattern,-1);

                Style.makeToast(ViewMatchActivity.this,"New Notification!");
                checkChatsForUnreadMessages();
            }

        }
    };
}
