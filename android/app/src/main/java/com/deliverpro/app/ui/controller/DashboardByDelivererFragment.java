// ui/controller/DashboardByDelivererFragment.java
package com.deliverpro.app.ui.controller;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.deliverpro.app.databinding.FragmentDashboardByDelivererBinding;
import com.deliverpro.app.model.DashboardResponse;
import com.deliverpro.app.network.ApiClient;
import com.deliverpro.app.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardByDelivererFragment extends Fragment {

    private FragmentDashboardByDelivererBinding binding;
    private ApiService apiService;
    private DelivererStatsAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardByDelivererBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = ApiClient.getClient(requireContext()).create(ApiService.class);

        // Setup RecyclerView
        adapter = new DelivererStatsAdapter(delivererStats -> {
            // Clic sur un livreur → ouvrir ses livraisons filtrées
            // (à implémenter avec DeliveriesActivity + filtre deliverer_id)
        });
        binding.rvDeliverers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDeliverers.setAdapter(adapter);

        loadDashboard();
    }

    private void loadDashboard() {
        binding.progressBar.setVisibility(View.VISIBLE);

        apiService.getDashboardByDeliverer().enqueue(new Callback<DashboardResponse>() {

            @Override
            public void onResponse(@NonNull Call<DashboardResponse> call,
                                   @NonNull Response<DashboardResponse> response) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    DashboardResponse data = response.body();

                    // Mettre à jour les compteurs
                    binding.tvTotalCount.setText(String.valueOf(data.getSummary().getTotal()));
                    binding.tvPendingCount.setText(String.valueOf(data.getSummary().getPending()));
                    binding.tvFailedCount.setText(String.valueOf(data.getSummary().getFailed()));

                    // Mettre à jour la liste
                    adapter.setData(data.getDeliverers());
                } else {
                    Toast.makeText(requireContext(),
                            "Erreur chargement dashboard", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<DashboardResponse> call, @NonNull Throwable t) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(),
                        "Erreur réseau : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}