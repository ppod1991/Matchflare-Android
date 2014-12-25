package com.peapod.matchflare;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.StringRes;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
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

import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class VerificationActivity extends Activity implements Button.OnClickListener, PhoneNumberDialog.PhoneNumberDialogListener, Callback<StringResponse> {

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
    boolean finishedProcessingContacts;
    TextView genderPreferenceErrorMessage;
    View progressIndicator;
    boolean toVerify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);


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
        progressIndicator.setVisibility(View.GONE);
        imageSkipButton.setText("Skip");
        genderPreferenceErrorMessage = (TextView) findViewById(R.id.gender_preference_error_message);

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

        chooseImageButton.setOnClickListener(this);
        startMatchingButton.setOnClickListener(this);
        imageSkipButton.setOnClickListener(this);
        nameButton.setOnClickListener(this);
        preferenceButton.setOnClickListener(this);

        nameLayout = (RelativeLayout) findViewById(R.id.name_layout);
        genderLayout = (LinearLayout) findViewById(R.id.my_gender_layout);
        preferenceLayout = (LinearLayout) findViewById(R.id.preference_layout);
        pictureLayout = (LinearLayout) findViewById(R.id.picture_layout);
        codeLayout = (LinearLayout) findViewById(R.id.code_layout);

        toVerifyPerson = new Person();

        genderRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                if (maleButton.isChecked()) {
                    toVerifyPerson.guessed_gender = "MALE";
                }
                else if (femaleButton.isChecked()) {
                    toVerifyPerson.guessed_gender = "FEMALE";
                }
                genderLayout.setVisibility(View.GONE);
                preferenceLayout.setVisibility(View.VISIBLE);
            }
        });

        PhoneNumberDialog phoneFragment = new PhoneNumberDialog();
        phoneFragment.show(getFragmentManager(),"phone_fragment");

        finishedProcessingContacts = getIntent().getBooleanExtra("finished_processing_contacts",false);
        if (!finishedProcessingContacts && ((Global) getApplication()).thisUser.contact_objects != null) {
            finishedProcessingContacts = true;
        }

        try {
            Cursor c = getApplication().getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
            c.moveToFirst();
            fullNameField.setText(c.getString(c.getColumnIndex("display_name")));
            c.close();
        }
        catch (Exception e) {
            //No full name found--leave text field as is

        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_verification, menu);
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
    public void onClick(View view) {

        if (view.getId() == R.id.name_button) {
            String potentialName = fullNameField.getText().toString().trim();
            String errorMessage = "";
            if (potentialName.length() <= 0) {
                errorMessage = "You need to enter your first and last name";
                nameInstructions.setText(errorMessage);
                nameInstructions.setTextColor(getResources().getColor(R.color.matchflare_pink));
            }
            else if (!potentialName.contains(" ")) {
                errorMessage = "You need to enter your first and last name";
                nameInstructions.setText(errorMessage);
                nameInstructions.setTextColor(getResources().getColor(R.color.matchflare_pink));
            }
            else {
                toVerifyPerson.guessed_full_name = fullNameField.getText().toString();
                nameLayout.setVisibility(View.GONE);
                genderLayout.setVisibility(View.VISIBLE);
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
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

            if (genderPreferences.size() > 0) {
                toVerifyPerson.gender_preferences = genderPreferences;
                preferenceLayout.setVisibility(View.GONE);
                pictureLayout.setVisibility(View.VISIBLE);
            }
            else {
                genderPreferenceErrorMessage.setText("Must choose at least one preference.");
                genderPreferenceErrorMessage.setVisibility(View.VISIBLE);

            }
        }
        else if (view.getId() == R.id.skip_picture_button) {
            if (imageURL != null && imageURL != "") {
                toVerifyPerson.image_url = imageURL;
            }
            pictureLayout.setVisibility(View.GONE);
            codeLayout.setVisibility(View.VISIBLE);
        }
        if (view.getId() == R.id.start_matching_button) {

            toVerify = true;
            InputMethodManager imm = (InputMethodManager)getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(verificationCode.getWindowToken(), 0);
            startMatchingButton.setEnabled(false);
            if (finishedProcessingContacts) {
                Style.makeToast(this,"Contact Objects: " + ((Global)getApplication()).thisUser.contact_objects.size());
                toVerifyPerson.contact_objects = ((Global)getApplication()).thisUser.contact_objects;
                Map<String, String> options = new HashMap<String, String>();
                options.put("device_id",((Global) getApplication()).getDeviceID());
                options.put("phone_number",rawPhoneNumber);
                options.put("input_verification_code", verificationCode.getText().toString());

                ((Global)getApplication()).ui.verifyVerificationSMS(toVerifyPerson, options, new VerifySMSCallback());
            }

            codeLayout.setVisibility(View.GONE);
            progressIndicator.setVisibility(View.VISIBLE);


        }
        else if (view.getId() == R.id.choose_picture_button) {

            Intent i = new Intent(VerificationActivity.this,CropImageActivity.class);
            startActivityForResult(i, 2);

        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        //Send sms
        Map<String, String> options = new HashMap<String, String>();
        String device_id = ((Global) getApplication()).getDeviceID();
        options.put("device_id", device_id);
        rawPhoneNumber = ((PhoneNumberDialog) dialog).getRawPhoneNumber();
        options.put("phone_number",rawPhoneNumber);
        Style.makeToast(this, (rawPhoneNumber));
        ((Global) getApplication()).ui.sendSMSVerification(options,this);
        ((Global) getApplication()).ui.getPicture(options, new Callback<StringResponse>() {

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
    }


    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {

    }

    @Override
    public void success(StringResponse response, Response response2) {

    }

    @Override
    public void failure(RetrofitError error) {

    }

    public class VerifySMSCallback implements Callback<Person> {

        @Override
        public void success(Person person, Response response) {
            ((Global)getApplication()).thisUser = person;
            ((Global)getApplication()).setAccessToken(person.access_token);

            progressIndicator.setVisibility(View.GONE);
            Intent i = new Intent(VerificationActivity.this, PresentMatchesActivity.class);
            startActivity(i);
            finish();
            Style.makeToast(VerificationActivity.this,"Get Ready, Cupid!");

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

        }

        @Override
        public void failure(RetrofitError error) {
            //Failed to verify the user
            toVerify = false;
            progressIndicator.setVisibility(View.GONE);
            codeLayout.setVisibility(View.VISIBLE);
            Log.e("Error Verifying User:", error.toString());
            Style.makeToast(VerificationActivity.this,"Invalid or Expired Code. Argh.");
            PhoneNumberDialog phoneFragment = new PhoneNumberDialog();
            phoneFragment.show(getFragmentManager(),"phone_fragment");
            startMatchingButton.setEnabled(true);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (requestCode == 2) {
          if (resultCode == RESULT_OK) {
                imageURL = data.getStringExtra("image_URL");
                Picasso.with(this).load(imageURL).fit().centerInside().transform(new CircleTransform()).into(profileThumbnail);

              imageSkipButton.setText("Next");
          }
      }
    };

    private BroadcastReceiver processedContactsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Style.makeToast(VerificationActivity.this,"Received broadcast");
            finishedProcessingContacts = true;
            if (toVerify) {  //If the user had finished the form and is waiting to start matching
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

    @Override
    public void onResume(){
        super.onResume();
        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(processedContactsReceiver,
                new IntentFilter("com.peapod.matchflare.FINISHED_PROCESSING_CONTACTS"));
    }

    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(processedContactsReceiver);
        super.onPause();
    }

}
