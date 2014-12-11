package com.peapod.matchflare;

import java.io.Serializable;

/**
 * Created by piyushpoddar on 10/27/14.
 */
public class Match implements Serializable {

    Person first_matchee;
    Person second_matchee;
    Person matcher;

    String match_status;
    int pair_id;
    int chat_id;
    boolean is_anonymous;
    String created_at;

    public String toString() {
        return "Match of " + first_matchee.guessed_full_name + " and " + second_matchee.guessed_full_name;
    }
}
