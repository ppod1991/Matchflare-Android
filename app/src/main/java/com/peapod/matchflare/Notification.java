package com.peapod.matchflare;

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

}
