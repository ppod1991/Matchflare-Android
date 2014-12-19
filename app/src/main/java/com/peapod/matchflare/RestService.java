package com.peapod.matchflare;

import android.support.annotation.StringRes;

import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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
        SplashActivity.MatchesAndPairs processContacts(@Body Contacts contacts, @QueryMap Map<String, Integer> options);

        @POST("/getMatches")
        void getMatches(@Body Person person, @QueryMap Map<String, Integer> options, Callback<Queue<Match>> cb);

        @GET("/match")
        void getMatch(@QueryMap Map<String, Integer> options, Callback<Match> cb);

        @POST("/postMatch")
        void addMatch(@Body Match match, @QueryMap Map<String, Integer> options, Callback<Integer> cb);

        @POST("/gcm/registrationId")
        void updateRegistrationId(@Body Person person, Callback<StringResponse> cb);

        @GET("/notifications")
        void getNotifications(@QueryMap Map<String, Integer> options, Callback<Notifications> cb);

        @GET("/notificationLists")
        void getNotificationLists(@QueryMap Map<String, Integer> options, Callback<NotificationLists> cb);

        @POST("/seeNotification")
        void seeNotification(@QueryMap Map<String, Integer> options, Callback<StringResponse> cb);

        @GET("/pendingMatches")
        void getPendingMatches(@QueryMap Map<String, Integer> options, Callback<ArrayList<Match>> cb);

        @POST("/match/respond")
        void respondToMatchRequest(@Body EvaluateResponse response, Callback<StringResponse> cb);

        @POST("/sendSMSVerification")
        void sendSMSVerification(@QueryMap Map<String, String> options, Callback<StringResponse> cb);

        @POST("/verifyVerificationSMS")
        void verifyVerificationSMS(@Body Person person, @QueryMap Map<String, String> options, Callback<Person> cb);

        @GET("/verifyAccessToken")
        void verifyAccessToken(@QueryMap Map<String, String> options, Callback<Person> cb);

        @GET("/pictureURL")
        void getPicture(@QueryMap Map<String, String> options, Callback<StringResponse> cb);

        @GET("/getScore")
        void getScore(@QueryMap Map<String, Integer> options, Callback<Integer> cb);

        @GET("/hasUnread")
        void hasUnread(@QueryMap Map<String, Integer> options, Callback<Boolean> cb);

        @POST("/removeContact")
        void removeContact(@QueryMap Map<String, Integer> options, Callback<StringResponse> cb);

        @POST("/blockContact")
        void blockContact(@QueryMap Map<String, Integer> options, Callback<StringResponse> cb);

        @POST("/updateProfile")
        void updateProfile(@Body Person this_user, Callback<Person> cb);

}
