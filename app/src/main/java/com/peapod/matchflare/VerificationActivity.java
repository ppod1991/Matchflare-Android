package com.peapod.matchflare;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;
import com.peapod.matchflare.Objects.Person;
import com.peapod.matchflare.Objects.StringResponse;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/*
 * The Activity used to register for Matchflare
 */
public class VerificationActivity extends Activity implements Button.OnClickListener, PhoneNumberDialog.PhoneNumberDialogListener, Callback<StringResponse> {

    //Activity Components
    Button startMatchingButton;
    Person toVerifyPerson;
    String rawPhoneNumber;
    EditText verificationCode;
    RadioButton maleButton;
    RadioButton femaleButton;
    CheckBox malesCheckBox;
    CheckBox femalesCheckBox;
    EditText fullNameField;
    String imageURL;
    Button chooseImageButton;
    Button nameButton;
    Button preferenceButton;
    Button imageSkipButton;
    RadioGroup genderRadioGroup;
    ImageView profileThumbnail;
    TextView nameInstructions;
    TextView smsInstructions;
    TextView genderInstructions;
    TextView interestedInInstructions;
    RelativeLayout nameLayout;
    LinearLayout genderLayout;
    LinearLayout preferenceLayout;
    LinearLayout pictureLayout;
    LinearLayout codeLayout;
    TextView genderPreferenceErrorMessage;
    View progressIndicator;

    //Activity Variables
    boolean finishedProcessingContacts;
    boolean toVerify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        //Get intent data -- check state of processing contacts
        finishedProcessingContacts = getIntent().getBooleanExtra("finished_processing_contacts",false);
        if (!finishedProcessingContacts && ((Global) getApplication()).thisUser.contact_objects != null) {
            finishedProcessingContacts = true;
        }

        //Retrieve Components
        startMatchingButton = (Button) findViewById(R.id.start_matching_button);
        verificationCode = (EditText) findViewById(R.id.verification_code_field);
        maleButton = (RadioButton) findViewById(R.id.my_gender_guy);
        femaleButton = (RadioButton) findViewById(R.id.my_gender_girl);
        malesCheckBox = (CheckBox) findViewById(R.id.guys_check_box);
        femalesCheckBox = (CheckBox) findViewById(R.id.girls_check_box);
        fullNameField = (EditText) findViewById(R.id.full_name_field);
        chooseImageButton = (Button) findViewById(R.id.choose_picture_button);
        profileThumbnail = (ImageView) findViewById(R.id.profile_pic_thumbnail);
        nameInstructions = (TextView) findViewById(R.id.full_name_instructions);
        genderInstructions = (TextView) findViewById(R.id.my_gender_instructions);
        smsInstructions = (TextView) findViewById(R.id.verification_code_instructions);
        interestedInInstructions = (TextView) findViewById(R.id.gender_preference_instructions);
        nameButton = (Button) findViewById(R.id.name_button);
        preferenceButton = (Button) findViewById(R.id.gender_preference_button);
        imageSkipButton = (Button) findViewById(R.id.skip_picture_button);
        genderRadioGroup = (RadioGroup) findViewById(R.id.my_gender_group);
        progressIndicator = findViewById(R.id.progress_indicator);
        genderPreferenceErrorMessage = (TextView) findViewById(R.id.gender_preference_error_message);

        nameLayout = (RelativeLayout) findViewById(R.id.name_layout);
        genderLayout = (LinearLayout) findViewById(R.id.my_gender_layout);
        preferenceLayout = (LinearLayout) findViewById(R.id.preference_layout);
        pictureLayout = (LinearLayout) findViewById(R.id.picture_layout);
        codeLayout = (LinearLayout) findViewById(R.id.code_layout);

        //Style Components
        progressIndicator.setVisibility(View.GONE);
        imageSkipButton.setText("Skip");

        Style.toOpenSans(this,verificationCode,"light");
        Style.toOpenSans(this,maleButton,"light");
        Style.toOpenSans(this,femaleButton,"light");
        Style.toOpenSans(this,malesCheckBox,"light");
        Style.toOpenSans(this,startMatchingButton,"light");
        Style.toOpenSans(this,femalesCheckBox,"light");
        Style.toOpenSans(this,fullNameField,"light");
        Style.toOpenSans(this,chooseImageButton,"light");
        Style.toOpenSans(this,nameButton,"light");
        Style.toOpenSans(this,preferenceButton,"light");
        Style.toOpenSans(this,imageSkipButton,"light");
        Style.toOpenSans(this,genderPreferenceErrorMessage,"light");
        Style.toOpenSans(this,nameInstructions,"regular");
        Style.toOpenSans(this,genderInstructions,"regular");
        Style.toOpenSans(this,smsInstructions,"regular");
        Style.toOpenSans(this,interestedInInstructions,"regular");

        //Set listeners
        chooseImageButton.setOnClickListener(this);
        startMatchingButton.setOnClickListener(this);
        imageSkipButton.setOnClickListener(this);
        nameButton.setOnClickListener(this);
        preferenceButton.setOnClickListener(this);

        toVerifyPerson = new Person(); //The Person object to populate
        genderRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (maleButton.isChecked()) {
                    toVerifyPerson.guessed_gender = "MALE";
                }
                else if (femaleButton.isChecked()) {
                    toVerifyPerson.guessed_gender = "FEMALE";
                }

                //Google Analytics
                Tracker t = ((Global) getApplication()).getTracker();
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("ui_action")
                        .setAction("button_press")
                        .setLabel("VerificationGenderSubmitted")
                        .build());

                genderLayout.setVisibility(View.GONE); //Transition to next layout
                preferenceLayout.setVisibility(View.VISIBLE);
            }
        });

        //Request phone number
        PhoneNumberDialog phoneFragment = new PhoneNumberDialog();
        phoneFragment.show(getFragmentManager(),"phone_fragment");

        try { //Try to prefil the user's name from Phone Info
            Cursor c = getApplication().getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
            c.moveToFirst();
            fullNameField.setText(c.getString(c.getColumnIndex("display_name")));
            c.close();
        }
        catch (Exception e) { //No full name found--leave text field blank

            //Google Analytics
            Tracker t = ((Global) getApplication()).getTracker();
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("(Verification) Failed to auto-fill user's name: " +
                            new StandardExceptionParser(VerificationActivity.this, null)
                                    .getDescription(Thread.currentThread().getName(), e))
                    .setFatal(false)
                    .build());
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        //Register for listener, listening for a broadcast indicating completed processing of contacts
        LocalBroadcastManager.getInstance(this).registerReceiver(processedContactsReceiver,
                new IntentFilter("com.peapod.matchflare.FINISHED_PROCESSING_CONTACTS"));

        //Google Analytics
        Tracker t = ((Global) this.getApplication()).getTracker();
        t.setScreenName("VerificationActivity");
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(processedContactsReceiver); // Unregister the receiver
        super.onPause();
    }

    @Override
    public void onClick(View view) { //Handle validation and transitions between input field

        Tracker t = ((Global) getApplication()).getTracker();
        if (view.getId() == R.id.name_button) { //Validate name

            String potentialName = fullNameField.getText().toString().trim();
            String errorMessage = "";
            if (potentialName.length() <= 0) { //If no name entered
                errorMessage = "You need to enter your first and last name";
                nameInstructions.setText(errorMessage);
                nameInstructions.setTextColor(getResources().getColor(R.color.matchflare_pink));

                t.send(new HitBuilders.EventBuilder()
                        .setCategory("ui_action")
                        .setAction("button_press")
                        .setLabel("VerificationInvalidName")
                        .build());
            }
            else if (!potentialName.contains(" ")) { //If no 'space' detected...
                errorMessage = "You need to enter your first and last name";
                nameInstructions.setText(errorMessage);
                nameInstructions.setTextColor(getResources().getColor(R.color.matchflare_pink));
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("ui_action")
                        .setAction("button_press")
                        .setLabel("VerificationInvalidName")
                        .build());
            }
            else { //If valid name, transition to gender layout
                toVerifyPerson.guessed_full_name = fullNameField.getText().toString();
                nameLayout.setVisibility(View.GONE);
                genderLayout.setVisibility(View.VISIBLE);
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

                t.send(new HitBuilders.EventBuilder()
                        .setCategory("ui_action")
                        .setAction("button_press")
                        .setLabel("VerificationNameSubmitted")
                        .build());
            }
        }
        else if (view.getId() == R.id.gender_preference_button) {
            ArrayList<String> genderPreferences = new ArrayList<String>();
            if (malesCheckBox.isChecked()) {
                genderPreferences.add("MALE");
            }

            if (femalesCheckBox.isChecked()) {
                genderPreferences.add("FEMALE");
            }

            if (genderPreferences.size() > 0) { //Proceed to picture layout...
                toVerifyPerson.gender_preferences = genderPreferences;
                preferenceLayout.setVisibility(View.GONE);
                pictureLayout.setVisibility(View.VISIBLE);

                t.send(new HitBuilders.EventBuilder()
                        .setCategory("ui_action")
                        .setAction("button_press")
                        .setLabel("VerificationPreferenceSubmitted")
                        .build());
            }
            else { //If no gender preference was given...
                genderPreferenceErrorMessage.setText("Must choose at least one preference.");
                genderPreferenceErrorMessage.setVisibility(View.VISIBLE);

                t.send(new HitBuilders.EventBuilder()
                        .setCategory("ui_action")
                        .setAction("button_press")
                        .setLabel("VerificationInvalidPreference")
                        .build());
            }
        }
        else if (view.getId() == R.id.skip_picture_button) {
            if (imageURL != null && imageURL != "") {
                toVerifyPerson.image_url = imageURL;
            }
            pictureLayout.setVisibility(View.GONE);
            codeLayout.setVisibility(View.VISIBLE);

            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("VerificationImageChosen")
                    .build());
        }
        if (view.getId() == R.id.start_matching_button) { //If submitting...

            toVerify = true;
            InputMethodManager imm = (InputMethodManager)getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(verificationCode.getWindowToken(), 0);
            startMatchingButton.setEnabled(false);

            codeLayout.setVisibility(View.GONE);
            progressIndicator.setVisibility(View.VISIBLE);

            if (finishedProcessingContacts) { //If done processing contacts, then try verifying code...
                toVerifyPerson.contact_objects = ((Global)getApplication()).thisUser.contact_objects;
                Map<String, String> options = new HashMap<String, String>();
                options.put("device_id",((Global) getApplication()).getDeviceID());
                options.put("phone_number",rawPhoneNumber);
                options.put("input_verification_code", verificationCode.getText().toString());

                ((Global)getApplication()).ui.verifyVerificationSMS(toVerifyPerson, options, new VerifySMSCallback());
            }

            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("VerificationCodeSubmitted")
                    .build());
        }
        else if (view.getId() == R.id.choose_picture_button) { //Start CropImageActivity

            Intent i = new Intent(VerificationActivity.this,CropImageActivity.class);
            startActivityForResult(i, 2);

            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("VerificationChooseImagePressed")
                    .build());
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {  //Handles input of phone number

        //Send Verification SMS
        Map<String, String> options = new HashMap<String, String>();
        String device_id = ((Global) getApplication()).getDeviceID();
        options.put("device_id", device_id);
        rawPhoneNumber = ((PhoneNumberDialog) dialog).getRawPhoneNumber();
        options.put("phone_number",rawPhoneNumber);

        ((Global) getApplication()).ui.sendSMSVerification(options,this);
        ((Global) getApplication()).ui.getPicture(options, new Callback<StringResponse>() { //If registered in past, then retrieve pic associated with this phone number

            @Override
            public void success(StringResponse response, Response response2) {
                imageURL = response.response;
                Picasso.with(VerificationActivity.this).load(response.response).fit().centerInside().transform(new CircleTransform()).into(profileThumbnail);
                imageSkipButton.setText("Next");
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("No verified image found", error.toString());
            }
        });

        //Google Analytics
        Tracker t = ((Global) getApplication()).getTracker();
        t.send(new HitBuilders.EventBuilder()
                .setCategory("ui_action")
                .setAction("button_press")
                .setLabel("VerificationPhoneNumberSubmitted")
                .build());
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {  //If did not provide phone number
        Tracker t = ((Global) getApplication()).getTracker();
        t.send(new HitBuilders.EventBuilder()
                .setCategory("ui_action")
                .setAction("button_press")
                .setLabel("VerificationPhoneNumberNotProvided")
                .build());
    }

    @Override
    public void success(StringResponse response, Response response2) {
        //Successfully sent verification SMS
    }

    @Override
    public void failure(RetrofitError error) { //Failed to send SMS
        //Google Analytics
        Tracker t = ((Global) getApplication()).getTracker();
        t.send(new HitBuilders.ExceptionBuilder()
                .setDescription("(Verification) Failed to send verification SMS: " +
                        new StandardExceptionParser(VerificationActivity.this, null)
                                .getDescription(Thread.currentThread().getName(), error))
                .setFatal(false)
                .build());
    }

    /*
     * Checks if the verification code was valid
     */
    public class VerifySMSCallback implements Callback<Person> {
        @Override
        public void success(Person person, Response response) { //Successful verification
            ((Global)getApplication()).thisUser = person; //Set this user to the returned used
            ((Global)getApplication()).setAccessToken(person.access_token); //Set the access token

            Tracker t = ((Global) getApplication()).getTracker();
            t.set("&uid", person.contact_id + ""); //Set user ID for Google Analytics

            //Proceed to PresentMatches activity
            progressIndicator.setVisibility(View.GONE);
            Intent i = new Intent(VerificationActivity.this, PresentMatchesActivity.class);
            startActivity(i);
            finish();
            Style.makeToast(VerificationActivity.this,"Get Ready, Cupid!");

            //Register for Push Notifications
            String SENDER_ID="614720100487";
            GCMRegistrarCompat.checkDevice(VerificationActivity.this);
            if (BuildConfig.DEBUG) {
                GCMRegistrarCompat.checkManifest(VerificationActivity.this);
            }

            final String regId=GCMRegistrarCompat.getRegistrationId(VerificationActivity.this);

            if (regId.length() == 0) {
                new RegisterTask(VerificationActivity.this).execute(SENDER_ID, person.contact_id + "");
            } else
            {
                Log.d(getClass().getSimpleName(), "Existing registration: " + regId);
            }

            //Google Analytics
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("VerificationDidSuccesfullyRegister")
                    .build());
        }

        @Override
        public void failure(RetrofitError error) { //Failed to verify the user

            toVerify = false;
            progressIndicator.setVisibility(View.GONE);
            codeLayout.setVisibility(View.VISIBLE);
            Log.e("Error Verifying User:", error.toString());
            Style.makeToast(VerificationActivity.this,"Invalid or Expired Code. Argh.");

            //Show phone number dialog so user can try again
            PhoneNumberDialog phoneFragment = new PhoneNumberDialog();
            phoneFragment.show(getFragmentManager(),"phone_fragment");
            startMatchingButton.setEnabled(true);
            maleButton.setChecked(false);
            femaleButton.setChecked(false);

            //Google Analyics
            Tracker t = ((Global) getApplication()).getTracker();

            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("(Verification) Failed to register user: " +
                            new StandardExceptionParser(VerificationActivity.this, null)
                                    .getDescription(Thread.currentThread().getName(), error))
                    .setFatal(false)
                    .build());
        }
    }

    //On return from Image Picker intent
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (requestCode == 2) {
          if (resultCode == RESULT_OK) {
              imageURL = data.getStringExtra("image_URL");
              Picasso.with(this).load(imageURL).fit().centerInside().transform(new CircleTransform()).into(profileThumbnail);
              imageSkipButton.setText("Next");
          }
      }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_verification, menu);
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

    /*
     * Receives broadcasts indicating SplashActivity has completed processing contacts
     */
    private BroadcastReceiver processedContactsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finishedProcessingContacts = true;
            if (toVerify) {  //If the user had finished the form and is waiting to start matching, then try verifying now
                progressIndicator.setVisibility(View.GONE);
                toVerifyPerson.contact_objects = ((Global)getApplication()).thisUser.contact_objects;
                Map<String, String> options = new HashMap<String, String>();
                options.put("device_id",((Global) getApplication()).getDeviceID());
                options.put("phone_number",rawPhoneNumber);
                options.put("input_verification_code", verificationCode.getText().toString());
                ((Global)getApplication()).ui.verifyVerificationSMS(toVerifyPerson, options, new VerifySMSCallback());
            }
        }
    };

}
