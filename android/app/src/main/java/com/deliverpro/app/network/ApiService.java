// network/ApiService.java
// Interface Retrofit — un endpoint = une méthode

package com.deliverpro.app.network;

import com.deliverpro.app.model.Alert;
import com.deliverpro.app.model.AlertRequest;
import com.deliverpro.app.model.DashboardResponse;
import com.deliverpro.app.model.Delivery;
import com.deliverpro.app.model.LoginRequest;
import com.deliverpro.app.model.LoginResponse;
import com.deliverpro.app.model.Message;
import com.deliverpro.app.model.SendMessageRequest;
import com.deliverpro.app.model.StatusUpdateRequest;
import com.deliverpro.app.model.TourResponse;
import com.deliverpro.app.model.User;
import com.deliverpro.app.model.UserProfile;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ── AUTH ──────────────────────────────────────────────────
    // Écran 01 — Login
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest body);

    // Écran 00 — Mot de passe oublié
    @POST("auth/forgot-password")
    Call<Void> forgotPassword(@Body LoginRequest body);

    // ── DASHBOARD CONTRÔLEUR ─────────────────────────────────
    // Écran 02 — Par livreur
    @GET("dashboard/by-deliverer")
    Call<DashboardResponse> getDashboardByDeliverer();

    // Écran 03 — Par client
    @GET("dashboard/by-client")
    Call<List<Delivery>> getDashboardByClient();

    // ── LIVRAISONS CONTRÔLEUR ────────────────────────────────
    // Écran 04 — Livraisons actives du jour
    @GET("deliveries/today")
    Call<List<Delivery>> getDeliveriesToday();

    // Écran 05 — Recherche avancée par période
    @GET("deliveries/search")
    Call<List<Delivery>> searchDeliveries(
            @Query("date_start")   String dateStart,
            @Query("date_end")     String dateEnd,
            @Query("deliverer_id") Integer delivererId,
            @Query("status")       String status,
            @Query("search")       String search
    );

    // Écran 06 — Détail livraison
    @GET("deliveries/{id}")
    Call<Delivery> getDeliveryDetail(@Path("id") int deliveryId);

    // ── LIVRAISONS LIVREUR ───────────────────────────────────
    // Écran 08 — Tournée du jour
    @GET("deliveries/driver/tour")
    Call<TourResponse> getDriverTour();

    // Écran 10 — Modifier le statut
    @PATCH("deliveries/{id}/status")
    Call<Delivery> updateDeliveryStatus(
            @Path("id") int deliveryId,
            @Body StatusUpdateRequest body
    );

    // ── MESSAGERIE ───────────────────────────────────────────
    // Écran 07 — Liste des conversations
    @GET("messages/conversations")
    Call<List<Message>> getConversations();

    // Écran 07 — Messages avec un utilisateur
    @GET("messages/{userId}")
    Call<List<Message>> getMessages(@Path("userId") int userId);

    // Écran 07 — Envoyer un message
    @POST("messages")
    Call<Message> sendMessage(@Body SendMessageRequest body);

    // ── ALERTES URGENCE ──────────────────────────────────────
    // Écran 11 — Envoyer une alerte
    @POST("alerts")
    Call<Void> sendAlert(@Body AlertRequest body);

    @GET("alerts/unread")
    Call<List<Alert>> getUnreadAlerts();
    @GET("users/deliverers")
    Call<List<User>> getDeliverers();
    @GET("auth/profile")
    Call<UserProfile> getProfile();
}