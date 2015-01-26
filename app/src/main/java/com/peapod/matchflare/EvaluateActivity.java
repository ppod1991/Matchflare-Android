package com.peapod.matchflare;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.DialogFragment;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class EvaluateActivity extends Activity implements Callback<Match>, View.OnClickListener, MatcherOptionsDialog.MatcherOptionsDialogListener {

    Match thisMatch;
    RestService ui = ((Global) getApplication()).ui;

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

    Person thisMatchee;
    Person otherMatchee;

    View progressIndicator;
    View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evaluate);

        progressIndicator = findViewById(R.id.progress_indicator);
        root = findViewById(R.id.root_evaluate);

        progressIndicator.setVisibility(View.VISIBLE);
        root.setVisibility(View.GONE);

        Bundle extras = this.getIntent().getExtras();
        thisMatch = (Match) extras.getSerializable("pair");

        matchButton = (ImageView) findViewById(R.id.evaluate_match_button);
        passButton = (ImageView) findViewById(R.id.evaluate_pass_button);
        chatButton = (ImageView) findViewById(R.id.chat_button);
        askButton = (ImageView) findViewById(R.id.ask_matcher_button);
        matcherOptionsButton = (ImageView) findViewById(R.id.matcher_option_dots);

        matcherOptionsButton.bringToFront();
        askButton.bringToFront();

        matcherImageView = (ImageView) findViewById(R.id.matcher_image_view);
        otherMatcheeImageView = (ImageView) findViewById(R.id.other_matchee_image_view);

        matcherName = (TextView) findViewById(R.id.matcher_name);
        otherMatcheeName = (TextView) findViewById(R.id.other_matchee_name);
        matchDescription = (TextView) findViewById(R.id.match_description);

        Style.toOpenSans(this,matcherName,"light");
        Style.toOpenSans(this,otherMatcheeName,"light");
        Style.toOpenSans(this,matchDescription,"light");

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
    }


    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter=new IntentFilter("com.peapod.matchflare.push_notification");
        LocalBroadcastManager.getInstance(this).registerReceiver(onEvent, filter);
        checkChatsForUnreadMessages();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onEvent);
        super.onPause();
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


    private class ButtonTouchListener implements View.OnTouchListener {

        ValueAnimator colorAnimation;
        @Override
        public boolean onTouch(View v, MotionEvent motionEvent) {

            Integer colorFrom;
            Integer colorTo;

            if (v.getId() == R.id.matcher_image_view || v.getId() == R.id.matcher_name) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    colorFrom = getResources().getColor(R.color.white);
                    colorTo = getResources().getColor(R.color.matchflare_pink);

                    if (colorAnimation != null && colorAnimation.isRunning()) {
                        colorAnimation.end();
                    }
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
                else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                    ImageView thisImage = (ImageView) currentLayout.findViewById(R.id.first_matchee_image_view);
//                    Animation shrink = AnimationUtils.loadAnimation(PresentMatchesActivity.this,R.anim.shrink);
//                    thisImage.startAnimation(shrink);
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

    private class LongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View view) {
            Vibrator v = (Vibrator) EvaluateActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            long[] pattern = {0,60,25,60};
            v.vibrate(pattern,-1);

            if (view.getId() == R.id.matcher_image_view || view.getId() == R.id.matcher_name || view.getId() == R.id.matcher_option_dots) {
                showMatcherOptionsDialog();
                return true;
            }

            return false;
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.evaluate, menu);
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

    public class UnseenObject {
        Boolean has_unseen;
    }

    public void checkChatsForUnreadMessages() {

        int contact_id = ((Global) getApplication()).thisUser.contact_id;
        if (contact_id > 0 && thisMatch != null) {
            Map<String, Integer> options = new HashMap<String, Integer>();
            options.put("contact_id",contact_id);
            options.put("chat_id",thisMatch.chat_id);
            ((Global) getApplication()).ui.hasUnread(options, new Callback<UnseenObject> () {

                @Override
                public void success(UnseenObject aBoolean, Response response) {
                    if (aBoolean != null && aBoolean.has_unseen.booleanValue() == true) {
                        chatButton.setImageDrawable(EvaluateActivity.this.getResources().getDrawable(R.drawable.new_message_chat_button));
                    }
                    else {
                        chatButton.setImageDrawable(EvaluateActivity.this.getResources().getDrawable(R.drawable.chat_button));

                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e("Failed to check if chat has unread messages",error.toString());
                }
            });

            Map<String, Integer> options2 = new HashMap<String, Integer>();
            options2.put("contact_id",((Global) getApplication()).thisUser.contact_id);
            options2.put("chat_id",thisMatchee.matcher_chat_id);
            ((Global) getApplication()).ui.hasUnread(options2, new Callback<UnseenObject> () {

                @Override
                public void success(UnseenObject aBoolean, Response response) {
                    if (aBoolean != null && aBoolean.has_unseen.booleanValue()) {
                        askButton.setImageDrawable(EvaluateActivity.this.getResources().getDrawable(R.drawable.new_message_chat_button));
                    }
                    else {
                        askButton.setImageDrawable(EvaluateActivity.this.getResources().getDrawable(R.drawable.ask_button));

                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e("Failed to check if chat has unread messages",error.toString());
                }
            });
        }


    }
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
            Picasso.with(this).load(matcherImage).fit().centerInside().transform(new CircleTransform()).into(matcherImageView);
            matcherName.setText("A friend");
        }
        else {
            Picasso.with(this).load(thisMatch.matcher.image_url).fit().centerInside().transform(new CircleTransform()).into(matcherImageView);
            matcherName.setText(thisMatch.matcher.guessed_full_name);
        }



        Picasso.with(this).load(otherMatchee.image_url).fit().centerInside().transform(new CircleTransform()).into(otherMatcheeImageView);
        otherMatcheeName.setText(otherMatchee.guessed_full_name);

//        int myContactID = ((Global) getApplication()).thisUser.contact_id;
//
//        if (match.first_matchee.contact_id == myContactID) {
//            matcherName.setText(match.second_matchee.guessed_full_name);
//        }
//        else if (match.second_matchee.contact_id == myContactID) {
//            matcherName.setText(match.first_matchee.guessed_full_name);
//        }
//        else {
//            matcherName.setText("ERROR :( Try again.");
//        }
//
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

        Log.e("Successfully retrieved the match", response.toString());

        thisMatch = match;
        setMatcheeName();
        progressIndicator.setVisibility(View.GONE);
        root.setVisibility(View.VISIBLE);

    }

    @Override
    public void failure(RetrofitError error) {
        Log.e("Failed to retrieve the match", error.toString());

    }

    @Override
    public void onClick(View view) {
        Log.e("Clicked to respond to match!", "woo");
        EvaluateResponse response = new EvaluateResponse();
        response.contact_id = ((Global) getApplication()).thisUser.contact_id;
        response.pair_id = thisMatch.pair_id;

        Intent i = new Intent(this,PresentMatchesActivity.class);


        if (view.getId() == R.id.evaluate_match_button) {
            response.decision = "ACCEPT";
            ui.respondToMatchRequest(response, new RespondCallback());
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
        }
        else if (view.getId() == R.id.evaluate_pass_button) {
            response.decision = "REJECT";
            ui.respondToMatchRequest(response, new RespondCallback());
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
        }
        else if (view.getId() == R.id.chat_button) {
            i = new Intent(this, ChatActivity.class);
            i.putExtra("chat_id", thisMatch.chat_id);
            i.putExtra("pair_id", thisMatch.pair_id);
            startActivity(i);
        }
        else if (view.getId() == R.id.ask_matcher_button) {
            i = new Intent(this, ChatActivity.class);
            i.putExtra("chat_id", thisMatchee.matcher_chat_id);
            i.putExtra("pair_id", thisMatch.pair_id);
            startActivity(i);
        }
        else if (view.getId() == R.id.matcher_option_dots || view.getId() == R.id.matcher_name || view.getId() == R.id.matcher_image_view) {
            showMatcherOptionsDialog();
        }



    }

    public class RespondCallback implements Callback<StringResponse> {
        @Override
        public void success(StringResponse response, Response response2) {
            Log.e("Successfully posted response to match", response.response);
        }

        @Override
        public void failure(RetrofitError error) {
            Log.e("Failure to post response to match", error.toString());
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

    public void showMatcherOptionsDialog() {
        MatcherOptionsDialog matcherOptionsDialog = MatcherOptionsDialog.newInstance(thisMatch.is_anonymous,thisMatch.matcher);
        matcherOptionsDialog.show(getFragmentManager(),"options_dialog");
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {

    }

    @Override
    public void onDialogChatWithMatcher(MatcherOptionsDialog dialog) {
        Intent i;
        i = new Intent(this, ChatActivity.class);
        i.putExtra("chat_id", thisMatchee.matcher_chat_id);
        i.putExtra("pair_id", thisMatch.pair_id);
        startActivity(i);
    }

    @Override
    public void onDialogBlockMatcher(MatcherOptionsDialog dialog) {
        Log.e("Matcher selected for blocking", "TO BLOCK!");

        //Add this contact to this user's blocked users list -- NEED TO IMPLEMENT
        Map<String, Integer> options = new HashMap<String, Integer>();
        options.put("contact_id",((Global)getApplication()).thisUser.contact_id);
        options.put("to_block_contact_id",thisMatch.matcher.contact_id);
        ((Global)getApplication()).ui.blockContact(options, new Callback<StringResponse>() {
            @Override
            public void success(StringResponse stringResponse, Response response) {
                Log.e("Successfully added this user to your block contacts", stringResponse.response);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("Failed to block this matcher", error.toString());
            }
        });

        Style.makeToast(this,"This matcher has been blocked");

    }

}
