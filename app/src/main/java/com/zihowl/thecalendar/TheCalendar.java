package com.zihowl.thecalendar;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import com.zihowl.thecalendar.data.repository.TheCalendarRepository;
import com.zihowl.thecalendar.data.session.SessionManager;
import com.zihowl.thecalendar.data.source.local.RealmDataSource;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class TheCalendar extends Application {
    private static final String PREFS_NAME = "TheCalendarPrefs";
    private static final String KEY_FIRST_LAUNCH = "isFirstLaunch";

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

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);

        SessionManager session = new SessionManager(this);
        TheCalendarRepository repository = TheCalendarRepository.getInstance(new RealmDataSource(), session);

        if (isFirstLaunch) {
            repository.initializeDummyData();
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        }

        // Always ensure counters reflect current tasks and notes
        repository.recalculateAllSubjectCounters();
    }
}