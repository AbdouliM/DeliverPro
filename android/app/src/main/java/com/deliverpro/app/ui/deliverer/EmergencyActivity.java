package com.deliverpro.app.ui.deliverer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.deliverpro.app.R;
import com.deliverpro.app.database.LocalDatabase;
import com.deliverpro.app.databinding.ActivityEmergencyBinding;
import com.deliverpro.app.model.AlertRequest;
import com.deliverpro.app.model.Delivery;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;
import com.deliverpro.app.ui.controller.MessagingActivity;
import com.deliverpro.app.ui.controller.ProfileActivity;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmergencyActivity extends AppCompatActivity {

    private ActivityEmergencyBinding binding;
    private ApiService apiService;
    private LocalDatabase localDb;
    private String selectedAlertType = "ACCIDENT";
    private boolean suppressNavListener = false;

    private double latitude  = 0;
    private double longitude = 0;
    private String locationName = "Position inconnue";
    private int    currentDeliveryId = 0;
    private String currentOrderNumber = "";

    private FusedLocationProviderClient fusedLocation;
    private static final int LOCATION_PERMISSION_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityEmergencyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getClient(this).create(ApiService.class);
        localDb    = LocalDatabase.getInstance(this);
        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        // Auto-fill order number from intent (passed from stop detail)
        // or find the current "next" stop from local DB
        String orderFromIntent = getIntent().getStringExtra("order_number");
        int deliveryFromIntent = getIntent().getIntExtra("delivery_id", 0);

        if (orderFromIntent != null && !orderFromIntent.isEmpty()) {
            currentOrderNumber   = orderFromIntent;
            currentDeliveryId    = deliveryFromIntent;
            binding.etOrderNumber.setText(currentOrderNumber);
        } else {
            // Auto-detect: find first PENDING stop in local tour
            autoFillNextStop();
        }

        setupAlertTypeChips();
        setupBottomNav();
        detectLocation();

        binding.btnSendAlert.setOnClickListener(v -> sendAlert());
    }

    @Override
    protected void onResume() {
        super.onResume();
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_emergency);
        suppressNavListener = false;
    }

    /** Finds the first PENDING delivery and pre-fills the order number field */
    private void autoFillNextStop() {
        List<Delivery> tour = localDb.getTour();
        for (Delivery d : tour) {
            if ("PENDING".equals(d.getStatus())) {
                currentDeliveryId  = d.getDeliveryId();
                currentOrderNumber = d.getOrderNumber();
                binding.etOrderNumber.setText(currentOrderNumber);
                break;
            }
        }
    }

    /** Requests GPS location and updates the location fields */
    private void detectLocation() {
        binding.tvLocation.setText("Détection en cours...");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        fusedLocation.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                latitude     = location.getLatitude();
                longitude    = location.getLongitude();
                locationName = String.format("%.5f, %.5f", latitude, longitude);
                binding.tvLocation.setText("📍 " + locationName);
            } else {
                binding.tvLocation.setText("Position non disponible");
            }
        }).addOnFailureListener(e ->
                binding.tvLocation.setText("Erreur localisation"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            detectLocation();
        } else {
            binding.tvLocation.setText("Permission localisation refusée");
        }
    }

    private void setupAlertTypeChips() {
        List<TextView> chips = Arrays.asList(
                binding.chipAccident, binding.chipClientIssue,
                binding.chipVehicle, binding.chipAddress, binding.chipSafety);
        List<String> types = Arrays.asList(
                "ACCIDENT", "CLIENT_ISSUE", "VEHICLE_BREAKDOWN",
                "ADDRESS_NOT_FOUND", "SAFETY_CONCERN");

        for (int i = 0; i < chips.size(); i++) {
            final String type = types.get(i);
            chips.get(i).setOnClickListener(v -> {
                selectedAlertType = type;
                for (TextView c : chips) {
                    c.setBackgroundResource(R.drawable.bg_chip);
                    c.setTextColor(Color.parseColor("#888888"));
                }
                ((TextView) v).setBackgroundResource(R.drawable.bg_chip_selected);
                ((TextView) v).setTextColor(Color.parseColor("#1A56A4"));
            });
        }
    }

    private void sendAlert() {
        String description = binding.etDescription.getText().toString().trim();
        if (description.isEmpty()) {
            binding.etDescription.setError("Décrivez le problème");
            return;
        }

        // Read order number from field (user may have edited it)
        String orderNumber = binding.etOrderNumber.getText().toString().trim();

        binding.btnSendAlert.setEnabled(false);
        binding.btnSendAlert.setText("Envoi en cours...");

        // Build location string for the alert
        String locString = (latitude != 0 && longitude != 0)
                ? String.format("%.5f, %.5f", latitude, longitude)
                : "Position non disponible";

        AlertRequest request = new AlertRequest(
                currentDeliveryId,
                selectedAlertType,
                description,
                latitude,
                longitude,
                locString
        );

        apiService.sendAlert(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> r) {
                binding.btnSendAlert.setEnabled(true);
                binding.btnSendAlert.setText("🚨  Envoyer l'alerte");
                if (r.isSuccessful()) {
                    Toast.makeText(EmergencyActivity.this,
                            "Alerte envoyée au contrôleur !", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(EmergencyActivity.this,
                            "Erreur envoi alerte", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                binding.btnSendAlert.setEnabled(true);
                binding.btnSendAlert.setText("🚨  Envoyer l'alerte");
                Toast.makeText(EmergencyActivity.this,
                        "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNav() {
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_emergency);
        suppressNavListener = false;

        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (suppressNavListener) return true;
            int id = item.getItemId();
            if (id == R.id.nav_my_deliveries) { goHome(); return true; }
            if (id == R.id.nav_emergency)     { return true; }
            if (id == R.id.nav_messaging)     { goTo(MessagingActivity.class); return true; }
            if (id == R.id.nav_profile)       { goTo(ProfileActivity.class);   return true; }
            return false;
        });
    }

    private void goHome() {
        Intent i = new Intent(this, DelivererMainActivity.class);
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
    protected void onDestroy() { super.onDestroy(); binding = null; }
}
/*package com.deliverpro.app.ui.deliverer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityEmergencyBinding;
import com.deliverpro.app.model.AlertRequest;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;
import com.deliverpro.app.ui.controller.MessagingActivity;
import com.deliverpro.app.ui.controller.ProfileActivity;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmergencyActivity extends AppCompatActivity {

    private ActivityEmergencyBinding binding;
    private ApiService apiService;
    private String selectedAlertType = "ACCIDENT";
    private boolean suppressNavListener = false;

    private double latitude  = 0;
    private double longitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityEmergencyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getClient(this).create(ApiService.class);

        // Auto-fill numéro de commande passé depuis DriverStopDetailActivity
        String orderNumber = getIntent().getStringExtra("order_number");
        if (orderNumber != null && !orderNumber.isEmpty()) {
            binding.etOrderNumber.setText(orderNumber);
        }

        setupAlertTypeChips();
        setupBottomNav();

        binding.tvLocation.setText("Position détectée automatiquement");
        binding.btnSendAlert.setOnClickListener(v -> sendAlert());
    }

    @Override
    protected void onResume() {
        super.onResume();
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_emergency);
        suppressNavListener = false;
    }

    private void setupAlertTypeChips() {
        List<TextView> chips = Arrays.asList(
                binding.chipAccident, binding.chipClientIssue,
                binding.chipVehicle, binding.chipAddress, binding.chipSafety);
        List<String> types = Arrays.asList(
                "ACCIDENT", "CLIENT_ISSUE", "VEHICLE_BREAKDOWN",
                "ADDRESS_NOT_FOUND", "SAFETY_CONCERN");

        for (int i = 0; i < chips.size(); i++) {
            final String type = types.get(i);
            chips.get(i).setOnClickListener(v -> {
                selectedAlertType = type;
                for (TextView c : chips) {
                    c.setBackgroundResource(R.drawable.bg_chip);
                    c.setTextColor(Color.parseColor("#888888"));
                }
                ((TextView) v).setBackgroundResource(R.drawable.bg_chip_selected);
                ((TextView) v).setTextColor(Color.parseColor("#1A56A4"));
            });
        }
    }

    private void sendAlert() {
        String description = binding.etDescription.getText().toString().trim();
        if (description.isEmpty()) {
            binding.etDescription.setError("Décrivez le problème");
            return;
        }

        binding.btnSendAlert.setEnabled(false);
        binding.btnSendAlert.setText("Envoi en cours...");

        AlertRequest request = new AlertRequest(
                0, selectedAlertType, description,
                latitude, longitude, binding.tvLocation.getText().toString());

        apiService.sendAlert(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> r) {
                binding.btnSendAlert.setEnabled(true);
                binding.btnSendAlert.setText("🚨  Send Emergency Alert");
                if (r.isSuccessful()) {
                    Toast.makeText(EmergencyActivity.this,
                            "Alerte envoyée au contrôleur !", Toast.LENGTH_LONG).show();
                    goHome();
                } else {
                    Toast.makeText(EmergencyActivity.this,
                            "Erreur envoi alerte", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                binding.btnSendAlert.setEnabled(true);
                binding.btnSendAlert.setText("🚨  Send Emergency Alert");
                Toast.makeText(EmergencyActivity.this,
                        "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNav() {
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_emergency);
        suppressNavListener = false;

        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (suppressNavListener) return true;
            int id = item.getItemId();

            if (id == R.id.nav_my_deliveries) {
                goHome();
                return true;
            }
            if (id == R.id.nav_emergency) {
                return true; // Déjà ici
            }
            if (id == R.id.nav_messaging) {
                // CORRECTION clé : FLAG_ACTIVITY_NEW_TASK + CLEAR_TASK
                // crée toujours une nouvelle instance propre
                // au lieu de remonter une ancienne depuis la pile
                Intent i = new Intent(this, MessagingActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                return true;
            }
            if (id == R.id.nav_profile) {
                Intent i = new Intent(this, ProfileActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                return true;
            }
            return false;
        });
    }

    private void goHome() {
        Intent i = new Intent(this, DelivererMainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    @Override
    protected void onDestroy() { super.onDestroy(); binding = null; }
}*/
/*package com.deliverpro.app.ui.deliverer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityEmergencyBinding;
import com.deliverpro.app.model.AlertRequest;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;
import com.deliverpro.app.ui.controller.MessagingActivity;
import com.deliverpro.app.ui.controller.ProfileActivity;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmergencyActivity extends AppCompatActivity {

    private ActivityEmergencyBinding binding;
    private ApiService apiService;
    private String selectedAlertType = "ACCIDENT";
    private boolean suppressNavListener = false;

    private double latitude  = 0;
    private double longitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityEmergencyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getClient(this).create(ApiService.class);

        String orderNumber = getIntent().getStringExtra("order_number");
        if (orderNumber != null) binding.etOrderNumber.setText(orderNumber);

        setupAlertTypeChips();
        setupBottomNav();

        binding.tvLocation.setText("Position détectée automatiquement");
        binding.btnSendAlert.setOnClickListener(v -> sendAlert());
    }

    @Override
    protected void onResume() {
        super.onResume();
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_emergency);
        suppressNavListener = false;
    }

    private void setupAlertTypeChips() {
        List<TextView> chips = Arrays.asList(
                binding.chipAccident, binding.chipClientIssue,
                binding.chipVehicle, binding.chipAddress, binding.chipSafety);
        List<String> types = Arrays.asList(
                "ACCIDENT", "CLIENT_ISSUE", "VEHICLE_BREAKDOWN",
                "ADDRESS_NOT_FOUND", "SAFETY_CONCERN");

        for (int i = 0; i < chips.size(); i++) {
            final String type = types.get(i);
            chips.get(i).setOnClickListener(v -> {
                selectedAlertType = type;
                for (TextView c : chips) {
                    c.setBackgroundResource(R.drawable.bg_chip);
                    c.setTextColor(Color.parseColor("#888888"));
                }
                ((TextView) v).setBackgroundResource(R.drawable.bg_chip_selected);
                ((TextView) v).setTextColor(Color.parseColor("#1A56A4"));
            });
        }
    }

    private void sendAlert() {
        String description = binding.etDescription.getText().toString().trim();
        if (description.isEmpty()) {
            binding.etDescription.setError("Décrivez le problème");
            return;
        }

        binding.btnSendAlert.setEnabled(false);
        binding.btnSendAlert.setText("Envoi en cours...");

        AlertRequest request = new AlertRequest(
                0, selectedAlertType, description,
                latitude, longitude, binding.tvLocation.getText().toString());

        apiService.sendAlert(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> r) {
                binding.btnSendAlert.setEnabled(true);
                binding.btnSendAlert.setText("🚨  Send Emergency Alert");
                if (r.isSuccessful()) {
                    Toast.makeText(EmergencyActivity.this,
                            "Alerte envoyée au contrôleur !", Toast.LENGTH_LONG).show();
                    finish(); // Go back to previous screen after sending
                } else {
                    Toast.makeText(EmergencyActivity.this,
                            "Erreur envoi alerte", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                binding.btnSendAlert.setEnabled(true);
                binding.btnSendAlert.setText("🚨  Send Emergency Alert");
                Toast.makeText(EmergencyActivity.this,
                        "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNav() {
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_emergency);
        suppressNavListener = false;

        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (suppressNavListener) return true;
            int id = item.getItemId();
            if (id == R.id.nav_my_deliveries) { goHome(); return true; }
            if (id == R.id.nav_emergency)     { return true; }
            if (id == R.id.nav_messaging)     { goTo(MessagingActivity.class); return true; }
            if (id == R.id.nav_profile)       { goTo(ProfileActivity.class);   return true; }
            return false;
        });
    }

    private void goHome() {
        Intent i = new Intent(this, DelivererMainActivity.class);
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
    protected void onDestroy() { super.onDestroy(); binding = null; }
}
*/
/*// ui/deliverer/EmergencyActivity.java — CORRIGÉ
// Problèmes résolus :
// 1. "Mes commandes" sélectionné au lieu de "Urgence"
// 2. Clic sur Messagerie/Profil bloqué — ne faisait rien
// 3. Clic sur "Mes commandes" ouvrait le dernier écran visité au lieu de la tournée

package com.deliverpro.app.ui.deliverer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityEmergencyBinding;
import com.deliverpro.app.model.AlertRequest;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;
import com.deliverpro.app.ui.controller.MessagingActivity;
import com.deliverpro.app.ui.controller.ProfileActivity;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmergencyActivity extends AppCompatActivity {

    private ActivityEmergencyBinding binding;
    private ApiService apiService;
    private String selectedAlertType = "ACCIDENT";

    private double latitude  = 0;
    private double longitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityEmergencyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getClient(this).create(ApiService.class);

        String orderNumber = getIntent().getStringExtra("order_number");
        if (orderNumber != null) binding.etOrderNumber.setText(orderNumber);

        setupAlertTypeChips();
        setupBottomNav();

        binding.tvLocation.setText("Position détectée automatiquement");
        binding.btnSendAlert.setOnClickListener(v -> sendAlert());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // CORRECTION 1 : forcer la sélection de l'onglet "Urgence"
        binding.bottomNav.setSelectedItemId(R.id.nav_emergency);
    }

    private void setupAlertTypeChips() {
        List<TextView> chips = Arrays.asList(
                binding.chipAccident, binding.chipClientIssue,
                binding.chipVehicle, binding.chipAddress, binding.chipSafety);
        List<String> types = Arrays.asList(
                "ACCIDENT", "CLIENT_ISSUE", "VEHICLE_BREAKDOWN",
                "ADDRESS_NOT_FOUND", "SAFETY_CONCERN");

        for (int i = 0; i < chips.size(); i++) {
            final String type = types.get(i);
            chips.get(i).setOnClickListener(v -> {
                selectedAlertType = type;
                for (TextView c : chips) {
                    c.setBackgroundResource(R.drawable.bg_chip);
                    c.setTextColor(Color.parseColor("#888888"));
                }
                ((TextView) v).setBackgroundResource(R.drawable.bg_chip_selected);
                ((TextView) v).setTextColor(Color.parseColor("#1A56A4"));
            });
        }
    }

    private void sendAlert() {
        String description = binding.etDescription.getText().toString().trim();

        if (description.isEmpty()) {
            binding.etDescription.setError("Décrivez le problème");
            return;
        }

        binding.btnSendAlert.setEnabled(false);
        binding.btnSendAlert.setText("Envoi en cours...");

        AlertRequest request = new AlertRequest(
                0, selectedAlertType, description,
                latitude, longitude, binding.tvLocation.getText().toString());

        apiService.sendAlert(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> r) {
                binding.btnSendAlert.setEnabled(true);
                binding.btnSendAlert.setText("🚨  Send Emergency Alert");
                if (r.isSuccessful()) {
                    Toast.makeText(EmergencyActivity.this,
                            "Alerte envoyée au contrôleur !", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(EmergencyActivity.this,
                            "Erreur envoi alerte", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                binding.btnSendAlert.setEnabled(true);
                binding.btnSendAlert.setText("🚨  Send Emergency Alert");
                Toast.makeText(EmergencyActivity.this,
                        "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNav() {
        // Pas de setSelectedItemId ici — fait dans onResume()
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_my_deliveries) {
                // CORRECTION 3 : naviguer vers DelivererMainActivity directement
                // au lieu de faire finish() qui ouvre le dernier écran visité
                Intent i = new Intent(this, DelivererMainActivity.class);
                // FLAG_ACTIVITY_CLEAR_TOP ferme tous les écrans au dessus
                // et ramène à DelivererMainActivity proprement
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                return true;
            }

            if (id == R.id.nav_emergency) {
                // Déjà sur cet écran — ne rien faire
                return true;
            }

            // CORRECTION 2 : Messagerie et Profil étaient bloqués
            // car ils n'étaient pas gérés dans le listener
            if (id == R.id.nav_messaging) {
                Intent i = new Intent(this, MessagingActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                return true;
            }

            if (id == R.id.nav_profile) {
                Intent i = new Intent(this, ProfileActivity.class);
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