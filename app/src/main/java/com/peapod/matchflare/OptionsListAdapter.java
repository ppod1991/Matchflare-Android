package com.peapod.matchflare;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.peapod.matchflare.Objects.Person;

/**
 * ArrayAdapter to filter from when displaying searchable list of friends
 */
public class OptionsListAdapter extends ArrayAdapter<Person> {

    public Person[] personList;

    public OptionsListAdapter(Context mContext, int textViewResourceId, Person[] mPersonList) {
        super(mContext, textViewResourceId, R.id.name_list_text_view, mPersonList);
        personList = mPersonList;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View row = super.getView(position, convertView, parent);

        //Retrieve components
        TextView displayText = (TextView) row.findViewById(R.id.name_list_text_view);

        //Style the components
        Style.toOpenSans(getContext(), displayText, "light");
        displayText.setText(displayText.getText());

        return(row);
    }
}
