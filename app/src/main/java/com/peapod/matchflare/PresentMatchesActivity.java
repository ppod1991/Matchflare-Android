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
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;
import com.peapod.matchflare.Objects.Notification;
import com.peapod.matchflare.Objects.NotificationLists;
import com.peapod.matchflare.Objects.Person;
import com.peapod.matchflare.Objects.StringResponse;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/*
 * The main activity where the user is presented with randomized pairs of friends for matching
 */
public class PresentMatchesActivity extends Activity implements MatcheeOptionsDialog.MatcheeOptionsDialogListener {

    //Activity components
    TextView firstContactTextview;
    TextView secondContactTextview;
    ImageView passButton;
    ImageView matchButton;
    TextView display;
    ImageView firstImage;
    ImageView secondImage;
    ImageView firstOptionDots;
    ImageView secondOptionDots;
    ImageView nextFirstOptionDots;
    ImageView nextSecondOptionDots;
    ImageView nextFirstImage;
    ImageView nextSecondImage;
    TextView nextFirstContactTextview;
    TextView nextSecondContactTextview;
    TextSwitcher resultDisplay;
    TextSwitcher scoreDisplay;
    CheckBox anonymousCheckbox;
    TextView notificationCountView;
    Menu myMenu;

    LinearLayout currentLayout;
    LinearLayout nextLayout;
    LinearLayout tempLayout;
    ViewSwitcher.ViewFactory viewFactory;
    ViewSwitcher.ViewFactory viewFactoryScore;

    RelativeLayout root;
    View progressIndicator;

    //Activity variables
    Queue<Match> matches = new LinkedList<Match>();
    Match currentMatch;
    Match nextMatch;
    String matchInstructions = "should they meet?";
    String eloInstructions = "who's the better catch?";
    String instructionsText;
    int matchflareScore = 0;
    MatcheeOptionsDialog optionsDialog;
    NotificationLists notificationsList = null;
    private float initialX;
    private boolean isCurrentlyDragging = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_present_matches);

        //Retrieve components
        progressIndicator = findViewById(R.id.progress_indicator);
        root = (RelativeLayout) findViewById(R.id.root_present_matches);
        display = (TextView) findViewById(R.id.instructions_results);
        firstImage = (ImageView) findViewById(R.id.first_matchee_image_view);
        secondImage = (ImageView) findViewById(R.id.second_matchee_image_view);
        resultDisplay = (TextSwitcher) findViewById(R.id.result_text);
        scoreDisplay = (TextSwitcher) findViewById(R.id.score_view);
        anonymousCheckbox = (CheckBox) findViewById(R.id.anonymous_checkbox);
        passButton = (ImageView) findViewById(R.id.pass_button);
        matchButton = (ImageView) findViewById(R.id.match_button);

        //Style the components
        Style.toOpenSans(this, display, "light");
        Style.toOpenSans(this,anonymousCheckbox,"light");

        root.setVisibility(View.GONE);
        progressIndicator.setVisibility(View.VISIBLE);
        display.bringToFront();
        display.setVisibility(View.INVISIBLE);
        matchButton.bringToFront();
        passButton.bringToFront();

        viewFactory = new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                //Create new textView and set the properties like color, size etc
                TextView myText = new TextView(PresentMatchesActivity.this);
                myText.setTextSize(15);
                myText.setAlpha(0.8f);
                myText.setGravity(Gravity.CENTER_HORIZONTAL);
                Style.toOpenSans(PresentMatchesActivity.this,myText,"light");
                return myText;
            }
        };
        resultDisplay.setFactory(viewFactory); //View Switcher for the Result of the current match

        //Set animation for result text switcher
        Animation in = AnimationUtils.loadAnimation(this,
                R.anim.name_slide_in);
        in.setDuration(200);
        Animation out = AnimationUtils.loadAnimation(this,
                R.anim.name_slide_out);
        out.setDuration(200);
        out.setInterpolator(new AccelerateDecelerateInterpolator());
        in.setInterpolator(new AccelerateDecelerateInterpolator());
        resultDisplay.setInAnimation(in);
        resultDisplay.setOutAnimation(out);
        resultDisplay.setText(matchInstructions);

        viewFactoryScore = new ViewSwitcher.ViewFactory() {

            @Override
            public View makeView() {

                // create new textView and set the properties like color, size etc
                TextView myText = new TextView(PresentMatchesActivity.this);
                myText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                myText.setAlpha(0.8f);
                Resources r = getResources();
                int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, r.getDisplayMetrics());
                myText.setPadding(px,px,px,px);
                myText.setTextColor(getResources().getColor(R.color.light_gray));
                Style.toOpenSans(PresentMatchesActivity.this,myText,"light");
                return myText;
            }
        };
        scoreDisplay.setFactory(viewFactoryScore); //View switcher for the Matchflare score

        //Set animation for the score display text switcher
        Animation fadeIn = AnimationUtils.loadAnimation(this,android.R.anim.fade_in);
        fadeIn.setDuration(150);
        Animation fadeOut = AnimationUtils.loadAnimation(this,android.R.anim.fade_out);
        fadeOut.setDuration(150);
        scoreDisplay.setInAnimation(fadeIn);
        scoreDisplay.setOutAnimation(fadeOut);
        scoreDisplay.setText("n/a");


        //Retrieve first sliding match layout
        currentLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.sliding_match, null);
        root.addView(currentLayout); //Add first sliding match layout to main layout
        initialX = currentLayout.getX();
        firstContactTextview = (TextView) currentLayout.findViewById(R.id.first_contact_textview);
        secondContactTextview = (TextView) currentLayout.findViewById(R.id.second_contact_textview);
        Style.toOpenSans(this,firstContactTextview,"light");
        Style.toOpenSans(this,secondContactTextview,"light");

        //Retrieve first sliding match layout components
        firstImage = (ImageView) currentLayout.findViewById(R.id.first_matchee_image_view);
        secondImage = (ImageView) currentLayout.findViewById(R.id.second_matchee_image_view);
        firstOptionDots = (ImageView) currentLayout.findViewById(R.id.first_option_dots);
        secondOptionDots = (ImageView) currentLayout.findViewById(R.id.second_option_dots);

        //Style first sliding match components
        firstOptionDots.bringToFront();
        secondOptionDots.bringToFront();


        //Retrieve second sliding match layout
        nextLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.sliding_match, null);
        nextLayout.setVisibility(View.GONE);
        root.addView(nextLayout); //Add second sliding match layout to main layout

        //Retrieve second sliding match layout
        nextFirstContactTextview = (TextView) nextLayout.findViewById(R.id.first_contact_textview);
        nextSecondContactTextview = (TextView) nextLayout.findViewById(R.id.second_contact_textview);
        nextFirstImage = (ImageView) nextLayout.findViewById(R.id.first_matchee_image_view);
        nextSecondImage = (ImageView) nextLayout.findViewById(R.id.second_matchee_image_view);
        nextFirstOptionDots = (ImageView) nextLayout.findViewById(R.id.first_option_dots);
        nextSecondOptionDots = (ImageView) nextLayout.findViewById(R.id.second_option_dots);

        //Style second sliding match components
        Style.toOpenSans(this,nextFirstContactTextview,"light");
        Style.toOpenSans(this,nextSecondContactTextview,"light");
        nextFirstOptionDots.bringToFront();
        nextSecondOptionDots.bringToFront();

        //Set listeners
        ButtonTouchListener touchListener = new ButtonTouchListener();
        firstImage.setOnTouchListener(touchListener);
        secondImage.setOnTouchListener(touchListener);
        nextFirstImage.setOnTouchListener(touchListener);
        nextSecondImage.setOnTouchListener(touchListener);
        firstContactTextview.setOnTouchListener(touchListener);
        secondContactTextview.setOnTouchListener(touchListener);
        nextFirstContactTextview.setOnTouchListener(touchListener);
        nextSecondContactTextview.setOnTouchListener(touchListener);

        ButtonListener buttonListener = new ButtonListener();
        passButton.setOnClickListener(buttonListener);
        matchButton.setOnClickListener(buttonListener);
        firstContactTextview.setOnClickListener(buttonListener);
        secondContactTextview.setOnClickListener(buttonListener);
        nextFirstContactTextview.setOnClickListener(buttonListener);
        nextSecondContactTextview.setOnClickListener(buttonListener);
        firstImage.setOnClickListener(buttonListener);
        secondImage.setOnClickListener(buttonListener);
        nextFirstImage.setOnClickListener(buttonListener);
        nextSecondImage.setOnClickListener(buttonListener);
        firstOptionDots.setOnClickListener(buttonListener);
        secondOptionDots.setOnClickListener(buttonListener);
        nextFirstOptionDots.setOnClickListener(buttonListener);
        nextSecondOptionDots.setOnClickListener(buttonListener);
        scoreDisplay.setOnClickListener(buttonListener);

        LongClickListener longClickListener = new LongClickListener();
        firstContactTextview.setOnLongClickListener(longClickListener);
        secondContactTextview.setOnLongClickListener(longClickListener);
        nextFirstContactTextview.setOnLongClickListener(longClickListener);
        nextSecondContactTextview.setOnLongClickListener(longClickListener);
        firstImage.setOnLongClickListener(longClickListener);
        secondImage.setOnLongClickListener(longClickListener);
        nextFirstImage.setOnLongClickListener(longClickListener);
        nextSecondImage.setOnLongClickListener(longClickListener);
        firstOptionDots.setOnLongClickListener(longClickListener);
        secondOptionDots.setOnLongClickListener(longClickListener);
        nextFirstOptionDots.setOnLongClickListener(longClickListener);
        nextSecondOptionDots.setOnLongClickListener(longClickListener);

        //Retrieve intent data
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getSerializable("matches") != null) {
            matches.addAll((ArrayList<Match>) extras.getSerializable("matches"));
        }

        //Load new match if no current match, else set the match
        if (matches.size() == 0) {
            Map<String,Integer> options = new HashMap<String,Integer>();
            options.put("contact_id",((Global) getApplication()).thisUser.contact_id);
            ((Global)getApplication()).ui.getMatches(((Global) getApplication()).thisUser, options, new MatchesCallback());
        }
        else {
            presentNextMatch(true);
            progressIndicator.setVisibility(View.GONE);
            root.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (((Global) getApplication()).thisUser.contact_id > 0) {

            invalidateOptionsMenu(); //Change menu if just registered
            Map<String, Integer> options = new HashMap<String, Integer>();
            int contact_id = ((Global) getApplication()).thisUser.contact_id;

            if (contact_id > 0) { //Get and set matchflare score if this is a registered user
                options.put("contact_id",contact_id);
                ((Global) getApplication()).ui.getScore(options, new postMatchCallback());
                ((Global) getApplication()).ui.getNotificationLists(options, new NotificationsListCallback());
            }
        };

        //Register for remote notifications
        IntentFilter filter=new IntentFilter("com.peapod.matchflare.push_notification");
        LocalBroadcastManager.getInstance(this).registerReceiver(onEvent, filter);

        //Google Analytics
        Tracker t = ((Global) this.getApplication()).getTracker();
        t.setScreenName("PresentMatchesActivity");
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    @Override
    public void onPause() {
        //Unregister from remote notifications
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onEvent);
        super.onPause();
    }

    /*
     * Changes the match result to the specified string for the specified time and goes back to the default string
     * @param text The text to momentarily change the result to
     * @param timeOnScreen The number of milliseconds for which to keep the momentarily result for
     */
    public void changeResult(String text,int timeOnScreen) {
        ((TextView) resultDisplay.getNextView()).setTextColor(getResources().getColor(R.color.matchflare_pink));
        resultDisplay.setText(text);
        resultDisplay.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((TextView) resultDisplay.getNextView()).setTextColor(getResources().getColor(R.color.light_gray));
                resultDisplay.setText(instructionsText);
            }
        }, timeOnScreen);
    };

    /*Changes the default string to the specified string
     *@param text The string to make default
     */
    public void changeDisplayText(String text) {
        instructionsText = text;
    };

    //Options Dialog Methods


    //Create and show the options dialog for the specified person
    public void showOptionsDialog(Boolean isFirst, Person chosenMatchee) {
        optionsDialog = MatcheeOptionsDialog.newInstance(isFirst, chosenMatchee);
        optionsDialog.show(getFragmentManager(),"options_dialog");

        //Google Analytics
        Tracker t = ((Global) getApplication()).getTracker();
        if (isFirst) {
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("PresentFirstMatcheeOptionsPressed")
                    .build());
        }
        else {
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("PresentSecondMatcheeOptionsPressed")
                    .build());
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        //Do nothing on cancel
    }

    @Override
    public void onDialogSetMatchee(Person selectedPerson, MatcheeOptionsDialog dialog) {
        //Called when user selected a person from the dialog

        currentMatch.wasEdited = true;
        showMatchButtons();

        //Replace the long-clicked matchee with the chosen matchee
        if (dialog.isFirstMatchee) {
            currentMatch.first_matchee = selectedPerson;
            ((TextView) currentLayout.findViewById(R.id.first_contact_textview)).setText(currentMatch.first_matchee.guessed_full_name);
            Picasso.with(this).load(currentMatch.first_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(((ImageView) currentLayout.findViewById(R.id.first_matchee_image_view)));
        }
        else {
            currentMatch.second_matchee = selectedPerson;
            ((TextView) currentLayout.findViewById(R.id.second_contact_textview)).setText(currentMatch.second_matchee.guessed_full_name);
            Picasso.with(this).load(currentMatch.second_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(((ImageView) currentLayout.findViewById(R.id.second_matchee_image_view)));
        }

        //Google Analytics
        Tracker t = ((Global) getApplication()).getTracker();
        t.send(new HitBuilders.EventBuilder()
                .setCategory("ui_action")
                .setAction("button_press")
                .setLabel("PresentArbitraryMatcheeSet")
                .build());
    }

    @Override
    public void onDialogRemoveMatchee(Person selectedPerson, MatcheeOptionsDialog dialog) {
        //Called when a user wants to stop seeing a given person

        //Send request to remove contact
        Map<String, Integer> options = new HashMap<String, Integer>();
        options.put("contact_id",((Global)getApplication()).thisUser.contact_id);
        options.put("to_remove_contact_id",selectedPerson.contact_id);
        ((Global)getApplication()).ui.removeContact(options, new Callback<StringResponse>() {
            @Override
            public void success(StringResponse stringResponse, Response response) {
                Log.e("Added user to removed", stringResponse.response);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("Failed to remove", error.toString());

                //Google Analytics
                Tracker t = ((Global) getApplication()).getTracker();
                t.send(new HitBuilders.ExceptionBuilder()
                        .setDescription("(PresentMatches) Failure to stop showing friend: " +
                                new StandardExceptionParser(PresentMatchesActivity.this, null)
                                        .getDescription(Thread.currentThread().getName(), error))
                        .setFatal(false)
                        .build());
            }
        });

        Style.makeToast(this,selectedPerson.guessed_full_name + " will not be shown in future matches");
        presentNextMatch(true); //Proceed to next match

        //Google Analytics
        Tracker t = ((Global) getApplication()).getTracker();
        t.send(new HitBuilders.EventBuilder()
                .setCategory("ui_action")
                .setAction("button_press")
                .setLabel("PresentMatcheeBlocked")
                .build());
    }

    //Listens for long-clicks for launching matchee options dialog
    private class LongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View view) {

            if (!isCurrentlyDragging) { //Only respond if the user is not current dragging
                Vibrator v = (Vibrator) PresentMatchesActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = {0,60,25,60};
                v.vibrate(pattern,-1);

                //Launch the options dialog with the right matchee
                if (view.getId() == R.id.first_contact_textview || view.getId() == R.id.first_matchee_image_view || view.getId() == R.id.first_option_dots) {
                    showOptionsDialog(true, currentMatch.first_matchee);
                    return true;
                }
                else if (view.getId() == R.id.second_contact_textview || view.getId() == R.id.second_matchee_image_view || view.getId() == R.id.second_option_dots) {
                    showOptionsDialog(false, currentMatch.second_matchee);
                    return true;
                }
            }
            return false;
        }
    }

    // ----- END OF OPTIONS DIALOG METHODS -----


    //Post the result of the currently shown match as 'ACCEPT' and brings about next pair
    public void makeMatch() {

        currentMatch.matcher.contact_id = ((Global) getApplication()).thisUser.contact_id;
        if (currentMatch.matcher == null || !(currentMatch.matcher.contact_id > 0)) { //If the user is not registered yet..

            currentLayout.setX(initialX); //Reset layout position
            resultDisplay.setCurrentText(matchInstructions); //Set result text to default

            //Show a dialog leading to registration intent
            AlertDialog dialog = new AlertDialog.Builder(PresentMatchesActivity.this)
                    .setTitle("Hold up, Cupid!")
                    .setMessage("You need to register before matching someone--it'll take <15 seconds!")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(PresentMatchesActivity.this, VerificationActivity.class);
                            startActivity(i);
                        }
                    })
                    .show();

            //Google Analytics
            Tracker t = ((Global) getApplication()).getTracker();
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("PresentAttemptedMatchWithoutRegistering")
                    .build());
        }
        //Else if the user is trying to match him/herself...
        else if (currentMatch.first_matchee.contact_id == ((Global) getApplication()).thisUser.contact_id || currentMatch.second_matchee.contact_id == ((Global) getApplication()).thisUser.contact_id) {
            currentLayout.setX(initialX);
            resultDisplay.setCurrentText(matchInstructions);

            Style.makeToast(PresentMatchesActivity.this,"Tricky tricky. Sorry, but you can't match yourself.");

            //Google Analytics
            Tracker t = ((Global) getApplication()).getTracker();
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("PresentAttemptedSelfMatch")
                    .build());
        }
        //Else if the user is trying to match the same person...
        else if (currentMatch.first_matchee.contact_id == currentMatch.second_matchee.contact_id) {
            currentLayout.setX(initialX);
            resultDisplay.setCurrentText(matchInstructions);

            Style.makeToast(PresentMatchesActivity.this,"Stop confusing us. You can't match the same person, silly.");

            //Google Analytics
            Tracker t = ((Global) getApplication()).getTracker();
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("PresentAttemptedSamePersonMatch")
                    .build());
        }
        else { //If the match is valid...

            //Check if this is the first match ever. If so, then alert with instructions
            final SharedPreferences prefs = this.getSharedPreferences(
                    "com.peapod.matchflare", Context.MODE_PRIVATE);
            final String firstMatchKey = "com.example.app.IS_NOT_FIRST_MATCH";
            boolean isNotFirstMatch = prefs.getBoolean(firstMatchKey,false);

            if (!isNotFirstMatch) {                 //Show instructional alert

                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Go Cupid!")
                        .setMessage("We're about to alert one friend (via SMS or push notification) that you think they should meet!*\n\n*std rates apply")
                        .setPositiveButton("Do it!", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //Save new user preference
                                prefs.edit().putBoolean(firstMatchKey,true).apply();
                                makeMatch();

                                Tracker t = ((Global) getApplication()).getTracker();
                                t.send(new HitBuilders.EventBuilder()
                                        .setCategory("ui_action")
                                        .setAction("button_press")
                                        .setLabel("PresentAcceptUponMatchInstructions")
                                        .build());
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                                Tracker t = ((Global) getApplication()).getTracker();
                                t.send(new HitBuilders.EventBuilder()
                                        .setCategory("ui_action")
                                        .setAction("button_press")
                                        .setLabel("PresentCancelUponMatchInstructions")
                                        .build());
                            }
                        })
                        .show();

                TextView messageView = (TextView)dialog.findViewById(android.R.id.message);
                messageView.setGravity(Gravity.CENTER);

                currentLayout.setX(initialX);
                resultDisplay.setCurrentText(matchInstructions);
            }
            else { //This user has made a match before, so post match result
                RestService ui = ((Global) getApplication()).ui;
                changeResult("match made!",900);

                currentMatch.match_status = "MATCHED";
                if (anonymousCheckbox.isChecked()) { //Determine if this match was made anonymously
                    currentMatch.is_anonymous = true;

                    //Google Analytics
                    Tracker t = ((Global) getApplication()).getTracker();
                    t.send(new HitBuilders.EventBuilder()
                            .setCategory("ui_action")
                            .setAction("button_press")
                            .setLabel("PresentMatchTriggeredAnonymously")
                            .build());
                }
                else {
                    currentMatch.is_anonymous = false;

                    //Google Analytics
                    Tracker t = ((Global) getApplication()).getTracker();
                    t.send(new HitBuilders.EventBuilder()
                            .setCategory("ui_action")
                            .setAction("button_press")
                            .setLabel("PresentMatchTriggeredUnanonymously")
                            .build());
                }

                ui.addMatch(currentMatch,null,new postMatchCallback()); //Post the result
                presentNextMatch(false); //Show the next match
            }
        }
    };

    //If the current match was passed...
    public void passMatch() {
        presentNextMatch(true); //Show the next match
        changeResult("match passed",900);

        //Google Analytics
        Tracker t = ((Global) getApplication()).getTracker();
        t.send(new HitBuilders.EventBuilder()
                .setCategory("ui_action")
                .setAction("button_press")
                .setLabel("PresentPassMatchTriggered")
                .build());
    };

    //Button Click Listener
    private class ButtonListener implements View.OnClickListener {
        public void onClick(View v) {

            if (v.getId() == R.id.match_button) {
               makeMatch();
            }
            else if (v.getId() == R.id.pass_button) {
               passMatch();
            }
            else if (v.getId() == R.id.first_option_dots) {
                showOptionsDialog(true, currentMatch.first_matchee);
            }
            else if (v.getId() == R.id.second_option_dots) {
                showOptionsDialog(false, currentMatch.second_matchee);
            }
            else if (v.getId() == R.id.score_view) { //Explanatory dialog explaining Matchflare score
                AlertDialog dialog = new AlertDialog.Builder(PresentMatchesActivity.this)
                        .setTitle("Your Matchflare Score: " + matchflareScore)
                        .setMessage("The more matches you make, the higher your score!\n\nThe higher your score, the more likely you are to get matched!")
                        .setPositiveButton("Got it!", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //Do nothing on accept
                            }
                        })
                        .show();

                TextView messageView = (TextView)dialog.findViewById(android.R.id.message);
                messageView.setGravity(Gravity.CENTER);

                //Google analytics
                Tracker t = ((Global) getApplication()).getTracker();
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("ui_action")
                        .setAction("button_press")
                        .setLabel("PresentScoreLabelTapped")
                        .build());
            }
        }
    }

    public void showMatchButtons() {
        passButton.setVisibility(View.VISIBLE);
        matchButton.setVisibility(View.VISIBLE);
        anonymousCheckbox.setVisibility(View.VISIBLE);
        changeDisplayText(matchInstructions);
    }

    public void hideMatchButtons() {
        passButton.setVisibility(View.INVISIBLE);
        matchButton.setVisibility(View.INVISIBLE);
        anonymousCheckbox.setVisibility(View.INVISIBLE);
        changeDisplayText(eloInstructions);
    }

    //Handles sliding of the current sliding layout
    private class ButtonTouchListener implements View.OnTouchListener {

        float xDelta;
        VelocityTracker mVelocityTracker = null;
        ValueAnimator colorAnimation;

        @Override
        public boolean onTouch(View v, MotionEvent motionEvent) {

            Integer colorFrom;
            Integer colorTo;

            if (v.getId() == R.id.first_contact_textview || v.getId() == R.id.first_matchee_image_view) { //If first contact...
                final TextView thisText = (TextView) currentLayout.findViewById(R.id.first_contact_textview);

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) { //On touch down, animate white to pink

                    //Animate color
                    colorFrom = getResources().getColor(R.color.white);
                    colorTo = getResources().getColor(R.color.matchflare_pink);
                    if (colorAnimation != null && colorAnimation.isRunning()) { //Cancel current animation
                        colorAnimation.end();
                    }
                    colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                    colorAnimation.setDuration(ViewConfiguration.getLongPressTimeout());
                    colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
                            thisText.setTextColor((Integer)animator.getAnimatedValue());
                        }
                    });
                    colorAnimation.start();

                    //Initialize and track x-position
                    xDelta = motionEvent.getRawX() - currentLayout.getX();
                    if (mVelocityTracker == null) {
                        mVelocityTracker = VelocityTracker.obtain();
                    }
                    else {
                        mVelocityTracker.clear();
                    }
                    mVelocityTracker.addMovement(motionEvent);

                }
                else if (motionEvent.getAction() == MotionEvent.ACTION_UP) { //On release, check position and change back color
                    isCurrentlyDragging = false;
                    resultDisplay.setCurrentText(matchInstructions);

                    //Animate color
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
                            thisText.setTextColor((Integer)animator.getAnimatedValue());
                        }
                    });
                    colorAnimation.start();

                    //Check resulting velocity and position to determine result of slide
                    mVelocityTracker.addMovement(motionEvent);
                    mVelocityTracker.computeCurrentVelocity(1000);
                    float xVelocity = VelocityTrackerCompat.getXVelocity(mVelocityTracker,motionEvent.getPointerId(motionEvent.getActionIndex()));
                    float dpMoved = dpFromPx(motionEvent.getRawX() - xDelta);
                    if ((dpFromPx(Math.abs(xVelocity)) > (100) && dpMoved < -15) || dpMoved < -35) { //Sufficient left position or left velocity
                        PresentMatchesActivity.this.passMatch();    //Match passed
                    }
                    else if ((dpFromPx(Math.abs(xVelocity)) > (100) && dpMoved > 15) || dpMoved > 35) { //Sufficient right position or right velocity
                        PresentMatchesActivity.this.makeMatch();   //Match accepted
                    }
                    else {
                        cancelSwipe();
                    }
                }
                else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) { //If the user is dragging...

                    //Track the movement
                    currentLayout.setX(motionEvent.getRawX() - xDelta);
                    mVelocityTracker.addMovement(motionEvent);
                    float dpMoved = dpFromPx(motionEvent.getRawX() - xDelta);
                    if (Math.abs(dpMoved) > 15) {
                        isCurrentlyDragging = true;
                    }

                    //Set status hint...
                    if (dpMoved > 35) {
                        resultDisplay.setCurrentText("yes");
                    }
                    else if (dpMoved < -35) {
                        resultDisplay.setCurrentText("no");
                    }
                    else {
                        resultDisplay.setCurrentText(matchInstructions);
                    }
                }
                else if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                    cancelSwipe();
                }
            }
            //Repeat for second textview
            else if (v.getId() == R.id.second_contact_textview || v.getId() == R.id.second_matchee_image_view) {
                final TextView thisText = (TextView) currentLayout.findViewById(R.id.second_contact_textview);
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                    colorFrom = getResources().getColor(R.color.white);
                    colorTo = getResources().getColor(R.color.matchflare_pink);
                    colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                    colorAnimation.setDuration(ViewConfiguration.getLongPressTimeout());
                    colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
                            thisText.setTextColor((Integer)animator.getAnimatedValue());
                        }
                    });
                    colorAnimation.start();

                    xDelta = motionEvent.getRawX() - currentLayout.getX();
                    if (mVelocityTracker == null) {
                        mVelocityTracker = VelocityTracker.obtain();
                    }
                    else {
                        mVelocityTracker.clear();
                    }
                    mVelocityTracker.addMovement(motionEvent);
                }
                else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    resultDisplay.setCurrentText(matchInstructions);
                    isCurrentlyDragging = false;

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
                            thisText.clearAnimation();
                            thisText.setTextColor((Integer)animator.getAnimatedValue());
                        }
                    });
                    colorAnimation.start();

                    mVelocityTracker.addMovement(motionEvent);
                    mVelocityTracker.computeCurrentVelocity(1000);

                    float xVelocity = VelocityTrackerCompat.getXVelocity(mVelocityTracker,motionEvent.getPointerId(motionEvent.getActionIndex()));
                    float dpMoved = dpFromPx(motionEvent.getRawX() - xDelta);
                    if ((dpFromPx(Math.abs(xVelocity)) > (100) && dpMoved < -15) || dpMoved < -35) {
                        PresentMatchesActivity.this.passMatch(); //Match passed
                    }
                    else if ((dpFromPx(Math.abs(xVelocity)) > (100) && dpMoved > 15) || dpMoved > 35) {
                        PresentMatchesActivity.this.makeMatch(); //Match accepted
                    }
                    else {
                        cancelSwipe();
                    }
                }
                else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    currentLayout.setX(motionEvent.getRawX() - xDelta);
                    mVelocityTracker.addMovement(motionEvent);
                    float dpMoved = dpFromPx(motionEvent.getRawX() - xDelta);
                    if (Math.abs(dpMoved) > 15) {
                        isCurrentlyDragging = true;
                    }

                    if (dpMoved > 35) {
                        resultDisplay.setCurrentText("yes");
                    }
                    else if (dpMoved < -35) {
                        resultDisplay.setCurrentText("no");
                    }
                    else {
                        resultDisplay.setCurrentText(matchInstructions);
                    }
                }
                else if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                    cancelSwipe();
                }
            }
            return false;
        }

        //Normalize based on screen density
        public float dpFromPx(float px) {
            return px / PresentMatchesActivity.this.getResources().getDisplayMetrics().density;
        }

        //Resets the tracking variables and cancels the swipe
        public void cancelSwipe() {
            currentLayout.setX(initialX);
            resultDisplay.setCurrentText(matchInstructions);
            mVelocityTracker.recycle();
            mVelocityTracker = null;
            isCurrentlyDragging = false;
        }
    }

    //Inner class to handle Retrofit return type
    public class MatchflareScore {
        int matchflare_score;
    }

    /*
 * Shows the next match to the user
 * @param slideLeft If the animation should slide out to the left
 */
    public void presentNextMatch(boolean slideLeft)
    {
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0,40};
        v.vibrate(pattern,-1);

        showMatchButtons();

        if (currentMatch == null) { //If there is no current match, then initialize both this and next match
            if (matches.size() > 1) {
                currentMatch = matches.poll(); //Set current match
                firstContactTextview.setText(currentMatch.first_matchee.guessed_full_name);
                secondContactTextview.setText(currentMatch.second_matchee.guessed_full_name);
                Picasso.with(this).load(currentMatch.first_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(firstImage);
                Picasso.with(this).load(currentMatch.second_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(secondImage);

                nextMatch = matches.poll(); //Set next match
                nextFirstContactTextview.setText(nextMatch.first_matchee.guessed_full_name);
                nextSecondContactTextview.setText(nextMatch.second_matchee.guessed_full_name);
                Picasso.with(this).load(nextMatch.first_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(nextFirstImage);
                Picasso.with(this).load(nextMatch.second_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(nextSecondImage);
            }
        }
        else {

            //Set the next layout with the info of the next match
            ((TextView) nextLayout.findViewById(R.id.first_contact_textview)).setText(nextMatch.first_matchee.guessed_full_name);
            ((TextView) nextLayout.findViewById(R.id.second_contact_textview)).setText(nextMatch.second_matchee.guessed_full_name);
            Picasso.with(this).load(nextMatch.first_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(((ImageView) nextLayout.findViewById(R.id.first_matchee_image_view)));
            Picasso.with(this).load(nextMatch.second_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(((ImageView) nextLayout.findViewById(R.id.second_matchee_image_view)));

            currentMatch = nextMatch; //Set next match to current
            nextMatch = matches.poll(); //Get next match and store it

            //Animate the layouts
            Animation slideOut;
            Animation slideIn;

            if (slideLeft) {
                slideOut = AnimationUtils.loadAnimation(this,R.anim.match_slide_out);
                slideIn = AnimationUtils.loadAnimation(this,R.anim.match_slide_in);
            }
            else {
                slideOut = AnimationUtils.loadAnimation(this,R.anim.match_slide_out_right);
                slideIn = AnimationUtils.loadAnimation(this,R.anim.match_slide_in_right);
            }

            slideOut.setInterpolator(new AccelerateDecelerateInterpolator());
            slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
            currentLayout.startAnimation(slideOut);
            currentLayout.setVisibility(View.GONE);
            nextLayout.setVisibility(View.VISIBLE);
            nextLayout.startAnimation(slideIn);

            tempLayout = currentLayout;
            currentLayout = nextLayout;
            nextLayout = tempLayout;

            currentLayout.setX(initialX);
        }


        if (matches.size() < 10) { //Load more matches if the number of stored matches is <10
            Log.e("Getting more matches", matches.size() + "");
            Map<String,Integer> options = new HashMap<String,Integer>();
            options.put("contact_id",((Global) getApplication()).thisUser.contact_id);
            ((Global)getApplication()).ui.getMatches(((Global) getApplication()).thisUser, options, new MatchesCallback());
        }
    }

    //Retrofit callback for getting matches
    private class MatchesCallback implements Callback<Queue<Match>> {

        @Override
        public void failure(RetrofitError err)
        {
            Log.e("Error Getting Matches:", err.toString());

            //Google analytics
            Tracker t = ((Global) getApplication()).getTracker();
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("(PresentMatches) Failure to retrieve matches: " +
                            new StandardExceptionParser(PresentMatchesActivity.this, null)
                                    .getDescription(Thread.currentThread().getName(), err))
                    .setFatal(false)
                    .build());
        }

        @Override
        public void success(Queue<Match> response, Response arg1)
        {
            Log.e("Matches returned:", response.toString());
            //Style.makeToast(PresentMatchesActivity.this,"New Matches Loaded!");
            matches.addAll(response);
            if (currentMatch == null) {
                PresentMatchesActivity.this.presentNextMatch(true);

                //Hide progress indicators
                progressIndicator.setVisibility(View.GONE);
                root.setVisibility(View.VISIBLE);
            }
        }
    }

    //Retrofit callback for posting result of a match
    private class postMatchCallback implements Callback<MatchflareScore> {
        @Override
        public void failure(RetrofitError err)
        {
            Log.e("Error Posting Match:", err.toString());

            //Google Analytics
            Tracker t = ((Global) getApplication()).getTracker();
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("(PresentMatches) Failure to post match result: " +
                            new StandardExceptionParser(PresentMatchesActivity.this, null)
                                    .getDescription(Thread.currentThread().getName(), err))
                    .setFatal(false)
                    .build());
        }

        @Override
        public void success(MatchflareScore response, Response arg1)
        {
            Log.e("Match posted w/: ", response + "");
            Integer difference = response.matchflare_score - matchflareScore;
            if (difference > 0) { //Temporarily show change in score
                matchflareScore = response.matchflare_score;
                scoreDisplay.setText("+" + difference);
                scoreDisplay.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scoreDisplay.setText("" + matchflareScore);
                    }
                }, 500);
            }

        }
    }

    //Retrofit callback for checking for new notifications
    public class NotificationsListCallback implements Callback<NotificationLists>
    {
        @Override
        public void success(NotificationLists response, Response response2) {
            Log.e("Notifications retrieved", response.toString());

            //Modify action bar badge icon accordingly
            notificationsList = response;
            if (notificationCountView != null) {
                notificationCountView.setText("0");
                notificationCountView.setVisibility(View.GONE);
                if (notificationsList.notifications.size() > 0) {
                    notificationCountView.setVisibility(View.VISIBLE);
                    notificationCountView.setText(notificationsList.notifications.size() + "");
                }
            }
        }

        @Override
        public void failure(RetrofitError error) {
            Log.e("Can't get pending", error.toString());

            //Google Analytics
            Tracker t = ((Global) getApplication()).getTracker();
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("(PresentMatches) Failure to notifications: " +
                            new StandardExceptionParser(PresentMatchesActivity.this, null)
                                    .getDescription(Thread.currentThread().getName(), error))
                    .setFatal(false)
                    .build());

        }
    }

    //Configures action bar menu and check for notifications and updates icon accordingly
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.present_matches, menu);

        View count = menu.findItem(R.id.notifications_icon).getActionView();
        notificationCountView = (TextView) count.findViewById(R.id.notification_count);

        int contact_id = ((Global) getApplication()).thisUser.contact_id;
        if (!(contact_id > 0)){ //If not registered, then set appropriate action bar items
            MenuItem updateProfileButton = menu.findItem(R.id.update_profile);
            MenuItem notificationsButton = menu.findItem(R.id.notifications_icon);
            MenuItem blockMatches = menu.findItem(R.id.stop_matches);

            updateProfileButton.setTitle("Register");
            notificationsButton.setVisible(false);
            blockMatches.setVisible(false);
            invalidateOptionsMenu();
        }
        else { //Toggle block all match/resume matches button
            if (((Global) getApplication()).thisUser.blocked_matches) {
                MenuItem blockMatches = menu.findItem(R.id.stop_matches);
                blockMatches.setTitle("Resume Getting Matches");
            }
        }

        //Set listeners
        count.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PresentMatchesActivity.this, NotificationActivity.class);
                startActivity(intent);
            }
        });

        if (notificationsList != null) {
            if (notificationsList.notifications.size() > 0) { //If there exists >0 notifications, then change badge
                notificationCountView.setVisibility(View.VISIBLE);
                notificationCountView.setText(notificationsList.notifications.size() + "");
            }
            else { //Hide notification badge
                notificationCountView.setText("0");
                notificationCountView.setVisibility(View.GONE);
            }
        }
        else {
            notificationCountView.setText("0");
            notificationCountView.setVisibility(View.GONE);
        }

        myMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.e("Menu Item Id", item.getItemId()+"");
        Intent intent;
        Tracker t = ((Global) getApplication()).getTracker();
        switch (item.getItemId()) {
            case android.R.id.home:
                intent = new Intent(this, PresentMatchesActivity.class);
                startActivity(intent);
                return true;
            case R.id.notifications_icon:
                intent = new Intent(this, NotificationActivity.class);
                startActivity(intent);

                //Google Analytics
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("ui_action")
                        .setAction("button_press")
                        .setLabel("PresentNotificationsPressed")
                        .build());
                return true;
            case R.id.update_profile:
                int contact_id = ((Global) getApplication()).thisUser.contact_id;
                if (!(contact_id > 0)){
                    intent = new Intent(this, VerificationActivity.class);
                    startActivity(intent);

                    //Google Analytics
                    t.send(new HitBuilders.EventBuilder()
                            .setCategory("ui_action")
                            .setAction("button_press")
                            .setLabel("PresentRegisterPressed")
                            .build());
                }
                else {
                    intent = new Intent(this, UpdateProfileActivity.class);
                    intent.putExtra("this_user", ((Global) getApplication()).thisUser);
                    startActivity(intent);

                    //Google Analytics
                    t.send(new HitBuilders.EventBuilder()
                            .setCategory("ui_action")
                            .setAction("button_press")
                            .setLabel("PresentUpdateProfilePressed")
                            .build());
                }
                return true;
            case R.id.stop_matches:

                //Google Analytics
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("ui_action")
                        .setAction("button_press")
                        .setLabel("PresentPreventStatusPressed")
                        .build());

                //Post request toggling whether this user will receive matches
                int contact_id2 = ((Global) getApplication()).thisUser.contact_id;
                HashMap<String, Integer> options = new HashMap<String, Integer>();
                options.put("contact_id", contact_id2);
                options.put("toPreventMatches",!((Global) getApplication()).thisUser.blocked_matches ? 1 : 0);
                ((Global) getApplication()).ui.preventMatches(options, new Callback<StringResponse>() {

                    @Override
                    public void success(StringResponse stringResponse, Response response) {
                        ((Global) getApplication()).thisUser.blocked_matches = !((Global) getApplication()).thisUser.blocked_matches;
                        if (((Global) getApplication()).thisUser.blocked_matches) {
                            Style.makeToast(PresentMatchesActivity.this,"You won't receive any more matches :(");
                            MenuItem blockMatches = myMenu.findItem(R.id.stop_matches);
                            blockMatches.setTitle("Resume Getting Matches");
                        }
                        else {
                            Style.makeToast(PresentMatchesActivity.this,"You are back on the market!");
                            MenuItem blockMatches = myMenu.findItem(R.id.stop_matches);
                            blockMatches.setTitle("Stop Getting Matches");
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e("Cant change prevent",error.getLocalizedMessage());

                        //Google Analytics
                        Tracker t = ((Global) getApplication()).getTracker();
                        t.send(new HitBuilders.ExceptionBuilder()
                                .setDescription("(PresentMatches) Failure to change prevent match status: " +
                                        new StandardExceptionParser(PresentMatchesActivity.this, null)
                                                .getDescription(Thread.currentThread().getName(), error))
                                .setFatal(false)
                                .build());
                    }
                });
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
        finish(); //Go to start activity if restored after a while
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

                //Recheck notifications on remote notification
                Map<String, Integer> options = new HashMap<String, Integer>();
                int contact_id = ((Global) getApplication()).thisUser.contact_id;
                if (contact_id > 0) {
                    options.put("contact_id",contact_id);
                    ((Global) getApplication()).ui.getNotificationLists(options, new NotificationsListCallback());
                }
            }
        }
    };
}
