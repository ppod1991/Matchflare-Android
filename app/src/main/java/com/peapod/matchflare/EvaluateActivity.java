package com.peapod.matchflare;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class EvaluateActivity extends Activity implements Callback<Match>, View.OnClickListener {

    Match thisMatch;
    RestService ui;

    ImageView matchButton;
    ImageView passButton;
    ImageView chatButton;

    TextView matcherName;
    TextView otherMatcheeName;

    ImageView matcherImageView;
    ImageView otherMatcheeImageView;

    Person thisMatchee;
    Person otherMatchee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evaluate);

        Bundle extras = this.getIntent().getExtras();
        thisMatch = (Match) extras.getSerializable("pair");

        matchButton = (ImageView) findViewById(R.id.evaluate_match_button);
        passButton = (ImageView) findViewById(R.id.evaluate_pass_button);
        chatButton = (ImageView) findViewById(R.id.chat_button);
        matcherImageView = (ImageView) findViewById(R.id.matcher_image_view);
        otherMatcheeImageView = (ImageView) findViewById(R.id.other_matchee_image_view);

        matcherName = (TextView) findViewById(R.id.matcher_name);
        otherMatcheeName = (TextView) findViewById(R.id.other_matchee_name);
        Style.toOpenSans(this,matcherName,"light");
        Style.toOpenSans(this,otherMatcheeName,"light");

        //Determine which person in the match the current user is...
        if (((Global) getApplication()).thisUser.contact_id == thisMatch.first_matchee.contact_id) {
            thisMatchee = thisMatch.first_matchee;
            otherMatchee = thisMatch.second_matchee;
        }
        else if (((Global) getApplication()).thisUser.contact_id == thisMatch.second_matchee.contact_id){
            thisMatchee = thisMatch.second_matchee;
            otherMatchee = thisMatch.first_matchee;
        };

        Picasso.with(this).load(thisMatch.matcher.image_url).fit().centerInside().transform(new CircleTransform()).into(matcherImageView);
        Picasso.with(this).load(otherMatchee.image_url).fit().centerInside().transform(new CircleTransform()).into(otherMatcheeImageView);
        matcherName.setText(thisMatch.matcher.guessed_full_name);
        otherMatcheeName.setText(otherMatchee.guessed_full_name);

        passButton.setOnClickListener(this);
        matchButton.setOnClickListener(this);
        chatButton.setOnClickListener(this);



        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint("https://matchflare.herokuapp.com")
                .build();
        ui = restAdapter.create(RestService.class);

        if (thisMatch == null) {
            int pair_id = extras.getInt("pair_id");
            Map<String,Integer> options = new HashMap<String,Integer>();
            options.put("pair_id",pair_id);
            ui.getMatch(options, this);

        }
        else {
            setMatcheeName(thisMatch);
        }



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.evaluate, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setMatcheeName(Match match) {
//        int myContactID = ((Global) getApplication()).thisUser.contact_id;
//
//        if (match.first_matchee.contact_id == myContactID) {
//            matcherName.setText(match.second_matchee.guessed_full_name);
//        }
//        else if (match.second_matchee.contact_id == myContactID) {
//            matcherName.setText(match.first_matchee.guessed_full_name);
//        }
//        else {
//            matcherName.setText("ERROR :( Try again.");
//        }
//
//        if (match.first_matchee.contact_status.equals("ACCEPT") && match.second_matchee.contact_status.equals("ACCEPT")) {
//            chatButton.setVisibility(View.VISIBLE);
//        }
//        else {
//            matchButton.setVisibility(View.VISIBLE);
//            passButton.setVisibility(View.VISIBLE);
//        }

    }

    @Override
    public void success(Match match, Response response) {

        Log.e("Successfully retrieved the match", response.toString());

        thisMatch = match;
        setMatcheeName(thisMatch);

    }

    @Override
    public void failure(RetrofitError error) {
        Log.e("Failed to retrieve the match", error.toString());

    }

    @Override
    public void onClick(View view) {
        Log.e("Clicked to respond to match!", "woo");
        EvaluateResponse response = new EvaluateResponse();
        response.contact_id = ((Global) getApplication()).thisUser.contact_id;
        response.pair_id = thisMatch.pair_id;

        Intent i = new Intent(this,PresentMatchesActivity.class);


        if (view.getId() == R.id.evaluate_match_button) {
            response.decision = "ACCEPT";
            ui.respondToMatchRequest(response, new RespondCallback());
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }
        else if (view.getId() == R.id.evaluate_pass_button) {
            response.decision = "REJECT";
            ui.respondToMatchRequest(response, new RespondCallback());
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }
        else if (view.getId() == R.id.chat_button) {
            i = new Intent(this, ChatActivity.class);
            i.putExtra("chat_id", thisMatch.chat_id);
        }

        startActivity(i);


    }

    public class RespondCallback implements Callback<StringResponse> {
        @Override
        public void success(StringResponse response, Response response2) {
            Log.e("Successfully posted response to match", response.response);
        }

        @Override
        public void failure(RetrofitError error) {
            Log.e("Failure to post response to match", error.toString());
        }
    }
}
