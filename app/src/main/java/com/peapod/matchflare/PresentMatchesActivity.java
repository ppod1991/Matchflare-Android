package com.peapod.matchflare;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.makeramen.RoundedImageView;
import com.makeramen.RoundedTransformationBuilder;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class PresentMatchesActivity extends Activity implements Callback<StringResponse>, MatcheeOptionsDialog.MatcheeOptionsDialogListener {

    Queue<Match> matches = new LinkedList<Match>();
    Match currentMatch;
    TextView firstContactTextview;
    TextView secondContactTextview;
    ImageView passButton;
    ImageView matchButton;
    TextView display;
    Match nextMatch;
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

    LinearLayout currentLayout;
    LinearLayout nextLayout;
    LinearLayout tempLayout;
    int currentColor;
    ViewSwitcher.ViewFactory viewFactory;
    ViewSwitcher.ViewFactory viewFactoryScore;

    RelativeLayout root;
    String matchInstructions = "should they meet?";
    String eloInstructions = "who's the better catch?";
   // Transformation transformation = new CircleTransform();
    String instructionsText;
    int matchflareScore = 0;
    View progressIndicator;

    MatcheeOptionsDialog optionsDialog;
    TextView notificationCountView;
    NotificationLists notificationsList = null;

    static final String SENDER_ID="614720100487";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_present_matches);
        progressIndicator = findViewById(R.id.progress_indicator);
        //Retrieve UI elements
        root = (RelativeLayout) findViewById(R.id.root_present_matches);
        display = (TextView) findViewById(R.id.instructions_results);
        Style.toOpenSans(this, display, "light");
        display.bringToFront();
        display.setVisibility(View.INVISIBLE);
        firstImage = (ImageView) findViewById(R.id.first_matchee_image_view);
        secondImage = (ImageView) findViewById(R.id.second_matchee_image_view);
        resultDisplay = (TextSwitcher) findViewById(R.id.result_text);
        scoreDisplay = (TextSwitcher) findViewById(R.id.score_view);
        // Set the ViewFactory of the TextSwitcher that will create TextView object when asked

        viewFactory = new ViewSwitcher.ViewFactory() {

            @Override
            public View makeView() {
                // TODO Auto-generated method stub
                // create new textView and set the properties like color, size etc
                TextView myText = new TextView(PresentMatchesActivity.this);
                myText.setTextSize(15);
                myText.setAlpha(0.8f);
                myText.setGravity(Gravity.CENTER_HORIZONTAL);
                Style.toOpenSans(PresentMatchesActivity.this,myText,"light");
                return myText;
            }
        };

        resultDisplay.setFactory(viewFactory);

        viewFactoryScore = new ViewSwitcher.ViewFactory() {

            @Override
            public View makeView() {
                // TODO Auto-generated method stub
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
        scoreDisplay.setFactory(viewFactoryScore);

//        Animation in = AnimationUtils.loadAnimation(this,
//                android.R.anim.fade_in);
        Animation in = AnimationUtils.loadAnimation(this,
                R.anim.name_slide_in);
        in.setDuration(200);

//        Animation out = AnimationUtils.loadAnimation(this,
//                android.R.anim.fade_out);
        Animation out = AnimationUtils.loadAnimation(this,
                R.anim.name_slide_out);

        out.setDuration(200);
        out.setInterpolator(new AccelerateDecelerateInterpolator());
        in.setInterpolator(new AccelerateDecelerateInterpolator());
        resultDisplay.setInAnimation(in);
        resultDisplay.setOutAnimation(out);
        resultDisplay.setText(matchInstructions);

        Animation fadeIn = AnimationUtils.loadAnimation(this,android.R.anim.fade_in);
        fadeIn.setDuration(150);
        Animation fadeOut = AnimationUtils.loadAnimation(this,android.R.anim.fade_out);
        fadeOut.setDuration(150);
        scoreDisplay.setInAnimation(fadeIn);
        scoreDisplay.setOutAnimation(fadeOut);
        scoreDisplay.setText("n/a");
        //resultDisplay.setVisibility(View.INVISIBLE);

//        nextFirstImage = (ImageView) findViewById(R.id.next_first_matchee_image_view);;
//        nextSecondImage = (ImageView) findViewById(R.id.next_second_matchee_image_view);;
//        nextFirstContactTextview = (TextView) findViewById(R.id.next_first_contact_textview);
//        nextSecondContactTextview = (TextView) findViewById(R.id.next_second_contact_textview);

        currentLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.sliding_match, null);
        root.addView(currentLayout);
        firstContactTextview = (TextView) currentLayout.findViewById(R.id.first_contact_textview);
        secondContactTextview = (TextView) currentLayout.findViewById(R.id.second_contact_textview);

        Style.toOpenSans(this,firstContactTextview,"light");
        Style.toOpenSans(this,secondContactTextview,"light");
        firstImage = (ImageView) currentLayout.findViewById(R.id.first_matchee_image_view);
        secondImage = (ImageView) currentLayout.findViewById(R.id.second_matchee_image_view);

        firstOptionDots = (ImageView) currentLayout.findViewById(R.id.first_option_dots);
        secondOptionDots = (ImageView) currentLayout.findViewById(R.id.second_option_dots);
        firstOptionDots.bringToFront();
        secondOptionDots.bringToFront();
        nextLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.sliding_match, null);
        nextLayout.setVisibility(View.GONE);
        root.addView(nextLayout);


        nextFirstContactTextview = (TextView) nextLayout.findViewById(R.id.first_contact_textview);
        nextSecondContactTextview = (TextView) nextLayout.findViewById(R.id.second_contact_textview);
        Style.toOpenSans(this,nextFirstContactTextview,"light");
        Style.toOpenSans(this,nextSecondContactTextview,"light");
        nextFirstImage = (ImageView) nextLayout.findViewById(R.id.first_matchee_image_view);
        nextSecondImage = (ImageView) nextLayout.findViewById(R.id.second_matchee_image_view);
        nextFirstOptionDots = (ImageView) nextLayout.findViewById(R.id.first_option_dots);
        nextSecondOptionDots = (ImageView) nextLayout.findViewById(R.id.second_option_dots);
        nextFirstOptionDots.bringToFront();
        nextSecondOptionDots.bringToFront();
        anonymousCheckbox = (CheckBox) findViewById(R.id.anonymous_checkbox);
        Style.toOpenSans(this,anonymousCheckbox,"light");

        passButton = (ImageView) findViewById(R.id.pass_button);
        matchButton = (ImageView) findViewById(R.id.match_button);
        matchButton.bringToFront();
        passButton.bringToFront();

        root.setVisibility(View.GONE);
        progressIndicator.setVisibility(View.VISIBLE);

        ButtonTouchListener touchListener = new ButtonTouchListener();
        firstImage.setOnTouchListener(touchListener);
        secondImage.setOnTouchListener(touchListener);
        nextFirstImage.setOnTouchListener(touchListener);
        nextSecondImage.setOnTouchListener(touchListener);
        firstContactTextview.setOnTouchListener(touchListener);
        secondContactTextview.setOnTouchListener(touchListener);
        nextFirstContactTextview.setOnTouchListener(touchListener);
        nextSecondContactTextview.setOnTouchListener(touchListener);

        //Attach listeners
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

        //changeDisplayText(matchInstructions);



        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getSerializable("matches") != null) {
            matches.addAll((ArrayList<Match>) extras.getSerializable("matches"));
        }

        if (matches.size() == 0) {
            Map<String,Integer> options = new HashMap<String,Integer>();
            options.put("contact_id",((Global) getApplication()).thisUser.contact_id);
            ((Global)getApplication()).ui.getMatches(((Global) getApplication()).thisUser, options, new MatchesCallback());
        }
        else {

            presentNextMatch();
            progressIndicator.setVisibility(View.GONE);
            root.setVisibility(View.VISIBLE);


        }



        //Round image transformation
//        transformation = new RoundedTransformationBuilder()
//                .borderColor(Color.BLACK)
//                .borderWidthDp(1)
//                .oval(true)
//                .build();

        //Register GCM for Push Notifications

//        GCMRegistrarCompat.checkDevice(this);
//        if (BuildConfig.DEBUG) {
//            GCMRegistrarCompat.checkManifest(this);
//        }
//
//        final String regId=GCMRegistrarCompat.getRegistrationId(this);
//
//        if (regId.length() == 0) {
//            new RegisterTask(this).execute(SENDER_ID, ((Global) getApplication()).thisUser.contact_id + "");
//        } else
//        {
//            Log.d(getClass().getSimpleName(), "Existing registration: "
//                    + regId);
//            //Toast.makeText(this, regId, Toast.LENGTH_LONG).show();
//        }



//        //Retrieve contacts for upsert with Contacts table
//        String[] contacts = {};
//
//        final String[] projection = new String[] {
//                ContactsContract.Contacts.Data._ID,
//                ContactsContract.Contacts.DISPLAY_NAME,
//                ContactsContract.Contacts.TIMES_CONTACTED,
//                ContactsContract.CommonDataKinds.Phone.NUMBER,
//                ContactsContract.CommonDataKinds.Photo.PHOTO_URI
//        };
//
//        String filter = ContactsContract.Contacts.HAS_PHONE_NUMBER + "==1 AND " + ContactsContract.CommonDataKinds.Phone.TYPE + " = " + ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE + " AND LENGTH(" + ContactsContract.Contacts.DISPLAY_NAME + ") > 6" + " AND LENGTH(" + ContactsContract.CommonDataKinds.Phone.NUMBER + ") >= 7 AND SUBSTR(" + ContactsContract.Contacts.DISPLAY_NAME + ",1,1) != '#' AND SUBSTR(" + ContactsContract.Contacts.DISPLAY_NAME + ",1,2) != 'GM GROUP BY' ";
//        Log.e("Filter", filter);
//
//        ContentResolver cr = getApplicationContext().getContentResolver(); //Activity/Application android.content.Context
//        contactsCursor = cr.query(ContactsContract.Data.CONTENT_URI, projection, filter, null, ContactsContract.Contacts.TIMES_CONTACTED + " DESC");
//
//
//        Set<Person> people = new HashSet<Person>(contactsCursor.getCount());
//
//        while (contactsCursor.moveToNext()) {
//            String full_name = contactsCursor.getString(1); //Get name
//            String raw_phone_number = contactsCursor.getString(3);  //Get raw phone number
//            Person toAdd = new Person(full_name,raw_phone_number);
//            people.add(toAdd);
//        }



        //Map<String,Integer> options = new HashMap<String,Integer>();
        //options.put("contact_id",((Global) getApplication()).thisUser.contact_id);
        //((Global) getApplication()).ui.processContacts(new Contacts(people), options, this);

    }

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
//        resultDisplay.setVisibility(View.VISIBLE);
//        display.setVisibility(View.INVISIBLE);
//
//        resultDisplay.setText(text);
//        Float alphaFrom = new Float(1.0);
//        Float alphaTo = new Float(0.0);
//
//        ValueAnimator alphaAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), alphaFrom, alphaTo);
//        alphaAnimation.setDuration(1000);
//        alphaAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//
//            @Override
//            public void onAnimationUpdate(ValueAnimator animator) {
//                resultDisplay.setAlpha((Float) animator.getAnimatedValue());
//
//            }
//
//        });
//        alphaAnimation.start();
//        AlphaAnimation animation1 = new AlphaAnimation(0.8f, 0.8f);
//        animation1.setDuration(800);
//        animation1.setAnimationListener(new Animation.AnimationListener() {
//            @Override
//            public void onAnimationStart(Animation animation) {
//
//            }
//
//            @Override
//            public void onAnimationEnd(Animation animation) {
//                display.setVisibility(View.VISIBLE);
//                resultDisplay.setVisibility(View.INVISIBLE);
//            }
//
//            @Override
//            public void onAnimationRepeat(Animation animation) {
//
//            }
//        });
//        resultDisplay.startAnimation(animation1);
    };

    public void changeDisplayText(String text) {
        //display.setText(text);
//        currentColor = getResources().getColor(R.color.light_gray);
//        resultDisplay.setText(text);
        instructionsText = text;
    };

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        Log.e("Negative click", "TO REMOVE!");
    }

    @Override
    public void onDialogSetMatchee(Person selectedPerson, MatcheeOptionsDialog dialog) {
        Log.e("Person selected", "TO REMOVE!");

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

        //Add to 'not present' list -- TO BE IMPLEMENTED
        //Style.makeToast(this,selectedPerson + " selected");

    }

    @Override
    public void onDialogRemoveMatchee(Person selectedPerson, MatcheeOptionsDialog dialog) {
        Log.e("Person selected for removal", "TO REMOVE!");

        //Add this contact to this user's blocked users list -- NEED TO IMPLEMENT
        Map<String, Integer> options = new HashMap<String, Integer>();
        options.put("contact_id",((Global)getApplication()).thisUser.contact_id);
        options.put("to_remove_contact_id",selectedPerson.contact_id);
        ((Global)getApplication()).ui.removeContact(options, new Callback<StringResponse>() {
            @Override
            public void success(StringResponse stringResponse, Response response) {
                Log.e("Successfully added this user to your removed contacts", stringResponse.response);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("Failed to add your friend to the contact list", error.toString());
            }
        });

        Style.makeToast(this,selectedPerson.guessed_full_name + " will not be shown in future matches");
        presentNextMatch();
    }

    private class LongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View view) {
            Vibrator v = (Vibrator) PresentMatchesActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            long[] pattern = {0,60,25,60};
            v.vibrate(pattern,-1);

            if (view.getId() == R.id.first_contact_textview || view.getId() == R.id.first_matchee_image_view || view.getId() == R.id.first_option_dots) {
                showOptionsDialog(true, currentMatch.first_matchee);
                return true;
            }
            else if (view.getId() == R.id.second_contact_textview || view.getId() == R.id.second_matchee_image_view || view.getId() == R.id.second_option_dots) {
                showOptionsDialog(false, currentMatch.second_matchee);
                return true;
            }
            return false;
        }
    }

    public void showOptionsDialog(Boolean isFirst, Person chosenMatchee) {
        optionsDialog = MatcheeOptionsDialog.newInstance(isFirst, chosenMatchee);
        optionsDialog.show(getFragmentManager(),"options_dialog");
    }

    private class ButtonTouchListener implements View.OnTouchListener {

        ValueAnimator colorAnimation;
        @Override
        public boolean onTouch(View v, MotionEvent motionEvent) {

            Integer colorFrom;
            Integer colorTo;

            if (v.getId() == R.id.first_contact_textview || v.getId() == R.id.first_matchee_image_view) {
                final TextView thisText = (TextView) currentLayout.findViewById(R.id.first_contact_textview);
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

                            thisText.setTextColor((Integer)animator.getAnimatedValue());
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
                            thisText.setTextColor((Integer)animator.getAnimatedValue());
                        }

                    });
                    colorAnimation.start();
                }
            }
            else if (v.getId() == R.id.second_contact_textview || v.getId() == R.id.second_matchee_image_view) {
                final TextView thisText = (TextView) currentLayout.findViewById(R.id.second_contact_textview);
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
//                    ImageView thisImage = (ImageView) currentLayout.findViewById(R.id.second_matchee_image_view);
//                    Animation grow = AnimationUtils.loadAnimation(PresentMatchesActivity.this,R.anim.grow);
//                    thisImage.startAnimation(grow);
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
                }
                else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                    ImageView thisImage = (ImageView) currentLayout.findViewById(R.id.second_matchee_image_view);
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
                            thisText.clearAnimation();
                            thisText.setTextColor((Integer)animator.getAnimatedValue());
                        }

                    });
                    colorAnimation.start();
                }
            }


            return false;
        }
    }

    private class ButtonListener implements View.OnClickListener {
        public void onClick(View v) {

            currentMatch.matcher.contact_id = ((Global) getApplication()).thisUser.contact_id;
            RestService ui = ((Global) getApplication()).ui;
            if (v.getId() == R.id.match_button) {
                if (currentMatch.matcher == null || !(currentMatch.matcher.contact_id > 0)) {
                    Style.makeToast(PresentMatchesActivity.this,"Cannot match. Need to register!!");
                    Intent i = new Intent(PresentMatchesActivity.this,VerificationActivity.class);
                    startActivity(i);
                    //presentNextMatch();
                }
                else if (currentMatch.first_matchee.contact_id == ((Global) getApplication()).thisUser.contact_id || currentMatch.second_matchee.contact_id == ((Global) getApplication()).thisUser.contact_id) {
                    Style.makeToast(PresentMatchesActivity.this,"Tricky tricky. Sorry, but you can't match yourself.");
                }
                else if (currentMatch.first_matchee.contact_id == currentMatch.second_matchee.contact_id) {
                    Style.makeToast(PresentMatchesActivity.this,"Stop confusing us. You can't match the same person, silly.");
                }
                else {
                    changeResult("match made",900);

                    currentMatch.match_status = "MATCHED";
                    if (anonymousCheckbox.isChecked()) {
                        currentMatch.is_anonymous = true;
                    }
                    else {
                        currentMatch.is_anonymous = false;
                    }

                    ui.addMatch(currentMatch,null,new postMatchCallback());
                    presentNextMatch();
                }
            }
            else if (v.getId() == R.id.pass_button) {
                changeResult("no match",300);
                passButton.setVisibility(View.INVISIBLE);
                matchButton.setVisibility(View.INVISIBLE);
                anonymousCheckbox.setVisibility(View.INVISIBLE);
                changeDisplayText(eloInstructions);
                //eloInstructions.setVisibility(View.VISIBLE);
            }
            else if (v.getId() == R.id.first_contact_textview || v.getId() == R.id.first_matchee_image_view) {
                currentMatch.match_status = "FIRST_CONTACT_WINS";
                ui.addMatch(currentMatch, null, new postMatchCallback());
                changeResult(currentMatch.first_matchee.guessed_full_name.toLowerCase() + " wins",700);
                presentNextMatch();
            }
            else if (v.getId() == R.id.second_contact_textview || v.getId() == R.id.second_matchee_image_view) {
                currentMatch.match_status = "SECOND_CONTACT_WINS";
                ui.addMatch(currentMatch, null, new postMatchCallback());
                changeResult(currentMatch.second_matchee.guessed_full_name.toLowerCase() + " wins",700);
                presentNextMatch();
            }
            else if (v.getId() == R.id.first_option_dots) {
                showOptionsDialog(true, currentMatch.first_matchee);
            }
            else if (v.getId() == R.id.second_option_dots) {
                showOptionsDialog(false, currentMatch.second_matchee);
            }
            //Post result of currently presented match
            //NEED TO IMPLEMENT


        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.present_matches, menu);

        View count = menu.findItem(R.id.notifications_icon).getActionView();
        notificationCountView = (TextView) count.findViewById(R.id.notification_count);

        count.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PresentMatchesActivity.this, NotificationActivity.class);
                startActivity(intent);
            }
        });

        if (notificationsList != null) {
            if (notificationsList.notifications.size() > 0) {
                notificationCountView.setVisibility(View.VISIBLE);
                notificationCountView.setText("1");
            }
            else {
                notificationCountView.setText("0");
                notificationCountView.setVisibility(View.GONE);
            }
        }


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.e("Menu Item Id", item.getItemId()+"");
        Intent intent;
        switch (item.getItemId()) {

            case android.R.id.home:
                //Do stuff
                intent = new Intent(this, PresentMatchesActivity.class);
                startActivity(intent);

                return true;
            case R.id.notifications_icon:
                //intent = new Intent(this, NotificationActivity.class);
                intent = new Intent(this, NotificationActivity.class);
                startActivity(intent);
                return true;
            case R.id.update_profile:
                //intent = new Intent(this, NotificationActivity.class);
                intent = new Intent(this, UpdateProfileActivity.class);
                intent.putExtra("this_user", ((Global) getApplication()).thisUser);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

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
        ((Global)getApplication()).ui.getMatches(((Global) getApplication()).thisUser, options, new MatchesCallback());
    }


    //Retrofit callback for getting matches
    private class MatchesCallback implements Callback<Queue<Match>> {
        //Retrofit callback for process contacts
        @Override
        public void failure(RetrofitError err)
        {
            Log.e("Error Getting Matches:", err.toString());
        }

        @Override
        public void success(Queue<Match> response, Response arg1)
        {
            Log.e("Matches successfully returned:", response.toString());
            Style.makeToast(PresentMatchesActivity.this,"New Matches Loaded!");
            matches.addAll(response);
            if (currentMatch == null) {

                PresentMatchesActivity.this.presentNextMatch();
                progressIndicator.setVisibility(View.GONE);
                root.setVisibility(View.VISIBLE);
            }

        }
    }

    //Retrofit callback for getting matches
    private class postMatchCallback implements Callback<Integer> {
        //Retrofit callback for process contacts
        @Override
        public void failure(RetrofitError err)
        {
            Log.e("Error Posting Match:", err.toString());
        }

        @Override
        public void success(Integer response, Response arg1)
        {
            Log.e("Match successfully posted with score:", response + "");
            Integer difference = response - matchflareScore;
            matchflareScore = response;
            //((TextView) scoreDisplay.getNextView()).setTextColor(getResources().getColor(R.color.matchflare_pink));
            scoreDisplay.setText("+" + difference);
            scoreDisplay.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //((TextView) scoreDisplay.getNextView()).setTextColor(getResources().getColor(R.color.light_gray));
                    scoreDisplay.setText("" + matchflareScore);
                }
            }, 500);
        }
    }

    public void presentNextMatch()
    {
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        long[] pattern = {0,40};
        v.vibrate(pattern,-1);


        passButton.setVisibility(View.VISIBLE);
        matchButton.setVisibility(View.VISIBLE);
        anonymousCheckbox.setVisibility(View.VISIBLE);
        changeDisplayText(matchInstructions);

        if (currentMatch == null) {
            if (matches.size() > 1) {
                currentMatch = matches.poll();
                firstContactTextview.setText(currentMatch.first_matchee.guessed_full_name);
                secondContactTextview.setText(currentMatch.second_matchee.guessed_full_name);
                Picasso.with(this).load(currentMatch.first_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(firstImage);
                Picasso.with(this).load(currentMatch.second_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(secondImage);
                nextMatch = matches.poll();
                nextFirstContactTextview.setText(nextMatch.first_matchee.guessed_full_name);
                nextSecondContactTextview.setText(nextMatch.second_matchee.guessed_full_name);
                Picasso.with(this).load(nextMatch.first_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(nextFirstImage);
                Picasso.with(this).load(nextMatch.second_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(nextSecondImage);

            }
        }
        else {


            ((TextView) nextLayout.findViewById(R.id.first_contact_textview)).setText(nextMatch.first_matchee.guessed_full_name);
            ((TextView) nextLayout.findViewById(R.id.second_contact_textview)).setText(nextMatch.second_matchee.guessed_full_name);
            Picasso.with(this).load(nextMatch.first_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(((ImageView) nextLayout.findViewById(R.id.first_matchee_image_view)));
            Picasso.with(this).load(nextMatch.second_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(((ImageView) nextLayout.findViewById(R.id.second_matchee_image_view)));

            currentMatch = nextMatch;
            nextMatch = matches.poll();

            Animation slideOut = AnimationUtils.loadAnimation(this,R.anim.match_slide_out);
            Animation slideIn = AnimationUtils.loadAnimation(this,R.anim.match_slide_in);
            slideOut.setInterpolator(new AccelerateDecelerateInterpolator());
            slideIn.setInterpolator(new AccelerateDecelerateInterpolator());
            currentLayout.startAnimation(slideOut);
            currentLayout.setVisibility(View.GONE);
            nextLayout.setVisibility(View.VISIBLE);
            nextLayout.startAnimation(slideIn);

            tempLayout = currentLayout;
            currentLayout = nextLayout;
            nextLayout = tempLayout;

        }


        if (matches.size() < 10) {
            Log.e("Getting more matches", matches.size() + "");
            Map<String,Integer> options = new HashMap<String,Integer>();
            options.put("contact_id",((Global) getApplication()).thisUser.contact_id);
            ((Global)getApplication()).ui.getMatches(((Global) getApplication()).thisUser, options, new MatchesCallback());
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
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (((Global) getApplication()).thisUser.contact_id > 0) {
            //Get and set matchflare score if this is a registered user
            Map<String, Integer> options = new HashMap<String, Integer>();
            options.put("contact_id",((Global) getApplication()).thisUser.contact_id);
            ((Global) getApplication()).ui.getScore(options, new postMatchCallback());
            ((Global) getApplication()).ui.getNotificationLists(options, new NotificationsListCallback());

        };

        IntentFilter filter=new IntentFilter(Global.ACTION_EVENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(onEvent, filter);
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
                Toast toast = Toast.makeText(context,notification.push_message,Toast.LENGTH_LONG);
                toast.show();
            }

        }
    };

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        Intent i = new Intent(this,SplashActivity.class);
        startActivity(i);
        finish();
    }

    public class NotificationsListCallback implements Callback<NotificationLists>
    {
        @Override
        public void success(NotificationLists response, Response response2) {
            Log.e("Notifications Lists retrieved", response.toString());
            notificationsList = response;
            if (notificationCountView != null) {
                if (notificationsList.notifications.size() > 0) {
                    notificationCountView.setVisibility(View.VISIBLE);
                    notificationCountView.setText("1");
                }
                else {
                    notificationCountView.setText("0");
                    notificationCountView.setVisibility(View.GONE);
                }
            }


        }

        @Override
        public void failure(RetrofitError error) {
            Log.e("Failure to get pending matches", error.toString());
        }
    }
}
