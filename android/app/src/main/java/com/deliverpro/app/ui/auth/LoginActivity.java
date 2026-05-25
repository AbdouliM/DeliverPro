// ui/auth/LoginActivity.java
package com.deliverpro.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.deliverpro.app.databinding.ActivityLoginBinding;
import com.deliverpro.app.model.LoginRequest;
import com.deliverpro.app.model.LoginResponse;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;
import com.deliverpro.app.ui.controller.ControllerMainActivity;
import com.deliverpro.app.ui.deliverer.DelivererMainActivity;
import com.deliverpro.app.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private ApiService           apiService;
    private SessionManager       session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ViewBinding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialisation
        apiService = ApiClient.getClient(this).create(ApiService.class);
        session    = SessionManager.getInstance(this);

        // Si déjà connecté → rediriger directement
        if (session.isLoggedIn()) {
            redirectByRole(session.getRole());
            return;
        }

        // Bouton Se connecter
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        // Lien Mot de passe oublié
        binding.tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });
    }

    private void attemptLogin() {
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Validation locale
        if (username.isEmpty()) {
            binding.etUsername.setError("Champ obligatoire");
            binding.etUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            binding.etPassword.setError("Champ obligatoire");
            binding.etPassword.requestFocus();
            return;
        }

        // Afficher le loader, désactiver le bouton
        setLoading(true);

        // Appel API
        LoginRequest request = new LoginRequest(username, password);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {

            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    LoginResponse.User user = body.getUser();

                    // Sauvegarder la session
                    session.saveSession(
                            body.getToken(),
                            user.getUserId(),
                            user.getUsername(),
                            user.getFullName(),
                            user.getRole(),
                            user.getPhone()
                    );

                    // Rediriger selon le rôle
                    redirectByRole(user.getRole());

                } else if (response.code() == 401) {
                    Toast.makeText(LoginActivity.this,
                            "Identifiants incorrects", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Erreur serveur (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this,
                        "Impossible de joindre le serveur.\nVérifiez votre connexion.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // Redirection selon le rôle — CONTROLLER ou DELIVERER
    private void redirectByRole(String role) {
        Intent intent;
        if ("CONTROLLER".equals(role)) {
            intent = new Intent(this, ControllerMainActivity.class);
        } else {
            intent = new Intent(this, DelivererMainActivity.class);
        }
        // Effacer la pile de navigation (ne pas pouvoir revenir au login)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Afficher/masquer le loader
    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
        binding.btnLogin.setText(loading ? "Connexion..." : "Se connecter");
        binding.etUsername.setEnabled(!loading);
        binding.etPassword.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}