package com.peapod.matchflare;

import java.io.Serializable;
import java.util.List;

/**
 * Created by piyushpoddar on 12/1/14.
 */
public class NotificationLists implements Serializable {

    public List<Notification> notifications;
    public List<Match> pending_matches;
    public List<Match> active_matcher_matches;
}
