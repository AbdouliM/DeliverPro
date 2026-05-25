package com.deliverpro.app.ui.deliverer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityDriverStopDetailBinding;

public class DriverStopDetailActivity extends AppCompatActivity {

    private ActivityDriverStopDetailBinding binding;

    // Garder en mémoire pour passer à EmergencyActivity
    private String currentOrderNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDriverStopDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        int    deliveryId  = getIntent().getIntExtra("delivery_id", -1);
        int    stopOrder   = getIntent().getIntExtra("stop_order", 0);
        String clientName  = getIntent().getStringExtra("client_name");
        String clientPhone = getIntent().getStringExtra("client_phone");
        String address     = getIntent().getStringExtra("address");
        String payment     = getIntent().getStringExtra("payment");
        int    nbArticles  = getIntent().getIntExtra("nb_articles", 0);
        double amount      = getIntent().getDoubleExtra("amount", 0);
        String status      = getIntent().getStringExtra("status");
        currentOrderNumber = getIntent().getStringExtra("order_number");

        // Remplir l'UI
        binding.tvStopTitle.setText("Arrêt #" + stopOrder);
        binding.tvClientNameToolbar.setText(clientName);
        binding.tvStatus.setText(formatStatus(status));
        binding.tvClientName.setText(clientName);
        binding.tvClientPhone.setText("📞 " + clientPhone);
        binding.tvAddress.setText(address);
        binding.tvNbArticles.setText(nbArticles + " articles");
        binding.tvAmount.setText(String.format("%.3f TND", amount));
        binding.tvPayment.setText(formatPayment(payment));
        binding.tvPaymentMethod.setText(formatPayment(payment));

        // Google Maps
        binding.btnOpenMaps.setOnClickListener(v -> {
            String query = Uri.encode(address);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("geo:0,0?q=" + query));
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://maps.google.com/?q=" + query)));
            }
        });

        // Modifier la livraison → écran 10
        binding.btnModify.setOnClickListener(v -> {
            Intent intent = new Intent(this, UpdateStatusActivity.class);
            intent.putExtra("delivery_id",  deliveryId);
            intent.putExtra("order_number", currentOrderNumber);
            intent.putExtra("client_name",  clientName);
            intent.putExtra("client_phone", clientPhone);
            intent.putExtra("amount",       amount);
            startActivity(intent);
        });

        setupBottomNav();
    }

    private void setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_my_deliveries) {
                finish();
                return true;
            }
            if (id == R.id.nav_emergency) {
                // CORRECTION : passer le numéro de commande à EmergencyActivity
                Intent i = new Intent(this, EmergencyActivity.class);
                if (currentOrderNumber != null) {
                    i.putExtra("order_number", currentOrderNumber);
                }
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                return true;
            }
            if (id == R.id.nav_messaging) {
                Intent i = new Intent(this,
                        com.deliverpro.app.ui.controller.MessagingActivity.class);
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

    private String formatStatus(String s) {
        if (s == null) return "";
        switch (s) {
            case "DELIVERED": return "Effectué";
            case "FAILED":    return "Échoué";
            default:          return "En attente";
        }
    }

    private String formatPayment(String p) {
        if (p == null) return "—";
        switch (p) {
            case "CASH":        return "Espèces";
            case "CARD":        return "Carte bancaire";
            case "ON_DELIVERY": return "Paiement à la livraison";
            default:            return p;
        }
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
    @Override protected void onDestroy() { super.onDestroy(); binding = null; }
}
/*
// ── DriverStopDetailActivity.java — Écran 09 ─────────────────

package com.deliverpro.app.ui.deliverer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.deliverpro.app.databinding.ActivityDriverStopDetailBinding;

public class DriverStopDetailActivity extends AppCompatActivity {

    private ActivityDriverStopDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDriverStopDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Récupérer les données passées depuis l'écran 08
        int    deliveryId  = getIntent().getIntExtra("delivery_id", -1);
        int    stopOrder   = getIntent().getIntExtra("stop_order", 0);
        String clientName  = getIntent().getStringExtra("client_name");
        String clientPhone = getIntent().getStringExtra("client_phone");
        String address     = getIntent().getStringExtra("address");
        String payment     = getIntent().getStringExtra("payment");
        int    nbArticles  = getIntent().getIntExtra("nb_articles", 0);
        double amount      = getIntent().getDoubleExtra("amount", 0);
        String status      = getIntent().getStringExtra("status");
        String orderNumber = getIntent().getStringExtra("order_number");

        // Remplir l'UI
        binding.tvStopTitle.setText("Arrêt #" + stopOrder);
        binding.tvClientNameToolbar.setText(clientName);
        binding.tvStatus.setText(formatStatus(status));
        binding.tvClientName.setText(clientName);
        binding.tvClientPhone.setText("📞 " + clientPhone);
        binding.tvAddress.setText(address);
        binding.tvNbArticles.setText(nbArticles + " articles");
        binding.tvAmount.setText(String.format("%.3f TND", amount));
        binding.tvPayment.setText(formatPayment(payment));
        binding.tvPaymentMethod.setText(formatPayment(payment));

        // Google Maps
        binding.btnOpenMaps.setOnClickListener(v -> {
            String query = Uri.encode(address);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("geo:0,0?q=" + query));
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://maps.google.com/?q=" + query)));
            }
        });

        // Modifier la livraison → écran 10
        binding.btnModify.setOnClickListener(v -> {
            Intent intent = new Intent(this, UpdateStatusActivity.class);
            intent.putExtra("delivery_id",  deliveryId);
            intent.putExtra("order_number", orderNumber);
            intent.putExtra("client_name",  clientName);
            intent.putExtra("client_phone", clientPhone);
            intent.putExtra("amount",       amount);
            startActivity(intent);
        });

        setupBottomNav();
    }

    private void setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == com.deliverpro.app.R.id.nav_my_deliveries) { finish(); return true; }
            if (id == com.deliverpro.app.R.id.nav_emergency) {
                startActivity(new Intent(this, EmergencyActivity.class)); return true; }
            return false;
        });
    }

    private String formatStatus(String s) {
        if (s == null) return "";
        switch (s) {
            case "DELIVERED": return "Effectué";
            case "FAILED":    return "Échoué";
            default:          return "En attente";
        }
    }

    private String formatPayment(String p) {
        if (p == null) return "—";
        switch (p) {
            case "CASH":        return "Espèces";
            case "CARD":        return "Carte bancaire";
            case "ON_DELIVERY": return "Paiement à la livraison";
            default:            return p;
        }
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
    @Override protected void onDestroy() { super.onDestroy(); binding = null; }
}
*/
