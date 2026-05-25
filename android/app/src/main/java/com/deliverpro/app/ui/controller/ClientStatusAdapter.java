// ui/controller/ClientStatusAdapter.java
package com.deliverpro.app.ui.controller;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.deliverpro.app.databinding.ItemClientStatusBinding;
import com.deliverpro.app.model.Delivery;

import java.util.ArrayList;
import java.util.List;

public class ClientStatusAdapter
        extends RecyclerView.Adapter<ClientStatusAdapter.ViewHolder> {

    public interface OnClientClickListener {
        void onClick(Delivery delivery);
    }

    private List<Delivery> data = new ArrayList<>();
    private final OnClientClickListener listener;

    public ClientStatusAdapter(OnClientClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Delivery> newData) {
        this.data = newData != null ? newData : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemClientStatusBinding binding = ItemClientStatusBinding.inflate(
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
        private final ItemClientStatusBinding binding;

        ViewHolder(ItemClientStatusBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Delivery delivery) {
            // Nom du client — utilise client.name si disponible, sinon client_name
            String name = delivery.getClient() != null
                    ? delivery.getClient().getName()
                    : delivery.getClientName();
            binding.tvClientName.setText(name);

            // Statut avec couleur
            switch (delivery.getStatus()) {
                case "FAILED":
                    binding.tvStatus.setText("Échoué");
                    binding.tvStatus.setTextColor(Color.parseColor("#E53935"));
                    break;
                case "PENDING":
                    binding.tvStatus.setText("En attente");
                    binding.tvStatus.setTextColor(Color.parseColor("#E07800"));
                    break;
                case "DELIVERED":
                    binding.tvStatus.setText("Livré");
                    binding.tvStatus.setTextColor(Color.parseColor("#43A047"));
                    break;
            }

            binding.getRoot().setOnClickListener(v -> listener.onClick(delivery));
        }
    }
}