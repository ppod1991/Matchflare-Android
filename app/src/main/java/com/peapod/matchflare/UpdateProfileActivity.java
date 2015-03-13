package com.peapod.matchflare;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
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
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/*
 * THe Activity that handles Updating the User's Profile
 */
public class UpdateProfileActivity extends Activity implements View.OnClickListener {

    //Activity Components
    Button startMatchingButton;
    Person toVerifyPerson;
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
    Button updateProfileButton;
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
    View progressIndicator;
    TextView genderPreferenceErrorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        //Retrieve components
        startMatchingButton = (Button) findViewById(R.id.start_matching_button);
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
        updateProfileButton = (Button) findViewById(R.id.update_profile_button);
        progressIndicator = findViewById(R.id.progress_indicator);
        progressIndicator.setVisibility(View.GONE);
        genderPreferenceErrorMessage = (TextView) findViewById(R.id.gender_preference_error_message);

        nameLayout = (RelativeLayout) findViewById(R.id.name_layout);
        genderLayout = (LinearLayout) findViewById(R.id.my_gender_layout);
        preferenceLayout = (LinearLayout) findViewById(R.id.preference_layout);
        pictureLayout = (LinearLayout) findViewById(R.id.picture_layout);

        //Style components
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
        Style.toOpenSans(this,nameInstructions,"regular");
        Style.toOpenSans(this,genderInstructions,"regular");
        Style.toOpenSans(this,smsInstructions,"regular");
        Style.toOpenSans(this,interestedInInstructions,"regular");
        Style.toOpenSans(this,updateProfileButton,"light");
        Style.toOpenSans(this,genderPreferenceErrorMessage,"light");

        genderLayout.setVisibility(View.VISIBLE);
        preferenceLayout.setVisibility(View.VISIBLE);
        pictureLayout.setVisibility(View.VISIBLE);

        nameButton.setVisibility(View.GONE);
        imageSkipButton.setVisibility(View.GONE);
        preferenceButton.setVisibility(View.GONE);

        //Set listeners
        chooseImageButton.setOnClickListener(this);
        updateProfileButton.setOnClickListener(this);

        //Get the current user from the intent and pre-fill forms
        toVerifyPerson = (Person) getIntent().getSerializableExtra("this_user");
        toVerifyPerson.contacts = null;
        toVerifyPerson.contact_objects = null;

        fullNameField.setText(toVerifyPerson.guessed_full_name);
        fullNameField.setEnabled(false);
        nameInstructions.setVisibility(View.GONE);

        if (toVerifyPerson.guessed_gender != null && toVerifyPerson.guessed_gender.equals("MALE")) {
            maleButton.setChecked(true);
            femaleButton.setChecked(false);
        }
        else if (toVerifyPerson.guessed_gender != null && toVerifyPerson.guessed_gender.equals("FEMALE")) {
            femaleButton.setChecked(true);
            maleButton.setChecked(false);
        }

        if (toVerifyPerson.gender_preferences != null && toVerifyPerson.gender_preferences.contains("MALE")) {
            malesCheckBox.setChecked(true);
        }

        if (toVerifyPerson.gender_preferences != null && toVerifyPerson.gender_preferences.contains("FEMALE")) {
            femalesCheckBox.setChecked(true);
        }

        Picasso.with(this).load(toVerifyPerson.image_url).fit().centerInside().transform(new CircleTransform()).into(profileThumbnail);
    }

    public void onResume() {
        super.onResume();

        //Google Analytics
        Tracker t = ((Global) this.getApplication()).getTracker();
        t.setScreenName("UpdateProfileActivity");
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.update_profile_button) { //Gather all form data...

            ArrayList<String> genderPreferences = new ArrayList<String>();
            if (malesCheckBox.isChecked()) {
                genderPreferences.add("MALE");
            }
            if (femalesCheckBox.isChecked()) {
                genderPreferences.add("FEMALE");
            }

            if (genderPreferences.size() <= 0) { //Check if at least one gender was chosen as preferred...
                genderPreferenceErrorMessage.setVisibility(View.VISIBLE);
                genderPreferenceErrorMessage.setText("Must choose at least one preference.");

                //Google Analytics
                Tracker t = ((Global) getApplication()).getTracker();
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("ui_action")
                        .setAction("button_press")
                        .setLabel("UpdateInvalidPreference")
                        .build());
            }
            else { //All is valid
                toVerifyPerson.gender_preferences = genderPreferences;
                toVerifyPerson.guessed_full_name = fullNameField.getText().toString();

                if (imageURL != null && imageURL != "") {
                    toVerifyPerson.image_url = imageURL;
                }
                if (maleButton.isChecked()) {
                    toVerifyPerson.guessed_gender = "MALE";
                }
                else if (femaleButton.isChecked()) {
                    toVerifyPerson.guessed_gender = "FEMALE";
                }

                progressIndicator.setVisibility(View.VISIBLE);
                updateProfileButton.setEnabled(false);

                //Post the updated profile
                ((Global) getApplication()).ui.updateProfile(toVerifyPerson,new Callback<Person>() {
                    @Override
                    public void success(Person person, Response response) {
                        Log.e("Updated your profile", person.toString());

                        //Modify the global current user with updated fields
                        ((Global) getApplication()).thisUser.guessed_gender = person.guessed_gender;
                        ((Global) getApplication()).thisUser.gender_preferences = person.gender_preferences;
                        ((Global) getApplication()).thisUser.image_url = person.image_url;

                        progressIndicator.setVisibility(View.GONE);
                        Style.makeToast(UpdateProfileActivity.this, "Profile Updated! How'd you get even more good looking?");

                        //Go back to Present Matches
                        Intent i = new Intent(UpdateProfileActivity.this,PresentMatchesActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(i);
                        finish(); //Don't allow back button to bring back this activity
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e("Cant update the profile", error.toString());
                        progressIndicator.setVisibility(View.GONE);
                        updateProfileButton.setEnabled(true);

                        Style.makeToast(UpdateProfileActivity.this, "Failed to update your profile. Try again.");

                        //Google Analytics
                        Tracker t = ((Global) getApplication()).getTracker();
                        t.send(new HitBuilders.ExceptionBuilder()
                                .setDescription("(Update) Failed to update profile: " +
                                        new StandardExceptionParser(UpdateProfileActivity.this, null)
                                                .getDescription(Thread.currentThread().getName(), error))
                                .setFatal(false)
                                .build());
                    }
                });

                //Google Analytics
                Tracker t = ((Global) getApplication()).getTracker();
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("ui_action")
                        .setAction("button_press")
                        .setLabel("UpdateUpdatePressed")
                        .build());
            }
        }
        else if (view.getId() == R.id.choose_picture_button) { //Start CropImageActivity

            Intent i = new Intent(UpdateProfileActivity.this,CropImageActivity.class);
            startActivityForResult(i, 2);

            //Google Analytics
            Tracker t = ((Global) getApplication()).getTracker();
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("UpdateChooseImagePressed")
                    .build());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_update_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //On return from choosing a new image
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                imageURL = data.getStringExtra("image_URL");
                Picasso.with(this).load(imageURL).fit().centerInside().transform(new CircleTransform()).into(profileThumbnail);
                imageSkipButton.setText("Next");
            }
        }
    };

}
