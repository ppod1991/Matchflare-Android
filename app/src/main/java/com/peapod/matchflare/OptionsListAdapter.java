package com.peapod.matchflare;

import android.content.Context;
import android.graphics.Color;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by piyushpoddar on 12/16/14.
 */
public class OptionsListAdapter extends ArrayAdapter<Person> {

    public Person[] personList;

    public OptionsListAdapter(Context mContext, int textViewResourceId, Person[] mPersonList) {
        super(mContext, textViewResourceId, R.id.name_list_text_view, mPersonList);
        personList = mPersonList;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View row = super.getView(position, convertView, parent);

        TextView displayText = (TextView) row.findViewById(R.id.name_list_text_view);
        Style.toOpenSans(getContext(), displayText, "light");
        displayText.setText(displayText.getText());
        return(row);
    }
}
