package com.xmobile.project0.Activity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import com.google.android.material.navigation.NavigationBarView;
import com.xmobile.project0.Adapter.Viewpager2Adapter;
import com.xmobile.project0.R;
import com.xmobile.project0.databinding.ActivityMainBinding;

import java.util.Locale;


public class MainActivity extends BaseActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setDisplaySystem();

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNotificationChannel();
        setupViewPager();
        setupBottomNavigation();
        setupGraphViewButton();
    }

    private void setDisplaySystem() {
        SharedPreferences prefs = getSharedPreferences("choose", MODE_PRIVATE);

        // Theme
        int themeId = prefs.getInt("chooseTheme", R.string.light);
        String theme = getString(themeId);

        if (theme.equals(getString(R.string.dark))) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            setTheme(R.style.Theme_Project0NoteApp_Dark);
        } else if (theme.equals(getString(R.string.light))) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            setTheme(R.style.Theme_Project0NoteApp);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            setTheme(nightMode == Configuration.UI_MODE_NIGHT_YES ?
                    R.style.Theme_Project0NoteApp_Dark : R.style.Theme_Project0NoteApp);
        }

        // Language
        int langId = prefs.getInt("chooseLanguage", 0);
        String lang = langId == 0 ? "vi" : getString(langId);
        if (lang.equals(getString(R.string.vietnamese))) lang = "vi";
        else if (lang.equals(getString(R.string.english))) lang = "en";
        else lang = "vi";

        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

    }

    private void setupBottomNavigation() {
        binding.navigationBar.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            int position = getViewPagerPositionByMenuId(itemId);
            binding.viewpager2.setCurrentItem(position);
            return true;
        });
    }

    /** Map ID menu sang vị trí ViewPager2 */
    private int getViewPagerPositionByMenuId(int itemId) {
        if (itemId == R.id.menu_home){
            return 0;
        } else if (itemId == R.id.menu_calendar) {
            return 1;
        } else if (itemId == R.id.menu_noti) {
            return 2;
        } else if (itemId == R.id.menu_setting) {
            return 3;
        }
        return 0;
    }

    /** Xử lý click nút mở Graph View */
    private void setupGraphViewButton() {
        binding.graphView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GraphActivity.class);
            intent.putExtra("depth_by_hop", true);
            startActivity(intent);
        });
    }

    /** Cài đặt ViewPager2 và đồng bộ với BottomNavigation */
    private void setupViewPager() {
        Viewpager2Adapter adapter = new Viewpager2Adapter(this);
        binding.viewpager2.setAdapter(adapter);
        binding.viewpager2.setOffscreenPageLimit(3);

        binding.viewpager2.registerOnPageChangeCallback(new OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int menuId = getMenuItemIdByPosition(position);
                binding.navigationBar.getMenu().findItem(menuId).setChecked(true);
            }
        });
    }

    /** Map vị trí sang ID menu tương ứng */
    private int getMenuItemIdByPosition(int position) {
        switch (position) {
            case 1: return R.id.menu_calendar;
            case 2: return R.id.menu_noti;
            case 3: return R.id.menu_setting;
            default: return R.id.menu_home;
        }
    }

    /** Tạo Notification Channel nếu Android >= O */
    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "reminder_channel",
                    "Nhắc nhở",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Kênh thông báo cho nhắc nhở");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.viewpager2.setAdapter(null);
        binding = null;
    }
}