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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class NotificationActivity extends Activity{

    public ExpandableListView notificationsList;
    public NotificationsAdapter notificationsAdapter;

    View progressIndicator;
    View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        progressIndicator = findViewById(R.id.progress_indicator);
        root = findViewById(R.id.root_notifications);

        notificationsList = (ExpandableListView) findViewById(R.id.notifications_list);
        notificationsAdapter = new NotificationsAdapter(NotificationActivity.this);

        notificationsList.setOnChildClickListener(new NotificationListItemClickListener());
        //notificationsList.setOnItemClickListener(new NotificationListItemClickListener());


        Map<String, Integer> optionsPending = new HashMap<String, Integer>();
        optionsPending.put("contact_id",((Global) getApplication()).thisUser.contact_id);
        ((Global) getApplication()).ui.getNotificationLists(optionsPending, new NotificationsListCallback());


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.notification, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        Map<String, Integer> optionsPending = new HashMap<String, Integer>();
        optionsPending.put("contact_id",((Global) getApplication()).thisUser.contact_id);
        ((Global) getApplication()).ui.getNotificationLists(optionsPending, new NotificationsListCallback());


        IntentFilter filter=new IntentFilter("com.peapod.matchflare.push_notification");
        LocalBroadcastManager.getInstance(this).registerReceiver(onEvent, filter);
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onEvent);
        super.onPause();
    }


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

    public class NotificationListItemClickListener implements ExpandableListView.OnChildClickListener {

        @Override
        public boolean onChildClick(ExpandableListView parent, View v,
                                    int groupPosition, int childPosition, long id) {
            final Object selected = notificationsAdapter.getChild(
                    groupPosition, childPosition);

            if (selected instanceof Notification) {
                Notification chosenNotification = (Notification) selected;

                Map<String,Integer> options = new HashMap<String,Integer>();
                options.put("notification_id",chosenNotification.notification_id);
                ((Global) getApplication()).ui.seeNotification(options, new seeNotificationCallback());

                Intent i = Notification.makeIntent(NotificationActivity.this,chosenNotification);
                if (i != null) {
                    startActivity(i);
                }
            }
            else if (selected instanceof Match) {

                //Check the group...NEED TO IMPLEMENT

                Match chosenMatch = (Match) selected;
                Intent intent;
                if (groupPosition == 2) {
                    intent = new Intent(NotificationActivity.this, ViewMatchActivity.class);
                }
                else {
                    intent = new Intent(NotificationActivity.this, EvaluateActivity.class);
                }

                intent.putExtra("pair", chosenMatch);
                startActivity(intent);
            }
            return true;
        }
    }

    public class seeNotificationCallback implements Callback<StringResponse>
    {
        @Override
        public void success(StringResponse response, Response response2) {
            Log.e("Notification successfully marked as seen", response.response);
        }

        @Override
        public void failure(RetrofitError error) {
           Log.e("Failure to mark notification as seen", error.toString());
        }
    }
//
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        Intent i = new Intent(this,SplashActivity.class);
        startActivity(i);
        finish();
    }

    public class NotificationsListCallback implements Callback<NotificationLists>
    {
        @Override
        public void success(NotificationLists response, Response response2) {
            Log.e("Notifications Lists retrieved", response.toString());

            notificationsAdapter.headers = new ArrayList<String>(Arrays.asList("Notifications", "Matches You're In", "Matches You've Made"));

            notificationsAdapter.notificationCollections.put(notificationsAdapter.headers.get(0), response.notifications);
            notificationsAdapter.notificationCollections.put(notificationsAdapter.headers.get(1), response.pending_matches);
            notificationsAdapter.notificationCollections.put(notificationsAdapter.headers.get(2), response.active_matcher_matches);

            notificationsList.setAdapter(notificationsAdapter);
            notificationsList.expandGroup(0);
            notificationsList.expandGroup(1);
            notificationsList.expandGroup(2);

            progressIndicator.setVisibility(View.GONE);
            root.setVisibility(View.VISIBLE);

        }

        @Override
        public void failure(RetrofitError error) {
            Log.e("Failure to get pending matches", error.toString());
        }
    }

}
