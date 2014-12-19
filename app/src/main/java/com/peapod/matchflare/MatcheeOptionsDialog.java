package com.peapod.matchflare;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

/**
 * Created by piyushpoddar on 12/15/14.
 */
public class MatcheeOptionsDialog extends DialogFragment {

    ArrayAdapter<Person> listAdapter;
    boolean isFirstMatchee;
    Person chosenMatchee;

    public static MatcheeOptionsDialog newInstance(boolean isFirst, Person mChosenMatchee) {
        MatcheeOptionsDialog thisDialog = new MatcheeOptionsDialog();
        thisDialog.isFirstMatchee = isFirst;
        thisDialog.chosenMatchee = mChosenMatchee;
        return thisDialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View optionsView = inflater.inflate(R.layout.dialog_matchee_options, null);
        builder.setView(optionsView);

        final ListView optionsList = (ListView) optionsView.findViewById(R.id.matchee_options_list);
        EditText searchBox = (EditText) optionsView.findViewById(R.id.contact_search_bar);
        TextView orText = (TextView) optionsView.findViewById(R.id.or_text_view);
        Button stopShowingContactButton = (Button) optionsView.findViewById(R.id.stop_showing_button);
        Style.toOpenSans(getActivity(),orText,"light");
        Style.toOpenSans(getActivity(),stopShowingContactButton,"light");
        Style.toOpenSans(getActivity(),searchBox,"light");
        stopShowingContactButton.setText("Stop Showing " + chosenMatchee.guessed_full_name);
        stopShowingContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onDialogRemoveMatchee(chosenMatchee, MatcheeOptionsDialog.this);
                MatcheeOptionsDialog.this.getDialog().cancel();
            }
        });
        Set<Person> contacts = ((Global) getActivity().getApplication()).thisUser.contact_objects;
        listAdapter = new OptionsListAdapter(getActivity(), R.layout.options_list_item,contacts.toArray(new Person[contacts.size()]));

        optionsList.setAdapter(listAdapter);

        searchBox.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                // When user changed the Text
                listAdapter.getFilter().filter(cs);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                          int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
            }
        });

        optionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Person selectedPerson = (Person) optionsList.getItemAtPosition(position);
                mListener.onDialogSetMatchee(selectedPerson, MatcheeOptionsDialog.this);
                MatcheeOptionsDialog.this.getDialog().cancel();
            }
        });

        // Add action buttons
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                MatcheeOptionsDialog.this.getDialog().cancel();
                mListener.onDialogNegativeClick(MatcheeOptionsDialog.this);
            }
        });

        return builder.create();
    }

    public interface MatcheeOptionsDialogListener {
        public void onDialogNegativeClick(DialogFragment dialog);
        public void onDialogSetMatchee(Person selectedPerson, MatcheeOptionsDialog dialog);
        public void onDialogRemoveMatchee(Person selectedPerson, MatcheeOptionsDialog dialog);
    }

    MatcheeOptionsDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (MatcheeOptionsDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement MatcheeOptionsDialogListener");
        }
    }


}


