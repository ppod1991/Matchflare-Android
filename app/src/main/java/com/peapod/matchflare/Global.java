package com.peapod.matchflare;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.peapod.matchflare.Objects.Person;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import retrofit.RestAdapter;

@ReportsCrashes(formKey = "",
        resToastText = R.string.crash_toast_text,
        mode = ReportingInteractionMode.TOAST, mailTo = "ppod1991@gmail.com")

/*
 * Global Application to keep shared instance of User
 */
public class Global extends Application {

    static final String ACTION_EVENT = "e";
    static String device_id;
    static RestService ui;
    Tracker t;
    public Person thisUser = new Person();

    public void onCreate() {
        super.onCreate();
        ACRA.init(this); //Crash reporting

        //Get (or create the device ID)
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

        //Create a rest adapter for the application to use
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint("https://matchflare.herokuapp.com")
                .build();
        ui = restAdapter.create(RestService.class);
    }

    //Stores this user's access token in Shared Preferences
    public void setAccessToken(String accessToken) {
        SharedPreferences prefs = this.getSharedPreferences(
                "com.peapod.matchflare", Context.MODE_PRIVATE);
        prefs.edit().putString("com.peapod.matchflare.access_token",accessToken).apply();
    };

    //Retrieves this user's access token from Shared Preferences
    public String getAccessToken() {
        SharedPreferences prefs = this.getSharedPreferences(
                "com.peapod.matchflare", Context.MODE_PRIVATE);
        return prefs.getString("com.peapod.matchflare.access_token", null);
    }

    //Returns this device's device ID
    public String getDeviceID() {
        return device_id;
    }

    //Returns the Google Analytics tracker
    synchronized Tracker getTracker() {
        if (t == null) {
            t = GoogleAnalytics.getInstance(this).newTracker(R.xml.global_tracker);
            GoogleAnalytics.getInstance(this).getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
        }
        return t;
    }

}

