package com.xmobile.project0.Adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.xmobile.project0.Fragment.CalendarFragment;
import com.xmobile.project0.Fragment.HomeFragment;
import com.xmobile.project0.Fragment.NotiFragment;
import com.xmobile.project0.Fragment.SettingFragment;

public class Viewpager2Adapter extends FragmentStateAdapter {
    public Viewpager2Adapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position){
            case 1:
                return new CalendarFragment();
            case 2:
                return new NotiFragment();
            case 3:
                return new SettingFragment();
            default:
                return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
