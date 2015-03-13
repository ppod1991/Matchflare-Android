package com.peapod.matchflare;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;

import com.peapod.matchflare.Objects.ChatMessage;
import com.peapod.matchflare.Objects.Notification;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;

/*
 * Android Activity to send and receive chat messages for a given chat
 */
public class ChatActivity extends Activity {

    private static final String TAG = "ChatActivity";

    //Activity Components
    private ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    private EditText chatText;
    private ImageView buttonSend;
    View progressIndicator;
    View root;

    //Websocket Variables
    private WebSocketConnection mConnection = new WebSocketConnection();
    private Handler mHandler;
    private Gson gson = new Gson();

    //Chat Descriptor Variables
    private Match thisMatch;
    private int chat_id;
    private int pair_id;
    private TextView chatDescription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //Initialize components
        progressIndicator = findViewById(R.id.progress_indicator);
        root = findViewById(R.id.root_chat);
        chatDescription = (TextView) findViewById(R.id.chat_description);
        buttonSend = (ImageView) findViewById(R.id.buttonSend);
        listView = (ListView) findViewById(R.id.chat_list_view);
        chatText = (EditText) findViewById(R.id.chatText);

        //Set initial visibility
        progressIndicator.setVisibility(View.VISIBLE);
        root.setVisibility(View.GONE);
        chatDescription.setVisibility(View.INVISIBLE);

        //Apply styles
        Style.toOpenSans(this,chatDescription,"light");
        Style.toOpenSans(this,chatText,"light");

        //Get intent params
        Bundle extras = this.getIntent().getExtras();
        chat_id = extras.getInt("chat_id");
        pair_id = extras.getInt("pair_id");

        //Set listeners
        chatText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    return sendChatMessage();
                }
                return false;
            }
        });

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                sendChatMessage();
            }
        });

        //Initialize listview
        chatArrayAdapter = new ChatArrayAdapter(this,R.layout.other_chat_message,0);
        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatArrayAdapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        //To scroll the list view to bottom on data change
        chatArrayAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatArrayAdapter.getCount() - 1);
            }
        });
    }

    /*
     * Attempts to send the text currently in the text view. Returns true if successful.
     */
    private boolean sendChatMessage(){
        if (!chatText.getText().equals("")) {

            //Construct chat message object
            ChatMessage chatToSend = new ChatMessage();
            chatToSend.content = chatText.getText().toString();
            chatToSend.chat_id = chat_id;
            chatToSend.type = "message";
            chatToSend.sender_contact_id = ((Global)getApplication()).thisUser.contact_id;
            mConnection.sendTextMessage(gson.toJson(chatToSend));  //Send the text over websockets
            chatText.setText("");  //Reset text field

            //Google Analytics
            Tracker t = ((Global) getApplication()).getTracker();
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("ChatDidSendChat")
                    .build());
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        this.start();

        //Register for notification receiver
        IntentFilter filter=new IntentFilter("com.peapod.matchflare.push_notification");
        LocalBroadcastManager.getInstance(this).registerReceiver(onEvent, filter);

        //Google Analytics
        Tracker t = ((Global) this.getApplication()).getTracker();
        t.setScreenName("ChatActivity");
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    @Override
    public void onPause() {
        //End websocket connection and ping interval
        if (mHandler != null) mHandler.removeCallbacks(pingServer);
        if (mConnection != null) mConnection.disconnect();

        //Unregister for notification receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onEvent);
        super.onPause();
    }

    /*
     * Creates and sets the message describing this match on top of the activity
     * @param m  The match of this current activity
     */
    public void setChatDescription(Match m) {

        int thisUserContactId = ((Global) getApplication()).thisUser.contact_id;
        String description = "";

        chatDescription.setVisibility(View.VISIBLE);

        if (m.chat_id == chat_id) {  //If this is the main chat
            if (m.first_matchee.contact_id == thisUserContactId) {  //Which user is the current user
                description = "Chat with " + firstName(m.second_matchee.guessed_full_name) + "!";
            }
            else if (m.second_matchee.contact_id == thisUserContactId) {
                description = "Chat with " + firstName(m.first_matchee.guessed_full_name) + "!";
            }

        }
        else if (m.first_matchee.matcher_chat_id == chat_id || m.second_matchee.matcher_chat_id == chat_id)  { //If this is a chat with the matcher...
            if (m.first_matchee.contact_id == thisUserContactId) {  //If this user is asking the matcher (and is first matchee)
                if (m.is_anonymous) {
                    description = "Ask your matcher about " + firstName(m.second_matchee.guessed_full_name) + "!";
                }
                else {
                    description = "Ask " + firstName(m.matcher.guessed_full_name) + " about " + firstName(m.second_matchee.guessed_full_name) + "!";
                }
            }
            else if (m.second_matchee.contact_id == thisUserContactId) { //If this user is asking the matcher (and is second matchee)
                if (m.is_anonymous) {
                    description = "Ask your matcher about " + firstName(m.first_matchee.guessed_full_name) + "!";
                }
                else {
                    description = "Ask " + firstName(m.matcher.guessed_full_name) + " about " + firstName(m.first_matchee.guessed_full_name) + "!";
                }
            }
            else if (m.matcher.contact_id == thisUserContactId) { //If this user is the matcher
                    if (m.first_matchee.matcher_chat_id == chat_id) { //Determine which matchee the other chatter is
                        description = "Answer " + firstName(m.first_matchee.guessed_full_name) + "'s questions about " + firstName(m.second_matchee.guessed_full_name);
                    }
                    else if (m.second_matchee.matcher_chat_id == chat_id) {
                        description = "Answer " + firstName(m.second_matchee.guessed_full_name) + "'s questions about " + firstName(m.first_matchee.guessed_full_name);
                    }
            }
        }
        else {  //If this is a rogue user not in the match!!
            chatDescription.setVisibility(View.GONE);

            //Google Analytics
            Tracker t = ((Global) getApplication()).getTracker();
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("Error: User " + thisUserContactId + " is not in the chat: " + this.chat_id)
                    .setFatal(false)
                    .build());
        }

        chatDescription.setText(description);  //Set the description
    }

    /*
     * Given a first and last name, return the first name
     * @param fullName A string with the full name of the user
     * @return The first name of the user
     */
    public String firstName(String fullName) {
        return fullName.split(" ", 2)[0];
    };


    /*
     * Initiates a web-socket connection with which to send and receive chat messages
     */
    private void start() {

        final String wsuri = "ws://matchflare.herokuapp.com/liveChat";

        try {
            WebSocketHandler wsHandler = new WebSocketHandler() {
                @Override
                public void onOpen() {
                    Log.d(TAG, "Status: Connected to " + wsuri);

                    //Construct and send 'set-up' message
                    ChatMessage initialMessage = new ChatMessage();
                    initialMessage.type = "set_chat_id";
                    initialMessage.chat_id = chat_id;
                    initialMessage.pair_id = pair_id;
                    initialMessage.sender_contact_id = ((Global) getApplication()).thisUser.contact_id;
                    String JSONMessage = gson.toJson(initialMessage);
                    mConnection.sendTextMessage(JSONMessage);

                    //Start-up ping Interval
                    mHandler = new Handler();
                    pingServer.run();
                }

                @Override
                public void onTextMessage(String payload) {

                    Log.d(TAG, "Got echo: " + payload);
                    ChatMessage m = gson.fromJson(payload, ChatMessage.class);

                    //If the incoming chat is the history of past messages
                    if (m.type.equals("history")) {

                        if (m.pair.is_anonymous) { //Set the matcher to anonymous
                            chatArrayAdapter.setAnonymousID(m.pair.matcher.contact_id);
                        }

                        //Show timestamp of chat message on click
                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position,
                                                    long id) {
                                TextView dateStamp = (TextView) view.findViewById(R.id.date_field);
                                if (dateStamp.getVisibility() == View.VISIBLE) {
                                    dateStamp.setVisibility(View.INVISIBLE);
                                    chatArrayAdapter.chatMessageList.get(position).timeShowing = false;
                                }
                                else {
                                    chatArrayAdapter.chatMessageList.get(position).timeShowing = true;
                                    dateStamp.setVisibility(View.VISIBLE);
                                }
                            }
                        });

                        chatArrayAdapter.chatMessageList = m.history;
                        thisMatch = m.pair;
                        listView.setSelection(chatArrayAdapter.getCount() - 1);

                        //Remove loading indicators
                        progressIndicator.setVisibility(View.GONE);
                        root.setVisibility(View.VISIBLE);

                        ChatActivity.this.setChatDescription(thisMatch);
                    }
                    else if (m.type.equals("message")) { //If received normal 'user' message

                        //Vibrate is the message if from someone other than current user
                        if (m.sender_contact_id != ((Global) ChatActivity.this.getApplication()).thisUser.contact_id) {
                            Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                            v.vibrate(100);
                        }
                        chatArrayAdapter.add(m);
                    }
                    else if (m.type.equals("error")) {  //Handle error chat message

                        Style.makeToast(ChatActivity.this, "Error receiving messages and/or chat history. Try again!");

                        //Google Analytics
                        Tracker t = ((Global) getApplication()).getTracker();
                        t.send(new HitBuilders.ExceptionBuilder()
                                .setDescription("Received error chat messages: " + m.toString())
                                .setFatal(false)
                                .build());
                    }
                }

                @Override
                public void onClose(int code, String reason) {
                    if(mHandler!= null) {  //Remove ping interval
                        mHandler.removeCallbacks(pingServer);
                        Log.d(TAG, "Connection lost.");
                    }
                }
            };

            //Create new connection, if does not exist
            if (mConnection == null) {
                mConnection = new WebSocketConnection();
                mConnection.connect(wsuri, wsHandler);
            }
            else {
                if (mConnection.isConnected()) {  //Disconnect and reconnect
                    mConnection.disconnect();
                    mConnection.connect(wsuri,wsHandler);
                }
                else { //Just connect
                    mConnection.connect(wsuri, wsHandler);
                }
            }

        } catch (WebSocketException e) {

            Log.e("Failed to start websocket!!", e.toString());
            Style.makeToast(ChatActivity.this,"Failed to start chat: " + e.getLocalizedMessage());

            //Google Analytics
            Tracker t = ((Global) getApplication()).getTracker();
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("Failed to start chat websocket: " +
                            new StandardExceptionParser(this, null)
                                    .getDescription(Thread.currentThread().getName(), e))
                    .setFatal(false)
                    .build());
        }
    }

    //Return to main activity on return
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        Intent i = new Intent(this,SplashActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_button_only, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.home_icon:
                intent = new Intent(this, PresentMatchesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); //Bring back existing PresentMatchesActivity, if exists
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //Receive remote notifications
    private BroadcastReceiver onEvent=new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Notification notification = (Notification) intent.getSerializableExtra("notification");
            if (notification != null) {

                //Vibrate
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = {0,100};
                v.vibrate(pattern,-1);

                Style.makeToast(ChatActivity.this,"New Notification!");
            }
        }
    };

    //Ping interval runnable to keep websocket connection alive
    Runnable pingServer = new Runnable() {
        public void run () {
            ChatMessage m = new ChatMessage("Ping from android");
            String JSONMessage = gson.toJson(m);
            mConnection.sendTextMessage(JSONMessage);
            mHandler.postDelayed(pingServer, 30000);
        }
    };
}
