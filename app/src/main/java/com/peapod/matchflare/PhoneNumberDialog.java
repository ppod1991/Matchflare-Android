package com.peapod.matchflare;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;

/*
 * DialogFragment to handle phone number input and sending of verification text message
 */
public class PhoneNumberDialog extends DialogFragment {

    EditText phoneNumberField;
    PhoneNumberDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_phone_number, null);         // Pass null as the parent view because its going in the dialog layout

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);

        //Retrieve dialog components
        phoneNumberField = (EditText) dialogView.findViewById(R.id.phone_number_field);
        TextView instructions = (TextView) dialogView.findViewById(R.id.phone_number_instructions);

        //Stylize the components
        Style.toOpenSans(getActivity(),phoneNumberField,"light");
        Style.toOpenSans(getActivity(),instructions,"light");

        try {  //Try to auto-extract & pre-fill the phone number
            TelephonyManager tMgr = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            String mPhoneNumber = tMgr.getLine1Number();
            Log.e("Phone found:", mPhoneNumber);
            if (mPhoneNumber != null && mPhoneNumber.length() > 0) {
                phoneNumberField.setText(mPhoneNumber);
            }
        }
        catch (Exception e) {
            Log.e("No phone number found:", e.toString());

            //Google analytics
            Tracker t = ((Global) this.getActivity().getApplication()).getTracker();
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("No autofill phone number found" +
                            new StandardExceptionParser(this.getActivity(), null)
                                    .getDescription(Thread.currentThread().getName(), e))
                    .setFatal(false)
                    .build());
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

    //Gets the phone number from the text field
    public String getRawPhoneNumber() {
       return  phoneNumberField.getText().toString();
    }

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
