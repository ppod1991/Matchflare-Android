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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;

public class GCMIntentService extends GCMBaseIntentServiceCompat {

    private static int NOTIFY_ID=1337;

    public GCMIntentService() {
        super("GCMIntentService");
    }

    @Override
    protected void onMessage(Intent message) {
        dumpEvent("onMessage", message);
    }

    @Override
    protected void onError(Intent message) {
        dumpEvent("onError", message);
    }

    @Override
    protected void onDeleted(Intent message) {
        dumpEvent("onDeleted", message);
    }

    private void dumpEvent(String event, Intent message) {

        Intent intent=new Intent(Global.ACTION_EVENT);
        String jsonData = message.getStringExtra("data");
        Gson gson = new Gson();
        Notification notification = gson.fromJson(jsonData,Notification.class);
        intent.putExtra("notification", notification);
        if (!LocalBroadcastManager.getInstance(this).sendBroadcast(intent)) {
            NotificationCompat.Builder b = new NotificationCompat.Builder(this);
            Intent ui = new Intent(this, NotificationActivity.class);
            b.setAutoCancel(true)
                    .setDefaults(android.app.Notification.DEFAULT_SOUND)
                    .setContentTitle(getString(R.string.notif_title))
                    .setContentText(notification.push_message)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setTicker(getString(R.string.notif_title))
                    .setContentIntent(PendingIntent.getActivity(this, 0, ui, 0));

            NotificationManager mgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mgr.notify(NOTIFY_ID, b.build());
        }
//        Bundle extras=message.getExtras();
//        for (String key : extras.keySet()) {
//            Log.d(getClass().getSimpleName(),
//                    String.format("%s: %s=%s", event, key,
//                            extras.getString(key)));
//
//        }
    }
}