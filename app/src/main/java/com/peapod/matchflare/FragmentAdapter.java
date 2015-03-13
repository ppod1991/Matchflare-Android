package com.peapod.matchflare;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.viewpagerindicator.IconPagerAdapter;

//Handle different pages for the instruction pager
public class FragmentAdapter extends FragmentPagerAdapter implements IconPagerAdapter {

    public FragmentAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getIconResId(int index) {
        return 0;
    }

    @Override
    public Fragment getItem(int position)
    {
        Fragment fragment = new FirstSplashFragment();
        switch(position){
            case 0:
                fragment = new FirstSplashFragment();
                break;
            case 1:
                fragment = new SecondSplashFragment();
                break;
            case 2:
                fragment = new ThirdSplashFragment();
                break;
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position){
        String title = "";
        switch(position){
            case 0:
                title = "First";
                break;
            case 1:
                title = "Second";
                break;
            case 2:
                title = "Third";
                break;
        }
        return title;
    }

}
