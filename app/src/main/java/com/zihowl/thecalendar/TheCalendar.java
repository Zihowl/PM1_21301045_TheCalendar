package com.zihowl.thecalendar;

import android.app.Application;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class TheCalendar extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);

        // --- CONFIGURACIÓN CORREGIDA ---
        // Ahora le pasamos nuestro módulo para que Realm conozca las clases.
        RealmConfiguration config = new RealmConfiguration.Builder()
                .name("thecalendar.realm")
                .schemaVersion(1)
                .modules(new AppRealmModule()) // <-- AÑADIR ESTA LÍNEA
                .allowWritesOnUiThread(true)
                .deleteRealmIfMigrationNeeded()
                .build();

        Realm.setDefaultConfiguration(config);
    }
}