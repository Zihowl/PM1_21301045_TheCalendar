package com.zihowl.thecalendar.data.sync;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class SyncManager {
    private static SyncManager INSTANCE;
    private final MutableLiveData<SyncStatus> status = new MutableLiveData<>(SyncStatus.OFFLINE);
    private final Context context;

    private SyncManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized SyncManager getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SyncManager(context);
        }
        return INSTANCE;
    }

    public LiveData<SyncStatus> getStatus() {
        return status;
    }

    public void scheduleSync() {
        updateStatus(SyncStatus.SYNCING);
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
        WorkManager.getInstance(context).enqueueUniqueWork("sync", ExistingWorkPolicy.APPEND_OR_REPLACE, request);
    }

    void updateStatus(SyncStatus newStatus) {
        status.postValue(newStatus);
    }
}
