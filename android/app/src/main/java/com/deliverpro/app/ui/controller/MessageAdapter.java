
// ── MessageAdapter.java ───────────────────────────────────────
package com.deliverpro.app.ui.controller;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.deliverpro.app.databinding.ItemMessageMineBinding;
import com.deliverpro.app.databinding.ItemMessageOtherBinding;
import com.deliverpro.app.model.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_MINE  = 1;
    private static final int VIEW_OTHER = 2;

    private final List<Message> data;
    private final int myUserId;

    public MessageAdapter(int myUserId) {
        this.data     = new ArrayList<>();
        this.myUserId = myUserId;
    }

    public void setData(List<Message> messages) {
        data.clear();
        if (messages != null) data.addAll(messages);
        notifyDataSetChanged();
    }

    public void addMessage(Message msg) {
        data.add(msg);
        notifyItemInserted(data.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        Message m = data.get(position);
        return (m.isMine() || m.getSenderId() == myUserId)
                ? VIEW_MINE : VIEW_OTHER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                      int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_MINE) {
            return new MineViewHolder(
                    ItemMessageMineBinding.inflate(inf, parent, false));
        } else {
            return new OtherViewHolder(
                    ItemMessageOtherBinding.inflate(inf, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        Message m = data.get(pos);
        String time = (m.getSentAt() != null && m.getSentAt().length() >= 16)
                ? m.getSentAt().substring(11, 16) : "";

        if (holder instanceof MineViewHolder) {
            ((MineViewHolder) holder).b.tvContent.setText(m.getContent());
            ((MineViewHolder) holder).b.tvTime.setText(time);
        } else {
            ((OtherViewHolder) holder).b.tvContent.setText(m.getContent());
            ((OtherViewHolder) holder).b.tvTime.setText(time);
        }
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class MineViewHolder extends RecyclerView.ViewHolder {
        final ItemMessageMineBinding b;
        MineViewHolder(ItemMessageMineBinding b) { super(b.getRoot()); this.b = b; }
    }

    static class OtherViewHolder extends RecyclerView.ViewHolder {
        final ItemMessageOtherBinding b;
        OtherViewHolder(ItemMessageOtherBinding b) { super(b.getRoot()); this.b = b; }
    }
}


