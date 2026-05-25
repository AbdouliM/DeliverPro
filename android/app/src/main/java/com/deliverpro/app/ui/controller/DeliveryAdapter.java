// ui/controller/DeliveryAdapter.java
package com.deliverpro.app.ui.controller;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.deliverpro.app.databinding.ItemDeliveryBinding;
import com.deliverpro.app.model.Delivery;

import java.util.ArrayList;
import java.util.List;

public class DeliveryAdapter extends RecyclerView.Adapter<DeliveryAdapter.ViewHolder> {

    public interface OnDeliveryClickListener {
        void onClick(Delivery delivery);
    }

    private List<Delivery> allData    = new ArrayList<>();
    private List<Delivery> filtered   = new ArrayList<>();
    private final OnDeliveryClickListener listener;

    public DeliveryAdapter(OnDeliveryClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Delivery> data) {
        this.allData  = data != null ? data : new ArrayList<>();
        this.filtered = new ArrayList<>(this.allData);
        notifyDataSetChanged();
    }

    // Filtre local rapide (barre de recherche écran 04)
    public void filter(String query) {
        filtered.clear();
        if (query == null || query.trim().isEmpty()) {
            filtered.addAll(allData);
        } else {
            String q = query.toLowerCase().trim();
            for (Delivery d : allData) {
                boolean matchOrder  = d.getOrderNumber() != null
                        && d.getOrderNumber().toLowerCase().contains(q);
                boolean matchClient = d.getClientName() != null
                        && d.getClientName().toLowerCase().contains(q);
                boolean matchDeliv  = d.getDelivererName() != null
                        && d.getDelivererName().toLowerCase().contains(q);
                if (matchOrder || matchClient || matchDeliv) filtered.add(d);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDeliveryBinding b = ItemDeliveryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(filtered.get(position));
    }

    @Override
    public int getItemCount() { return filtered.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDeliveryBinding binding;

        ViewHolder(ItemDeliveryBinding b) {
            super(b.getRoot());
            this.binding = b;
        }

        void bind(Delivery d) {
            binding.tvOrderNumber.setText(d.getOrderNumber());

            // Nom client
            String clientName = d.getClient() != null
                    ? d.getClient().getName() : d.getClientName();
            binding.tvClientName.setText(clientName);

            // Livreur + date
            String info = "Livreur : " + (d.getDelivererName() != null ? d.getDelivererName() : "—");
            if (d.getScheduledDate() != null) info += "  •  " + d.getScheduledDate();
            binding.tvDelivererInfo.setText(info);

            // Statut avec couleur
            switch (d.getStatus()) {
                case "DELIVERED":
                    binding.tvStatus.setText("Livré");
                    binding.tvStatus.setTextColor(Color.parseColor("#43A047"));
                    break;
                case "PENDING":
                    binding.tvStatus.setText("En attente");
                    binding.tvStatus.setTextColor(Color.parseColor("#E07800"));
                    break;
                case "FAILED":
                    binding.tvStatus.setText("Échoué");
                    binding.tvStatus.setTextColor(Color.parseColor("#E53935"));
                    break;
            }

            binding.getRoot().setOnClickListener(v -> listener.onClick(d));
        }
    }
}