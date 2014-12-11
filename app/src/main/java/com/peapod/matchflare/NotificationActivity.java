package com.peapod.matchflare;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);


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

                if (chosenNotification.notification_type.equals("MATCHEE_NEW_MATCH")) {
                    Intent intent = new Intent(NotificationActivity.this, EvaluateActivity.class);
                    intent.putExtra("pair_id",chosenNotification.pair_id);
                    startActivity(intent);
                }
                else if (chosenNotification.notification_type.equals("MATCHEE_MATCH_ACCEPTED")) {
                    Intent intent = new Intent(NotificationActivity.this, ChatActivity.class);
                    intent.putExtra("chat_id",chosenNotification.chat_id);
                    startActivity(intent);
                }
            }
            else if (selected instanceof Match) {

                //Check the group...NEED TO IMPLEMENT

                Match chosenMatch = (Match) selected;
                Intent intent = new Intent(NotificationActivity.this, EvaluateActivity.class);
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

        }

        @Override
        public void failure(RetrofitError error) {
            Log.e("Failure to get pending matches", error.toString());
        }
    }
}
