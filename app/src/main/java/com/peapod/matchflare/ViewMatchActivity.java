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
import retrofit.RetrofitError;
import retrofit.client.Response;


public class ViewMatchActivity extends Activity implements Callback<Match>, View.OnClickListener {

    Match thisMatch;
    RestService ui = ((Global) getApplication()).ui;

    ImageView firstImage;
    ImageView secondImage;

    ImageView firstChatButton;
    ImageView secondChatButton;

    TextView viewStatusText;
    TextView firstMatcheeName;
    TextView secondMatcheeName;

    View progressIndicator;
    View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_match);

        progressIndicator = findViewById(R.id.progress_indicator);
        root = findViewById(R.id.root_match_view);

        progressIndicator.setVisibility(View.VISIBLE);
        root.setVisibility(View.GONE);

        Bundle extras = this.getIntent().getExtras();
        thisMatch = (Match) extras.getSerializable("pair");

        firstImage = (ImageView) findViewById(R.id.first_matchee_image_view);
        secondImage = (ImageView) findViewById(R.id.second_matchee_image_view);

        firstMatcheeName = (TextView) findViewById(R.id.first_contact_textview);
        secondMatcheeName = (TextView) findViewById(R.id.second_contact_textview);
        viewStatusText = (TextView) findViewById(R.id.view_status_text);

        Style.toOpenSans(this,firstMatcheeName,"light");
        Style.toOpenSans(this,secondMatcheeName,"light");
        Style.toOpenSans(this,viewStatusText,"light");

        firstChatButton = (ImageView) findViewById(R.id.first_chat_button);
        secondChatButton = (ImageView) findViewById(R.id.second_chat_button);

        firstChatButton.setOnClickListener(this);
        secondChatButton.setOnClickListener(this);

        if (thisMatch == null) {
            int pair_id = extras.getInt("pair_id");
            Map<String,Integer> options = new HashMap<String,Integer>();
            options.put("pair_id",pair_id);
            ui.getMatch(options, this);
        }
        else {
            setMatcheeName();
            progressIndicator.setVisibility(View.GONE);
            root.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.first_chat_button) {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("chat_id", thisMatch.first_matchee.matcher_chat_id);
            i.putExtra("pair_id", thisMatch.pair_id);
            startActivity(i);
        }
        else if (view.getId() == R.id.second_chat_button) {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("chat_id", thisMatch.second_matchee.matcher_chat_id);
            i.putExtra("pair_id", thisMatch.pair_id);
            startActivity(i);
        }
    }

    public void setMatcheeName() {

        Picasso.with(this).load(thisMatch.first_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(firstImage);
        Picasso.with(this).load(thisMatch.second_matchee.image_url).fit().centerInside().transform(new CircleTransform()).into(secondImage);
        firstMatcheeName.setText(thisMatch.first_matchee.guessed_full_name);
        secondMatcheeName.setText(thisMatch.second_matchee.guessed_full_name);

        Map<String, Integer> options = new HashMap<String, Integer>();
        options.put("contact_id",((Global) getApplication()).thisUser.contact_id);
        options.put("chat_id",thisMatch.first_matchee.matcher_chat_id);
        ((Global) getApplication()).ui.hasUnread(options, new Callback<Boolean> () {

            @Override
            public void success(Boolean aBoolean, Response response) {
                if (aBoolean != null && aBoolean.booleanValue() == true) {
                    firstChatButton.setImageDrawable(ViewMatchActivity.this.getResources().getDrawable(R.drawable.new_message_chat_button));
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("Failed to check if chat has unread messages",error.toString());
            }
        });

        Map<String, Integer> options2 = new HashMap<String, Integer>();
        options2.put("contact_id",((Global) getApplication()).thisUser.contact_id);
        options2.put("chat_id",thisMatch.second_matchee.matcher_chat_id);
        ((Global) getApplication()).ui.hasUnread(options2, new Callback<Boolean> () {

            @Override
            public void success(Boolean aBoolean, Response response) {
                if (aBoolean != null && aBoolean.booleanValue() == true) {
                    secondChatButton.setImageDrawable(ViewMatchActivity.this.getResources().getDrawable(R.drawable.new_message_chat_button));
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("Failed to check if chat has unread messages",error.toString());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_view_match, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void success(Match match, Response response) {
        setMatcheeName();
        progressIndicator.setVisibility(View.GONE);
        root.setVisibility(View.VISIBLE);
    }

    @Override
    public void failure(RetrofitError error) {
        Style.makeToast(this, "Failed to get your match. Try again later!");
        Log.e("Failed to retrieve match for viewing", error.toString());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        Intent i = new Intent(this,SplashActivity.class);
        startActivity(i);
        finish();
    }
}
