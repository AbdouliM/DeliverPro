// sync/SyncScheduler.java
// Planifie le worker de synchronisation batch à 23h59 chaque jour

package com.deliverpro.app.sync;

import android.content.Context;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class SyncScheduler {

    private static final String WORK_NAME = "deliverpro_batch_sync";
    private static final String TAG       = "SyncScheduler";

    /**
     * Planifie la synchronisation batch tous les jours à 23h59.
     * À appeler une seule fois au démarrage de l'app livreur.
     */
    public static void scheduleDailySync(Context context) {
        // Calculer le délai jusqu'à 23h59 ce soir
        long delayMinutes = getDelayUntil2359();

        Log.d(TAG, "Batch sync planifié dans " + delayMinutes
                + " minutes (23h59 ce soir)");

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class,
                24, TimeUnit.HOURS  // Répéter chaque 24h
        )
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Ne pas remplacer si déjà planifié
                syncRequest
        );
    }

    /**
     * Calculer le nombre de minutes restantes jusqu'à 23h59 ce soir.
     */
    private static long getDelayUntil2359() {
        Calendar now    = Calendar.getInstance();
        Calendar target = Calendar.getInstance();

        target.set(Calendar.HOUR_OF_DAY, 23);
        target.set(Calendar.MINUTE,      59);
        target.set(Calendar.SECOND,       0);
        target.set(Calendar.MILLISECOND,  0);

        // Si 23h59 est déjà passé aujourd'hui → planifier pour demain
        if (now.after(target)) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }

        long diffMs = target.getTimeInMillis() - now.getTimeInMillis();
        return TimeUnit.MILLISECONDS.toMinutes(diffMs);
    }

    /**
     * Annuler la synchronisation planifiée (au logout).
     */
    public static void cancelSync(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        Log.d(TAG, "Batch sync annulé");
    }
}