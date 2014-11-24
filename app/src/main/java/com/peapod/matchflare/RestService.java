package com.peapod.matchflare;

import android.support.annotation.StringRes;

import java.util.ArrayList;
import java.util.Map;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.QueryMap;


/**
 * Created by piyushpoddar on 10/24/14.
 */
public interface RestService {

        @POST("/processContacts")
        void processContacts(@Body Contacts contacts, @QueryMap Map<String, Integer> options, Callback<StringResponse> cb);

        @GET("/getMatches")
        void getMatches(@QueryMap Map<String, Integer> options, Callback<Matches> cb);

        @POST("/postMatch")
        void addMatch(@Body Match match, @QueryMap Map<String, Integer> options, Callback<StringResponse> cb);

        @POST("/gcm/registrationId")
        void updateRegistrationId(@Body Person person, Callback<StringResponse> cb);

}
