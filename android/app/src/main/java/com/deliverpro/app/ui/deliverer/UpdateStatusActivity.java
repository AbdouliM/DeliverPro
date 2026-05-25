// ui/deliverer/UpdateStatusActivity.java — VERSION CORRIGÉE
// Remplace le fichier précédent — utilise SyncManager au lieu d'appeler l'API directement

package com.deliverpro.app.ui.deliverer;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.deliverpro.app.databinding.ActivityUpdateStatusBinding;
import com.deliverpro.app.sync.SyncManager;

public class UpdateStatusActivity extends AppCompatActivity {

    private ActivityUpdateStatusBinding binding;
    private String selectedStatus = "DELIVERED";
    private int    deliveryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityUpdateStatusBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        deliveryId = getIntent().getIntExtra("delivery_id", -1);

        // Remplir infos commande
        binding.tvDeliveryRef.setText(
                getIntent().getStringExtra("order_number")
                        + " · " + getIntent().getStringExtra("client_name"));
        binding.tvOrderNumber.setText(getIntent().getStringExtra("order_number"));
        binding.tvClientName.setText(getIntent().getStringExtra("client_name"));
        binding.tvClientPhone.setText(getIntent().getStringExtra("client_phone"));
        binding.tvAmount.setText(String.format("%.3f TND",
                getIntent().getDoubleExtra("amount", 0)));

        setupOptionSelection();
        binding.btnCancel.setOnClickListener(v -> finish());
        binding.btnConfirm.setOnClickListener(v -> confirmStatus());
    }

    private void setupOptionSelection() {
        selectOption("DELIVERED");
        binding.optionDelivered.setOnClickListener(v -> selectOption("DELIVERED"));
        binding.optionPending.setOnClickListener(v -> selectOption("PENDING"));
        binding.optionFailed.setOnClickListener(v -> selectOption("FAILED"));
    }

    private void selectOption(String status) {
        selectedStatus = status;

        // Reset
        binding.radioDelivered.setChecked(false);
        binding.radioPending.setChecked(false);
        binding.radioFailed.setChecked(false);
        binding.optionDelivered.setBackgroundResource(
                com.deliverpro.app.R.drawable.bg_option_default);
        binding.optionPending.setBackgroundResource(
                com.deliverpro.app.R.drawable.bg_option_default);
        binding.optionFailed.setBackgroundResource(
                com.deliverpro.app.R.drawable.bg_option_default);

        switch (status) {
            case "DELIVERED":
                binding.radioDelivered.setChecked(true);
                binding.optionDelivered.setBackgroundResource(
                        com.deliverpro.app.R.drawable.bg_option_selected_green);
                binding.etRemark.setVisibility(View.GONE);
                break;
            case "PENDING":
                binding.radioPending.setChecked(true);
                binding.optionPending.setBackgroundResource(
                        com.deliverpro.app.R.drawable.bg_option_selected_orange);
                binding.etRemark.setVisibility(View.GONE);
                break;
            case "FAILED":
                binding.radioFailed.setChecked(true);
                binding.optionFailed.setBackgroundResource(
                        com.deliverpro.app.R.drawable.bg_option_selected_red);
                binding.etRemark.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void confirmStatus() {
        String remark = binding.etRemark.getText().toString().trim();

        if ("FAILED".equals(selectedStatus) && remark.isEmpty()) {
            binding.etRemark.setError("La remarque est obligatoire pour Non livré");
            binding.etRemark.requestFocus();
            return;
        }

        binding.btnConfirm.setEnabled(false);
        binding.btnConfirm.setText("Envoi...");

        // ── LOGIQUE MÉTIER CORRECTE ───────────────────────────
        // SyncManager décide lui-même :
        //   DELIVERED → local uniquement, batch 23h59
        //   FAILED    → local + API immédiatement
        //   PENDING   → local uniquement
        // ─────────────────────────────────────────────────────
        SyncManager.getInstance(this).updateStatus(
                deliveryId,
                selectedStatus,
                remark.isEmpty() ? null : remark,
                new SyncManager.SyncCallback() {

                    @Override
                    public void onSuccess(int syncedCount) {
                        String msg;
                        switch (selectedStatus) {
                            case "DELIVERED":
                                msg = "✅ Livré — synchronisation automatique ce soir à 23h59";
                                break;
                            case "FAILED":
                                msg = "⚠️ Échec signalé — contrôleur notifié immédiatement";
                                break;
                            default:
                                msg = "Statut mis à jour";
                        }
                        Toast.makeText(UpdateStatusActivity.this,
                                msg, Toast.LENGTH_LONG).show();

                        // Retourner à la tournée
                        finish();
                    }

                    @Override
                    public void onFailure(String error) {
                        binding.btnConfirm.setEnabled(true);
                        binding.btnConfirm.setText("Confirmer");

                        if ("FAILED".equals(selectedStatus)) {
                            // Statut sauvegardé localement même sans réseau
                            // Sera re-tenté automatiquement
                            Toast.makeText(UpdateStatusActivity.this,
                                    "Échec signalé localement — sera envoyé dès que "
                                            + "la connexion sera rétablie",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(UpdateStatusActivity.this,
                                    "Statut sauvegardé localement", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onDestroy() { super.onDestroy(); binding = null; }
}