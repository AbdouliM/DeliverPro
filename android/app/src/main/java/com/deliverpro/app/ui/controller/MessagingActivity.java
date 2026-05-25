package com.deliverpro.app.ui.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityMessagingListBinding;
import com.deliverpro.app.model.Message;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;
import com.deliverpro.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessagingActivity extends AppCompatActivity {

    private ActivityMessagingListBinding binding;
    private ApiService apiService;
    private ConversationAdapter adapter;
    private SessionManager session;
    private boolean suppressNavListener = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityMessagingListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        apiService = ApiClient.getClient(this).create(ApiService.class);
        session    = SessionManager.getInstance(this);

        adapter = new ConversationAdapter(conv -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("receiver_id",    conv.getUserId());
            intent.putExtra("receiver_name",  conv.getFullName());
            //intent.putExtra("receiver_phone", conv.getPhone());
            startActivity(intent);
        }, session.isController()); // pass role so adapter knows when to show 🚨
        binding.rvConversations.setLayoutManager(new LinearLayoutManager(this));
        binding.rvConversations.setAdapter(adapter);

        setupBottomNav();
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        suppressNavListener = true;
        int tabId = session.isController() ? R.id.nav_messages : R.id.nav_messaging;
        binding.bottomNav.setSelectedItemId(tabId);
        suppressNavListener = false;
        loadData();
    }

    private void loadData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        apiService.getConversations().enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> r) {
                binding.progressBar.setVisibility(View.GONE);
                List<Message> convos = (r.isSuccessful() && r.body() != null)
                        ? r.body() : new ArrayList<>();
                adapter.setData(convos);
            }
            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(MessagingActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void setupBottomNav() {
        // Inflate correct menu based on role
        binding.bottomNav.getMenu().clear();
        if (session.isController()) {
            binding.bottomNav.inflateMenu(R.menu.menu_controller_bottom);
        } else {
            binding.bottomNav.inflateMenu(R.menu.menu_deliverer_bottom);
        }
        suppressNavListener = true;
        int tabId = session.isController() ? R.id.nav_messages : R.id.nav_messaging;
        binding.bottomNav.setSelectedItemId(tabId);
        suppressNavListener = false;

        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (suppressNavListener) return true;
            int id = item.getItemId();
            if (id == R.id.nav_home || id == R.id.nav_my_deliveries) { goHome(); return true; }
            if (id == R.id.nav_deliveries) { goTo(DeliveriesActivity.class); return true; }
            if (id == R.id.nav_messages || id == R.id.nav_messaging) { return true; }
            if (id == R.id.nav_emergency) {
                goTo(com.deliverpro.app.ui.deliverer.EmergencyActivity.class); return true;
            }
            if (id == R.id.nav_profile) { goTo(ProfileActivity.class); return true; }
            return false;
        });
    }

    private void goHome() {
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
}
/*package com.deliverpro.app.ui.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityMessagingListBinding;
import com.deliverpro.app.model.Alert;
import com.deliverpro.app.model.Message;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;
import com.deliverpro.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessagingActivity extends AppCompatActivity {

    private ActivityMessagingListBinding binding;
    private ApiService apiService;
    private ConversationAdapter adapter;
    private SessionManager session;
    private boolean suppressNavListener = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityMessagingListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        apiService = ApiClient.getClient(this).create(ApiService.class);
        session    = SessionManager.getInstance(this);

        adapter = new ConversationAdapter(conv -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("receiver_id",   conv.getUserId());
            intent.putExtra("receiver_name", conv.getFullName());
            startActivity(intent);
        });
        binding.rvConversations.setLayoutManager(new LinearLayoutManager(this));
        binding.rvConversations.setAdapter(adapter);

        setupBottomNav();
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        suppressNavListener = true;
        int tabId = session.isController() ? R.id.nav_messages : R.id.nav_messaging;
        binding.bottomNav.setSelectedItemId(tabId);
        suppressNavListener = false;
        loadData();
    }

    private void loadData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        apiService.getConversations().enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> r) {
                List<Message> convos = (r.isSuccessful() && r.body() != null)
                        ? r.body() : new ArrayList<>();
                if (session.isDeliverer()) {
                    loadAlertsThenMerge(convos);
                } else {
                    binding.progressBar.setVisibility(View.GONE);
                    adapter.setData(convos);
                }
            }
            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(MessagingActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAlertsThenMerge(List<Message> convos) {
        apiService.getUnreadAlerts().enqueue(new Callback<List<Alert>>() {
            @Override
            public void onResponse(Call<List<Alert>> call, Response<List<Alert>> r) {
                binding.progressBar.setVisibility(View.GONE);
                List<Alert> alerts = (r.isSuccessful() && r.body() != null)
                        ? r.body() : new ArrayList<>();
                adapter.setData(merge(convos, alerts));
            }
            @Override
            public void onFailure(Call<List<Alert>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                adapter.setData(convos);
            }
        });
    }

    private List<Message> merge(List<Message> convos, List<Alert> alerts) {
        if (alerts.isEmpty()) return convos;
        Map<Integer, Alert> alertMap = new HashMap<>();
        for (Alert a : alerts) alertMap.put(a.getSenderId(), a);

        Set<Integer> matched = new HashSet<>();
        List<Message> urgent = new ArrayList<>();
        List<Message> normal = new ArrayList<>();

        for (Message conv : convos) {
            Alert a = alertMap.get(conv.getUserId());
            if (a != null) {
                conv.setUrgent(true);
                conv.setLastMessage(a.getDescription());
                conv.setLastSentAt(a.getSentAt());
                matched.add(conv.getUserId());
                urgent.add(conv);
            } else {
                normal.add(conv);
            }
        }
        for (Alert a : alerts) {
            if (!matched.contains(a.getSenderId())) {
                Message synthetic = new Message();
                synthetic.setUserId(a.getSenderId());
                synthetic.setFullName(a.getSenderName());
                synthetic.setUrgent(true);
                synthetic.setLastMessage(a.getDescription());
                synthetic.setLastSentAt(a.getSentAt());
                urgent.add(synthetic);
            }
        }
        List<Message> merged = new ArrayList<>();
        merged.addAll(urgent);
        merged.addAll(normal);
        return merged;
    }

    private void setupBottomNav() {
        // Inflate correct menu based on role
        binding.bottomNav.getMenu().clear();
        if (session.isController()) {
            binding.bottomNav.inflateMenu(R.menu.menu_controller_bottom);
        } else {
            binding.bottomNav.inflateMenu(R.menu.menu_deliverer_bottom);
        }
        suppressNavListener = true;
        int tabId = session.isController() ? R.id.nav_messages : R.id.nav_messaging;
        binding.bottomNav.setSelectedItemId(tabId);
        suppressNavListener = false;

        binding.bottomNav.setOnItemSelectedListener(item -> {
            if (suppressNavListener) return true;
            int id = item.getItemId();
            if (id == R.id.nav_home || id == R.id.nav_my_deliveries) { goHome(); return true; }
            if (id == R.id.nav_deliveries) { goTo(DeliveriesActivity.class); return true; }
            if (id == R.id.nav_messages || id == R.id.nav_messaging) { return true; }
            if (id == R.id.nav_emergency) {
                goTo(com.deliverpro.app.ui.deliverer.EmergencyActivity.class); return true;
            }
            if (id == R.id.nav_profile) { goTo(ProfileActivity.class); return true; }
            return false;
        });
    }

    private void goHome() {
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
/*package com.deliverpro.app.ui.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityMessagingListBinding;
import com.deliverpro.app.model.Message;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;
import com.deliverpro.app.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessagingActivity extends AppCompatActivity {

    private ActivityMessagingListBinding binding;
    private ApiService apiService;
    private ConversationAdapter adapter;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityMessagingListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        apiService = ApiClient.getClient(this).create(ApiService.class);
        session    = SessionManager.getInstance(this);

        adapter = new ConversationAdapter(conv -> {
            Intent intent = new Intent(this, ChatActivity.class);
            // FIX: use getUserId() — the contact's user ID from the conversations API,
            // NOT getSenderId() which is the last message sender and is 0 when no messages exist.
            intent.putExtra("receiver_id",   conv.getUserId());
            intent.putExtra("receiver_name", conv.getFullName());
            startActivity(intent);
        });
        binding.rvConversations.setLayoutManager(new LinearLayoutManager(this));
        binding.rvConversations.setAdapter(adapter);

        setupBottomNav();
        loadConversations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConversations();
        if (session.isController()) {
            binding.bottomNav.setSelectedItemId(R.id.nav_messages);
        } else {
            binding.bottomNav.setSelectedItemId(R.id.nav_messaging);
        }
    }

    private void loadConversations() {
        binding.progressBar.setVisibility(View.VISIBLE);
        apiService.getConversations().enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call,
                                   Response<List<Message>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setData(response.body());
                } else {
                    Toast.makeText(MessagingActivity.this,
                            "Erreur chargement messages", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(MessagingActivity.this,
                        "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNav() {
        binding.bottomNav.getMenu().clear();
        if (session.isController()) {
            binding.bottomNav.inflateMenu(R.menu.menu_controller_bottom);
            setupControllerNav();
        } else {
            binding.bottomNav.inflateMenu(R.menu.menu_deliverer_bottom);
            setupDelivererNav();
        }
    }

    private void setupControllerNav() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { finish(); return true; }
            if (id == R.id.nav_deliveries) {
                Intent i = new Intent(this, DeliveriesActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i); return true;
            }
            if (id == R.id.nav_messages) { return true; }
            if (id == R.id.nav_profile) {
                Intent i = new Intent(this, ProfileActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i); return true;
            }
            return false;
        });
    }

    private void setupDelivererNav() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_my_deliveries) { finish(); return true; }
            if (id == R.id.nav_messaging) { return true; }
            if (id == R.id.nav_emergency) {
                Intent i = new Intent(this,
                        com.deliverpro.app.ui.deliverer.EmergencyActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i); return true;
            }
            if (id == R.id.nav_profile) {
                Intent i = new Intent(this, ProfileActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i); return true;
            }
            return false;
        });
    }

    @Override
    protected void onDestroy() { super.onDestroy(); binding = null; }
}*/
/*// ui/controller/MessagingActivity.java — CORRIGÉ
// Problème résolu : menu contrôleur affiché même pour le livreur
package com.deliverpro.app.ui.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.deliverpro.app.R;
import com.deliverpro.app.databinding.ActivityMessagingListBinding;
import com.deliverpro.app.model.Message;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;
import com.deliverpro.app.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessagingActivity extends AppCompatActivity {

    private ActivityMessagingListBinding binding;
    private ApiService apiService;
    private ConversationAdapter adapter;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityMessagingListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        apiService = ApiClient.getClient(this).create(ApiService.class);
        session    = SessionManager.getInstance(this);

        adapter = new ConversationAdapter(conv -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("receiver_id",   conv.getSenderId());
            intent.putExtra("receiver_name", conv.getFullName());
            startActivity(intent);
        });
        binding.rvConversations.setLayoutManager(new LinearLayoutManager(this));
        binding.rvConversations.setAdapter(adapter);

        setupBottomNav();
        loadConversations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConversations();
        // Corriger l'onglet sélectionné selon le rôle
        if (session.isController()) {
            binding.bottomNav.setSelectedItemId(R.id.nav_messages);
        } else {
            binding.bottomNav.setSelectedItemId(R.id.nav_messaging);
        }
    }

    private void loadConversations() {
        binding.progressBar.setVisibility(View.VISIBLE);
        apiService.getConversations().enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call,
                                   Response<List<Message>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setData(response.body());
                } else {
                    Toast.makeText(MessagingActivity.this,
                            "Erreur chargement messages", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(MessagingActivity.this,
                        "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNav() {
        // ── CORRECTION PRINCIPALE : menu selon le rôle ────────
        binding.bottomNav.getMenu().clear();
        if (session.isController()) {
            binding.bottomNav.inflateMenu(R.menu.menu_controller_bottom);
            setupControllerNav();
        } else {
            binding.bottomNav.inflateMenu(R.menu.menu_deliverer_bottom);
            setupDelivererNav();
        }
    }

    private void setupControllerNav() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finish(); return true;
            }
            if (id == R.id.nav_deliveries) {
                Intent i = new Intent(this, DeliveriesActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i); return true;
            }
            if (id == R.id.nav_messages) {
                // Déjà sur cet écran
                return true;
            }
            if (id == R.id.nav_profile) {
                Intent i = new Intent(this, ProfileActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i); return true;
            }
            return false;
        });
    }

    private void setupDelivererNav() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_my_deliveries) {
                finish(); return true;
            }
            if (id == R.id.nav_messaging) {
                // Déjà sur cet écran
                return true;
            }
            if (id == R.id.nav_emergency) {
                Intent i = new Intent(this,
                        com.deliverpro.app.ui.deliverer.EmergencyActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i); return true;
            }
            if (id == R.id.nav_profile) {
                Intent i = new Intent(this, ProfileActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i); return true;
            }
            return false;
        });
    }

    @Override
    protected void onDestroy() { super.onDestroy(); binding = null; }
}*/