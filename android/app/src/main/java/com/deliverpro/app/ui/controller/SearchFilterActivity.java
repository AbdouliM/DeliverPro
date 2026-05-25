package com.deliverpro.app.ui.controller;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.deliverpro.app.databinding.ActivitySearchFilterBinding;
import com.deliverpro.app.model.Delivery;
import com.deliverpro.app.model.User;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFilterActivity extends AppCompatActivity {

    private ActivitySearchFilterBinding binding;
    private ApiService apiService;

    private String selectedStatus    = "ALL";
    private String selectedDateStart = null;
    private String selectedDateEnd   = null;

    // Deliverer list loaded from API
    private List<User>   delivererList   = new ArrayList<>();
    private List<String> delivererNames  = new ArrayList<>();
    private int          selectedDelivererId = 0; // 0 = tous

    private final SimpleDateFormat sdf     = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat display = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivitySearchFilterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        apiService = ApiClient.getClient(this).create(ApiService.class);

        setupDatePickers();
        setupQuickDateButtons();
        setupStatusChips();
        loadDeliverers(); // loads from API then sets up spinner

        binding.btnSearch.setOnClickListener(v -> performSearch());
    }

    // ── Date pickers ─────────────────────────────────────────

    private void setupDatePickers() {
        binding.tvDateStart.setOnClickListener(v -> showDatePicker(true));
        binding.tvDateEnd.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            cal.set(year, month, day);
            String formatted = sdf.format(cal.getTime());
            String displayed = display.format(cal.getTime());
            if (isStart) {
                selectedDateStart = formatted;
                binding.tvDateStart.setText(displayed);
            } else {
                selectedDateEnd = formatted;
                binding.tvDateEnd.setText(displayed);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setupQuickDateButtons() {
        Calendar cal = Calendar.getInstance();

        binding.btnToday.setOnClickListener(v -> {
            selectedDateStart = sdf.format(cal.getTime());
            selectedDateEnd   = sdf.format(cal.getTime());
            binding.tvDateStart.setText(display.format(cal.getTime()));
            binding.tvDateEnd.setText(display.format(cal.getTime()));
            highlightQuickBtn(binding.btnToday);
        });

        binding.btnThisWeek.setOnClickListener(v -> {
            Calendar start = Calendar.getInstance();
            start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
            Calendar end = Calendar.getInstance();
            end.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek() + 6);
            selectedDateStart = sdf.format(start.getTime());
            selectedDateEnd   = sdf.format(end.getTime());
            binding.tvDateStart.setText(display.format(start.getTime()));
            binding.tvDateEnd.setText(display.format(end.getTime()));
            highlightQuickBtn(binding.btnThisWeek);
        });

        binding.btnThisMonth.setOnClickListener(v -> {
            Calendar start = Calendar.getInstance();
            start.set(Calendar.DAY_OF_MONTH, 1);
            Calendar end = Calendar.getInstance();
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
            selectedDateStart = sdf.format(start.getTime());
            selectedDateEnd   = sdf.format(end.getTime());
            binding.tvDateStart.setText(display.format(start.getTime()));
            binding.tvDateEnd.setText(display.format(end.getTime()));
            highlightQuickBtn(binding.btnThisMonth);
        });

        binding.btnToday.performClick();
    }

    private void highlightQuickBtn(TextView selected) {
        for (TextView btn : Arrays.asList(
                binding.btnToday, binding.btnThisWeek, binding.btnThisMonth)) {
            btn.setBackgroundResource(com.deliverpro.app.R.drawable.bg_chip);
            btn.setTextColor(Color.parseColor("#888888"));
        }
        selected.setBackgroundResource(com.deliverpro.app.R.drawable.bg_chip_selected);
        selected.setTextColor(Color.parseColor("#1A56A4"));
    }

    private void setupStatusChips() {
        List<TextView> chips = Arrays.asList(
                binding.chipAll, binding.chipDelivered,
                binding.chipPending, binding.chipFailed);
        List<String> statuses = Arrays.asList("ALL", "DELIVERED", "PENDING", "FAILED");

        for (int i = 0; i < chips.size(); i++) {
            final String status = statuses.get(i);
            chips.get(i).setOnClickListener(v -> {
                selectedStatus = status;
                for (TextView c : chips) {
                    c.setBackgroundResource(com.deliverpro.app.R.drawable.bg_chip);
                    c.setTextColor(Color.parseColor("#888888"));
                }
                ((TextView) v).setBackgroundResource(
                        com.deliverpro.app.R.drawable.bg_chip_selected);
                ((TextView) v).setTextColor(Color.parseColor("#1A56A4"));
            });
        }
    }

    // ── Deliverer spinner ─────────────────────────────────────

    private void loadDeliverers() {
        // Show placeholder while loading
        delivererNames.clear();
        delivererNames.add("Tous les livreurs");
        setSpinnerAdapter();

        apiService.getDeliverers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    delivererList.clear();
                    delivererNames.clear();
                    delivererNames.add("Tous les livreurs"); // index 0 = no filter
                    for (User u : r.body()) {
                        delivererList.add(u);
                        delivererNames.add(u.getFullName());
                    }
                    setSpinnerAdapter();
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(SearchFilterActivity.this,
                        "Impossible de charger les livreurs", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setSpinnerAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, delivererNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDeliverer.setAdapter(adapter);

        binding.spinnerDeliverer.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent,
                                               android.view.View view, int position, long id) {
                        if (position == 0) {
                            selectedDelivererId = 0; // tous
                        } else {
                            selectedDelivererId = delivererList.get(position - 1).getUserId();
                        }
                    }
                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {
                        selectedDelivererId = 0;
                    }
                });
    }

    // ── Search ────────────────────────────────────────────────

    private void performSearch() {
        String search = binding.etSearchText.getText().toString().trim();
        String status = "ALL".equals(selectedStatus) ? null : selectedStatus;
        Integer delivererId = selectedDelivererId > 0 ? selectedDelivererId : null;

        apiService.searchDeliveries(
                selectedDateStart, selectedDateEnd,
                delivererId, status,
                search.isEmpty() ? null : search
        ).enqueue(new Callback<List<Delivery>>() {
            @Override
            public void onResponse(Call<List<Delivery>> call,
                                   Response<List<Delivery>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Intent result = new Intent();
                    result.putExtra("date_start",    selectedDateStart);
                    result.putExtra("date_end",      selectedDateEnd);
                    result.putExtra("deliverer_id",  selectedDelivererId);
                    result.putExtra("status",        selectedStatus);
                    result.putExtra("search",        search);
                    result.putExtra("result_count",  response.body().size());
                    setResult(RESULT_OK, result);
                    finish();
                } else {
                    Toast.makeText(SearchFilterActivity.this,
                            "Erreur recherche", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<Delivery>> call, Throwable t) {
                Toast.makeText(SearchFilterActivity.this,
                        "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onDestroy() { super.onDestroy(); binding = null; }
}
/*// ui/controller/SearchFilterActivity.java — Écran 05
package com.deliverpro.app.ui.controller;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.deliverpro.app.databinding.ActivitySearchFilterBinding;
import com.deliverpro.app.model.Delivery;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SearchFilterActivity extends AppCompatActivity {

    private ActivitySearchFilterBinding binding;
    private ApiService apiService;

    private String selectedStatus    = "ALL";
    private String selectedDateStart = null;
    private String selectedDateEnd   = null;

    private final SimpleDateFormat sdf     = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat display = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivitySearchFilterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        apiService = ApiClient.getClient(this).create(ApiService.class);

        setupDatePickers();
        setupQuickDateButtons();
        setupStatusChips();
        setupDelivererSpinner();

        binding.btnSearch.setOnClickListener(v -> performSearch());
    }

    private void setupDatePickers() {
        binding.tvDateStart.setOnClickListener(v -> showDatePicker(true));
        binding.tvDateEnd.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            cal.set(year, month, day);
            String formatted = sdf.format(cal.getTime());
            String displayed = display.format(cal.getTime());
            if (isStart) {
                selectedDateStart = formatted;
                binding.tvDateStart.setText(displayed);
            } else {
                selectedDateEnd = formatted;
                binding.tvDateEnd.setText(displayed);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setupQuickDateButtons() {
        Calendar cal = Calendar.getInstance();

        // Aujourd'hui
        binding.btnToday.setOnClickListener(v -> {
            selectedDateStart = sdf.format(cal.getTime());
            selectedDateEnd   = sdf.format(cal.getTime());
            binding.tvDateStart.setText(display.format(cal.getTime()));
            binding.tvDateEnd.setText(display.format(cal.getTime()));
            highlightQuickBtn(binding.btnToday);
        });

        // Cette semaine
        binding.btnThisWeek.setOnClickListener(v -> {
            Calendar start = Calendar.getInstance();
            start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
            Calendar end = Calendar.getInstance();
            end.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek() + 6);
            selectedDateStart = sdf.format(start.getTime());
            selectedDateEnd   = sdf.format(end.getTime());
            binding.tvDateStart.setText(display.format(start.getTime()));
            binding.tvDateEnd.setText(display.format(end.getTime()));
            highlightQuickBtn(binding.btnThisWeek);
        });

        // Ce mois
        binding.btnThisMonth.setOnClickListener(v -> {
            Calendar start = Calendar.getInstance();
            start.set(Calendar.DAY_OF_MONTH, 1);
            Calendar end = Calendar.getInstance();
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
            selectedDateStart = sdf.format(start.getTime());
            selectedDateEnd   = sdf.format(end.getTime());
            binding.tvDateStart.setText(display.format(start.getTime()));
            binding.tvDateEnd.setText(display.format(end.getTime()));
            highlightQuickBtn(binding.btnThisMonth);
        });

        // Sélection par défaut : Aujourd'hui
        binding.btnToday.performClick();
    }

    private void highlightQuickBtn(TextView selected) {
        // Reset tous les boutons
        for (TextView btn : Arrays.asList(
                binding.btnToday, binding.btnThisWeek, binding.btnThisMonth)) {
            btn.setBackgroundResource(com.deliverpro.app.R.drawable.bg_chip);
            btn.setTextColor(Color.parseColor("#888888"));
        }
        // Activer le sélectionné
        selected.setBackgroundResource(com.deliverpro.app.R.drawable.bg_chip_selected);
        selected.setTextColor(Color.parseColor("#1A56A4"));
    }

    private void setupStatusChips() {
        List<TextView> chips = Arrays.asList(
                binding.chipAll, binding.chipDelivered,
                binding.chipPending, binding.chipFailed);
        List<String> statuses = Arrays.asList("ALL", "DELIVERED", "PENDING", "FAILED");

        for (int i = 0; i < chips.size(); i++) {
            final String status = statuses.get(i);
            chips.get(i).setOnClickListener(v -> {
                selectedStatus = status;
                // Reset tous les chips
                for (TextView c : chips) {
                    c.setBackgroundResource(com.deliverpro.app.R.drawable.bg_chip);
                    c.setTextColor(Color.parseColor("#888888"));
                }
                // Activer le sélectionné
                ((TextView) v).setBackgroundResource(
                        com.deliverpro.app.R.drawable.bg_chip_selected);
                ((TextView) v).setTextColor(Color.parseColor("#1A56A4"));
            });
        }
    }

    private void setupDelivererSpinner() {
        // Spinner simple avec "Tous les livreurs" pour l'instant
        // En production : charger la liste depuis l'API
        String[] items = {"Tous les livreurs"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDeliverer.setAdapter(adapter);
    }

    private void performSearch() {
        String search = binding.etSearchText.getText().toString().trim();
        String status = "ALL".equals(selectedStatus) ? null : selectedStatus;

        apiService.searchDeliveries(
                selectedDateStart, selectedDateEnd, null, status,
                search.isEmpty() ? null : search
        ).enqueue(new retrofit2.Callback<List<Delivery>>() {

            @Override
            public void onResponse(retrofit2.Call<List<Delivery>> call,
                                   retrofit2.Response<List<Delivery>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Renvoyer les résultats à DeliveriesActivity
                    Intent result = new Intent();
                    result.putExtra("date_start",    selectedDateStart);
                    result.putExtra("date_end",      selectedDateEnd);
                    result.putExtra("status",        selectedStatus);
                    result.putExtra("search",        search);
                    result.putExtra("result_count",  response.body().size());
                    setResult(RESULT_OK, result);
                    finish();
                } else {
                    Toast.makeText(SearchFilterActivity.this,
                            "Erreur recherche", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<Delivery>> call, Throwable t) {
                Toast.makeText(SearchFilterActivity.this,
                        "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}*/