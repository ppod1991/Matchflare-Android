package com.peapod.matchflare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;


public class ChatActivity extends Activity {

    private static final String TAG = "ChatActivity";

    private ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    private EditText chatText;
    private Button buttonSend;

    private static final WebSocketConnection mConnection = new WebSocketConnection();
    private Handler mHandler;
    private Gson gson = new Gson();
    private int chat_id;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Bundle extras = this.getIntent().getExtras();
        chat_id = extras.getInt("chat_id");

        buttonSend = (Button) findViewById(R.id.buttonSend);
        listView = (ListView) findViewById(R.id.chat_list_view);

        chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(), R.layout.other_chat_message);
        listView.setAdapter(chatArrayAdapter);

        chatText = (EditText) findViewById(R.id.chatText);
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

        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatArrayAdapter);

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
    }

    @Override
    public void onStop() {
        mHandler.removeCallbacks(pingServer);
        mConnection.disconnect();

        super.onStop();
    }

    Runnable pingServer = new Runnable() {

        public void run () {
            ChatMessage m = new ChatMessage("Ping from android");
            String JSONMessage = gson.toJson(m);
            mConnection.sendTextMessage(JSONMessage);
            mHandler.postDelayed(pingServer, 30000);
        }
    };

    /*
     * Initiates a web-socket connection to send the node path to company
     */
    private void start() {

        final String wsuri = "ws://matchflare.herokuapp.com/liveChat";

        try {
            mConnection.connect(wsuri, new WebSocketHandler() {

                @Override
                public void onOpen() {
                    Log.d(TAG, "Status: Connected to " + wsuri);
                    ChatMessage initialMessage = new ChatMessage();
                    initialMessage.type = "set_chat_id";
                    initialMessage.chat_id = chat_id;
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
                        chatArrayAdapter.chatMessageList = m.history;
                    }
                    else if (m.type.equals("message")) {
                        chatArrayAdapter.add(m);
                    }

                }

                @Override
                public void onClose(int code, String reason) {
                    if(mHandler!= null) {
                        mHandler.removeCallbacks(pingServer);
                        Log.d(TAG, "Connection lost.");
                    }

                }
            });
        } catch (WebSocketException e) {

            Log.e("Failed to start websocket!!", e.toString());
        }
    }
}
