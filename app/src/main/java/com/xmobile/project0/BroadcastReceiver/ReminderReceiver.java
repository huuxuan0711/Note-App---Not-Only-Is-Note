package com.xmobile.project0.BroadcastReceiver;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import com.xmobile.project0.Activity.NoteActivity;
import com.xmobile.project0.Database.NoteDatabase;
import com.xmobile.project0.Entities.Noti;
import com.xmobile.project0.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ReminderReceiver extends BroadcastReceiver {
    private Noti noti;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();


    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult result = goAsync(); // giữ broadcast sống lâu hơn
        noti = (Noti) intent.getSerializableExtra("noti");
        try{
            NoteDatabase db = NoteDatabase.getDatabase(context);
            compositeDisposable.add(
                    db.notiDao().updateNotified(noti.getId(), true)
                            .doOnComplete(() -> Log.d("NotificationDebug", "isNotified updated for ID: " + noti.getId()))
                            .andThen(db.noteDao().getNoteWithId(noti.getIdNote()))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(note -> {
                                // Sau khi update xong và lấy được Note → gửi thông báo
                                Intent serviceIntent = new Intent(context, NoteActivity.class);
                                serviceIntent.putExtra("note", note);
                                serviceIntent.putExtra("isUpdate", true);
                                serviceIntent.putExtra("noti", noti);

                                PendingIntent pendingIntent = PendingIntent.getActivity(
                                        context,
                                        0,
                                        serviceIntent,
                                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                );

                                String timeText = new SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault()).format(new Date());

                                Notification notification = new NotificationCompat.Builder(context, "reminder_channel")
                                        .setSmallIcon(R.drawable.beacon_icon)
                                        .setContentTitle("Reminder - " + timeText) // tiêu đề
                                        .setContentText(noti.getTitle() + "\n" + noti.getContent())         // nội dung nhắc nhở
                                        .setStyle(new NotificationCompat.BigTextStyle()
                                                .bigText(noti.getTitle() + "\n" + noti.getContent()))       // mở rộng nội dung khi dài
                                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                                        .setAutoCancel(true)
                                        .setContentIntent(pendingIntent)
                                        .build();

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    NotificationChannel channel = new NotificationChannel(
                                            "reminder_channel",
                                            "Nhắc Nhở",
                                            NotificationManager.IMPORTANCE_HIGH
                                    );
                                    channel.setDescription("Thông báo nhắc nhở");
                                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                    if (notificationManager != null) {
                                        notificationManager.createNotificationChannel(channel); // sẽ không tạo lại nếu đã tồn tại
                                    }
                                }

                                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                notificationManager.notify(noti.getId(), notification);

                                result.finish();
                            }, throwable -> {
                                Log.e("NotificationError", "Error updating and sending notification", throwable);
                                result.finish();
                            })
            );
        }catch (Exception e){
            Log.e("ReminderReceiver", "Unhandled exception", e);
            result.finish();
        }

        // Nếu là nhắc lại mỗi tháng/năm, cập nhật lại alarm
        if (noti.getOption().equals("Mỗi tháng") || noti.getOption().equals("Mỗi năm")) {
            updateAlarm(intent, context);
        }
    }


    @SuppressLint("ScheduleExactAlarm")
    private void updateAlarm(Intent intent, Context context){
        Calendar next = Calendar.getInstance();
        long currentTriggerTime = intent.getLongExtra("triggerTime", System.currentTimeMillis());

        if (noti.getOption().equals("Mỗi tháng")){
            // Tính thời gian tháng tiếp theo
            next.setTimeInMillis(currentTriggerTime);
            next.add(Calendar.MONTH, 1);

        }else if (noti.getOption().equals("Mỗi năm")){
            next.setTimeInMillis(currentTriggerTime);
            next.add(Calendar.YEAR, 1);
        }

        // Gửi lại Alarm mới
        Intent newIntent = new Intent(context, ReminderReceiver.class);
        newIntent.putExtras(intent); // Giữ lại các thông tin cũ
        newIntent.putExtra("triggerTime", next.getTimeInMillis());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                noti.getId(),
                newIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.getTimeInMillis(),
                pendingIntent
        );
    }
}

//fix: không hiện layout thông báo