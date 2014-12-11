package com.peapod.matchflare;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;

import com.squareup.picasso.Picasso;

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
    ImageView profileThumbnail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        startMatchingButton = (Button) findViewById(R.id.start_matching_button);
        startMatchingButton.setOnClickListener(this);

        verificationCode = (EditText) findViewById(R.id.verification_code_field);
        maleButton = (RadioButton) findViewById(R.id.my_gender_guy);
        femaleButton = (RadioButton) findViewById(R.id.my_gender_girl);
        malesCheckBox = (CheckBox) findViewById(R.id.guys_check_box);
        femalesCheckBox = (CheckBox) findViewById(R.id.girls_check_box);
        fullNameField = (EditText) findViewById(R.id.full_name_field);
        chooseImageButton = (Button) findViewById(R.id.choose_picture_button);
        profileThumbnail = (ImageView) findViewById(R.id.profile_pic_thumbnail);

        chooseImageButton.setOnClickListener(this);


        PhoneNumberDialog phoneFragment = new PhoneNumberDialog();
        phoneFragment.show(getFragmentManager(),"phone_fragment");

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
        if (view.getId() == R.id.start_matching_button) {

            toVerifyPerson = new Person();

            toVerifyPerson.guessed_full_name = fullNameField.getText().toString();
            if (maleButton.isChecked()) {
                toVerifyPerson.guessed_gender = "MALE";
            }
            else if (femaleButton.isChecked()) {
                toVerifyPerson.guessed_gender = "FEMALE";
            }

            ArrayList<String> genderPreferences = new ArrayList<String>();
            if (malesCheckBox.isChecked()) {
                genderPreferences.add("MALE");
            }

            if (femalesCheckBox.isChecked()) {
                genderPreferences.add("FEMALE");
            }

            if (genderPreferences.size() > 0) {
                toVerifyPerson.gender_preferences = genderPreferences;
            }
            toVerifyPerson.contact_objects = ((Global)getApplication()).thisUser.contact_objects;
            toVerifyPerson.image_url = imageURL;

            Map<String, String> options = new HashMap<String, String>();
            options.put("device_id",((Global) getApplication()).getDeviceID());
            options.put("phone_number",rawPhoneNumber);
            options.put("input_verification_code", verificationCode.getText().toString());

            ((Global)getApplication()).ui.verifyVerificationSMS(toVerifyPerson, options, new VerifySMSCallback());

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

            Intent i = new Intent(VerificationActivity.this, PresentMatchesActivity.class);
            startActivity(i);
            Style.makeToast(VerificationActivity.this,"Get Ready, Cupid!");
        }

        @Override
        public void failure(RetrofitError error) {
            //Failed to verify the user
            Log.e("Error Verifying User:", error.toString());
            Style.makeToast(VerificationActivity.this,"Invalid or Expired Code. Argh.");
            PhoneNumberDialog phoneFragment = new PhoneNumberDialog();
            phoneFragment.show(getFragmentManager(),"phone_fragment");
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (requestCode == 2) {
          if (resultCode == RESULT_OK) {
                imageURL = data.getStringExtra("image_URL");
                Picasso.with(this).load(imageURL).into(profileThumbnail);
          }
      }
    };
}
