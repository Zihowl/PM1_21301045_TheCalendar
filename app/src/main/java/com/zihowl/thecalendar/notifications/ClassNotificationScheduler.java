package com.zihowl.thecalendar.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;
import com.zihowl.thecalendar.data.session.SessionManager;
import com.zihowl.thecalendar.data.source.local.RealmDataSource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClassNotificationScheduler {

    private static final Map<String, Integer> DAYS = new HashMap<>();
    static {
        DAYS.put("Lunes", Calendar.MONDAY);
        DAYS.put("Martes", Calendar.TUESDAY);
        DAYS.put("Miércoles", Calendar.WEDNESDAY);
        DAYS.put("Miercoles", Calendar.WEDNESDAY);
        DAYS.put("Jueves", Calendar.THURSDAY);
        DAYS.put("Viernes", Calendar.FRIDAY);
        DAYS.put("Sábado", Calendar.SATURDAY);
        DAYS.put("Sabado", Calendar.SATURDAY);
        DAYS.put("Domingo", Calendar.SUNDAY);
    }

    public static void scheduleNextClass(Context context) {
        TheCalendarRepository repo = TheCalendarRepository.getInstance(new RealmDataSource(), new SessionManager(context));
        List<Subject> subjects = repo.getSubjects();
        if (subjects == null || subjects.isEmpty()) return;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Calendar now = Calendar.getInstance();
        long nowMillis = now.getTimeInMillis();
        long bestTime = Long.MAX_VALUE;
        String bestSubject = null;

        for (Subject s : subjects) {
            String schedule = s.getSchedule();
            if (schedule == null) continue;
            for (String line : schedule.split("\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(" ", 2);
                if (parts.length < 2) continue;
                Integer day = DAYS.get(parts[0]);
                if (day == null) continue;
                String[] times = parts[1].trim().split(" - ");
                if (times.length != 2) continue;
                try {
                    Date start = sdf.parse(times[0]);
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_WEEK, day);
                    cal.set(Calendar.HOUR_OF_DAY, start.getHours());
                    cal.set(Calendar.MINUTE, start.getMinutes());
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    if (cal.getTimeInMillis() <= nowMillis) {
                        cal.add(Calendar.WEEK_OF_YEAR, 1);
                    }
                    long trigger = cal.getTimeInMillis() - 5 * 60 * 1000;
                    if (trigger > nowMillis && trigger < bestTime) {
                        bestTime = trigger;
                        bestSubject = s.getName();
                    }
                } catch (ParseException ignored) { }
            }
        }

        if (bestSubject != null) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(context, ClassAlarmReceiver.class);
            i.putExtra("subjectName", bestSubject);
            PendingIntent pi = PendingIntent.getBroadcast(context, 1001, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            if (am != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, bestTime, pi);
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, bestTime, pi);
                }
            }
        }
    }
}
