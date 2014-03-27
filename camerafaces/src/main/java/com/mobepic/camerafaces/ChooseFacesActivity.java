package com.mobepic.camerafaces;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import com.mobepic.camerafaces.fragments.ChooseFacesFragment;
import com.mobepic.camerafaces.fragments.LocalGalleryFragment;

import com.astuetz.PagerSlidingTabStrip;

/**
 * Activity to start with.
 */
public class ChooseFacesActivity extends FragmentActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_choose_faces);// Initialize the ViewPager and set an adapter

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new TabsAdapter(getSupportFragmentManager()));

        // Bind the tabs to the ViewPager
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setShouldExpand(true);
        tabs.setMinimumHeight(28);
        tabs.setIndicatorHeight(14);
        tabs.setIndicatorColor(getResources().getColor(R.color.accent));
        tabs.setViewPager(pager);
    }
    class TabsAdapter extends FragmentPagerAdapter {
        private CharSequence[] tabs = {
                "Faces",
                "Photos"
        };

        TabsAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public CharSequence getPageTitle(int i) {
            return tabs[i];
        }

        @Override
        public Fragment getItem(int i) {
            if (i==0) {
                return new ChooseFacesFragment();
            } else {
                return new LocalGalleryFragment();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}