package com.zihowl.thecalendar.data.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.zihowl.thecalendar.data.repository.TheCalendarRepository;
import com.zihowl.thecalendar.data.source.local.RealmDataSource;
import com.zihowl.thecalendar.data.source.remote.ApiService;
import com.zihowl.thecalendar.data.source.remote.RetrofitClient;
import com.zihowl.thecalendar.data.session.SessionManager;

public class SyncWorker extends Worker {
    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SyncManager.getInstance(getApplicationContext()).updateStatus(SyncStatus.SYNCING);
        SessionManager session = new SessionManager(getApplicationContext());
        ApiService api = RetrofitClient.getClient(session).create(ApiService.class);
        TheCalendarRepository repository = TheCalendarRepository.getInstance(new RealmDataSource(), session);
        try {
            repository.syncWithRemote(api);
            SyncManager.getInstance(getApplicationContext()).updateStatus(SyncStatus.COMPLETE);
            return Result.success();
        } catch (Exception e) {
            SyncManager.getInstance(getApplicationContext()).updateStatus(SyncStatus.ERROR);
            return Result.retry();
        }
    }
}
