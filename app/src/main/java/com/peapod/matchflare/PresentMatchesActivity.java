package com.peapod.matchflare;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
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


public class PresentMatchesActivity extends Activity implements Callback<StringResponse> {


    Cursor contactsCursor;
    Person myContactId;
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

    ImageView nextFirstImage;
    ImageView nextSecondImage;
    TextView nextFirstContactTextview;
    TextView nextSecondContactTextview;
    TextSwitcher resultDisplay;
    CheckBox anonymousCheckbox;

    LinearLayout currentLayout;
    LinearLayout nextLayout;
    LinearLayout tempLayout;
    int currentColor;
    ViewSwitcher.ViewFactory viewFactory;
    RelativeLayout root;
    String matchInstructions = "should they meet?";
    String eloInstructions = "who's the better catch?";
   // Transformation transformation = new CircleTransform();
    String instructionsText;

    static final String SENDER_ID="614720100487";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_present_matches);

        //Retrieve UI elements
        root = (RelativeLayout) findViewById(R.id.root_present_matches);
        display = (TextView) findViewById(R.id.instructions_results);
        Style.toOpenSans(this, display, "light");
        display.bringToFront();
        display.setVisibility(View.INVISIBLE);
        firstImage = (ImageView) findViewById(R.id.first_matchee_image_view);
        secondImage = (ImageView) findViewById(R.id.second_matchee_image_view);
        resultDisplay = (TextSwitcher) findViewById(R.id.result_text);
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

        nextLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.sliding_match, null);
        nextLayout.setVisibility(View.GONE);
        root.addView(nextLayout);


        nextFirstContactTextview = (TextView) nextLayout.findViewById(R.id.first_contact_textview);
        nextSecondContactTextview = (TextView) nextLayout.findViewById(R.id.second_contact_textview);
        Style.toOpenSans(this,nextFirstContactTextview,"light");
        Style.toOpenSans(this,nextSecondContactTextview,"light");
        nextFirstImage = (ImageView) nextLayout.findViewById(R.id.first_matchee_image_view);
        nextSecondImage = (ImageView) nextLayout.findViewById(R.id.second_matchee_image_view);

        anonymousCheckbox = (CheckBox) findViewById(R.id.anonymous_checkbox);
        Style.toOpenSans(this,anonymousCheckbox,"light");

        passButton = (ImageView) findViewById(R.id.pass_button);
        matchButton = (ImageView) findViewById(R.id.match_button);
        matchButton.bringToFront();
        passButton.bringToFront();

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

    private class ButtonTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent motionEvent) {

            Integer colorFrom;
            Integer colorTo;

            if (v.getId() == R.id.first_contact_textview || v.getId() == R.id.first_matchee_image_view) {
                final TextView thisText = (TextView) currentLayout.findViewById(R.id.first_contact_textview);
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    colorFrom = getResources().getColor(R.color.white);
                    colorTo = getResources().getColor(R.color.matchflare_pink);

                    ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                    colorAnimation.setDuration(200);
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

                    ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                    colorAnimation.setDuration(200);
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

                    ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                    colorAnimation.setDuration(200);
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

                    ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                    colorAnimation.setDuration(200);
                    colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
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

        Log.e("Menu Item Id", item.getItemId()+"");
        Intent intent;
        switch (item.getItemId()) {

            case android.R.id.home:
                //Do stuff
                intent = new Intent(this, PresentMatchesActivity.class);
                startActivity(intent);

                return true;
            case R.id.notifications:
                //intent = new Intent(this, NotificationActivity.class);
                intent = new Intent(this, NotificationActivity.class);
                startActivity(intent);
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
            }

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
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        long[] pattern = {0,50,20,50};
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
}
