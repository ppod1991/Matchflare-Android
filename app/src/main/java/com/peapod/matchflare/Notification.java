package com.peapod.matchflare;

import android.content.Context;
import android.content.Intent;

import java.io.Serializable;

/**
 * Created by piyushpoddar on 11/24/14.
 */
public class Notification implements Serializable{

    public int notification_id;
    public String push_message;
    public String notification_type;
    public int pair_id;
    public int chat_id;
    public boolean seen;
    public int target_contact_id;

    public String toString() {
        return push_message;
    }

    public static Intent makeIntent(Context c, Notification n) {
        if (n.notification_type.equals("MATCHEE_NEW_MATCH"))
         {
            Intent intent = new Intent(c, EvaluateActivity.class);
            intent.putExtra("pair_id",n.pair_id);
            return intent;
        }
        else if (n.notification_type.equals("MATCHEE_MATCH_ACCEPTED") ||
                n.notification_type.equals("MATCHER_QUESTION_ASKED") ||
                n.notification_type.equals("MATCHEE_QUESTION_ANSWERED") ||
                n.notification_type.equals("MATCHEE_MESSAGE_SENT")) {
            Intent intent = new Intent(c, ChatActivity.class);
            intent.putExtra("chat_id",n.chat_id);
            intent.putExtra("pair_id", n.pair_id);
            return intent;
        }
        else if (n.notification_type.equals("MATCHER_ONE_MATCH_ACCEPTED") ||
                n.notification_type.equals("MATCHER_BOTH_ACCEPTED")){
            Intent intent = new Intent(c, ViewMatchActivity.class);
            intent.putExtra("pair_id",n.pair_id);
            return intent;
        }
        return null;
    }
}
