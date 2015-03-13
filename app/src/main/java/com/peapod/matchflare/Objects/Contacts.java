package com.peapod.matchflare.Objects;

import java.util.Set;

//Standard Contacts object for Retrofit. Stores a set of contacts.
public class Contacts {

   Set<Person> contacts;

   public Contacts(Set<Person> myContacts) {
       contacts = myContacts;
   }
}
