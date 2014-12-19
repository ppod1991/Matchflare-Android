package com.peapod.matchflare;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

/**
 * Created by piyushpoddar on 12/17/14.
 */
public class MatcherOptionsDialog extends DialogFragment {

    Person matcher;
    Boolean isAnonymous;

    public static MatcherOptionsDialog newInstance(boolean mIsAnonymous, Person mMatcher) {
        MatcherOptionsDialog thisDialog = new MatcherOptionsDialog();
        thisDialog.matcher = mMatcher;
        thisDialog.isAnonymous = mIsAnonymous;
        return thisDialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View optionsView = inflater.inflate(R.layout.dialog_matcher_options, null);
        builder.setView(optionsView);

        Button blockMatcherButton = (Button) optionsView.findViewById(R.id.block_matcher_button);
        Button chatWithMatcherButton = (Button) optionsView.findViewById(R.id.chat_with_matcher_button);

        Style.toOpenSans(getActivity(),blockMatcherButton,"light");
        Style.toOpenSans(getActivity(),chatWithMatcherButton,"light");
        if (isAnonymous) {
            blockMatcherButton.setText("Block this Matcher");
            chatWithMatcherButton.setText("Ask this matcher a question!");
        }
        else {
            blockMatcherButton.setText("Block " + matcher.guessed_full_name);
            chatWithMatcherButton.setText("Ask " + matcher.guessed_full_name + " a question!");
        }

        blockMatcherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onDialogBlockMatcher(MatcherOptionsDialog.this);
                MatcherOptionsDialog.this.getDialog().cancel();
            }
        });

        chatWithMatcherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onDialogChatWithMatcher(MatcherOptionsDialog.this);
                MatcherOptionsDialog.this.getDialog().cancel();
            }
        });

        // Add action buttons
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                MatcherOptionsDialog.this.getDialog().cancel();
                mListener.onDialogNegativeClick(MatcherOptionsDialog.this);
            }
        });

        return builder.create();
    }

    public interface MatcherOptionsDialogListener {
        public void onDialogNegativeClick(DialogFragment dialog);
        public void onDialogChatWithMatcher(MatcherOptionsDialog dialog);
        public void onDialogBlockMatcher(MatcherOptionsDialog dialog);
    }

    MatcherOptionsDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (MatcherOptionsDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement MatcherOptionsDialogListener");
        }
    }


}

