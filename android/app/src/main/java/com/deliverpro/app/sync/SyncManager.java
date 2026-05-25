// sync/SyncManager.java
// Logique de synchronisation centrale :
//   FAILED   → remontée IMMÉDIATE vers la base centrale
//   DELIVERED → attente du BATCH 23h59

package com.deliverpro.app.sync;

import android.content.Context;
import android.util.Log;

import com.deliverpro.app.database.LocalDatabase;
import com.deliverpro.app.model.Delivery;
import com.deliverpro.app.model.StatusUpdateRequest;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SyncManager {

    private static final String TAG = "SyncManager";

    private static SyncManager instance;
    private final Context context;

    public interface SyncCallback {
        void onSuccess(int syncedCount);
        void onFailure(String error);
    }

    public static synchronized SyncManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new SyncManager(ctx.getApplicationContext());
        }
        return instance;
    }

    private SyncManager(Context context) {
        this.context = context;
    }

    // ── APPELÉ DEPUIS UpdateStatusActivity ───────────────────

    /**
     * Logique principale de mise à jour du statut.
     *
     * DELIVERED → sauvegarde UNIQUEMENT en local (pas d'appel réseau)
     *             sera synchronisé ce soir à 23h59
     *
     * FAILED    → sauvegarde en local ET envoie IMMÉDIATEMENT au serveur
     *             pour que le contrôleur puisse intervenir
     *
     * PENDING   → sauvegarde uniquement en local
     */
    public void updateStatus(int deliveryId, String status,
                             String failureReason, SyncCallback callback) {

        // 1. Toujours sauvegarder localement d'abord
        LocalDatabase.getInstance(context)
                .updateStatusLocal(deliveryId, status, failureReason);

        if ("FAILED".equals(status)) {
            // Remontée immédiate pour les livraisons échouées
            syncFailedImmediately(deliveryId, status, failureReason, callback);
        } else {
            // DELIVERED et PENDING → local uniquement
            Log.d(TAG, "Statut " + status + " sauvegardé localement — "
                    + ("DELIVERED".equals(status)
                    ? "synchronisation ce soir à 23h59"
                    : "aucune sync nécessaire"));
            if (callback != null) callback.onSuccess(0);
        }
    }

    // ── SYNC IMMÉDIATE — FAILED ───────────────────────────────

    private void syncFailedImmediately(int deliveryId, String status,
                                       String failureReason, SyncCallback callback) {
        Log.d(TAG, "FAILED détecté — remontée immédiate vers la base centrale");

        ApiService api = ApiClient.getClient(context).create(ApiService.class);
        StatusUpdateRequest request = new StatusUpdateRequest(status, failureReason);

        api.updateDeliveryStatus(deliveryId, request)
                .enqueue(new Callback<Delivery>() {

                    @Override
                    public void onResponse(Call<Delivery> call, Response<Delivery> r) {
                        if (r.isSuccessful()) {
                            // Marquer comme synchronisé dans la base locale
                            LocalDatabase.getInstance(context).markAsSynced(deliveryId);
                            Log.d(TAG, "FAILED synchronisé avec succès — " +
                                    "contrôleur notifié en temps réel");
                            if (callback != null) callback.onSuccess(1);
                        } else {
                            // Garder is_synced = 0 → sera retenté au prochain sync
                            Log.e(TAG, "Erreur sync FAILED : " + r.code());
                            if (callback != null)
                                callback.onFailure("Erreur serveur " + r.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<Delivery> call, Throwable t) {
                        // Pas de réseau → la livraison reste is_synced=0
                        // Le batch du soir tentera une nouvelle synchronisation
                        Log.e(TAG, "Pas de réseau — FAILED sera synchronisé plus tard : "
                                + t.getMessage());
                        if (callback != null)
                            callback.onFailure("Pas de connexion réseau");
                    }
                });
    }

    // ── BATCH SYNC — DELIVERED ────────────────────────────────

    /**
     * Synchronisation batch des livraisons DELIVERED.
     * Appelé par SyncWorker à 23h59 automatiquement.
     * Peut aussi être déclenché manuellement si nécessaire.
     */
    public void syncDeliveredBatch(SyncCallback callback) {
        List<Delivery> pending =
                LocalDatabase.getInstance(context).getUnsyncedDelivered();

        if (pending.isEmpty()) {
            Log.d(TAG, "Batch sync — aucune livraison DELIVERED à synchroniser");
            if (callback != null) callback.onSuccess(0);
            return;
        }

        Log.d(TAG, "Batch sync — " + pending.size()
                + " livraison(s) DELIVERED à synchroniser");

        ApiService api = ApiClient.getClient(context).create(ApiService.class);
        final int[] syncedCount = {0};
        final int   total       = pending.size();

        for (Delivery d : pending) {
            StatusUpdateRequest request = new StatusUpdateRequest("DELIVERED", null);
            api.updateDeliveryStatus(d.getDeliveryId(), request)
                    .enqueue(new Callback<Delivery>() {

                        @Override
                        public void onResponse(Call<Delivery> call,
                                               Response<Delivery> response) {
                            if (response.isSuccessful()) {
                                LocalDatabase.getInstance(context)
                                        .markAsSynced(d.getDeliveryId());
                                syncedCount[0]++;
                                Log.d(TAG, "Sync batch OK — DEL-"
                                        + d.getOrderNumber() + " synchronisé");
                            }
                            // Si toutes les livraisons ont été traitées
                            if (syncedCount[0] == total && callback != null) {
                                callback.onSuccess(syncedCount[0]);
                            }
                        }

                        @Override
                        public void onFailure(Call<Delivery> call, Throwable t) {
                            Log.e(TAG, "Échec sync batch pour "
                                    + d.getOrderNumber() + " : " + t.getMessage());
                            // Sera retenté au prochain batch
                        }
                    });
        }
    }

    // Aussi tenter de re-synchroniser les FAILED qui ont échoué (pas de réseau)
    public void retrySyncFailed(SyncCallback callback) {
        List<Delivery> failedPending =
                LocalDatabase.getInstance(context).getUnsyncedFailed();

        if (failedPending.isEmpty()) {
            if (callback != null) callback.onSuccess(0);
            return;
        }

        Log.d(TAG, "Retry sync FAILED — " + failedPending.size()
                + " livraison(s) à re-synchroniser");

        for (Delivery d : failedPending) {
            syncFailedImmediately(d.getDeliveryId(), "FAILED",
                    d.getFailureReason(), null);
        }
    }
}