// sync/SyncWorker.java
// Worker WorkManager — déclenché automatiquement à 23h59 chaque jour
// Synchronise toutes les livraisons DELIVERED vers la base centrale Oracle

package com.deliverpro.app.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context,
                      @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Batch sync démarré — " + new java.util.Date());

        // Utiliser un latch pour attendre la fin des appels async
        CountDownLatch latch          = new CountDownLatch(1);
        AtomicBoolean  success        = new AtomicBoolean(false);

        SyncManager.getInstance(getApplicationContext())
                .syncDeliveredBatch(new SyncManager.SyncCallback() {

                    @Override
                    public void onSuccess(int syncedCount) {
                        Log.d(TAG, "Batch sync terminé — "
                                + syncedCount + " livraison(s) synchronisée(s)");
                        success.set(true);
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Batch sync échoué : " + error);
                        success.set(false);
                        latch.countDown();
                    }
                });

        // Aussi re-tenter les FAILED qui n'ont pas pu être synchronisés
        SyncManager.getInstance(getApplicationContext())
                .retrySyncFailed(null);

        try {
            // Attendre max 5 minutes
            latch.await(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sync interrompue");
            return Result.retry();
        }

        return success.get() ? Result.success() : Result.retry();
    }
}