package com.peapod.matchflare;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.ViewGroup.LayoutParams;

import org.w3c.dom.Text;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by piyushpoddar on 11/26/14.
 */
public class ChatArrayAdapter extends ArrayAdapter {

    private TextView chatText;
    public List<ChatMessage> chatMessageList = new ArrayList<ChatMessage>();
    private RelativeLayout singleMessageContainer;
    private int fortyDp;
    private int twoDp;
    private int sixDp;
    private int anonymousID;

    public void add(ChatMessage object) {
        chatMessageList.add(object);
        super.add(object);
    }

    public ChatArrayAdapter(Context context, int textViewResourceId, int mAnonymousID) {
        super(context, textViewResourceId);

        float scale = context.getResources().getDisplayMetrics().density;
        fortyDp = (int) (40*scale + 0.5f);
        twoDp = (int) (2*scale + 0.5f);
        sixDp = (int) (5*scale + 0.5f);
        anonymousID = mAnonymousID;
    }

    public void setAnonymousID(int mAnonymousID) {
        anonymousID = mAnonymousID;
    }

    public int getCount() {
        return this.chatMessageList.size();
    }

    public ChatMessage getItem(int index) {
        return this.chatMessageList.get(index);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View row =  convertView;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.other_chat_message, parent, false);
        }
        singleMessageContainer =  (RelativeLayout) (row.findViewById(R.id.single_message_container));
        ChatMessage chatMessageObj = getItem(position);
        chatText = (TextView) row.findViewById(R.id.single_message);

        String name = "";

        if (chatMessageObj.sender_contact_id == anonymousID) {
            name = "Matcher";
        }
        else {
            name = chatMessageObj.guessed_full_name.split(" ", 2)[0];
        }

        Style.toOpenSans(getContext(),chatText,"light");

        //Add timestamp
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

        timeStamp.setText(formattedDate);
        timeStamp.setTextSize(sixDp);

        if (chatMessageObj.timeShowing) {
            timeStamp.setVisibility(View.VISIBLE);
        }
        else {
            timeStamp.setVisibility(View.INVISIBLE);
        }

        Style.toOpenSans(getContext(),timeStamp,"light");
        timeStamp.setTextColor(getContext().getResources().getColor(R.color.light_gray));

        if (chatMessageObj.sender_contact_id == ((Global) getContext().getApplicationContext()).thisUser.contact_id) {
            chatText.setBackgroundResource(R.drawable.final_chat_gray);
            singleMessageContainer.setGravity(Gravity.RIGHT);
            singleMessageContainer.setPadding(twoDp, twoDp, twoDp, twoDp);
            name = "Me";

            chatText.setTextColor(Color.BLACK);

            RelativeLayout.LayoutParams dateParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dateParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            dateParams.addRule(RelativeLayout.ALIGN_PARENT_START);
            dateParams.addRule(RelativeLayout.CENTER_VERTICAL);
            dateParams.setMargins(sixDp,sixDp,sixDp,sixDp);
            timeStamp.setLayoutParams(dateParams);

            RelativeLayout.LayoutParams containerParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            containerParams.addRule(RelativeLayout.RIGHT_OF,R.id.date_field);
            containerParams.addRule(RelativeLayout.END_OF,R.id.date_field);
            chatText.setLayoutParams(containerParams);

        }
        else {
            chatText.setBackgroundResource(R.drawable.final_chat_blue);
            singleMessageContainer.setGravity(Gravity.LEFT);
            singleMessageContainer.setPadding(twoDp, twoDp, twoDp, twoDp);
            chatText.setTextColor(Color.BLACK);

            RelativeLayout.LayoutParams dateParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dateParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            dateParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            dateParams.addRule(RelativeLayout.CENTER_VERTICAL);
            dateParams.setMargins(sixDp,sixDp,sixDp,sixDp);
            timeStamp.setLayoutParams(dateParams);

            RelativeLayout.LayoutParams containerParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            containerParams.addRule(RelativeLayout.LEFT_OF,R.id.date_field);
            containerParams.addRule(RelativeLayout.START_OF,R.id.date_field);
            chatText.setLayoutParams(containerParams);

        }
        //chatText.setMovementMethod(LinkMovementMethod.getInstance());
        chatText.setAutoLinkMask(Linkify.ALL);
        chatText.setText(Html.fromHtml("<b>" + name + ": " + "</b>" + chatMessageObj.content + ""));

        //displayText.setText(nodes[position].toString());
        //chatText.setText("woo");
        return row;
    }

}