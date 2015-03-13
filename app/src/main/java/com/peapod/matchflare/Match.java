package com.peapod.matchflare;

import com.peapod.matchflare.Objects.Person;

import java.io.Serializable;


/*
 * Match object for use with Retrofit
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
    Boolean has_unseen;
    boolean wasEdited = false;  //Keeps track as to whether the user edited the current match. If edited, then the user cannot rate either matchee

    public String toString() {
        return first_matchee.guessed_full_name + " and " + second_matchee.guessed_full_name;
    }
}
