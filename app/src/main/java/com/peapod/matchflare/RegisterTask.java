package com.peapod.matchflare;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by piyushpoddar on 12/19/14.
 */
//public class RegisterTask extends AsyncTask<Context, Void, Void> {

//    Context context;
//    static final String SENDER_ID="614720100487";
//
//    @Override
//    protected Void doInBackground(Context... contexts) {
//
//        context = contexts[0];
//        //Register GCM for Push Notifications
//
//        String SENDER_ID="614720100487";
//        GCMRegistrarCompat.checkDevice(context);
//        if (BuildConfig.DEBUG) {
//            GCMRegistrarCompat.checkManifest(context);
//        }
//
//        final String regId=GCMRegistrarCompat.getRegistrationId(context);
//
//        if (regId.length() == 0) {
//            new RegisterTask(context).execute(SENDER_ID, ((Global) context.getApplicationContext()).thisUser.contact_id + "");
//        } else
//        {
//            Log.d(getClass().getSimpleName(), "Existing registration: " + regId);
//        }

//       return null;
//    }

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

//}
