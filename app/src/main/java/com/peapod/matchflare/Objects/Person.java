package com.peapod.matchflare.Objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;

//Standard Person class for Retrofit
public class Person implements Serializable {

    public String guessed_full_name;
    public String raw_phone_number;
    public int contact_id;
    public String guessed_gender;
    public boolean verified;
    public String image_url;
    public String registration_id;
    public String contact_status;
    public int matcher_chat_id;
    public ArrayList<String> gender_preferences;
    public String access_token;
    public int age;
    public Set<Person> contact_objects;
    public int[] contacts;
    public boolean blocked_matches;

    public Person(String myName, String phoneNumber) {
        guessed_full_name = myName;
        raw_phone_number = phoneNumber;
    };

    public Person() {

    };

    public String toString() {
        if (guessed_full_name.equals("")) {
            return "Anonymous friend";
        }
        else {
            return guessed_full_name;
        }

    }

}