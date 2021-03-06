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
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;
import com.peapod.matchflare.Objects.Notification;

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

        //Google Analytics
        Tracker t = ((Global) getApplication()).getTracker();
        t.send(new HitBuilders.ExceptionBuilder()
                .setDescription("Received GCM Error Message" + message)
                .setFatal(false)
                .build());
    }

    @Override
    protected void onDeleted(Intent message) {
        dumpEvent("onDeleted", message);
    }

    private void dumpEvent(String event, Intent message) {

        if (event.equals("onMessage")) {

            //Handled received remote notification
            Intent intent=new Intent("com.peapod.matchflare.push_notification");
            String jsonData = message.getStringExtra("data");
            Gson gson = new Gson();

            try {
                Notification notification = gson.fromJson(jsonData,Notification.class);
                if (notification != null) {

                    intent.putExtra("notification", notification);
                    String notificationTitle = "Matchflare Notification!";

                    //Set notifcation title based on notification type
                    if (notification.notification_type != null) {
                        if (notification.notification_type.equals("USER_REMINDER")) {
                            notificationTitle = "What do you think of them?";
                        }
                        else if (notification.notification_type.equals("MATCHER_ONE_MATCH_ACCEPTED")) {
                            notificationTitle = "Match Accepted!";
                        }
                        else if (notification.notification_type.equals("MATCHER_MESSAGING_STARTED")) {
                            notificationTitle = "They started talking!";
                        }
                        else if (notification.notification_type.equals("MATCHER_QUESTION_ASKED")) {
                            notificationTitle = "New Question?";
                        }
                        else if (notification.notification_type.equals("MATCHEE_NEW_MATCH")) {
                            notificationTitle = "New Match!";
                        }
                        else if (notification.notification_type.equals("MATCHEE_MATCH_ACCEPTED")) {
                            notificationTitle = "Match made!";
                        }
                        else if (notification.notification_type.equals("MATCHEE_QUESTION_ANSWERED")) {
                            notificationTitle = "Question Answered!";
                        }
                        else if (notification.notification_type.equals("MATCHEE_MESSAGE_SENT")) {
                            notificationTitle = "New Message!";
                        }
                    }

                    //If no local activity received the notification, then set phone notification
                    if (!LocalBroadcastManager.getInstance(this).sendBroadcast(intent)) {
                        NotificationCompat.Builder b = new NotificationCompat.Builder(this);
                        Intent ui = new Intent(this, SplashActivity.class);
                        ui.putExtra("notification",notification);
                        ui.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        long[] vibrate = {0,100};
                        b.setAutoCancel(true)
                                .setContentTitle(notificationTitle)
                                .setContentText(notification.push_message)
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setTicker(notification.push_message)
                                .setLights(Color.argb(255,250,69,118),2000,500)
                                .setVibrate(vibrate)
                                .setContentIntent(PendingIntent.getActivity(this, 0, ui, PendingIntent.FLAG_UPDATE_CURRENT));

                        NotificationManager mgr =
                                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        mgr.notify(NOTIFY_ID, b.build());
                    }
                }

            }
            catch (Error e) {
                Log.e("Cant parse notification", e.toString());

                //Google Analytics
                Tracker t = ((Global) getApplication()).getTracker();
                t.send(new HitBuilders.ExceptionBuilder()
                        .setDescription("(GCMIntentService) Could not parse notification push message: " +
                                new StandardExceptionParser(this, null)
                                        .getDescription(Thread.currentThread().getName(), e))
                        .setFatal(false)
                        .build());
            }
        }
    }
}