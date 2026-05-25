package com.deliverpro.app.ui.controller;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityDeliveriesBinding;
import com.deliverpro.app.model.Delivery;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeliveriesActivity extends AppCompatActivity {

    private ActivityDeliveriesBinding binding;
    private ApiService apiService;
    private DeliveryAdapter adapter;
    private boolean suppressNavListener = false;

    public static final int REQUEST_FILTER = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityDeliveriesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        apiService = ApiClient.getClient(this).create(ApiService.class);
        setupRecyclerView();
        setupSearch();
        setupBottomNav();

        binding.btnFilter.setOnClickListener(v ->
                startActivityForResult(
                        new Intent(this, SearchFilterActivity.class), REQUEST_FILTER));

        loadTodayDeliveries();
    }

    @Override
    protected void onResume() {
        super.onResume();
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_deliveries);
        suppressNavListener = false;
    }

    private void setupRecyclerView() {
        adapter = new DeliveryAdapter(delivery -> {
            Intent intent = new Intent(this, DeliveryDetailActivity.class);
            intent.putExtra("delivery_id", delivery.getDeliveryId());
            startActivity(intent);
        });
        binding.rvDeliveries.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDeliveries.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                updateEmptyState();
            }
        });
    }

    private void loadTodayDeliveries() {
        setLoading(true);
        binding.tvResultLabel.setText("LIVRAISONS DU JOUR ");
        apiService.getDeliveriesToday().enqueue(new Callback<List<Delivery>>() {
            @Override
            public void onResponse(Call<List<Delivery>> call, Response<List<Delivery>> r) {
                setLoading(false);
                if (r.isSuccessful() && r.body() != null) {
                    adapter.setData(r.body());
                    updateEmptyState();
                } else {
                    Toast.makeText(DeliveriesActivity.this, "Erreur chargement", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<Delivery>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(DeliveriesActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FILTER && resultCode == RESULT_OK && data != null) {
            String dateStart = data.getStringExtra("date_start");
            String dateEnd   = data.getStringExtra("date_end");
            int delivererId  = data.getIntExtra("deliverer_id", 0);
            String status    = data.getStringExtra("status");
            String search    = data.getStringExtra("search");
            int count        = data.getIntExtra("result_count", 0);
            binding.tvResultLabel.setText("RÉSULTATS — " + count + " livraisons");
            loadFilteredDeliveries(dateStart, dateEnd,
                    delivererId > 0 ? delivererId : null, status, search);
        }
    }

    private void loadFilteredDeliveries(String dateStart, String dateEnd,
                                        Integer delivererId, String status, String search) {
        setLoading(true);
        apiService.searchDeliveries(dateStart, dateEnd, delivererId, status, search)
                .enqueue(new Callback<List<Delivery>>() {
                    @Override
                    public void onResponse(Call<List<Delivery>> call, Response<List<Delivery>> r) {
                        setLoading(false);
                        if (r.isSuccessful() && r.body() != null) {
                            adapter.setData(r.body());
                            updateEmptyState();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Delivery>> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(DeliveriesActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupBottomNav() {
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_deliveries);
        suppressNavListener = false;

        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (suppressNavListener) return true;
            int id = item.getItemId();
            if (id == R.id.nav_home)       { goHome(); return true; }
            if (id == R.id.nav_deliveries) { return true; }
            if (id == R.id.nav_messages)   { goTo(MessagingActivity.class); return true; }
            if (id == R.id.nav_profile)    { goTo(ProfileActivity.class);   return true; }
            return false;
        });
    }

    // Go back to home and clear this activity off the stack
    private void goHome() {
        Intent i = new Intent(this, ControllerMainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    // Navigate to another tab — clear any existing instance first
    private void goTo(Class<?> cls) {
        Intent i = new Intent(this, cls);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish(); // finish self so back stack doesn't grow
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.rvDeliveries.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState() {
        boolean empty = adapter.getItemCount() == 0;
        binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvDeliveries.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() { super.onDestroy(); binding = null; }
}
/*// ui/controller/DeliveriesActivity.java — CORRIGÉ
package com.deliverpro.app.ui.controller;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityDeliveriesBinding;
import com.deliverpro.app.model.Delivery;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeliveriesActivity extends AppCompatActivity {

    private ActivityDeliveriesBinding binding;
    private ApiService apiService;
    private DeliveryAdapter adapter;

    public static final int REQUEST_FILTER = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityDeliveriesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        setupRecyclerView();
        setupSearch();
        setupBottomNav();

        binding.btnFilter.setOnClickListener(v ->
                startActivityForResult(
                        new Intent(this, SearchFilterActivity.class), REQUEST_FILTER));

        loadTodayDeliveries();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Corriger l'onglet sélectionné à chaque retour sur cet écran
        binding.bottomNav.setSelectedItemId(R.id.nav_deliveries);
    }

    private void setupRecyclerView() {
        adapter = new DeliveryAdapter(delivery -> {
            Intent intent = new Intent(this, DeliveryDetailActivity.class);
            intent.putExtra("delivery_id", delivery.getDeliveryId());
            startActivity(intent);
        });
        binding.rvDeliveries.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDeliveries.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                updateEmptyState();
            }
        });
    }

    private void loadTodayDeliveries() {
        setLoading(true);
        binding.tvResultLabel.setText("LIVRAISONS DU JOUR — EN ATTENTE + ÉCHOUÉ");

        apiService.getDeliveriesToday().enqueue(new Callback<List<Delivery>>() {
            @Override
            public void onResponse(Call<List<Delivery>> call, Response<List<Delivery>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setData(response.body());
                    updateEmptyState();
                } else {
                    Toast.makeText(DeliveriesActivity.this,
                            "Erreur chargement", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<Delivery>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(DeliveriesActivity.this,
                        "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FILTER && resultCode == RESULT_OK && data != null) {
            String dateStart = data.getStringExtra("date_start");
            String dateEnd   = data.getStringExtra("date_end");
            int delivererId  = data.getIntExtra("deliverer_id", 0);
            String status    = data.getStringExtra("status");
            String search    = data.getStringExtra("search");
            int count        = data.getIntExtra("result_count", 0);

            binding.tvResultLabel.setText("RÉSULTATS — " + count + " livraisons");
            loadFilteredDeliveries(dateStart, dateEnd,
                    delivererId > 0 ? delivererId : null, status, search);
        }
    }

    private void loadFilteredDeliveries(String dateStart, String dateEnd,
                                        Integer delivererId, String status, String search) {
        setLoading(true);
        apiService.searchDeliveries(dateStart, dateEnd, delivererId, status, search)
                .enqueue(new Callback<List<Delivery>>() {
                    @Override
                    public void onResponse(Call<List<Delivery>> call,
                                           Response<List<Delivery>> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            adapter.setData(response.body());
                            updateEmptyState();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Delivery>> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(DeliveriesActivity.this,
                                "Erreur réseau", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupBottomNav() {
        // Pas de setSelectedItemId ici — fait dans onResume()
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // Revenir au dashboard sans recréer l'activité
                finish();
                return true;

            } else if (id == R.id.nav_deliveries) {
                // Déjà sur cet écran — ne rien faire
                return true;

            } else if (id == R.id.nav_messages) {
                Intent i = new Intent(this, MessagingActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                return true;

            } else if (id == R.id.nav_profile) {
                Intent i = new Intent(this, ProfileActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                return true;
            }
            return false;
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.rvDeliveries.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState() {
        boolean empty = adapter.getItemCount() == 0;
        binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvDeliveries.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() { super.onDestroy(); binding = null; }
}*/