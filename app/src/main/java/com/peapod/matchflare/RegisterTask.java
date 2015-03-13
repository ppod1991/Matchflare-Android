package com.peapod.matchflare;

import android.content.Context;
import android.util.Log;


//Register device for GCM (if not already registered)
public class RegisterTask extends GCMRegistrarCompat.BaseRegisterTask {
    RegisterTask(Context context) {
        super(context);
    }
    @Override
    public void onPostExecute(String regid) {
        Log.d(getClass().getSimpleName(), "registered as: " + regid);
    }
}

