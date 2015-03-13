package com.peapod.matchflare;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.peapod.matchflare.Objects.ChatMessage;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/*
 * ArrayAdapter to handle the formatting and display of a set of chat messages
 */
public class ChatArrayAdapter extends ArrayAdapter {

    //Activity components
    private TextView chatText;
    public List<ChatMessage> chatMessageList = new ArrayList<ChatMessage>();
    private RelativeLayout singleMessageContainer;

    //Constants
    private int fortyDp;
    private int twoDp;
    private int sixDp;
    private int anonymousID; //The ID of the user to be shown anonymously, 0 if none are anon

    /*
     * Add a chat message to this chat list
     * @param message The message to add
     */
    public void add(ChatMessage message) {
        chatMessageList.add(message);
        super.add(message);
    }

    public ChatArrayAdapter(Context context, int textViewResourceId, int mAnonymousID) {
        super(context, textViewResourceId);

        //Compute density-related constants
        float scale = context.getResources().getDisplayMetrics().density;
        fortyDp = (int) (40*scale + 0.5f);
        twoDp = (int) (2*scale + 0.5f);
        sixDp = (int) (5*scale + 0.5f);
        anonymousID = mAnonymousID;
    }

    /*
     * Sets the user with the given ID as anonymous in the chat
     * @param mAnonymousID the ID of the user to be shown anonymously
     */
    public void setAnonymousID(int mAnonymousID) {
        anonymousID = mAnonymousID;
    }

    //Returns the number of chat messages
    public int getCount() {
        return this.chatMessageList.size();
    }

    //Returns the chat message at the specified index
    public ChatMessage getItem(int index) {
        return this.chatMessageList.get(index);
    }

    //Constructs the view of a given chat message
    public View getView(int position, View convertView, ViewGroup parent) {

        View row =  convertView;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.other_chat_message, parent, false);
        }

        //Get the views of this chat message
        singleMessageContainer =  (RelativeLayout) (row.findViewById(R.id.single_message_container));
        chatText = (TextView) row.findViewById(R.id.single_message);
        Style.toOpenSans(getContext(),chatText,"light");


        //Get the message of this chat
        ChatMessage chatMessageObj = getItem(position);

        //Set the name of the sender (or anonymous)
        String name = "";
        if (chatMessageObj.sender_contact_id == anonymousID) {
            name = "Matcher";
        }
        else {
            name = chatMessageObj.guessed_full_name.split(" ", 2)[0];
        }

        //Add timestamp to the chat message
        TextView timeStamp = (TextView) row.findViewById(R.id.date_field);
        String rawDate = chatMessageObj.created_at;
        String formattedDate = "";
        try {
            TimeZone utcZone = TimeZone.getTimeZone("UTC");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            dateFormat.setTimeZone(utcZone);
            Date date = dateFormat.parse(rawDate);

            dateFormat = new SimpleDateFormat("EEE, MMM d\n h:mm a");
            dateFormat.setTimeZone(TimeZone.getDefault());
            formattedDate = dateFormat.format(date);
        }
        catch (ParseException e) {
            formattedDate = "";
        }

        //Format the timestamp
        if (chatMessageObj.timeShowing) timeStamp.setVisibility(View.VISIBLE);
        else timeStamp.setVisibility(View.INVISIBLE);

        timeStamp.setText(formattedDate);
        timeStamp.setTextSize(sixDp);
        Style.toOpenSans(getContext(),timeStamp,"light");
        timeStamp.setTextColor(getContext().getResources().getColor(R.color.light_gray));

        //Set the chat message container to left or right (+ other styling) depending on if the current user was also the sender
        if (chatMessageObj.sender_contact_id == ((Global) getContext().getApplicationContext()).thisUser.contact_id) { //If this user sent it...
            name = "Me";

            chatText.setBackgroundResource(R.drawable.final_chat_gray);
            chatText.setTextColor(Color.BLACK);

            singleMessageContainer.setGravity(Gravity.RIGHT);
            singleMessageContainer.setPadding(twoDp, twoDp, twoDp, twoDp);

            //Timestamp layout
            RelativeLayout.LayoutParams dateParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dateParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            dateParams.addRule(RelativeLayout.ALIGN_PARENT_START);
            dateParams.addRule(RelativeLayout.CENTER_VERTICAL);
            dateParams.setMargins(sixDp,sixDp,sixDp,sixDp);
            timeStamp.setLayoutParams(dateParams);

            //Container layout
            RelativeLayout.LayoutParams containerParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            containerParams.addRule(RelativeLayout.RIGHT_OF,R.id.date_field);
            containerParams.addRule(RelativeLayout.END_OF,R.id.date_field);
            chatText.setLayoutParams(containerParams);
        }
        else { //This message was sent by some other user...

            chatText.setBackgroundResource(R.drawable.final_chat_blue);
            chatText.setTextColor(Color.BLACK);

            singleMessageContainer.setGravity(Gravity.LEFT);
            singleMessageContainer.setPadding(twoDp, twoDp, twoDp, twoDp);

            //Timestamp layout
            RelativeLayout.LayoutParams dateParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dateParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            dateParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            dateParams.addRule(RelativeLayout.CENTER_VERTICAL);
            dateParams.setMargins(sixDp,sixDp,sixDp,sixDp);
            timeStamp.setLayoutParams(dateParams);

            //Container layout
            RelativeLayout.LayoutParams containerParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            containerParams.addRule(RelativeLayout.LEFT_OF,R.id.date_field);
            containerParams.addRule(RelativeLayout.START_OF,R.id.date_field);
            chatText.setLayoutParams(containerParams);
        }

        chatText.setAutoLinkMask(Linkify.ALL); //Make messages auto-link
        chatText.setText(Html.fromHtml("<b>" + name + ": " + "</b>" + chatMessageObj.content + "")); //Make the name bold

        return row;
    }

}