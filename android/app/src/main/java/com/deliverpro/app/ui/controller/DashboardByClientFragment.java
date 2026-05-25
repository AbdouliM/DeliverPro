// ui/controller/DashboardByClientFragment.java
package com.deliverpro.app.ui.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.deliverpro.app.databinding.FragmentDashboardByClientBinding;
import com.deliverpro.app.model.Delivery;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardByClientFragment extends Fragment {

    private FragmentDashboardByClientBinding binding;
    private ApiService apiService;
    private ClientStatusAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardByClientBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = ApiClient.getClient(requireContext()).create(ApiService.class);

        // Clic sur un client → ouvrir le détail livraison (écran 06)
        adapter = new ClientStatusAdapter(delivery -> {
            Intent intent = new Intent(requireContext(), DeliveryDetailActivity.class);
            intent.putExtra("delivery_id", delivery.getDeliveryId());
            startActivity(intent);
        });

        binding.rvClients.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvClients.setAdapter(adapter);

        loadClients();
    }

    private void loadClients() {
        binding.progressBar.setVisibility(View.VISIBLE);

        apiService.getDashboardByClient().enqueue(new Callback<List<Delivery>>() {

            @Override
            public void onResponse(@NonNull Call<List<Delivery>> call,
                                   @NonNull Response<List<Delivery>> response) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    adapter.setData(response.body());
                } else {
                    Toast.makeText(requireContext(),
                            "Erreur chargement clients", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Delivery>> call, @NonNull Throwable t) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(),
                        "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}