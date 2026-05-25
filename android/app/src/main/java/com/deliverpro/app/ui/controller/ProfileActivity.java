package com.deliverpro.app.ui.controller;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityProfileBinding;
import com.deliverpro.app.model.UserProfile;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;
import com.deliverpro.app.ui.auth.LoginActivity;
import com.deliverpro.app.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private SessionManager session;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session    = SessionManager.getInstance(this);
        apiService = ApiClient.getClient(this).create(ApiService.class);

        bindLocalData();
        setupBottomNav();
        fetchFullProfile();

        binding.btnSettings.setOnClickListener(v ->
                Toast.makeText(this, "Paramètres — à venir", Toast.LENGTH_SHORT).show());
        binding.btnChangePassword.setOnClickListener(v ->
                Toast.makeText(this, "Changement de mot de passe — à venir",
                        Toast.LENGTH_SHORT).show());
        binding.btnLogout.setOnClickListener(v -> confirmLogout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.bottomNav.setSelectedItemId(R.id.nav_profile);
    }

    private void bindLocalData() {
        String fullName = session.getFullName();
        String phone    = session.getPhone();
        String role     = session.getRole();
        String username = session.getUsername();

        if (fullName != null && fullName.contains(" ")) {
            String[] p = fullName.split(" ");
            binding.tvAvatarLarge.setText(
                    ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase());
        }
        binding.tvFullName.setText(fullName != null ? fullName : "—");
        binding.tvRoleId.setText("CONTROLLER".equals(role) ? "Contrôleur" : "Livreur");
        binding.tvPhone.setText(phone != null ? phone : "—");
        binding.tvUsername.setText(username != null ? username : "—");
        binding.tvAddress.setText("Chargement...");
        binding.tvHireDate.setText("Chargement...");
    }

    private void fetchFullProfile() {
        apiService.getProfile().enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    UserProfile p = response.body();
                    binding.tvAddress.setText(
                            p.getAddress() != null && !p.getAddress().isEmpty()
                                    ? p.getAddress() : "—");
                    if (p.getHireDate() != null && p.getHireDate().length() >= 10) {
                        binding.tvHireDate.setText(
                                formatDate(p.getHireDate().substring(0, 10)));
                    } else {
                        binding.tvHireDate.setText("—");
                    }
                } else {
                    binding.tvAddress.setText("—");
                    binding.tvHireDate.setText("—");
                }
            }
            @Override
            public void onFailure(Call<UserProfile> call, Throwable t) {
                if (binding == null) return;
                binding.tvAddress.setText("—");
                binding.tvHireDate.setText("—");
            }
        });
    }

    // "2020-01-10" → "10 janvier 2020"
    private String formatDate(String iso) {
        try {
            String[] p = iso.split("-");
            String[] months = {"janvier","février","mars","avril","mai","juin",
                    "juillet","août","septembre","octobre","novembre","décembre"};
            return p[2] + " " + months[Integer.parseInt(p[1]) - 1] + " " + p[0];
        } catch (Exception e) { return iso; }
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Se déconnecter")
                .setMessage("Voulez-vous vraiment vous déconnecter ?")
                .setPositiveButton("Oui", (d, w) -> {
                    session.logout();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .setNegativeButton("Annuler", null).show();
    }

    private void setupBottomNav() {
        binding.bottomNav.getMenu().clear();
        binding.bottomNav.inflateMenu(session.isController()
                ? R.menu.menu_controller_bottom : R.menu.menu_deliverer_bottom);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home || id == R.id.nav_my_deliveries) {
                finish(); return true;
            }
            if (id == R.id.nav_deliveries) {
                startActivity(new Intent(this, DeliveriesActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)); return true;
            }
            if (id == R.id.nav_messages || id == R.id.nav_messaging) {
                startActivity(new Intent(this, MessagingActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)); return true;
            }
            if (id == R.id.nav_emergency) {
                startActivity(new Intent(this,
                        com.deliverpro.app.ui.deliverer.EmergencyActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)); return true;
            }
            if (id == R.id.nav_profile) { return true; }
            return false;
        });
    }

    @Override
    protected void onDestroy() { super.onDestroy(); binding = null; }
}
/*package com.deliverpro.app.ui.controller;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityProfileBinding;
import com.deliverpro.app.ui.auth.LoginActivity;
import com.deliverpro.app.utils.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private SessionManager session;
    private boolean suppressNavListener = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        session = SessionManager.getInstance(this);
        bindUI();
        setupBottomNav();

        binding.btnSettings.setOnClickListener(v ->
                Toast.makeText(this, "Paramètres — à venir", Toast.LENGTH_SHORT).show());
        binding.btnChangePassword.setOnClickListener(v ->
                Toast.makeText(this, "Changement de mot de passe — à venir", Toast.LENGTH_SHORT).show());
        binding.btnLogout.setOnClickListener(v -> confirmLogout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_profile);
        suppressNavListener = false;
    }

    private void bindUI() {
        String fullName = session.getFullName();
        String username = session.getUsername();
        String phone    = session.getPhone();
        String role     = session.getRole();
        if (fullName != null && fullName.contains(" ")) {
            String[] p = fullName.split(" ");
            binding.tvAvatarLarge.setText(("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase());
        }
        binding.tvFullName.setText(fullName != null ? fullName : "—");
        binding.tvRoleId.setText("CONTROLLER".equals(role) ? "Contrôleur" : "Livreur");
        binding.tvPhone.setText(phone != null ? phone : "—");
        binding.tvUsername.setText(username != null ? username : "—");
        binding.tvAddress.setText("—");
        binding.tvHireDate.setText("—");
    }

    private void confirmLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Se déconnecter")
                .setMessage("Voulez-vous vraiment vous déconnecter ?")
                .setPositiveButton("Oui", (d, w) -> {
                    session.logout();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void setupBottomNav() {
        binding.bottomNav.getMenu().clear();
        binding.bottomNav.inflateMenu(session.isController()
                ? R.menu.menu_controller_bottom
                : R.menu.menu_deliverer_bottom);

        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_profile);
        suppressNavListener = false;

        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (suppressNavListener) return true;
            int id = item.getItemId();
            if (id == R.id.nav_home || id == R.id.nav_my_deliveries) { goHome(); return true; }
            if (id == R.id.nav_deliveries) { goTo(DeliveriesActivity.class); return true; }
            if (id == R.id.nav_messages || id == R.id.nav_messaging) { goTo(MessagingActivity.class); return true; }
            if (id == R.id.nav_emergency) {
                goTo(com.deliverpro.app.ui.deliverer.EmergencyActivity.class); return true;
            }
            if (id == R.id.nav_profile) { return true; }
            return false;
        });
    }

    private void goHome() {
        // Clear everything above home and go back to it
        Class<?> homeClass = session.isController()
                ? ControllerMainActivity.class
                : com.deliverpro.app.ui.deliverer.DelivererMainActivity.class;
        Intent i = new Intent(this, homeClass);
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
}*/
/*// ui/controller/ProfileActivity.java — CORRIGÉ
// Problème résolu : mauvais onglet sélectionné (nav_messages au lieu de nav_profile)
package com.deliverpro.app.ui.controller;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityProfileBinding;
import com.deliverpro.app.ui.auth.LoginActivity;
import com.deliverpro.app.utils.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = SessionManager.getInstance(this);

        bindUI();
        setupBottomNav();

        binding.btnSettings.setOnClickListener(v ->
                Toast.makeText(this, "Paramètres — à venir", Toast.LENGTH_SHORT).show());

        binding.btnChangePassword.setOnClickListener(v ->
                Toast.makeText(this, "Changement de mot de passe — à venir",
                        Toast.LENGTH_SHORT).show());

        binding.btnLogout.setOnClickListener(v -> confirmLogout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Corriger l'onglet sélectionné à chaque fois
        binding.bottomNav.setSelectedItemId(R.id.nav_profile);
    }

    private void bindUI() {
        String fullName = session.getFullName();
        String username = session.getUsername();
        String phone    = session.getPhone();
        String role     = session.getRole();

        if (fullName != null && fullName.contains(" ")) {
            String[] p = fullName.split(" ");
            binding.tvAvatarLarge.setText(
                    ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase());
        }

        binding.tvFullName.setText(fullName != null ? fullName : "—");
        String roleLabel = "CONTROLLER".equals(role) ? "Contrôleur" : "Livreur";
        binding.tvRoleId.setText(roleLabel);
        binding.tvPhone.setText(phone != null ? phone : "—");
        binding.tvUsername.setText(username != null ? username : "—");
        binding.tvAddress.setText("—");
        binding.tvHireDate.setText("—");
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Se déconnecter")
                .setMessage("Voulez-vous vraiment vous déconnecter ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    session.logout();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void setupBottomNav() {
        // Menu selon le rôle
        binding.bottomNav.getMenu().clear();
        boolean isController = session.isController();
        binding.bottomNav.inflateMenu(isController
                ? R.menu.menu_controller_bottom
                : R.menu.menu_deliverer_bottom);

        // Pas de setSelectedItemId ici — fait dans onResume()

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // Retour accueil selon le rôle
            if (id == R.id.nav_home) {
                finish(); return true;
            }
            if (id == R.id.nav_my_deliveries) {
                finish(); return true;
            }

            if (id == R.id.nav_deliveries) {
                Intent i = new Intent(this, DeliveriesActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i); return true;
            }

            if (id == R.id.nav_messages || id == R.id.nav_messaging) {
                Intent i = new Intent(this, MessagingActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i); return true;
            }

            if (id == R.id.nav_emergency) {
                Intent i = new Intent(this,
                        com.deliverpro.app.ui.deliverer.EmergencyActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i); return true;
            }

            if (id == R.id.nav_profile) {
                // Déjà sur cet écran
                return true;
            }

            return false;
        });
    }

    @Override
    protected void onDestroy() { super.onDestroy(); binding = null; }
}*/