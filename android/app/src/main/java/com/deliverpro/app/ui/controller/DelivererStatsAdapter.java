// ui/controller/DelivererStatsAdapter.java
package com.deliverpro.app.ui.controller;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.deliverpro.app.databinding.ItemDelivererStatsBinding;
import com.deliverpro.app.model.DashboardResponse;

import java.util.ArrayList;
import java.util.List;

public class DelivererStatsAdapter
        extends RecyclerView.Adapter<DelivererStatsAdapter.ViewHolder> {

    public interface OnDelivererClickListener {
        void onClick(DashboardResponse.DelivererStats stats);
    }

    private List<DashboardResponse.DelivererStats> data = new ArrayList<>();
    private final OnDelivererClickListener listener;

    public DelivererStatsAdapter(OnDelivererClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<DashboardResponse.DelivererStats> newData) {
        this.data = newData != null ? newData : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDelivererStatsBinding binding = ItemDelivererStatsBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() { return data.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDelivererStatsBinding binding;

        ViewHolder(ItemDelivererStatsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DashboardResponse.DelivererStats stats) {
            binding.tvDelivererName.setText(stats.getDelivererName());

            // ⏰ En attente
            binding.tvPending.setText("⏰ " + stats.getPending() + " en attente");

            // ▲ Échoué — rouge si > 0
            String failedText = "▲ " + stats.getFailed() + " échoué";
            binding.tvFailed.setText(failedText);
            if (stats.getFailed() == 0) {
                binding.tvFailed.setAlpha(0.4f);
            } else {
                binding.tvFailed.setAlpha(1f);
            }

            binding.getRoot().setOnClickListener(v -> listener.onClick(stats));
        }
    }
}