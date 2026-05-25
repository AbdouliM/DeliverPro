package com.deliverpro.app.ui.controller;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityControllerMainBinding;
import com.deliverpro.app.utils.SessionManager;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ControllerMainActivity extends AppCompatActivity {

    private ActivityControllerMainBinding binding;
    private SessionManager session;
    private boolean suppressNavListener = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityControllerMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        session = SessionManager.getInstance(this);
        setupToolbar();
        setupTabs();
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        suppressNavListener = false;
    }

    private void setupToolbar() {
        String date = new SimpleDateFormat("EEEE, d MMM", new Locale("fr")).format(new Date());
        binding.tvToolbarDate.setText(date);
        String fullName = session.getFullName();
        if (fullName != null) {
            String[] parts = fullName.split(" ");
            String initials = parts.length >= 2
                    ? String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)
                    : fullName.substring(0, Math.min(2, fullName.length()));
            binding.tvAvatar.setText(initials.toUpperCase());
        }
    }

    private void setupTabs() {
        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @Override public int getItemCount() { return 2; }
            @Override
            public Fragment createFragment(int position) {
                return position == 0
                        ? new DashboardByDelivererFragment()
                        : new DashboardByClientFragment();
            }
        };
        binding.viewPager.setAdapter(adapter);
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(position == 0 ? "Par livreur" : "Par client")
        ).attach();
    }

    private void setupBottomNav() {
        suppressNavListener = true;
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        suppressNavListener = false;

        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (suppressNavListener) return true;
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                binding.viewPager.setCurrentItem(0);
                return true;
            }
            if (id == R.id.nav_deliveries) { goTo(DeliveriesActivity.class); return true; }
            if (id == R.id.nav_messages)   { goTo(MessagingActivity.class);  return true; }
            if (id == R.id.nav_profile)    { goTo(ProfileActivity.class);    return true; }
            return false;
        });
    }

    // Always start a fresh instance — no stacking, no reordering
    private void goTo(Class<?> cls) {
        Intent i = new Intent(this, cls);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    protected void onDestroy() { super.onDestroy(); binding = null; }
}
/*// ui/controller/ControllerMainActivity.java — CORRIGÉ
package com.deliverpro.app.ui.controller;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityControllerMainBinding;
import com.deliverpro.app.utils.SessionManager;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ControllerMainActivity extends AppCompatActivity {

    private ActivityControllerMainBinding binding;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityControllerMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        session = SessionManager.getInstance(this);
        setupToolbar();
        setupTabs();
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Remettre l'onglet Accueil actif quand on revient sur cet écran
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
    }

    private void setupToolbar() {
        String date = new SimpleDateFormat("EEEE, d MMM", new Locale("fr")).format(new Date());
        binding.tvToolbarDate.setText(date);

        String fullName = session.getFullName();
        if (fullName != null && fullName.length() >= 2) {
            String[] parts = fullName.split(" ");
            String initials = parts.length >= 2
                    ? String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)
                    : fullName.substring(0, 2);
            binding.tvAvatar.setText(initials.toUpperCase());
        }
    }

    private void setupTabs() {
        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @Override public int getItemCount() { return 2; }

            @Override
            public Fragment createFragment(int position) {
                return position == 0
                        ? new DashboardByDelivererFragment()
                        : new DashboardByClientFragment();
            }
        };
        binding.viewPager.setAdapter(adapter);
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) ->
                tab.setText(position == 0 ? "Par livreur" : "Par client")
        ).attach();
    }

    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_home);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // Déjà sur cet écran — pas de nouvelle Activity
                binding.viewPager.setCurrentItem(0);
                binding.tvToolbarTitle.setText("Tableau de bord");
                return true;

            } else if (id == R.id.nav_deliveries) {
                Intent i = new Intent(this, DeliveriesActivity.class);
                // Éviter d'empiler plusieurs copies de la même Activity
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
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

    @Override
    protected void onDestroy() { super.onDestroy(); binding = null; }
}*/