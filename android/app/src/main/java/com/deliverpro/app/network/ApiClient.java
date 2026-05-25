// network/ApiClient.java
// Singleton Retrofit — utilisé partout dans l'app pour appeler l'API

package com.deliverpro.app.network;

import android.content.Context;

import com.deliverpro.app.utils.SessionManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // URL de base de l'API — changer l'IP selon ton réseau
    // Émulateur Android Studio  : http://10.0.2.2:3000/api/
    // Téléphone physique        : http://0.0.0.0:3000/api/  (IP de ton PC)
    private static final String BASE_URL = "http://10.0.2.2:3000/api/";

    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {

            // Intercepteur de logs (visible dans Logcat)
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Intercepteur JWT — ajoute automatiquement le token à chaque requête
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        String token = SessionManager.getInstance(context).getToken();
                        Request original = chain.request();

                        Request.Builder builder = original.newBuilder()
                                .header("Content-Type", "application/json");

                        if (token != null && !token.isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }

                        return chain.proceed(builder.build());
                    })
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    // Réinitialiser le client (utile après logout)
    public static void reset() {
        retrofit = null;
    }
}