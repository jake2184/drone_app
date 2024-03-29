package jake.imperial.drone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.loader.StreamLoader;

import java.net.CookieHandler;
import java.net.CookieManager;

import jake.imperial.drone.fragments.ConnectionFragment;
import jake.imperial.drone.fragments.ControlFragment;
import jake.imperial.drone.fragments.GraphFragment;
import jake.imperial.drone.fragments.LogFragment;
import jake.imperial.drone.fragments.MapFragment;
import jake.imperial.drone.fragments.VideoFragment;
import jake.imperial.drone.utils.Constants;
import jake.imperial.drone.utils.MqttHandler;
import jake.imperial.drone.utils.TopicFactory;

public class MainActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;
    private String[] mPageTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private DroneApplication app;

    public String session;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MqttHandler.getInstance(getApplicationContext()).unsubscribe(TopicFactory.getEventTopic("+", "+", "+"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        app = (DroneApplication) getApplication();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mPageTitles = getResources().getStringArray(R.array.page_titles);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {

                app.setCurrentRunningActivity(mSectionsPagerAdapter.getPageFragmentName(position));
            }

        });
        app.setCurrentRunningActivity(mSectionsPagerAdapter.getPageFragmentName(0));
        Log.d("SETUP", app.getCurrentRunningActivity());

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Change current drone
                final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setTitle("Current Drone:")
                        .create();


                final Spinner v = new Spinner(getApplicationContext());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_spinner_dropdown_item, app.getDroneNames());
                v.setAdapter(adapter);

                v.setSelection(adapter.getPosition(app.getCurrentDrone()));
                v.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        ((TextView) parent.getChildAt(0)).setTextColor(Color.BLACK);
                        app.setCurrentDrone(v.getSelectedItem().toString());
                        Intent intent = new Intent(Constants.APP_ID + "." + Constants.INTENT_DRONE_CHANGE);
                        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                dialog.setView(v);
                dialog.show();

            }
        });


        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_item_single, mPageTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,R.string.drawer_open, R.string.drawer_close){

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        MqttHandler.getInstance(getApplicationContext()).unsubscribe(TopicFactory.getEventTopic("pi", "drone", "+"));

        CookieHandler.setDefault(Ion.getDefault(getApplicationContext()).getCookieMiddleware().getCookieManager());

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        // If the nav drawer is open, hide action items related to the content view
//        //boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
//        //menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
//        return super.onPrepareOptionsMenu(menu);
//    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id){
            // Change page
            mViewPager.setCurrentItem(position);
            mDrawerList.setItemChecked(position, true);
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if(mDrawerToggle.onOptionsItemSelected(item)){
            return true;
        }

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setPagerView(int num){
        mViewPager.setCurrentItem(num);
    }

    /*
    private void showAlert(String message){
        new AlertDialog.Builder(this)
                .setTitle("Alert:")
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {}
                }).show();

    }
    */





    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // Order should match that defined in string-array page_titles
            switch(position){
                case 0:
                    return ConnectionFragment.newInstance();
                case 1:
                    return VideoFragment.newInstance();
                case 2:
                    return ControlFragment.newInstance();
                case 3:
                    return LogFragment.newInstance();
                case 4:
                    return MapFragment.newInstance();
                case 5:
                    return GraphFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return mPageTitles.length;
            //return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mPageTitles[position];
//            switch (position) {
//                case 0:
//                    return "Connection";
//                case 1:
//                    return "Video Stream";
//                case 2:
//                    return "Control";
//                case 3:
//                    return "Log";
//                case 4:
//                    return "Map";
//            }
//            return null;
        }

        public String getPageFragmentName(int position){
            switch(position){
                case 0:
                    return ConnectionFragment.class.getName();
                case 1:
                    return VideoFragment.class.getName();
                case 2:
                    return ControlFragment.class.getName();
                case 3:
                    return LogFragment.class.getName();
                case 4:
                    return MapFragment.class.getName();
                case 5:
                    return GraphFragment.class.getName();


            }
            return null;
        }

    }
}
