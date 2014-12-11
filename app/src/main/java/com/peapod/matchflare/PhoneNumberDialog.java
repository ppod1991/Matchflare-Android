package com.peapod.matchflare;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;


public class PhoneNumberDialog extends DialogFragment {


    public EditText phoneNumberField;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View dialogView = inflater.inflate(R.layout.dialog_phone_number, null);
        builder.setView(dialogView);

        phoneNumberField = (EditText) dialogView.findViewById(R.id.phone_number_field);
        try {
            TelephonyManager tMgr = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            String mPhoneNumber = tMgr.getLine1Number();
            Log.e("Phone found:", mPhoneNumber);
            if (mPhoneNumber != null && mPhoneNumber.length() > 0) {
                phoneNumberField.setText(mPhoneNumber);
            }
        }
        catch (Exception e) {
            Log.e("No phone number found:", e.toString());
            //No phone number found
        }

        // Add action buttons
        builder.setPositiveButton(R.string.send_sms, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                mListener.onDialogPositiveClick(PhoneNumberDialog.this);
            }
        })
        .setNegativeButton(R.string.cancel_sms, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                PhoneNumberDialog.this.getDialog().cancel();
                mListener.onDialogNegativeClick(PhoneNumberDialog.this);
            }
        });


        return builder.create();
    }

    public interface PhoneNumberDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    public String getRawPhoneNumber() {
       return  phoneNumberField.getText().toString();
    }

    PhoneNumberDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (PhoneNumberDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement PhoneNumberDialogListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle extras) {
        super.onActivityCreated(extras);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
}
