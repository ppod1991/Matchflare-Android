package com.peapod.matchflare;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by piyushpoddar on 11/26/14.
 */
public class ChatArrayAdapter extends ArrayAdapter {

    private TextView chatText;
    public List<ChatMessage> chatMessageList = new ArrayList<ChatMessage>();
    private LinearLayout singleMessageContainer;
    private int fortyDp;
    private int twoDp;

    public void add(ChatMessage object) {
        chatMessageList.add(object);
        super.add(object);
    }

    public ChatArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);

        float scale = context.getResources().getDisplayMetrics().density;
        fortyDp = (int) (40*scale + 0.5f);
        twoDp = (int) (2*scale + 0.5f);
    }

    public int getCount() {
        return this.chatMessageList.size();
    }

    public ChatMessage getItem(int index) {
        return this.chatMessageList.get(index);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.other_chat_message, parent, false);
        }
        singleMessageContainer = (LinearLayout) row.findViewById(R.id.single_message_container);
        ChatMessage chatMessageObj = getItem(position);
        chatText = (TextView) row.findViewById(R.id.single_message);
        chatText.setText(chatMessageObj.guessed_full_name + ": " + chatMessageObj.content);
        Style.toOpenSans(getContext(),chatText,"light");


        if (chatMessageObj.sender_contact_id == ((Global) getContext().getApplicationContext()).thisUser.contact_id) {
            chatText.setBackgroundResource(R.drawable.final_chat_gray);
            singleMessageContainer.setGravity(Gravity.RIGHT);
            singleMessageContainer.setPadding(fortyDp,twoDp,twoDp,twoDp);
            chatText.setText("Me: " + chatMessageObj.content);
            chatText.setTextColor(Color.BLACK);

        }
        else {
            chatText.setBackgroundResource(R.drawable.final_chat_blue);
            singleMessageContainer.setGravity(Gravity.LEFT);
            singleMessageContainer.setPadding(twoDp,twoDp,fortyDp,twoDp);
            chatText.setText(chatMessageObj.guessed_full_name + ": " + chatMessageObj.content);
            chatText.setTextColor(Color.BLACK);
        }

        return row;
    }

}