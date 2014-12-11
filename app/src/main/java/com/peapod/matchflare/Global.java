package com.peapod.matchflare;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import retrofit.RestAdapter;


/**
 * Created by piyushpoddar on 11/20/14.
 */

@ReportsCrashes(formKey = "",
        resToastText = R.string.crash_toast_text,
        mode = ReportingInteractionMode.TOAST, mailTo = "ppod1991@gmail.com")
public class Global extends Application {

    static final String ACTION_EVENT = "e";
    static String device_id;
    static RestService ui;

    public Person thisUser = new Person();

    public void onCreate() {
        super.onCreate();
        ACRA.init(this);
        // EmailIntentSender e = new EmailIntentSender


        SharedPreferences prefs = this.getSharedPreferences(
                "com.peapod.matchflare", Context.MODE_PRIVATE);
        device_id = prefs.getString("com.peapod.matchflare.device_id", null);

        if (device_id == null || device_id.equals("")) {
            device_id = Settings.Secure.getString(this.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            if (device_id == null) {
                Integer i = new Integer((int) ((Math.random() * (999999999 - 100000000 + 1)) + 100000000));
                device_id = i.toString();
            }
            prefs.edit().putString("com.peapod.matchflare.device_id",device_id).apply();

        }

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint("https://matchflare.herokuapp.com")
                .build();
        ui = restAdapter.create(RestService.class);

    }

    public void setAccessToken(String accessToken) {
        SharedPreferences prefs = this.getSharedPreferences(
                "com.peapod.matchflare", Context.MODE_PRIVATE);
        prefs.edit().putString("com.peapod.matchflare.access_token",accessToken).apply();
    };

    public String getAccessToken() {
        SharedPreferences prefs = this.getSharedPreferences(
                "com.peapod.matchflare", Context.MODE_PRIVATE);
        return prefs.getString("com.peapod.matchflare.access_token", null);
    }

    public String getDeviceID() {
        return device_id;
    }
}

