package com.peapod.matchflare.Objects;

import com.peapod.matchflare.Match;
import com.peapod.matchflare.Objects.Notification;

import java.io.Serializable;
import java.util.List;

//Standard object container for different notification types
public class NotificationLists implements Serializable {

    public List<Notification> notifications;
    public List<Match> pending_matches;
    public List<Match> active_matcher_matches;
}
