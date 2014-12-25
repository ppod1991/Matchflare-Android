package com.peapod.matchflare;

/***
 Copyright (c) 2013 CommonsWare, LLC
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 by applicable law or agreed to in writing, software distributed under the
 License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 OF ANY KIND, either express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 From _The Busy Coder's Guide to Android Development_
 http://commonsware.com/Android
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import java.util.HashMap;
import java.util.Map;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class GCMBroadcastReceiverCompat extends BroadcastReceiver implements Callback<StringResponse> {


    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setClass(context, GCMIntentService.class);

        String regId = intent.getExtras().getString("registration_id");
        if(regId != null && !regId.equals("")) {

            ((Global) context.getApplicationContext()).thisUser.registration_id = regId;

            GCMRegistrarCompat.setRegistrationId(context,regId);

            //Toast.makeText(context,regId,Toast.LENGTH_LONG);
            Log.e("Registration ID", regId);

            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setLogLevel(RestAdapter.LogLevel.FULL)
                    .setEndpoint("https://matchflare.herokuapp.com")
                    .build();
            RestService ui = restAdapter.create(RestService.class);
            ui.updateRegistrationId(((Global) context.getApplicationContext()).thisUser, this);

      /* Do what ever you want with the regId eg. send it to your server */
        }

        WakefulIntentService.sendWakefulWork(context, intent);
    }

    public void failure(RetrofitError err)
    {
        Log.e("Error Updating Registration Id:", err.toString());
    }

    @Override
    public void success(StringResponse response, Response arg1)
    {
        Log.e("Registration Id successfully updated:", response.toString());
    }
}