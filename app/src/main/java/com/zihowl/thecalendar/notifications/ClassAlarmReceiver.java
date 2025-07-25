package com.zihowl.thecalendar.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.zihowl.thecalendar.R;

public class ClassAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String subject = intent.getStringExtra("subjectName");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Próxima clase")
                .setContentText("En 5 minutos comienza " + subject)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(1001, builder.build());
        ClassNotificationScheduler.scheduleNextClass(context);
    }
}
