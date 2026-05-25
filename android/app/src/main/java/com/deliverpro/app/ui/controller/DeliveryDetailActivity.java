package com.deliverpro.app.ui.controller;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityDeliveryDetailBinding;
import com.deliverpro.app.model.Delivery;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeliveryDetailActivity extends AppCompatActivity {

    private ActivityDeliveryDetailBinding binding;
    private ApiService apiService;
    private Delivery currentDelivery;
    private boolean suppressNavListener = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityDeliveryDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        apiService = ApiClient.getClient(this).create(ApiService.class);

        int deliveryId = getIntent().getIntExtra("delivery_id", -1);
        if (deliveryId == -1) { finish(); return; }

        loadDeliveryDetail(deliveryId);
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_deliveries);
        suppressNavListener = false;
    }

    private void loadDeliveryDetail(int id) {
        apiService.getDeliveryDetail(id).enqueue(new Callback<Delivery>() {
            @Override
            public void onResponse(Call<Delivery> call, Response<Delivery> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentDelivery = response.body();
                    bindUI(currentDelivery);
                } else {
                    Toast.makeText(DeliveryDetailActivity.this,
                            "Livraison non trouvée", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override
            public void onFailure(Call<Delivery> call, Throwable t) {
                Toast.makeText(DeliveryDetailActivity.this,
                        "Erreur réseau", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void bindUI(Delivery d) {
        binding.tvOrderNumber.setText(d.getOrderNumber());

        switch (d.getStatus()) {
            case "DELIVERED":
                binding.tvStatus.setText("Livré");
                binding.tvStatus.setTextColor(Color.parseColor("#43A047"));
                break;
            case "PENDING":
                binding.tvStatus.setText("En attente");
                binding.tvStatus.setTextColor(Color.parseColor("#E07800"));
                break;
            case "FAILED":
                binding.tvStatus.setText("Échoué");
                binding.tvStatus.setTextColor(Color.parseColor("#E53935"));
                break;
        }

        String clientName  = d.getClient() != null ? d.getClient().getName()  : d.getClientName();
        String clientPhone = d.getClient() != null ? d.getClient().getPhone() : d.getClientPhone();

        binding.tvClientName.setText(clientName);
        binding.tvClientPhone.setText("📞 " + clientPhone);

        String address = d.getDeliveryAddress() != null
                ? d.getDeliveryAddress() + ", " + d.getDeliveryCity()
                : "Adresse non disponible";
        binding.tvAddress.setText(address);

        binding.tvAmount.setText(String.format("%.3f TND", d.getAmount()));
        binding.tvPayment.setText(formatPayment(d.getPaymentMethod()));
        String delivererName = d.getDeliverer() != null
                ? d.getDeliverer().getName() : d.getDelivererName();
        binding.tvDelivererName.setText(delivererName);

        if (d.isFailed() && d.getFailureReason() != null) {
            binding.layoutFailureBanner.setVisibility(View.VISIBLE);
            binding.tvFailureReason.setText(d.getFailureReason());
        }

        binding.tvOpenMaps.setOnClickListener(v -> {
            String query = Uri.encode(address);
            Uri uri = Uri.parse("geo:0,0?q=" + query);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://maps.google.com/?q=" + query)));
            }
        });

        binding.btnCall.setOnClickListener(v -> {
            if (clientPhone != null) {
                startActivity(new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + clientPhone)));
            }
        });

        binding.btnMessage.setOnClickListener(v -> {
            if (d.getDeliverer() != null) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("receiver_id",   d.getDeliverer().getUserId());
                intent.putExtra("receiver_name", d.getDeliverer().getName());
                startActivity(intent);
            }
        });
    }

    private String formatPayment(String method) {
        if (method == null) return "—";
        switch (method) {
            case "CASH":        return "Espèces";
            case "CARD":        return "Carte bancaire";
            case "ON_DELIVERY": return "Paiement à la livraison";
            default:            return method;
        }
    }

    private void setupBottomNav() {
        // Set initial selection WITHOUT triggering listener
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_deliveries);
        suppressNavListener = false;

        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (suppressNavListener) return true;
            int id = item.getItemId();
            if (id == R.id.nav_home)       { goHome(); return true; }
            if (id == R.id.nav_deliveries) { finish(); return true; } // go back to list
            if (id == R.id.nav_messages)   { goTo(MessagingActivity.class); return true; }
            if (id == R.id.nav_profile)    { goTo(ProfileActivity.class);   return true; }
            return false;
        });
    }

    private void goHome() {
        Intent i = new Intent(this, ControllerMainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    private void goTo(Class<?> cls) {
        Intent i = new Intent(this, cls);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onDestroy() { super.onDestroy(); binding = null; }
}
/*// ui/controller/DeliveryDetailActivity.java — Écran 06
package com.deliverpro.app.ui.controller;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityDeliveryDetailBinding;
import com.deliverpro.app.model.Delivery;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeliveryDetailActivity extends AppCompatActivity {

    private ActivityDeliveryDetailBinding binding;
    private ApiService apiService;
    private Delivery currentDelivery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityDeliveryDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        apiService = ApiClient.getClient(this).create(ApiService.class);

        int deliveryId = getIntent().getIntExtra("delivery_id", -1);
        if (deliveryId == -1) { finish(); return; }

        loadDeliveryDetail(deliveryId);
        setupBottomNav();
    }

    private void loadDeliveryDetail(int id) {
        apiService.getDeliveryDetail(id).enqueue(new Callback<Delivery>() {

            @Override
            public void onResponse(Call<Delivery> call, Response<Delivery> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentDelivery = response.body();
                    bindUI(currentDelivery);
                } else {
                    Toast.makeText(DeliveryDetailActivity.this,
                            "Livraison non trouvée", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Delivery> call, Throwable t) {
                Toast.makeText(DeliveryDetailActivity.this,
                        "Erreur réseau", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void bindUI(Delivery d) {
        // Toolbar
        binding.tvOrderNumber.setText(d.getOrderNumber());

        // Statut avec couleur
        switch (d.getStatus()) {
            case "DELIVERED":
                binding.tvStatus.setText("Livré");
                binding.tvStatus.setTextColor(Color.parseColor("#43A047"));
                break;
            case "PENDING":
                binding.tvStatus.setText("En attente");
                binding.tvStatus.setTextColor(Color.parseColor("#E07800"));
                break;
            case "FAILED":
                binding.tvStatus.setText("Échoué");
                binding.tvStatus.setTextColor(Color.parseColor("#E53935"));
                break;
        }

        // Infos client
        String clientName = d.getClient() != null
                ? d.getClient().getName() : d.getClientName();
        String clientPhone = d.getClient() != null
                ? d.getClient().getPhone() : d.getClientPhone();

        binding.tvClientName.setText(clientName);
        binding.tvClientPhone.setText("📞 " + clientPhone);

        // Adresse
        String address = d.getDeliveryAddress() != null
                ? d.getDeliveryAddress() + ", " + d.getDeliveryCity()
                : "Adresse non disponible";
        binding.tvAddress.setText(address);

        // Infos commande
        binding.tvAmount.setText(String.format("%.3f TND", d.getAmount()));
        binding.tvPayment.setText(formatPayment(d.getPaymentMethod()));
        String delivererName = d.getDeliverer() != null
                ? d.getDeliverer().getName() : d.getDelivererName();
        binding.tvDelivererName.setText(delivererName);

        // Bannière FAILED — afficher uniquement si échoué
        if (d.isFailed() && d.getFailureReason() != null) {
            binding.layoutFailureBanner.setVisibility(View.VISIBLE);
            binding.tvFailureReason.setText(d.getFailureReason());
        }

        // Bouton Maps
        binding.tvOpenMaps.setOnClickListener(v -> {
            String query = Uri.encode(address);
            Uri uri = Uri.parse("geo:0,0?q=" + query);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Fallback navigateur
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://maps.google.com/?q=" + query)));
            }
        });

        // Bouton Appeler le client
        binding.btnCall.setOnClickListener(v -> {
            if (clientPhone != null) {
                startActivity(new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + clientPhone)));
            }
        });

        // Bouton Message → ouvrir messagerie avec le livreur
        binding.btnMessage.setOnClickListener(v -> {
            if (d.getDeliverer() != null) {
                Intent intent = new Intent(this, MessagingActivity.class);
                intent.putExtra("receiver_id",   d.getDeliverer().getUserId());
                intent.putExtra("receiver_name", d.getDeliverer().getName());
                startActivity(intent);
            }
        });
    }

    private String formatPayment(String method) {
        if (method == null) return "—";
        switch (method) {
            case "CASH":        return "Espèces";
            case "CARD":        return "Carte bancaire";
            case "ON_DELIVERY": return "Paiement à la livraison";
            default:            return method;
        }
    }

    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_deliveries);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { finish(); return true; }
            if (id == R.id.nav_deliveries) {
                startActivity(new Intent(this, DeliveriesActivity.class));
                return true;
            }
            if (id == R.id.nav_messages) {
                startActivity(new Intent(this, MessagingActivity.class));
                return true;
            }
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}*/