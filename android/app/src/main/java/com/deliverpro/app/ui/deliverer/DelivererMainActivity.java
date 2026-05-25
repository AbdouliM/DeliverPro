package com.deliverpro.app.ui.deliverer;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.graphics.Color;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityDelivererMainBinding;
import com.deliverpro.app.database.LocalDatabase;
import com.deliverpro.app.model.Delivery;
import com.deliverpro.app.model.TourResponse;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;
import com.deliverpro.app.sync.SyncScheduler;
import com.deliverpro.app.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DelivererMainActivity extends AppCompatActivity {

    private ActivityDelivererMainBinding binding;
    private ApiService    apiService;
    private StopAdapter   adapter;
    private LocalDatabase localDb;
    private boolean suppressNavListener = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding   = ActivityDelivererMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        apiService = ApiClient.getClient(this).create(ApiService.class);
        localDb    = LocalDatabase.getInstance(this);

        SyncScheduler.scheduleDailySync(this);

        // Avatar initials
        String name = SessionManager.getInstance(this).getFullName();
        if (name != null && name.contains(" ")) {
            String[] p = name.split(" ");
            binding.tvAvatar.setText(("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase());
        }

        // Style the summary pills to match the photo
        stylePill(binding.tvDoneCount,      "#E8F5E9", "#43A047"); // green
        stylePill(binding.tvRemainingCount, "#FFF3E0", "#E07800"); // orange

        adapter = new StopAdapter(delivery -> {
            Intent intent = new Intent(this, DriverStopDetailActivity.class);
            intent.putExtra("delivery_id",  delivery.getDeliveryId());
            intent.putExtra("stop_order",   delivery.getStopOrder());
            intent.putExtra("client_name",  delivery.getClientName());
            intent.putExtra("client_phone", delivery.getClientPhone());
            intent.putExtra("address",
                    delivery.getDeliveryAddress() + ", " + delivery.getDeliveryCity());
            intent.putExtra("payment",      delivery.getPaymentMethod());
            intent.putExtra("nb_articles",  delivery.getNbArticles());
            intent.putExtra("amount",       delivery.getAmount());
            intent.putExtra("status",       delivery.getStatus());
            intent.putExtra("order_number", delivery.getOrderNumber());
            startActivity(intent);
        });
        binding.rvStops.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStops.setAdapter(adapter);

        setupBottomNav();

        if (localDb.hasTourLoaded()) {
            loadFromLocal();
        } else {
            loadFromApi();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_my_deliveries);
        suppressNavListener = false;
        // Always reload from local DB on resume so status changes from
        // DriverStopDetailActivity are reflected immediately in the list
        if (localDb.hasTourLoaded()) {
            loadFromLocal();
        } else {
            loadFromApi();
        }
    }

    private void loadFromLocal() {
        updateUI(localDb.getTour());
    }

    private void loadFromApi() {
        binding.progressBar.setVisibility(View.VISIBLE);
        apiService.getDriverTour().enqueue(new Callback<TourResponse>() {
            @Override
            public void onResponse(Call<TourResponse> call, Response<TourResponse> r) {
                binding.progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    List<Delivery> stops = r.body().getStops();
                    localDb.saveTour(stops);
                    updateUI(stops);
                } else {
                    Toast.makeText(DelivererMainActivity.this,
                            "Erreur chargement tournée", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<TourResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(DelivererMainActivity.this,
                        "Pas de connexion — données locales utilisées",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(List<Delivery> stops) {
        if (stops == null || stops.isEmpty()) return;

        long done      = stops.stream().filter(d -> "DELIVERED".equals(d.getStatus())).count();
        long remaining = stops.stream().filter(d -> !"DELIVERED".equals(d.getStatus())).count();

        // Toolbar subtitle  e.g. "Aujourd'hui — 28 arrêts"
        binding.tvSubtitle.setText("Aujourd'hui — " + stops.size() + " arrêts");

        // Summary pills  e.g. "✓ 16 Effectuées"  "⏰ 12 Restantes"
        binding.tvDoneCount.setText("✓ " + done + " Effectuées");
        binding.tvRemainingCount.setText("⏰ " + remaining + " Restantes");

        adapter.setData(stops);
    }

    /**
     * Applies a rounded pill background to a summary TextView.
     * Matches the badge style shown in the photo.
     */
    private void stylePill(android.widget.TextView tv, String bgHex, String textHex) {
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(Color.parseColor(bgHex));
        pill.setCornerRadius(100f); // fully rounded
        pill.setStroke(1, Color.parseColor(textHex));
        tv.setBackground(pill);
        tv.setTextColor(Color.parseColor(textHex));
        int px = (int) (8 * tv.getContext().getResources().getDisplayMetrics().density);
        int py = (int) (4 * tv.getContext().getResources().getDisplayMetrics().density);
        tv.setPadding(px * 2, py, px * 2, py);
    }

    private void setupBottomNav() {
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_my_deliveries);
        suppressNavListener = false;

        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (suppressNavListener) return true;
            int id = item.getItemId();
            if (id == R.id.nav_my_deliveries) { return true; }
            if (id == R.id.nav_messaging)     { goTo(com.deliverpro.app.ui.controller.MessagingActivity.class); return true; }
            if (id == R.id.nav_emergency)     { goTo(EmergencyActivity.class); return true; }
            if (id == R.id.nav_profile)       { goTo(com.deliverpro.app.ui.controller.ProfileActivity.class); return true; }
            return false;
        });
    }

    private void goTo(Class<?> cls) {
        Intent i = new Intent(this, cls);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    protected void onDestroy() { super.onDestroy(); binding = null; }
}
/*package com.deliverpro.app.ui.deliverer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityDelivererMainBinding;
import com.deliverpro.app.database.LocalDatabase;
import com.deliverpro.app.model.Delivery;
import com.deliverpro.app.model.TourResponse;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;
import com.deliverpro.app.sync.SyncScheduler;
import com.deliverpro.app.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DelivererMainActivity extends AppCompatActivity {

    private ActivityDelivererMainBinding binding;
    private ApiService    apiService;
    private StopAdapter   adapter;
    private LocalDatabase localDb;
    private boolean suppressNavListener = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding   = ActivityDelivererMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        apiService = ApiClient.getClient(this).create(ApiService.class);
        localDb    = LocalDatabase.getInstance(this);

        SyncScheduler.scheduleDailySync(this);

        String name = SessionManager.getInstance(this).getFullName();
        if (name != null && name.contains(" ")) {
            String[] p = name.split(" ");
            binding.tvAvatar.setText(("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase());
        }

        adapter = new StopAdapter(delivery -> {
            Intent intent = new Intent(this, DriverStopDetailActivity.class);
            intent.putExtra("delivery_id",  delivery.getDeliveryId());
            intent.putExtra("stop_order",   delivery.getStopOrder());
            intent.putExtra("client_name",  delivery.getClientName());
            intent.putExtra("client_phone", delivery.getClientPhone());
            intent.putExtra("address",
                    delivery.getDeliveryAddress() + ", " + delivery.getDeliveryCity());
            intent.putExtra("payment",      delivery.getPaymentMethod());
            intent.putExtra("nb_articles",  delivery.getNbArticles());
            intent.putExtra("amount",       delivery.getAmount());
            intent.putExtra("status",       delivery.getStatus());
            intent.putExtra("order_number", delivery.getOrderNumber());
            startActivity(intent);
        });
        binding.rvStops.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStops.setAdapter(adapter);

        setupBottomNav();

        if (localDb.hasTourLoaded()) {
            loadFromLocal();
        } else {
            loadFromApi();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_my_deliveries);
        suppressNavListener = false;
        if (localDb.hasTourLoaded()) loadFromLocal();
    }

    private void loadFromLocal() {
        List<Delivery> stops = localDb.getTour();
        updateUI(stops);
    }

    private void loadFromApi() {
        binding.progressBar.setVisibility(View.VISIBLE);
        apiService.getDriverTour().enqueue(new Callback<TourResponse>() {
            @Override
            public void onResponse(Call<TourResponse> call, Response<TourResponse> r) {
                binding.progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    List<Delivery> stops = r.body().getStops();
                    localDb.saveTour(stops);
                    updateUI(stops);
                } else {
                    Toast.makeText(DelivererMainActivity.this,
                            "Erreur chargement tournée", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<TourResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(DelivererMainActivity.this,
                        "Pas de connexion — données locales utilisées",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(List<Delivery> stops) {
        if (stops == null || stops.isEmpty()) return;
        long done      = stops.stream().filter(d -> "DELIVERED".equals(d.getStatus())).count();
        long remaining = stops.stream().filter(d -> !"DELIVERED".equals(d.getStatus())).count();
        binding.tvSubtitle.setText("Aujourd'hui — " + stops.size() + " arrêts");
        binding.tvDoneCount.setText("✓ " + done + " Effectuées");
        binding.tvRemainingCount.setText("⏰ " + remaining + " Restantes");
        adapter.setData(stops);
    }

    private void setupBottomNav() {
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_my_deliveries);
        suppressNavListener = false;

        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (suppressNavListener) return true;
            int id = item.getItemId();
            if (id == R.id.nav_my_deliveries) { return true; }
            if (id == R.id.nav_messaging)  { goTo(com.deliverpro.app.ui.controller.MessagingActivity.class); return true; }
            if (id == R.id.nav_emergency)  { goTo(EmergencyActivity.class); return true; }
            if (id == R.id.nav_profile)    { goTo(com.deliverpro.app.ui.controller.ProfileActivity.class); return true; }
            return false;
        });
    }

    // Tab navigation: finish self so stack stays flat (Home + 1 screen max)
    private void goTo(Class<?> cls) {
        Intent i = new Intent(this, cls);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        // Do NOT finish home — it's the root, we need it in the stack
    }

    @Override
    protected void onDestroy() { super.onDestroy(); binding = null; }
}*/
/*// ui/deliverer/DelivererMainActivity.java — CORRIGÉ
package com.deliverpro.app.ui.deliverer;

import com.deliverpro.app.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.deliverpro.app.databinding.ActivityDelivererMainBinding;
import com.deliverpro.app.database.LocalDatabase;
import com.deliverpro.app.model.Delivery;
import com.deliverpro.app.model.TourResponse;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;
import com.deliverpro.app.sync.SyncScheduler;
import com.deliverpro.app.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DelivererMainActivity extends AppCompatActivity {

    private ActivityDelivererMainBinding binding;
    private ApiService    apiService;
    private StopAdapter   adapter;
    private LocalDatabase localDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding   = ActivityDelivererMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        apiService = ApiClient.getClient(this).create(ApiService.class);
        localDb    = LocalDatabase.getInstance(this);

        SyncScheduler.scheduleDailySync(this);

        // Avatar
        String name = SessionManager.getInstance(this).getFullName();
        if (name != null && name.contains(" ")) {
            String[] p = name.split(" ");
            binding.tvAvatar.setText(("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase());
        }

        adapter = new StopAdapter(delivery -> {
            Intent intent = new Intent(this, DriverStopDetailActivity.class);
            intent.putExtra("delivery_id",  delivery.getDeliveryId());
            intent.putExtra("stop_order",   delivery.getStopOrder());
            intent.putExtra("client_name",  delivery.getClientName());
            intent.putExtra("client_phone", delivery.getClientPhone());
            intent.putExtra("address",
                    delivery.getDeliveryAddress() + ", " + delivery.getDeliveryCity());
            intent.putExtra("payment",      delivery.getPaymentMethod());
            intent.putExtra("nb_articles",  delivery.getNbArticles());
            intent.putExtra("amount",       delivery.getAmount());
            intent.putExtra("status",       delivery.getStatus());
            intent.putExtra("order_number", delivery.getOrderNumber());
            startActivity(intent);
        });
        binding.rvStops.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStops.setAdapter(adapter);

        setupBottomNav();

        if (localDb.hasTourLoaded()) {
            loadFromLocal();
        } else {
            loadFromApi();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Corriger l'onglet sélectionné
        binding.bottomNav.setSelectedItemId(R.id.nav_my_deliveries);
        if (localDb.hasTourLoaded()) loadFromLocal();
    }

    private void loadFromLocal() {
        List<Delivery> stops = localDb.getTour();
        updateUI(stops);
    }

    private void loadFromApi() {
        binding.progressBar.setVisibility(View.VISIBLE);
        apiService.getDriverTour().enqueue(new Callback<TourResponse>() {
            @Override
            public void onResponse(Call<TourResponse> call, Response<TourResponse> r) {
                binding.progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    List<Delivery> stops = r.body().getStops();
                    localDb.saveTour(stops);
                    updateUI(stops);
                } else {
                    Toast.makeText(DelivererMainActivity.this,
                            "Erreur chargement tournée", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<TourResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(DelivererMainActivity.this,
                        "Pas de connexion — données locales utilisées",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(List<Delivery> stops) {
        if (stops == null || stops.isEmpty()) return;
        long done      = stops.stream().filter(d -> "DELIVERED".equals(d.getStatus())).count();
        long remaining = stops.stream().filter(d -> !"DELIVERED".equals(d.getStatus())).count();
        binding.tvSubtitle.setText("Aujourd'hui — " + stops.size() + " arrêts");
        binding.tvDoneCount.setText("✓ " + done + " Effectuées");
        binding.tvRemainingCount.setText("⏰ " + remaining + " Restantes");
        adapter.setData(stops);
    }

    private void setupBottomNav() {
        // Pas de setSelectedItemId ici — fait dans onResume()
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_my_deliveries) {
                // Déjà sur cet écran
                return true;
            }
            if (id == R.id.nav_messaging) {
                Intent i = new Intent(this,
                        com.deliverpro.app.ui.controller.MessagingActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                return true;
            }
            if (id == R.id.nav_emergency) {
                Intent i = new Intent(this, EmergencyActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                return true;
            }
            if (id == R.id.nav_profile) {
                Intent i = new Intent(this,
                        com.deliverpro.app.ui.controller.ProfileActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onDestroy() { super.onDestroy(); binding = null; }
}*/