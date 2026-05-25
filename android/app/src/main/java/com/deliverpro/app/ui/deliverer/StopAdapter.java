package com.deliverpro.app.ui.deliverer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.deliverpro.app.databinding.ItemStopBinding;
import com.deliverpro.app.model.Delivery;

import java.util.ArrayList;
import java.util.List;

public class StopAdapter extends RecyclerView.Adapter<StopAdapter.ViewHolder> {

    public interface OnStopClickListener { void onClick(Delivery d); }

    private List<Delivery> data = new ArrayList<>();
    private final OnStopClickListener listener;

    public StopAdapter(OnStopClickListener listener) { this.listener = listener; }

    public void setData(List<Delivery> stops) {
        this.data = stops != null ? stops : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemStopBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() { return data.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        final ItemStopBinding b;
        ViewHolder(ItemStopBinding b) { super(b.getRoot()); this.b = b; }

        void bind(Delivery d) {
            // Stop number badge  e.g. #17
            b.tvStopNumber.setText("#" + d.getStopOrder());

            // Client info
            b.tvClientName.setText(d.getClientName());
            b.tvCity.setText(d.getDeliveryCity() != null ? d.getDeliveryCity() : "");
            b.tvPhone.setText(d.getClientPhone() != null ? d.getClientPhone() : "");

            // Status tag + card highlight
            String status = d.getStatus();
            if (status == null) status = "PENDING";

            switch (status) {
                case "DELIVERED":
                    b.tvStatusBadge.setText("Effectué");
                    b.tvStatusBadge.setTextColor(Color.parseColor("#27AE60"));
                    b.getRoot().setBackgroundColor(Color.WHITE);
                    setStopBadgeColor(b, "#27AE60");
                    break;

                case "FAILED":
                    b.tvStatusBadge.setText("Échoué");
                    b.tvStatusBadge.setTextColor(Color.parseColor("#E74C3C"));
                    b.getRoot().setBackgroundColor(Color.WHITE);
                    setStopBadgeColor(b, "#AAAAAA");
                    break;

                case "PENDING":
                default:
                    boolean isNext = isNextStop(d);
                    if (isNext) {
                        b.tvStatusBadge.setText("Suivant");
                        b.tvStatusBadge.setTextColor(Color.parseColor("#E07B00"));
                        b.getRoot().setBackgroundColor(Color.parseColor("#FFF5EB"));
                        // Orange stroke border via outline
                        b.getRoot().setBackground(buildOrangeBorder(b.getRoot().getContext()));
                        setStopBadgeColor(b, "#E07B00");
                    } else {
                        b.tvStatusBadge.setText("À venir");
                        b.tvStatusBadge.setTextColor(Color.parseColor("#888888"));
                        b.getRoot().setBackgroundColor(Color.WHITE);
                        setStopBadgeColor(b, "#CCCCCC");
                    }
                    break;
            }

            b.getRoot().setOnClickListener(v -> listener.onClick(d));
        }

        // "Next" stop = first non-DELIVERED and non-FAILED stop in the list
        private boolean isNextStop(Delivery d) {
            for (Delivery stop : data) {
                String s = stop.getStatus();
                if (!"DELIVERED".equals(s) && !"FAILED".equals(s)) {
                    return stop.getDeliveryId() == d.getDeliveryId();
                }
            }
            return false;
        }
    }

    // Tints the stop number badge circle to the given color
    private void setStopBadgeColor(ItemStopBinding b, String hex) {
        android.graphics.drawable.Drawable bg = b.tvStopNumber.getBackground();
        if (bg != null) {
            bg = bg.mutate();
            bg.setColorFilter(Color.parseColor(hex),
                    android.graphics.PorterDuff.Mode.SRC_IN);
            b.tvStopNumber.setBackground(bg);
        }
    }

    // Builds an orange-bordered background for the "Suivant" card
    private android.graphics.drawable.Drawable buildOrangeBorder(android.content.Context ctx) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Color.parseColor("#FFF5EB"));
        gd.setStroke(3, Color.parseColor("#E07B00"));
        gd.setCornerRadius(12f);
        return gd;
    }
}
/*package com.deliverpro.app.ui.deliverer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.deliverpro.app.databinding.ItemStopBinding;
import com.deliverpro.app.model.Delivery;

import java.util.ArrayList;
import java.util.List;

public class StopAdapter extends RecyclerView.Adapter<StopAdapter.ViewHolder> {

    public interface OnStopClickListener { void onClick(Delivery d); }

    private List<Delivery> data = new ArrayList<>();
    private final OnStopClickListener listener;

    public StopAdapter(OnStopClickListener listener) { this.listener = listener; }

    public void setData(List<Delivery> stops) {
        this.data = stops != null ? stops : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemStopBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() { return data.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        final ItemStopBinding b;
        ViewHolder(ItemStopBinding b) { super(b.getRoot()); this.b = b; }

        void bind(Delivery d) {
            // Stop number badge  e.g. #17
            b.tvStopNumber.setText("#" + d.getStopOrder());

            // Client info
            b.tvClientName.setText(d.getClientName());
            b.tvCity.setText(d.getDeliveryCity() != null ? d.getDeliveryCity() : "");
            b.tvPhone.setText(d.getClientPhone() != null ? d.getClientPhone() : "");

            // Status tag + card highlight
            String status = d.getStatus();
            if (status == null) status = "PENDING";

            switch (status) {
                case "DELIVERED":
                    b.tvStatusBadge.setText("Effectué");
                    b.tvStatusBadge.setTextColor(Color.parseColor("#27AE60"));
                    b.getRoot().setBackgroundColor(Color.WHITE);
                    setStopBadgeColor(b, "#27AE60");
                    break;

                case "FAILED":
                    b.tvStatusBadge.setText("Échoué");
                    b.tvStatusBadge.setTextColor(Color.parseColor("#E74C3C"));
                    b.getRoot().setBackgroundColor(Color.WHITE);
                    setStopBadgeColor(b, "#AAAAAA");
                    break;

                case "PENDING":
                default:
                    boolean isNext = isNextStop(d);
                    if (isNext) {
                        b.tvStatusBadge.setText("Suivant");
                        b.tvStatusBadge.setTextColor(Color.parseColor("#E07B00"));
                        b.getRoot().setBackgroundColor(Color.parseColor("#FFF5EB"));
                        // Orange stroke border via outline
                        b.getRoot().setBackground(buildOrangeBorder(b.getRoot().getContext()));
                        setStopBadgeColor(b, "#E07B00");
                    } else {
                        b.tvStatusBadge.setText("À venir");
                        b.tvStatusBadge.setTextColor(Color.parseColor("#888888"));
                        b.getRoot().setBackgroundColor(Color.WHITE);
                        setStopBadgeColor(b, "#CCCCCC");
                    }
                    break;
            }

            b.getRoot().setOnClickListener(v -> listener.onClick(d));
        }

        // "Next" stop = first non-DELIVERED stop in the full list
        private boolean isNextStop(Delivery d) {
            for (Delivery stop : data) {
                if (!"DELIVERED".equals(stop.getStatus())) {
                    return stop.getDeliveryId() == d.getDeliveryId();
                }
            }
            return false;
        }
    }

    // Tints the stop number badge circle to the given color
    private void setStopBadgeColor(ItemStopBinding b, String hex) {
        android.graphics.drawable.Drawable bg = b.tvStopNumber.getBackground();
        if (bg != null) {
            bg = bg.mutate();
            bg.setColorFilter(Color.parseColor(hex),
                    android.graphics.PorterDuff.Mode.SRC_IN);
            b.tvStopNumber.setBackground(bg);
        }
    }

    // Builds an orange-bordered background for the "Suivant" card
    private android.graphics.drawable.Drawable buildOrangeBorder(android.content.Context ctx) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Color.parseColor("#FFF5EB"));
        gd.setStroke(3, Color.parseColor("#E07B00"));
        gd.setCornerRadius(12f);
        return gd;
    }
}*/
/*
// ── StopAdapter.java ─────────────────────────────────────────
package com.deliverpro.app.ui.deliverer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.deliverpro.app.databinding.ItemStopBinding;
import com.deliverpro.app.model.Delivery;
import java.util.ArrayList;
import java.util.List;

public class StopAdapter extends RecyclerView.Adapter<StopAdapter.ViewHolder> {

    public interface OnStopClickListener { void onClick(Delivery d); }

    private List<Delivery> data = new ArrayList<>();
    private final OnStopClickListener listener;

    public StopAdapter(OnStopClickListener listener) { this.listener = listener; }

    public void setData(List<Delivery> d) {
        this.data = d != null ? d : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemStopBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) { h.bind(data.get(pos)); }

    @Override public int getItemCount() { return data.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemStopBinding b;
        ViewHolder(ItemStopBinding b) { super(b.getRoot()); this.b = b; }

        void bind(Delivery d) {
            b.tvStopNumber.setText(String.valueOf(d.getStopOrder()));
            b.tvClientName.setText(d.getClientName());
            b.tvCity.setText("📍 " + (d.getDeliveryCity() != null ? d.getDeliveryCity() : ""));
            b.tvPhone.setText("📞 " + (d.getClientPhone() != null ? d.getClientPhone() : ""));

            switch (d.getStatus()) {
                case "DELIVERED":
                    b.tvStatusBadge.setText("Effectué");
                    b.tvStatusBadge.setTextColor(Color.parseColor("#43A047"));
                    b.tvStopNumber.setBackgroundResource(
                            com.deliverpro.app.R.drawable.bg_stop_number);
                    b.getRoot().setAlpha(0.6f);
                    break;
                case "PENDING":
                    // Arrêt "Suivant" = premier PENDING de la liste
                    boolean isNext = data.stream()
                            .filter(x -> "PENDING".equals(x.getStatus()))
                            .findFirst()
                            .map(x -> x.getDeliveryId() == d.getDeliveryId())
                            .orElse(false);
                    b.tvStatusBadge.setText(isNext ? "Suivant" : "À venir");
                    b.tvStatusBadge.setTextColor(isNext
                            ? Color.parseColor("#E07800") : Color.parseColor("#888888"));
                    b.getRoot().setAlpha(1f);
                    break;
                case "FAILED":
                    b.tvStatusBadge.setText("Échoué");
                    b.tvStatusBadge.setTextColor(Color.parseColor("#E53935"));
                    b.getRoot().setAlpha(1f);
                    break;
            }
            b.getRoot().setOnClickListener(v -> listener.onClick(d));
        }
    }
}
*/