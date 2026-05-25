// ui/auth/ForgotPasswordActivity.java
package com.deliverpro.app.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.deliverpro.app.databinding.ActivityForgotPasswordBinding;
import com.deliverpro.app.model.LoginRequest;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding    = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getClient(this).create(ApiService.class);

        // Bouton retour
        binding.btnBack.setOnClickListener(v -> finish());

        // Bouton envoyer le lien
        binding.btnSendLink.setOnClickListener(v -> sendResetLink());
    }

    private void sendResetLink() {
        String email = binding.etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            binding.etEmail.setError("Champ obligatoire");
            binding.etEmail.requestFocus();
            return;
        }

        setLoading(true);

        LoginRequest request = new LoginRequest(email);
        apiService.forgotPassword(request).enqueue(new Callback<Void>() {

            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                setLoading(false);
                // Toujours afficher le même message (sécurité)
                Toast.makeText(ForgotPasswordActivity.this,
                        "Si cet email existe, un lien a été envoyé.",
                        Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this,
                        "Impossible de joindre le serveur.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSendLink.setEnabled(!loading);
        binding.etEmail.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}