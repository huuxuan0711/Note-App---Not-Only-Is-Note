package com.xmobile.project0.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.xmobile.project0.Activity.DeletedNotesActivity;
import com.xmobile.project0.Adapter.FeatureAdapter;
import com.xmobile.project0.Database.NoteDatabase;
import com.xmobile.project0.Helper.ItemFeatureClickListener;
import com.xmobile.project0.R;
import com.xmobile.project0.databinding.FragmentSettingBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SettingFragment extends BaseFragment implements ItemFeatureClickListener {
    private FragmentSettingBinding binding;
    private List<String> listChoose = new ArrayList<>();
    private List<Integer> listChooseID = new ArrayList<>();
    private FeatureAdapter adapter;
    private RecyclerView recyclerViewFeature;
    private int currentType = -1;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public SettingFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingBinding.inflate(inflater, container, false);
        initControl();
        return binding.getRoot();
    }

    private void initControl() {
        displaySizeTrash();

        binding.layoutTheme.setOnClickListener(v -> featureSetting(1));

        binding.layoutFont.setOnClickListener(v -> featureSetting(2));

        binding.layoutLanguages.setOnClickListener(v -> featureSetting(3));

        binding.layoutDeletedNote.setOnClickListener(v -> deletedNotes());
    }

    private void displaySizeTrash() {
        compositeDisposable.add(
                NoteDatabase.getDatabase(requireContext())
                        .noteDao()
                        .countDeletedNotes()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                size -> binding.trashSize.setText(String.valueOf(size)),
                                throwable -> Log.e("displaySizeTrash", "Error getting trash size", throwable)
                        )
        );
    }

    private void deletedNotes() {
        Intent intent = new Intent(requireContext(), DeletedNotesActivity.class);
        startActivity(intent);
    }

    private void featureSetting(int type) {
        currentType = type;

        ConstraintLayout bottomSheet = binding.getRoot().findViewById(R.id.bottomSheetFeatureSetting);
        View dimView = binding.dimView;
        setupBottomSheet(bottomSheet, dimView);
        setupCancelButton(bottomSheet);
        setupDimViewTouchBlocker(dimView);

        TextView txtFeatureName = bottomSheet.findViewById(R.id.txtFeatureName);
        recyclerViewFeature = bottomSheet.findViewById(R.id.recyclerViewFeature);

        setupFeatureData(type, txtFeatureName);
        setupRecyclerView(type);

        setupBottomSheetCallbacks(dimView);
    }

    private void setupBottomSheet(View bottomSheet, View dimView) {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        dimView.setVisibility(View.VISIBLE);
    }

    private void setupCancelButton(View bottomSheet) {
        TextView txtCancel = bottomSheet.findViewById(R.id.txtCancel);
        txtCancel.setOnClickListener(v -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED));
    }

    private void setupDimViewTouchBlocker(View dimView) {
        dimView.setOnTouchListener((v, event) -> true); // Block all touch events
    }

    private void setupFeatureData(int type, TextView txtFeatureName) {
        listChoose.clear();
        listChooseID.clear();

        switch (type) {
            case 1: // Theme
                txtFeatureName.setText(getString(R.string.theme));
                addOptions(new int[]{R.string.dark, R.string.light, R.string.auto});
                break;
            case 2: // Font
                txtFeatureName.setText(getString(R.string.system_font));
                listChoose.addAll(Arrays.asList("Lato", "Nunito Sans", "Open Sans", "Roboto", "Inter"));
                break;
            case 3: // Language
                txtFeatureName.setText(getString(R.string.language));
                addOptions(new int[]{R.string.vietnamese, R.string.english});
                break;
        }
    }

    private void addOptions(int[] optionIds) {
        for (int id : optionIds) {
            listChoose.add(getString(id));
            listChooseID.add(id);
        }
    }

    private void setupRecyclerView(int type) {
        adapter = new FeatureAdapter(listChoose, requireContext(), type, SettingFragment.this);
        recyclerViewFeature.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewFeature.setAdapter(adapter);
    }

    private void setupBottomSheetCallbacks(View dimView) {
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    dimView.setVisibility(View.VISIBLE);
                } else if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    dimView.setVisibility(View.GONE);
                    bottomSheetBehavior.removeBottomSheetCallback(this);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                dimView.setAlpha(slideOffset);
            }
        });
    }

    @Override
    public void onFeatureClick(int position) {
        String chosenItem = listChoose.get(position);
        int chosenResId = currentType == 1 || currentType == 3 ? listChooseID.get(position) : -1;

        switch (currentType) {
            case 1:
                applyTheme(chosenItem, chosenResId);
                break;
            case 2:
                applyFont(chosenItem);
                break;
            case 3:
                applyLanguage(chosenItem, chosenResId);
                break;
        }

        adapter.notifyDataSetChanged();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void applyTheme(String chosenItem, int chosenResId) {
        saveToPreferences("chooseTheme", chosenResId);

        if (chosenItem.equals(getString(R.string.auto))) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            int themeRes = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES)
                    ? R.style.Theme_Project0NoteApp_Dark
                    : R.style.Theme_Project0NoteApp;
            requireContext().setTheme(themeRes);
        } else if (chosenItem.equals(getString(R.string.light))) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            requireContext().setTheme(R.style.Theme_Project0NoteApp);
        } else if (chosenItem.equals(getString(R.string.dark))) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            requireContext().setTheme(R.style.Theme_Project0NoteApp_Dark);
        }
    }

    private void applyFont(String fontName) {
        saveToPreferences("chooseFont", fontName);
        requireActivity().recreate();
    }

    private void applyLanguage(String chosenItem, int chosenResId) {
        String languageCode = "";

        if (chosenItem.equals(getString(R.string.vietnamese))) {
            languageCode = "vi";
        } else if (chosenItem.equals(getString(R.string.english))) {
            languageCode = "en";
        }

        saveToPreferences("chooseLanguage", chosenResId);

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);

        resources.updateConfiguration(config, resources.getDisplayMetrics());
        requireActivity().recreate();
    }

    private void saveToPreferences(String key, int value) {
        requireContext().getSharedPreferences("choose", Context.MODE_PRIVATE)
                .edit()
                .putInt(key, value)
                .apply();
    }

    private void saveToPreferences(String key, String value) {
        requireContext().getSharedPreferences("choose", Context.MODE_PRIVATE)
                .edit()
                .putString(key, value)
                .apply();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (bottomSheetBehavior != null){
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        displaySizeTrash();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("SettingFragment", "onDestroyView called");
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.clear();
        }
    }

}

//chức năng: thay đổi chế độ nền (tối/sáng/tự động), font chữ hệ thống, ngôn ngữ (tiếng việt, tiếng anh), toggle chuyển sang markdown/thường (yêu cầu khởi động lại), thùng rác (giữ 7 ngày)
//chế độ nền -> ấn vào chuyển sang fragment chọn
//font chữ hệ thống -> ấn vào chuyển sang fragment chọn
//ngôn ngữ -> ấn vào chuyển sang fragment chọn
//markdown/thường -> ấn toggle
//thùng rác -> ấn vào chuyển sang fragment chứa danh sách note đã xóa

//chế độ nền, font chữ, ngôn ngữ chung một layout bottom sheet nhưng khác dữ liệu vào
//thùng rác là một layout khác

//bỏ
//Chế độ markdown: button tạo note mới thêm hình markdown ở góc trên phải, và ghi chú nào viết bằng markdown thêm hình markdown ở bên trái tiêu đề, và trong note cũng thêm ở bên trái edittext tiêu đề, thay webview content bằng edittext thường nhúng markdown