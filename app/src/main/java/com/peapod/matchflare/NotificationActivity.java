package com.peapod.matchflare;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;
import com.peapod.matchflare.Objects.Notification;
import com.peapod.matchflare.Objects.NotificationLists;
import com.peapod.matchflare.Objects.StringResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/*
 * Activity used to show notification lists to user and take them to appropriate activity on click
 */
public class NotificationActivity extends Activity {

    //Activity Components
    ExpandableListView notificationsList;
    NotificationsAdapter notificationsAdapter;
    View progressIndicator;
    View root;

    Tracker t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        //Retrieve components
        progressIndicator = findViewById(R.id.progress_indicator);
        root = findViewById(R.id.root_notifications);
        notificationsList = (ExpandableListView) findViewById(R.id.notifications_list);

        //Set listeners
        notificationsAdapter = new NotificationsAdapter(NotificationActivity.this);
        notificationsList.setOnChildClickListener(new NotificationListItemClickListener());

        t = ((Global) this.getApplication()).getTracker();
    }

    @Override
    public void onResume() {
        super.onResume();

        //Retrieve notifications
        Map<String, Integer> optionsPending = new HashMap<String, Integer>();
        optionsPending.put("contact_id",((Global) getApplication()).thisUser.contact_id);
        ((Global) getApplication()).ui.getNotificationLists(optionsPending, new NotificationsListCallback());

        //Register for remote notifications
        IntentFilter filter=new IntentFilter("com.peapod.matchflare.push_notification");
        LocalBroadcastManager.getInstance(this).registerReceiver(onEvent, filter);

        //Google analytics
        t.setScreenName("NotificationActivity");
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    @Override
    public void onPause() {
        //Unregister remote notification receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onEvent);
        super.onPause();
    }

    //Handles when a notification item was clicked (Header and actual Notification)
    public class NotificationListItemClickListener implements ExpandableListView.OnChildClickListener {

        @Override
        public boolean onChildClick(ExpandableListView parent, View v,
                                    int groupPosition, int childPosition, long id) {

            final Object selected = notificationsAdapter.getChild(
                    groupPosition, childPosition);

            if (selected instanceof Notification) {
                Notification chosenNotification = (Notification) selected;

                //Mark notification as seen
                Map<String,Integer> options = new HashMap<String,Integer>();
                options.put("notification_id",chosenNotification.notification_id);
                ((Global) getApplication()).ui.seeNotification(options, new seeNotificationCallback());

                Intent i = Notification.makeIntent(NotificationActivity.this,chosenNotification);
                if (i != null) {
                    startActivity(i);
                }

                //Google Analytics
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("ui_action")
                        .setAction("button_press")
                        .setLabel("NotificationNotificationTapped")
                        .build());
            }
            else if (selected instanceof Match) {

                Match chosenMatch = (Match) selected;
                Intent intent;
                if (groupPosition == 2) {
                    intent = new Intent(NotificationActivity.this, ViewMatchActivity.class);

                    //Google Analytics
                    t.send(new HitBuilders.EventBuilder()
                            .setCategory("ui_action")
                            .setAction("button_press")
                            .setLabel("NotificationActiveMatcherMatchTapped")
                            .build());
                }
                else {
                    intent = new Intent(NotificationActivity.this, EvaluateActivity.class);

                    //Google Analytics
                    t.send(new HitBuilders.EventBuilder()
                            .setCategory("ui_action")
                            .setAction("button_press")
                            .setLabel("NotificationPendingMatchTapped")
                            .build());
                }

                intent.putExtra("pair", chosenMatch);
                startActivity(intent);
            }
            else {

                //Google Analytics
                t.send(new HitBuilders.ExceptionBuilder()
                        .setDescription("(NotificationActivity) Tapped item was unrecognized")
                        .setFatal(false)
                        .build());
            }
            return true;
        }
    }

    //Callback for Marking notification as seen
    public class seeNotificationCallback implements Callback<StringResponse>
    {
        @Override
        public void success(StringResponse response, Response response2) {
            Log.e("Marked as seen", response.response);
        }

        @Override
        public void failure(RetrofitError error) {
           Log.e("Failed to mark as seen", error.toString());

           t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("Failure to mark notification as seen" +
                            new StandardExceptionParser(NotificationActivity.this, null)
                                    .getDescription(Thread.currentThread().getName(), error))
                    .setFatal(false)
                    .build());
        }
    }

    //Callback for when Notifications are retrieved
    public class NotificationsListCallback implements Callback<NotificationLists>
    {
        @Override
        public void success(NotificationLists response, Response response2) {
            Log.e("Notifications retrieved", response.toString());

            //Add data
            notificationsAdapter.headers = new ArrayList<String>(Arrays.asList("Notifications", "Matches You're In", "Matches You've Made"));
            notificationsAdapter.notificationCollections.put(notificationsAdapter.headers.get(0), response.notifications);
            notificationsAdapter.notificationCollections.put(notificationsAdapter.headers.get(1), response.pending_matches);
            notificationsAdapter.notificationCollections.put(notificationsAdapter.headers.get(2), response.active_matcher_matches);
            notificationsList.setAdapter(notificationsAdapter);

            //Start expanded
            notificationsList.expandGroup(0);
            notificationsList.expandGroup(1);
            notificationsList.expandGroup(2);

            //Remove loading indicators
            progressIndicator.setVisibility(View.GONE);
            root.setVisibility(View.VISIBLE);

        }

        @Override
        public void failure(RetrofitError error) {
            Log.e("Cant get pendingMatches", error.toString());

            //Google Analytics
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("Failure to get pending matches" +
                            new StandardExceptionParser(NotificationActivity.this, null)
                                    .getDescription(Thread.currentThread().getName(), error))
                    .setFatal(false)
                    .build());
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        Intent i = new Intent(this,SplashActivity.class);
        startActivity(i);
        finish(); //Go to main activity if restored to here
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_button_only, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent;
        switch (item.getItemId()) {

            case R.id.home_icon:
                //Do stuff
                intent = new Intent(this, PresentMatchesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //Handle new remote notification
    private BroadcastReceiver onEvent=new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Notification notification = (Notification) intent.getSerializableExtra("notification");
            if (notification != null) {

                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = {0,100};
                v.vibrate(pattern,-1);

                Map<String, Integer> options = new HashMap<String, Integer>();
                int contact_id = ((Global) getApplication()).thisUser.contact_id;

                if (contact_id > 0) {
                    options.put("contact_id",contact_id);
                    ((Global) getApplication()).ui.getNotificationLists(options, new NotificationsListCallback());
                }
            }
        }
    };

}
