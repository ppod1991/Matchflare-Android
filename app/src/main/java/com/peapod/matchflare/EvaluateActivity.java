package com.peapod.matchflare;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;
import com.peapod.matchflare.Objects.EvaluateResponse;
import com.peapod.matchflare.Objects.Notification;
import com.peapod.matchflare.Objects.Person;
import com.peapod.matchflare.Objects.StringResponse;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/*
 * Activity used to show when a user has been matched with a matchee.
 * Allows the user to evaluate the match, block the matcher, or go to the chats
 */
public class EvaluateActivity extends Activity implements Callback<Match>, View.OnClickListener, MatcherOptionsDialog.MatcherOptionsDialogListener {

    //Activity Components
    ImageView matchButton;
    ImageView passButton;
    ImageView chatButton;
    ImageView askButton;
    ImageView matcherOptionsButton;
    TextView matcherName;
    TextView otherMatcheeName;
    TextView matchDescription;
    ImageView matcherImageView;
    ImageView otherMatcheeImageView;
    View progressIndicator;
    View root;

    //Activity Variables
    Match thisMatch;
    RestService ui = ((Global) getApplication()).ui;
    Person thisMatchee;
    Person otherMatchee;
    Tracker t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evaluate);

        //Retrieve components
        progressIndicator = findViewById(R.id.progress_indicator);
        root = findViewById(R.id.root_evaluate);
        matchButton = (ImageView) findViewById(R.id.evaluate_match_button);
        passButton = (ImageView) findViewById(R.id.evaluate_pass_button);
        chatButton = (ImageView) findViewById(R.id.chat_button);
        askButton = (ImageView) findViewById(R.id.ask_matcher_button);
        matcherOptionsButton = (ImageView) findViewById(R.id.matcher_option_dots);
        matcherImageView = (ImageView) findViewById(R.id.matcher_image_view);
        otherMatcheeImageView = (ImageView) findViewById(R.id.other_matchee_image_view);
        matcherName = (TextView) findViewById(R.id.matcher_name);
        otherMatcheeName = (TextView) findViewById(R.id.other_matchee_name);
        matchDescription = (TextView) findViewById(R.id.match_description);

        //Get intent data
        Bundle extras = this.getIntent().getExtras();
        thisMatch = (Match) extras.getSerializable("pair");

        //Set appearances
        progressIndicator.setVisibility(View.VISIBLE);
        root.setVisibility(View.GONE);
        matcherOptionsButton.bringToFront();
        askButton.bringToFront();
        Style.toOpenSans(this,matcherName,"light");
        Style.toOpenSans(this,otherMatcheeName,"light");
        Style.toOpenSans(this,matchDescription,"light");

        if (thisMatch == null) {
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

        //Set Listeners
        passButton.setOnClickListener(this);
        matchButton.setOnClickListener(this);
        chatButton.setOnClickListener(this);
        askButton.setOnClickListener(this);
        matcherOptionsButton.setOnClickListener(this);
        matcherImageView.setOnClickListener(this);
        matcherName.setOnClickListener(this);

        ButtonTouchListener touchListener = new ButtonTouchListener();
        matcherImageView.setOnTouchListener(touchListener);
        matcherName.setOnTouchListener(touchListener);

        LongClickListener longClickListener = new LongClickListener();
        matcherImageView.setOnLongClickListener(longClickListener);
        matcherName.setOnLongClickListener(longClickListener);
        matcherOptionsButton.setOnLongClickListener(longClickListener);


        //Check if first time...if so, then give instructional alert
        final SharedPreferences prefs = this.getSharedPreferences(
                "com.peapod.matchflare", Context.MODE_PRIVATE);
        final String firstEvaluateKey = "com.example.app.IS_NOT_FIRST_EVALUATE";
        boolean isNotFirstEvaluate = prefs.getBoolean(firstEvaluateKey,false);

        if (!isNotFirstEvaluate) {
            //Show instructions
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("What do you think?")
                    .setMessage("Tap the ✓ if interested. If not, tap ✖︎. If you aren't sure, ask the matcher a question. \n Remember, the other person won't know your response unless you BOTH tap ✓!")
                    .setPositiveButton("Got it!", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //Save new user preference
                            prefs.edit().putBoolean(firstEvaluateKey, true).apply();
                        }
                    })
                    .show();

            TextView messageView = (TextView)dialog.findViewById(android.R.id.message);
            messageView.setGravity(Gravity.CENTER);
        }

        t = ((Global) this.getApplication()).getTracker();
    }

    @Override
    public void onResume() {
        super.onResume();

        //Register for Remote Notifications
        IntentFilter filter=new IntentFilter("com.peapod.matchflare.push_notification");
        LocalBroadcastManager.getInstance(this).registerReceiver(onEvent, filter);
        checkChatsForUnreadMessages(); //Check if the chats have new messages

        //Google Analytics
        t.setScreenName("EvaluateActivity");
        t.send(new HitBuilders.AppViewBuilder().build());

    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onEvent); //Remove receiver
        super.onPause();
    }

    //Inner class (for Retrofit) to check if a message has an unseen message
    public class UnseenObject {
        Boolean has_unseen;
    }

    //Checks both chats for unread messages and updates the icon if it does
    public void checkChatsForUnreadMessages() {

        int contact_id = ((Global) getApplication()).thisUser.contact_id;
        if (contact_id > 0 && thisMatch != null) { //If the user exists...

            //Check main chat for unread messages
            Map<String, Integer> options = new HashMap<String, Integer>();
            options.put("contact_id",contact_id);
            options.put("chat_id",thisMatch.chat_id);
            ((Global) getApplication()).ui.hasUnread(options, new Callback<UnseenObject> () {

                @Override
                public void success(UnseenObject aBoolean, Response response) {

                    //Sets the appropriate image
                    if (aBoolean != null && aBoolean.has_unseen.booleanValue() == true) {
                        chatButton.setImageDrawable(EvaluateActivity.this.getResources().getDrawable(R.drawable.new_message_chat_button));
                    }
                    else {
                        chatButton.setImageDrawable(EvaluateActivity.this.getResources().getDrawable(R.drawable.chat_button));

                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e("Failed to check unread",error.toString());

                    //Google Analytics
                    t.send(new HitBuilders.ExceptionBuilder()
                            .setDescription("(Evaluate) Failed to check if main chat has unread: " +
                                    new StandardExceptionParser(EvaluateActivity.this, null)
                                            .getDescription(Thread.currentThread().getName(), error))
                            .setFatal(false)
                            .build());
                }
            });

            //Checks the matcher chat for unread messages
            Map<String, Integer> options2 = new HashMap<String, Integer>();
            options2.put("contact_id",((Global) getApplication()).thisUser.contact_id);
            options2.put("chat_id",thisMatchee.matcher_chat_id);
            ((Global) getApplication()).ui.hasUnread(options2, new Callback<UnseenObject> () {

                @Override
                public void success(UnseenObject aBoolean, Response response) {
                    //Set the appropriate image
                    if (aBoolean != null && aBoolean.has_unseen.booleanValue()) {
                        askButton.setImageDrawable(EvaluateActivity.this.getResources().getDrawable(R.drawable.new_message_chat_button));
                    }
                    else {
                        askButton.setImageDrawable(EvaluateActivity.this.getResources().getDrawable(R.drawable.ask_button));

                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e("Failed to check unread",error.toString());

                    //Google Analytics
                    t.send(new HitBuilders.ExceptionBuilder()
                            .setDescription("(Evaluate) Failed to check if matcher chat has unread: " +
                                    new StandardExceptionParser(EvaluateActivity.this, null)
                                            .getDescription(Thread.currentThread().getName(), error))
                            .setFatal(false)
                            .build());
                }
            });
        }


    }

    //Set the description and images for this match
    public void setMatcheeName() {

        //Determine which person in the match the current user is...
        if (((Global) getApplication()).thisUser.contact_id == thisMatch.first_matchee.contact_id) {
            thisMatchee = thisMatch.first_matchee;
            otherMatchee = thisMatch.second_matchee;
        }
        else if (((Global) getApplication()).thisUser.contact_id == thisMatch.second_matchee.contact_id){
            thisMatchee = thisMatch.second_matchee;
            otherMatchee = thisMatch.first_matchee;
        }
        else {
            Style.makeToast(this,"ERROR! You are not in this match!");

            //Google Analytics
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("User is not in the match (Evaluate)")
                    .setFatal(false)
                    .build());
            return;
        }

        if (thisMatch.is_anonymous) {
            int matcherImage;
            if (thisMatch.matcher.guessed_gender.equals("MALE")) {
                matcherImage = R.drawable.male;
            }
            else if (thisMatch.matcher.guessed_gender.equals("FEMALE")) {
                matcherImage = R.drawable.female;
            }
            else {
                matcherImage = R.drawable.unknown_gender;
            }

            //Load anonymous image
            Picasso.with(this).load(matcherImage).fit().centerInside().transform(new CircleTransform()).into(matcherImageView);
            matcherName.setText("A friend");
        }
        else {
            Picasso.with(this).load(thisMatch.matcher.image_url).fit().centerInside().transform(new CircleTransform()).into(matcherImageView);
            matcherName.setText(thisMatch.matcher.guessed_full_name);
        }

        //Set other matchee image
        Picasso.with(this).load(otherMatchee.image_url).fit().centerInside().transform(new CircleTransform()).into(otherMatcheeImageView);
        otherMatcheeName.setText(otherMatchee.guessed_full_name);

        //Set the description text and button visibility
        String statusText = "thinks you'd be good with";
        if (thisMatch.first_matchee.contact_status.equals("ACCEPT") && thisMatch.second_matchee.contact_status.equals("ACCEPT")) {
            chatButton.setVisibility(View.VISIBLE);
            matchButton.setVisibility(View.INVISIBLE);
            passButton.setVisibility(View.INVISIBLE);
            statusText = "recommended " + otherMatchee.guessed_full_name + " and you both accepted!";
        }
        else if (thisMatchee.contact_status.equals("NOTIFIED")) {
            matchButton.setVisibility(View.VISIBLE);
            passButton.setVisibility(View.VISIBLE);
            chatButton.setVisibility(View.INVISIBLE);
            statusText = "thinks you'd be good with";
        }
        else if (otherMatchee.contact_status.equals("NOTIFIED")) {
            chatButton.setVisibility(View.INVISIBLE);
            matchButton.setVisibility(View.INVISIBLE);
            passButton.setVisibility(View.INVISIBLE);
            statusText = "recommended " + otherMatchee.guessed_full_name + " and you accepted. Waiting for...";
        }

        matchDescription.setText(statusText);
        checkChatsForUnreadMessages();
    }


    @Override
    public void success(Match match, Response response) {

        Log.e("Retrieved the match", response.toString());
        thisMatch = match;
        setMatcheeName();

        //Stop loading indicators
        progressIndicator.setVisibility(View.GONE);
        root.setVisibility(View.VISIBLE);
    }

    @Override
    public void failure(RetrofitError error) {
        Log.e("Can't retrieve match", error.toString());

        //Google Analytics
        t.send(new HitBuilders.ExceptionBuilder()
                .setDescription("(Evaluate) Failed to retrieve the match: " +
                        new StandardExceptionParser(EvaluateActivity.this, null)
                                .getDescription(Thread.currentThread().getName(), error))
                .setFatal(false)
                .build());
    }

    @Override
    public void onClick(View view) {
        Log.e("Responded to match!", "woo");
        EvaluateResponse response = new EvaluateResponse();

        //Prepare to post response
        response.contact_id = ((Global) getApplication()).thisUser.contact_id;
        response.pair_id = thisMatch.pair_id;
        Intent i = new Intent(this,PresentMatchesActivity.class);

        if (view.getId() == R.id.evaluate_match_button) { //If the match was accepted...
            response.decision = "ACCEPT";
            ui.respondToMatchRequest(response, new RespondCallback());
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);

            //Google Analytics
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("EvaluateMatchButtonPressed")
                    .build());

        }
        else if (view.getId() == R.id.evaluate_pass_button) { //If the match was passed
            response.decision = "REJECT";
            ui.respondToMatchRequest(response, new RespondCallback());
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);

            //Google Analytics
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("EvaluatePassButtonPressed")
                    .build());
        }
        else if (view.getId() == R.id.chat_button) {
            i = new Intent(this, ChatActivity.class);
            i.putExtra("chat_id", thisMatch.chat_id);
            i.putExtra("pair_id", thisMatch.pair_id);
            startActivity(i);

            //Google Analytics
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("EvaluateOhterMatcheeChatButtonPressed")
                    .build());

        }
        else if (view.getId() == R.id.ask_matcher_button) {
            i = new Intent(this, ChatActivity.class);
            i.putExtra("chat_id", thisMatchee.matcher_chat_id);
            i.putExtra("pair_id", thisMatch.pair_id);
            startActivity(i);

            //Google Analytics
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("EvaluateMatcherChatPressed")
                    .build());
        }
        else if (view.getId() == R.id.matcher_option_dots || view.getId() == R.id.matcher_name || view.getId() == R.id.matcher_image_view) {
            showMatcherOptionsDialog();
        }
    }

    public class RespondCallback implements Callback<StringResponse> {
        @Override
        public void success(StringResponse response, Response response2) {
            Log.e("Posted match response", response.response);
        }

        @Override
        public void failure(RetrofitError error) {
            Log.e("Failure to respond", error.toString());

            //Google Analytics
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("(Evaluate) Failed to post response to match: " +
                            new StandardExceptionParser(EvaluateActivity.this, null)
                                .getDescription(Thread.currentThread().getName(), error))
                    .setFatal(false)
                    .build());
        }
    }


    //Matcher Dialog Methods

    //Shows the matcher options dialog
    public void showMatcherOptionsDialog() {
        MatcherOptionsDialog matcherOptionsDialog = MatcherOptionsDialog.newInstance(thisMatch.is_anonymous,thisMatch.matcher);
        matcherOptionsDialog.show(getFragmentManager(),"options_dialog");

        //Google Analytics
        t.send(new HitBuilders.EventBuilder()
                .setCategory("ui_action")
                .setAction("button_press")
                .setLabel("EvaluateMatcherOptionsPressed")
                .build());
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        //Do nothing on cancel
    }

    @Override
    public void onDialogChatWithMatcher(MatcherOptionsDialog dialog) { //Start matcher chat activity
        Intent i;
        i = new Intent(this, ChatActivity.class);
        i.putExtra("chat_id", thisMatchee.matcher_chat_id);
        i.putExtra("pair_id", thisMatch.pair_id);
        startActivity(i);

        //Google Analytics
        t.send(new HitBuilders.EventBuilder()
                .setCategory("ui_action")
                .setAction("button_press")
                .setLabel("EvaluateMatcherChatPressed")
                .build());
    }

    @Override
    public void onDialogBlockMatcher(MatcherOptionsDialog dialog) {
        Log.e("Matcher blocked!", "TO BLOCK!");

        //Add this contact to this user's blocked users list
        Map<String, Integer> options = new HashMap<String, Integer>();
        options.put("contact_id",((Global)getApplication()).thisUser.contact_id);
        options.put("to_block_contact_id",thisMatch.matcher.contact_id);

        ((Global)getApplication()).ui.blockContact(options, new Callback<StringResponse>() {
            @Override
            public void success(StringResponse stringResponse, Response response) {
                Log.e("Added to blocked", stringResponse.response);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("Failed to block", error.toString());

                //Google Analytics
                t.send(new HitBuilders.ExceptionBuilder()
                        .setDescription("(Evaluate) Failed to block the contact: " +
                                new StandardExceptionParser(EvaluateActivity.this, null)
                                        .getDescription(Thread.currentThread().getName(), error))
                        .setFatal(false)
                        .build());
            }
        });

        Style.makeToast(this,"This matcher has been blocked");

        //Google Analytics
        t.send(new HitBuilders.EventBuilder()
                .setCategory("ui_action")
                .setAction("button_press")
                .setLabel("EvaluateBlockMatcherPressed")
                .build());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Intent i = new Intent(this,SplashActivity.class);
        startActivity(i);
        finish(); //Go to main activity if restored
    }

    //Handles showing alert for long-press on matcher image
    private class LongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View view) {
            Vibrator v = (Vibrator) EvaluateActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern = {0,60,25,60};
            v.vibrate(pattern,-1);

            if (view.getId() == R.id.matcher_image_view || view.getId() == R.id.matcher_name || view.getId() == R.id.matcher_option_dots) {
                showMatcherOptionsDialog();
                return true;
            }
            return false;
        }
    }

    //Handles color animation when touching matcher image
    private class ButtonTouchListener implements View.OnTouchListener {

        ValueAnimator colorAnimation;

        @Override
        public boolean onTouch(View v, MotionEvent motionEvent) {

            Integer colorFrom;
            Integer colorTo;

            if (v.getId() == R.id.matcher_image_view || v.getId() == R.id.matcher_name) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) { //White to pink on touch 'down'

                    colorFrom = getResources().getColor(R.color.white);
                    colorTo = getResources().getColor(R.color.matchflare_pink);

                    if (colorAnimation != null && colorAnimation.isRunning()) { //End running animation
                        colorAnimation.end();
                    }

                    //Set-up and start animation
                    colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                    colorAnimation.setDuration(ViewConfiguration.getLongPressTimeout());
                    colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
                            matcherName.setTextColor((Integer)animator.getAnimatedValue());
                        }
                    });
                    colorAnimation.start();
                }
                else if (motionEvent.getAction() == MotionEvent.ACTION_UP) { //End animation on touch 'up' i.e. Pink to White
                    colorTo = getResources().getColor(R.color.white);
                    colorFrom = getResources().getColor(R.color.matchflare_pink);

                    if (colorAnimation != null && colorAnimation.isRunning()) {
                        colorAnimation.end();
                    }
                    colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                    colorAnimation.setDuration(150);
                    colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
                            matcherName.setTextColor((Integer)animator.getAnimatedValue());
                        }

                    });
                    colorAnimation.start();
                }
            }
            return false;
        }
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
                intent = new Intent(this, PresentMatchesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

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

                Style.makeToast(EvaluateActivity.this,"New Notification!");
                checkChatsForUnreadMessages();
            }
        }
    };
}
