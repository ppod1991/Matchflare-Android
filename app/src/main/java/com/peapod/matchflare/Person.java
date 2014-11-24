package com.peapod.matchflare;

/**
 * Created by piyushpoddar on 10/23/14.
 */
public class Person {

    public String guessed_full_name;
    public String raw_phone_number;
    public int contact_id;
    public String gender;
    public boolean isVerified;
    public String profile_picture_url;
    public String registration_id;

    public Person(String myName, String phoneNumber) {
        guessed_full_name = myName;
        raw_phone_number = phoneNumber;
    };

    public Person() {

    };



}