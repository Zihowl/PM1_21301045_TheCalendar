package com.zihowl.thecalendar;

import android.app.Application;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;
import com.zihowl.thecalendar.data.session.SessionManager;
import com.zihowl.thecalendar.data.source.local.RealmDataSource;
import com.zihowl.thecalendar.notifications.NotificationHelper;
import com.zihowl.thecalendar.notifications.ClassNotificationScheduler;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class TheCalendar extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);

        RealmConfiguration config = new RealmConfiguration.Builder()
                .name("thecalendar.realm")
                .schemaVersion(1)
                .modules(new AppRealmModule())
                .allowWritesOnUiThread(true)
                .deleteRealmIfMigrationNeeded()
                .build();

        Realm.setDefaultConfiguration(config);

        SessionManager session = new SessionManager(this);
        TheCalendarRepository repository = TheCalendarRepository.getInstance(new RealmDataSource(), session);

        // Always ensure counters reflect current tasks and notes
        repository.recalculateAllSubjectCounters();

        NotificationHelper.createNotificationChannel(this);
        ClassNotificationScheduler.scheduleNextClass(this);
    }
}