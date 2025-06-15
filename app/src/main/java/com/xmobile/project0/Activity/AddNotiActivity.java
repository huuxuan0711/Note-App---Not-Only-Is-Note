package com.xmobile.project0.Activity;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import com.xmobile.project0.BroadcastReceiver.ReminderReceiver;
import com.xmobile.project0.Database.NoteDatabase;
import com.xmobile.project0.Entities.Noti;
import com.xmobile.project0.R;
import com.xmobile.project0.databinding.ActivityAddNotiBinding;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AddNotiActivity extends BaseActivity {
    private ActivityAddNotiBinding binding;
    private Noti noti, currentNoti;
    private boolean isSelectedBtnHour = false, isSelectedBtnDate = false;
    private int state = 0, yearSelected, month, day;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddNotiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initUI();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initUI() {
        setupEventHandlers();
        setupInitialDisplay();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupEventHandlers() {
        binding.txtCancel.setOnClickListener(v -> finish());
        binding.txtDone.setOnClickListener(v -> { if (state == 1) addNoti(); });

        binding.btnHour.setOnClickListener(v -> {
            isSelectedBtnHour = !isSelectedBtnHour;
            isSelectedBtnDate = false;
            updateButtonStyles();
            setupHourPicker();
        });

        binding.btnDate.setOnClickListener(v -> {
            isSelectedBtnDate = !isSelectedBtnDate;
            isSelectedBtnHour = false;
            updateButtonStyles();
            setupDatePicker();
        });

        binding.layoutPickOptionRepetition.setOnClickListener(this::showRepetitionOptions);
    }

    private void showRepetitionOptions(View anchor) {
        View layout = LayoutInflater.from(this).inflate(R.layout.layout_option_repetition, null);
        PopupWindow popup = new PopupWindow(layout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, location[0] + anchor.getWidth(), location[1] + anchor.getHeight() + 15);

        int[] ids = {R.id.txt_option_none, R.id.txt_option_daily, R.id.txt_option_weekly, R.id.txt_option_biweekly, R.id.txt_option_monthly, R.id.txt_option_yearly};
        for (int id : ids) {
            TextView tv = layout.findViewById(id);
            if (tv.getText().toString().equals(binding.txtOption.getText().toString())) {
                tv.setTextColor(getResources().getColor(R.color.blue));
            } else {
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(R.attr.colorText2, typedValue, true);
                tv.setTextColor(typedValue.data);
            }
            tv.setOnClickListener(v -> {
                binding.txtOption.setText(tv.getText());
                popup.dismiss();
            });
        }
    }

    private void setupInitialDisplay() {
        Intent intent = getIntent();
        noti = (Noti) intent.getSerializableExtra("noti");
        currentNoti = noti != null ? noti : new Noti();

        if (noti != null) {
            populateNotificationDetails();
        } else {
            populateNoteDetails(intent);
        }

        Calendar calendar = Calendar.getInstance();
        binding.btnHour.setText(String.format(Locale.getDefault(), "%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)));
        binding.btnDate.setText(String.format(Locale.getDefault(), getString(R.string.format), calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)));
        binding.txtDone.setTextColor(getColor(R.color.pale_darkBlue));

        // Đặt màu nền mặc định cho btnHour và btnDate
        int currentMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        int bgColor = (currentMode == Configuration.UI_MODE_NIGHT_YES) ? R.color.darkBlue : R.color.blue;
        binding.btnHour.setBackgroundColor(getColor(bgColor));
        binding.btnDate.setBackgroundColor(getColor(bgColor));
    }

    private void populateNotificationDetails() {
        binding.txtHeader.setText(getString(R.string.modify_reminder));
        binding.txtTitle.setText(noti.getTitle());
        binding.txtDescription.setVisibility(noti.getDescriptionNote() != null ? View.VISIBLE : View.GONE);
        binding.txtDescription.setText(noti.getDescriptionNote());
        binding.btnHour.setText(noti.getHour());
        binding.btnDate.setText(noti.getDate());
        binding.txtOption.setText(noti.getOption());
        binding.edtContentNoti.setText(noti.getContent());
    }

    private void populateNoteDetails(Intent intent) {
        currentNoti.setIdNote(intent.getIntExtra("idNote", 0));
        currentNoti.setTitle(intent.getStringExtra("titleNote"));
        binding.txtTitle.setText(currentNoti.getTitle());

        String description = intent.getStringExtra("descriptionNote");
        binding.txtDescription.setVisibility(description != null ? View.VISIBLE : View.GONE);
        binding.txtDescription.setText(description);
        currentNoti.setDescriptionNote(description);

        String pathImage = intent.getStringExtra("pathImageNote");
        currentNoti.setPathImage(pathImage);
    }

    private void updateButtonStyles() {
        updateStyle(binding.btnHour, isSelectedBtnHour, binding.layoutPickHour);
        updateStyle(binding.btnDate, isSelectedBtnDate, binding.datePicker);
    }

    @SuppressLint("ResourceAsColor")
    private void updateStyle(Button button, boolean selected, View relatedView) {
        int currentMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int bgColor = selected ? R.color.paleBlue : R.color.blue;
        int textColor = selected ? R.color.lightGray : R.color.white;

        if (currentMode == Configuration.UI_MODE_NIGHT_YES) {
            bgColor = selected ? R.color.pale_darkBlue : R.color.darkBlue;
            textColor = selected ? R.color.blue : R.color.white;
        }

        button.setBackgroundColor(getResources().getColor(bgColor));
        button.setTextColor(getResources().getColor(textColor));
        relatedView.setVisibility(selected ? View.VISIBLE : View.GONE);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupDatePicker() {
        binding.datePicker.setOnDateChangedListener((view, year, monthOfYear, dayOfMonth) -> {
            binding.btnDate.setText(String.format(Locale.getDefault(), getString(R.string.format), dayOfMonth, monthOfYear + 1, year));
            int hour = binding.wheelHour.getSelectedPosition();
            int minute = binding.wheelMinute.getSelectedPosition();
            if (isFutureDateTime(year, monthOfYear, dayOfMonth, hour, minute)) {
                state = 1;
                binding.txtDone.setTextColor(getColor(R.color.blue));
                yearSelected = year;
                month = monthOfYear + 1;
                day = dayOfMonth;
            } else {
                state = 0;
                binding.txtDone.setTextColor(getColor(R.color.pale_darkBlue));
            }
        });
    }

    private void setupHourPicker() {
        List<String> hours = new ArrayList<>(), minutes = new ArrayList<>();
        for (int i = 0; i < 24; i++) hours.add(String.format("%02d", i));
        for (int i = 0; i < 60; i++) minutes.add(String.format("%02d", i));

        binding.wheelHour.setData(hours);
        binding.wheelMinute.setData(minutes);
        binding.wheelHour.setOnItemSelectedListener((view, adapter, i) -> updateTime());
        binding.wheelMinute.setOnItemSelectedListener((view, adapter, i) -> updateTime());
    }

    private void updateTime() {
        binding.btnHour.setText(String.format(Locale.getDefault(), "%02d:%02d", binding.wheelHour.getSelectedPosition(), binding.wheelMinute.getSelectedPosition()));
    }

    private boolean isFutureDateTime(int year, int month, int day, int hour, int minute) {
        Calendar selected = Calendar.getInstance();
        selected.set(year, month, day, hour, minute);
        return selected.after(Calendar.getInstance());
    }

    private void addNoti() {
        currentNoti.setContent(binding.edtContentNoti.getText().toString());
        currentNoti.setDate(binding.btnDate.getText().toString());
        currentNoti.setHour(binding.btnHour.getText().toString());
        currentNoti.setOption(binding.txtOption.getText().toString());
        currentNoti.setNotified(false);
        NoteDatabase db = NoteDatabase.getDatabase(AddNotiActivity.this);

        compositeDisposable.add(
                db.notiDao().insertNoti(currentNoti)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(id -> {
                            currentNoti.setId(id.intValue()); // GÁN LẠI ID !!!
                            Log.d("NotiDebug", "Inserted Noti ID = " + currentNoti.getId());

                            createNotificationChannel();
                            scheduleNotification(currentNoti);
                            setResult(RESULT_OK);
                            finish();
                        }, throwable -> {
                            Log.e("AddNotiError", "Lỗi khi thêm noti", throwable);
                        })
        );
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("reminder_channel", "Nhắc Nhở", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Thông báo nhắc nhở");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleNotification(Noti noti) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("noti", noti);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, noti.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (getIntent().getStringExtra("noti") != null) {
            alarmManager.cancel(pendingIntent);
        }

        Calendar calendar = Calendar.getInstance();
        int hour = binding.wheelHour.getSelectedPosition();
        int minute = binding.wheelMinute.getSelectedPosition();
        calendar.set(yearSelected, month - 1, day, hour, minute);

        long triggerTimeMillis = calendar.getTimeInMillis() - 60 * 1000; // Giảm 1 phút để bù delay

        String option = noti.getOption();
        if (option.equals(getString(R.string.repeat_none))) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
            );
        } else if (option.equals(getString(R.string.repeat_daily))) {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        } else if (option.equals(getString(R.string.repeat_weekly))) {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    7 * AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        } else if (option.equals(getString(R.string.repeat_biweekly))) {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    14 * AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        } else if (option.equals(getString(R.string.repeat_monthly))) {
            alarmManager.setExactAndAllowWhileIdle( // xử lý thủ công trong Receiver
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
            );
        } else if (option.equals(getString(R.string.repeat_yearly))) {
            alarmManager.setExactAndAllowWhileIdle( // xử lý thủ công trong Receiver
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
            );
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        compositeDisposable.clear();
    }
}

//clean done