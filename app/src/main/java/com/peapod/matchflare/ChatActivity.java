package com.peapod.matchflare;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;

import org.w3c.dom.Text;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;


public class ChatActivity extends Activity {

    private static final String TAG = "ChatActivity";

    private ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    private EditText chatText;
    private ImageView buttonSend;
    private Match thisMatch;
    private WebSocketConnection mConnection = new WebSocketConnection();
    private Handler mHandler;
    private Gson gson = new Gson();
    private int chat_id;
    private int pair_id;
    private TextView chatDescription;

    View progressIndicator;
    View root;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        progressIndicator = findViewById(R.id.progress_indicator);
        root = findViewById(R.id.root_chat);

        chatDescription = (TextView) findViewById(R.id.chat_description);
        Style.toOpenSans(this,chatDescription,"light");
        chatDescription.setVisibility(View.INVISIBLE);

        progressIndicator.setVisibility(View.VISIBLE);
        root.setVisibility(View.GONE);

        Bundle extras = this.getIntent().getExtras();
        chat_id = extras.getInt("chat_id");
        pair_id = extras.getInt("pair_id");
        buttonSend = (ImageView) findViewById(R.id.buttonSend);
        listView = (ListView) findViewById(R.id.chat_list_view);


        chatText = (EditText) findViewById(R.id.chatText);
        Style.toOpenSans(this,chatText,"light");
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
        chatArrayAdapter = new ChatArrayAdapter(this,R.layout.other_chat_message,0);
        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatArrayAdapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);


        //to scroll the list view to bottom on data change
        chatArrayAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatArrayAdapter.getCount() - 1);
            }
        });


    }


    private boolean sendChatMessage(){
        if (!chatText.getText().equals("")) {
            ChatMessage chatToSend = new ChatMessage();
            chatToSend.content = chatText.getText().toString();
            chatToSend.chat_id = chat_id;
            chatToSend.type = "message";
            chatToSend.sender_contact_id = ((Global)getApplication()).thisUser.contact_id;
            mConnection.sendTextMessage(gson.toJson(chatToSend));
            chatText.setText("");
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat, menu);
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
        this.start();

        IntentFilter filter=new IntentFilter("com.peapod.matchflare.push_notification");
        LocalBroadcastManager.getInstance(this).registerReceiver(onEvent, filter);

    }

    @Override
    public void onPause() {
        if (mHandler != null) {
            mHandler.removeCallbacks(pingServer);
        }

        if (mConnection != null) {
            mConnection.disconnect();
        }

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

                Style.makeToast(ChatActivity.this,"New Notification!");
            }

        }
    };

    Runnable pingServer = new Runnable() {

        public void run () {
            ChatMessage m = new ChatMessage("Ping from android");
            String JSONMessage = gson.toJson(m);
            mConnection.sendTextMessage(JSONMessage);
            mHandler.postDelayed(pingServer, 30000);
        }
    };

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
        }

        chatDescription.setText(description);
    }

    public String firstName(String fullName) {
        return fullName.split(" ", 2)[0];
    };
    /*
     * Initiates a web-socket connection to send the node path to company
     */
    private void start() {

        final String wsuri = "ws://matchflare.herokuapp.com/liveChat";

        try {

            WebSocketHandler wsHandler = new WebSocketHandler() {

                @Override
                public void onOpen() {
                    Log.d(TAG, "Status: Connected to " + wsuri);
                    ChatMessage initialMessage = new ChatMessage();
                    initialMessage.type = "set_chat_id";
                    initialMessage.chat_id = chat_id;
                    initialMessage.pair_id = pair_id;
                    initialMessage.sender_contact_id = ((Global) getApplication()).thisUser.contact_id;
                    String JSONMessage = gson.toJson(initialMessage);
                    mConnection.sendTextMessage(JSONMessage);

                    mHandler = new Handler();
                    pingServer.run();

                }

                @Override
                public void onTextMessage(String payload) {
                    Log.d(TAG, "Got echo: " + payload);
                    ChatMessage m = gson.fromJson(payload, ChatMessage.class);
                    if (m.type.equals("history")) {

                        if (m.pair.is_anonymous) {
                            chatArrayAdapter.setAnonymousID(m.pair.matcher.contact_id);
                        }
                        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position,
                                                    long id) {
                                //Show date on click
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

                        progressIndicator.setVisibility(View.GONE);
                        root.setVisibility(View.VISIBLE);
                        ChatActivity.this.setChatDescription(thisMatch);
                    }
                    else if (m.type.equals("message")) {
                        if (m.sender_contact_id != ((Global) ChatActivity.this.getApplication()).thisUser.contact_id) {
                            Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                            // Vibrate for 500 milliseconds
                            v.vibrate(100);
                        }
                        chatArrayAdapter.add(m);
                    }
                    else if (m.type.equals("error")) {
                        Style.makeToast(ChatActivity.this, "Error receiving messages and/or chat history. Try again!");
                    }

                }

                @Override
                public void onClose(int code, String reason) {
                    if(mHandler!= null) {
                        mHandler.removeCallbacks(pingServer);
                        Log.d(TAG, "Connection lost.");
                    }

                }
            };

            if (mConnection == null) {
                mConnection = new WebSocketConnection();
                mConnection.connect(wsuri, wsHandler);
            }
            else {
                if (mConnection.isConnected()) {
                    mConnection.disconnect();

                    mConnection.connect(wsuri,wsHandler);
                }
                else {
                    mConnection.connect(wsuri, wsHandler);
                }
            }



        } catch (WebSocketException e) {

            Log.e("Failed to start websocket!!", e.toString());
            Style.makeToast(ChatActivity.this,"Failed to start chat: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        Intent i = new Intent(this,SplashActivity.class);
        startActivity(i);
        finish();
    }
}
