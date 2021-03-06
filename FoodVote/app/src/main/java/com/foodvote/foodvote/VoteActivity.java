package com.foodvote.foodvote;


import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import android.content.Intent;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;
import com.foodvote.foodvote.R;

import com.foodvote.model.Place;
import com.foodvote.model.PlaceManager;
import com.foodvote.model.PlaceParser;
import com.foodvote.model.Round;
import com.foodvote.model.User;
import com.foodvote.socket.SocketIO;
import com.foodvote.yelp.YelpAPI;
import com.gc.materialdesign.views.Slider;

import java.util.ArrayList;
import java.util.List;

public class VoteActivity extends ActionBarActivity {

    //FragmentPagerAdapter adapterViewPager;

    PlaceManager pm;
    SocketIO socket;

    Slider slider;

    int roundNum;

    TextView cardAName;
    TextView cardADesc;
    TextView cardBName;
    TextView cardBDesc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote);

        slider = (Slider) findViewById(R.id.slider);
        slider.setValue(50);

        cardAName = (TextView) findViewById(R.id.cardAName);
        cardADesc = (TextView) findViewById(R.id.cardADesc);
        cardBName = (TextView) findViewById(R.id.cardBName);
        cardBDesc = (TextView) findViewById(R.id.cardBDesc);

        //ViewPager for left and right cards
//        ViewPager vpPager = (ViewPager) findViewById(R.id.vpPager);
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        adapterViewPager = new VotePagerAdapter(fragmentManager);
//        vpPager.setAdapter(adapterViewPager);

        //Tabs
//        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
//        tabs.setViewPager(vpPager);


        pm = PlaceManager.getInstance();

        // socket init
        socket = SocketIO.getInstance();

        // listeners
        socket.onNewRound(this, new SocketIO.OnNewRoundListener() {
            @Override
            public void onNewRound(int i, Round round) {
                roundNum = i;
                getPlaces(round);
            }
        });

        socket.onUserVoted(this, new SocketIO.OnUserVotedListener() {
            @Override
            public void onUserVoted(User user, Round round) {
                Log.d("User Voted", user.getName());
            }
        });

        socket.onRoundEnded(this, new SocketIO.OnRoundEndedListener() {
            @Override
            public void onRoundEnded(int i, Round round) {
               return;
            }
        });

        socket.onVotingEnd(this, new SocketIO.OnVotingEndListener() {
            @Override
            public void onVotingEnd(List<Round> rounds, String result) {
                startWinnerActivity(rounds, result);
            }
        });
    }

    private void getPlaces(Round round) {
        String idA = round.getPlaceA();
        String idB = round.getPlaceB();
        YelpAPI yelp = new YelpAPI();
        String jsonA = yelp.searchByBusinessId(idA);
        String jsonB = yelp.searchByBusinessId(idB);
        PlaceParser pp = new PlaceParser();
        pp.parse(jsonA);
        Place A = pm.findPlaceById(idA);
        Place B = pm.findPlaceById(idB);
        cardAName.setText(A.getName());
        cardADesc.setText(makeDescText(A));
        cardBName.setText(B.getName());
        cardBDesc.setText(makeDescText(B));
    }

    private void setRound(Round round) {
        Place A = pm.findPlaceById(round.getPlaceA());
        Place B = pm.findPlaceById(round.getPlaceB());

        cardAName.setText(A.getName());
        cardADesc.setText(makeDescText(A));
        cardBName.setText(B.getName());
        cardBDesc.setText(makeDescText(B));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_vote, menu);
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
        if (id == R.id.action_vote) {
            Log.d("Slider", Integer.toString(slider.getValue()));
            socket.registerVote(roundNum, slider.getValue());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



//    public static class VotePagerAdapter extends FragmentPagerAdapter {
//        private static int NUM_ITEMS = 2;
//        private VoteCardFragment cardA;
//        private VoteCardFragment cardB;
//
//        public VotePagerAdapter(FragmentManager fm) {
//            super(fm);
//            cardA = VoteCardFragment.newInstance();
//            cardB = VoteCardFragment.newInstance();
//        }
//
//        @Override
//        public int getCount() {
//            return NUM_ITEMS;
//        }
//
//        @Override
//        public Fragment getItem(int position) {
//            switch (position) {
//                case 0:
//                    return cardA;
//                case 1:
//                    return cardB;
//                default:
//                    return new Fragment();
//            }
//        }
//
//        @Override
//        public CharSequence getPageTitle(int position) {
//            switch (position) {
//                case 0:
//                    return "Place A";
//                case 1:
//                    return "Place B";
//                default:
//                    return "";
//            }
//        }
//
//        public void setPlaceCardA(Place place) {
//            cardA.setPlace(place);
//        }
//
//        public void setPlaceCardB(Place place) {
//            cardB.setPlace(place);
//        }
//    }


    private void startWinnerActivity(List<Round> rounds, String result) {
        Intent intent = new Intent(this, WinnerActivity.class);

        intent.putExtra("result", result);
        intent.putParcelableArrayListExtra("rounds", (ArrayList<Round>) rounds);

        startActivity(intent);
    }

    private String makeDescText(Place p) {
        String descText = "";
        for (String str : p.getDisplayAddress()) {
            descText = descText + str + ", ";
        }
        if (!p.getDisplayPhone().equals("")) {
            descText = descText + System.getProperty("line.separator") + p.getDisplayPhone();
        }
        if (p.getRating() != 0) {
            Double rating = p.getRating() / 2.0;
            String strRating = String.valueOf(rating);
            descText = descText + System.getProperty("line.separator") + strRating + " out of 5 stars";
        }
        return descText;
    }
}
