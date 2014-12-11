package com.peapod.matchflare;

import java.util.ArrayList;

/**
 * Created by piyushpoddar on 11/26/14.
 */
public class ChatMessage {

    public String content;
    public int sender_contact_id;
    public String guessed_full_name;
    public int chat_id;
    public String type;
    public ArrayList<ChatMessage> history;

    public ChatMessage(String myContent) {
        content = myContent;
    }

    public ChatMessage() {
    }

    public String toString() {
        return content;
    }
}
