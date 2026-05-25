
// ── ChatActivity.java — écran chat ───────────────────────────
package com.deliverpro.app.ui.controller;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.deliverpro.app.databinding.ActivityChatBinding;
import com.deliverpro.app.model.Message;
import com.deliverpro.app.model.SendMessageRequest;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;
import com.deliverpro.app.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private ApiService apiService;
    private MessageAdapter adapter;
    private int receiverId;
    private int myUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding      = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        apiService = ApiClient.getClient(this).create(ApiService.class);
        myUserId   = SessionManager.getInstance(this).getUserId();
        receiverId = getIntent().getIntExtra("receiver_id", -1);
        String receiverName = getIntent().getStringExtra("receiver_name");

        // Toolbar
        binding.tvReceiverName.setText(receiverName != null ? receiverName : "");
        if (receiverName != null && receiverName.contains(" ")) {
            String[] p = receiverName.split(" ");
            binding.tvAvatar.setText(
                    ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase());
        }

        // RecyclerView messages
        adapter = new MessageAdapter(myUserId);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true); // Afficher depuis le bas
        binding.rvMessages.setLayoutManager(llm);
        binding.rvMessages.setAdapter(adapter);

        loadMessages();

        // Bouton envoyer
        binding.btnSend.setOnClickListener(v -> sendMessage());

        // Touche "Envoyer" du clavier
        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void loadMessages() {
        apiService.getMessages(receiverId).enqueue(new Callback<List<Message>>() {

            @Override
            public void onResponse(Call<List<Message>> call,
                                   Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setData(response.body());
                    // Scroll vers le dernier message
                    if (adapter.getItemCount() > 0) {
                        binding.rvMessages.scrollToPosition(
                                adapter.getItemCount() - 1);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Toast.makeText(ChatActivity.this,
                        "Erreur chargement messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String content = binding.etMessage.getText().toString().trim();
        if (content.isEmpty()) return;

        binding.etMessage.setText("");
        binding.btnSend.setEnabled(false);

        SendMessageRequest request = new SendMessageRequest(receiverId, content);
        apiService.sendMessage(request).enqueue(new Callback<Message>() {

            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {
                binding.btnSend.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.addMessage(response.body());
                    binding.rvMessages.scrollToPosition(
                            adapter.getItemCount() - 1);
                } else {
                    Toast.makeText(ChatActivity.this,
                            "Erreur envoi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                binding.btnSend.setEnabled(true);
                Toast.makeText(ChatActivity.this,
                        "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onDestroy() { super.onDestroy(); binding = null; }
}