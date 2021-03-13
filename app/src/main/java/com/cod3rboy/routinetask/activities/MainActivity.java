package com.cod3rboy.routinetask.activities;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.view.ActionMode;
import androidx.core.view.GravityCompat;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.view.MenuItem;
import android.view.View;

import com.cod3rboy.routinetask.BuildConfig;
import com.cod3rboy.routinetask.R;
import com.cod3rboy.routinetask.fragments.AboutFragment;
import com.cod3rboy.routinetask.fragments.PomodoroFragment;
import com.cod3rboy.routinetask.fragments.PomodoroStatsFragment;
import com.cod3rboy.routinetask.fragments.TaskListFragment;
import com.cod3rboy.routinetask.fragments.TaskStatsFragment;
import com.cod3rboy.routinetask.logging.Logger;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    public static final String KEY_NAV_SELECTED_ITEM = "nav_selected_item";

    public static final String KEY_FRAGMENT_TYPE = "fragment_type";
    public static final int FRAGMENT_TODAY = 100;
    public static final int FRAGMENT_ROUTINE = 101;
    public static final int FRAGMENT_ONE_TIME = 102;
    public static final int FRAGMENT_POMODORO = 103;
    public static final int FRAGMENT_STATISTICS = 104;
    public static final int FRAGMENT_ABOUT = 105;

    /**
     * Used to store the last screen title. For use in {@link #refreshActionBar()} ()}.
     */
    private CharSequence mTitle;

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private ActionBarDrawerToggle mToggle;

    private int mSelectedDrawerItem;
    private BottomNavigationView mStatsBottomNav;

    private MaterialToolbar mToolbar;

    // Current Fragment being displayed in the Main Activity
    private Fragment mCurrentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        boolean batteryOptimisationConfirmed = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.pref_battery_optimisation), false);
        if (!batteryOptimisationConfirmed) {
            // Launch Battery Optimisation Activity for first app launch
            Intent intent = new Intent(this, BatteryOptimizeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

        setContentView(R.layout.activity_main);

        mToolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.bringToFront();
        mToggle = new ActionBarDrawerToggle(this,
                mDrawerLayout,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        mDrawerLayout.addDrawerListener(mToggle);
        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                if (mCurrentFragment != null) {
                    // Show fragment selected by user in navigation drawer
                    showCurrentFragment();
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
        mToolbar.setNavigationOnClickListener(v -> {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START))
                mDrawerLayout.closeDrawer(GravityCompat.START);
            else
                mDrawerLayout.openDrawer(GravityCompat.START);
        });
        mNavigationView.setNavigationItemSelectedListener(this);

        mStatsBottomNav = findViewById(R.id.stats_bottom_nav);
        setStatsBottomNavVisible(false);
        mStatsBottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.stats_bottom_nav_task:
                        Logger.d(LOG_TAG, "Selected Tasks Statistics from Stats Bottom Nav");
                        setCurrentFragment(TaskStatsFragment.getInstance(FRAGMENT_STATISTICS));
                        showCurrentFragment();
                        break;
                    case R.id.stats_bottom_nav_pomodoro:
                        Logger.d(LOG_TAG, "Selected Pomodoro Statistics from Stats Bottom Nav");
                        setCurrentFragment(PomodoroStatsFragment.getInstance(FRAGMENT_STATISTICS));
                        showCurrentFragment();
                        break;
                }
                return true;
            }
        });
        mStatsBottomNav.setOnNavigationItemReselectedListener(new BottomNavigationView.OnNavigationItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem item) {
            }
        });

        // Find initial drawer item to select
        mSelectedDrawerItem = R.id.nav_item_whats_today;
        if (savedInstanceState != null) {
            mSelectedDrawerItem = savedInstanceState.getInt(KEY_NAV_SELECTED_ITEM, R.id.nav_item_whats_today);
            setStatsBottomNavVisible(mSelectedDrawerItem == R.id.nav_item_my_progress);
        } else {
            // Activity is started for first time
            Intent i = getIntent();
            if (i != null) {
                mSelectedDrawerItem = i.getIntExtra(KEY_NAV_SELECTED_ITEM, R.id.nav_item_whats_today);
                setCurrentFragment(mSelectedDrawerItem);
                showCurrentFragment();
            }
        }
        mNavigationView.setCheckedItem(mSelectedDrawerItem);

        if (!BuildConfig.DEBUG) mNavigationView.getMenu().removeItem(R.id.nav_item_crash);

    }

    public MaterialToolbar getSupportToolbar() {
        return mToolbar;
    }


    public void setStatsBottomNavVisible(boolean visible) {
        if (visible) mStatsBottomNav.setVisibility(View.VISIBLE);
        else mStatsBottomNav.setVisibility(View.GONE);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mToggle.syncState();
    }

    public void onSectionAttached(int fragmentType) {
        switch (fragmentType) {
            case FRAGMENT_TODAY:
                mTitle = getString(R.string.drawer_item_today);
                break;
            case FRAGMENT_ROUTINE:
                mTitle = getString(R.string.drawer_item_routine_tasks);
                break;
            case FRAGMENT_ONE_TIME:
                mTitle = getString(R.string.drawer_item_onetime_tasks);
                break;
            case FRAGMENT_POMODORO:
                mTitle = getString(R.string.drawer_item_pomodoro);
                break;
            case FRAGMENT_STATISTICS:
                mTitle = getString(R.string.drawer_item_progress);
                break;
            case FRAGMENT_ABOUT:
                mTitle = getString(R.string.drawer_item_about);
                break;
            //@todo Add a settings fragment case too
        }
    }


    public void refreshActionBar(String subtitle) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mTitle);
        if (subtitle != null) actionBar.setSubtitle(subtitle);
        else actionBar.setSubtitle("");
    }

    public void refreshActionBar() {
        refreshActionBar(null);
    }

    @Override
    public void onSupportActionModeStarted(@NonNull ActionMode mode) {
        super.onSupportActionModeStarted(mode);
        Logger.d(LOG_TAG, "Action mode started");
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START);
    }

    @Override
    public void onSupportActionModeFinished(@NonNull ActionMode mode) {
        super.onSupportActionModeFinished(mode);
        Logger.d(LOG_TAG, "Action mode Finished");
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START);
    }


    private void startShareIntent() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_share_text));
        intent.setType("text/plain");
        startActivity(intent);
    }

    private void showAppInPlayStore(final String appId) {
        Intent rateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("market://details?id=%s", appId)));
        boolean marketFound = false;

        // Find all the applications able to handle our rateIntent
        final List<ResolveInfo> otherApps = getPackageManager().queryIntentActivities(rateIntent, 0);
        for (ResolveInfo otherApp : otherApps) {
            // Look for Google Play Application
            if (otherApp.activityInfo.packageName.contentEquals("com.android.vending")) {
                ActivityInfo otherAppActivity = otherApp.activityInfo;
                ComponentName componentName = new ComponentName(otherAppActivity.applicationInfo.packageName, otherAppActivity.name);
                // Make sure it does NOT open in the stack of my own app activity
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // Task reparenting if needed
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                // If Google Play was already open in a search result this make sure it still
                // go to this app page
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                // This make sure only the Google Play app is allowed to intercept the intent
                rateIntent.setComponent(componentName);
                startActivity(rateIntent);
                marketFound = true;
                break;
            }
        }

        // If Google Play app not present in device, open web browser
        if (!marketFound) {
            startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(String.format("https://play.google.com/store/apps/details?id=%s", appId))));
        }
    }

    private void sendEmailToDeveloper() {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:cod3rboy@hotmail.com"));
        startActivity(intent);
    }

    private void setCurrentFragment(@NonNull Fragment fragment) {
        mCurrentFragment = fragment;
    }

    private void setCurrentFragment(int selectedItemId) {
        switch (selectedItemId) {
            case R.id.nav_item_whats_today:
                Logger.d(LOG_TAG, "Selected Today Task item from Navigation Drawer");
                setCurrentFragment(TaskListFragment.getInstance(FRAGMENT_TODAY));
                break;
            case R.id.nav_item_routine_tasks:
                Logger.d(LOG_TAG, "Selected Routine Tasks item from Navigation Drawer");
                setCurrentFragment(TaskListFragment.getInstance(FRAGMENT_ROUTINE));
                break;
            case R.id.nav_item_onetime_tasks:
                Logger.d(LOG_TAG, "Selected One Time Tasks item from Navigation Drawer");
                setCurrentFragment(mCurrentFragment = TaskListFragment.getInstance(FRAGMENT_ONE_TIME));
                break;
            case R.id.nav_item_pomodoro:
                Logger.d(LOG_TAG, "Selected Pomodoro item from Navigation Drawer");
                setCurrentFragment(mCurrentFragment = PomodoroFragment.getInstance(FRAGMENT_POMODORO));
                break;
            case R.id.nav_item_my_progress:
                Logger.d(LOG_TAG, "Selected My Progress item from Navigation Drawer");
                if (mStatsBottomNav.getSelectedItemId() != R.id.stats_bottom_nav_task) {
                    mStatsBottomNav.setSelectedItemId(R.id.stats_bottom_nav_task);
                } else {
                    Logger.d(LOG_TAG, "Selected Tasks Statistics from Stats Bottom Nav");
                    setCurrentFragment(TaskStatsFragment.getInstance(FRAGMENT_STATISTICS));
                }
                break;
            case R.id.nav_item_about:
                Logger.d(LOG_TAG, "Selected About item from Navigation Drawer");
                setCurrentFragment(AboutFragment.getInstance(FRAGMENT_ABOUT));
                break;
            case R.id.nav_item_preferences:
                Logger.d(LOG_TAG, "Selected My Preferences item from Navigation Drawer");
                // Start Settings Activity
                Intent settingsActivityIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsActivityIntent);
                break;
            case R.id.nav_item_share:
                Logger.d(LOG_TAG, "Selected Share item from Navigation Drawer");
                startShareIntent();
                break;
            case R.id.nav_item_rate:
                Logger.d(LOG_TAG, "Selected Rating item from Navigation Drawer");
                showAppInPlayStore(getPackageName());
                break;
            case R.id.nav_item_feedback:
                Logger.d(LOG_TAG, "Selected Feedback item from Navigation Drawer");
                sendEmailToDeveloper();
                break;
            case R.id.nav_item_donate:
                Logger.d(LOG_TAG, "Selected Donation item from Navigation Drawer");
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.donation_dialog_title)
                        .setMessage(R.string.donation_dialog_message)
                        .setPositiveButton(R.string.donation_dialog_positive_btn, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String donationAppId = getString(R.string.donation_app_id);
                                showAppInPlayStore(donationAppId);
                            }
                        })
                        .setNeutralButton(R.string.donation_dialog_neutral_btn, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
                break;
            case R.id.nav_item_crash:
                Logger.d(LOG_TAG, "Manually crashing Routine Task app");
                throw new RuntimeException("This Exception is thrown manually for testing purposes.");
        }
        mSelectedDrawerItem = selectedItemId;
        setStatsBottomNavVisible(mSelectedDrawerItem == R.id.nav_item_my_progress);
    }

    private void showCurrentFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, mCurrentFragment)
                .commit();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_NAV_SELECTED_ITEM, mSelectedDrawerItem);
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        setCurrentFragment(item.getItemId());
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}