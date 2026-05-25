package com.deliverpro.app.ui.controller;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.deliverpro.app.databinding.ItemConversationBinding;
import com.deliverpro.app.model.Message;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter
        extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onClick(Message conv);
    }

    private List<Message> data = new ArrayList<>();
    private final OnConversationClickListener listener;
    private final boolean isController; // true = show 🚨 for unread alert messages

    public ConversationAdapter(OnConversationClickListener listener, boolean isController) {
        this.listener     = listener;
        this.isController = isController;
    }

    public void setData(List<Message> newData) {
        this.data = newData != null ? newData : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemConversationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() { return data.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemConversationBinding b;
        ViewHolder(ItemConversationBinding b) { super(b.getRoot()); this.b = b; }

        void bind(Message conv) {
            // Avatar initials
            String name = conv.getFullName() != null ? conv.getFullName() : "—";
            b.tvName.setText(name);
            String[] parts = name.split(" ");
            String initials = parts.length >= 2
                    ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                    : name.substring(0, Math.min(2, name.length()));
            b.tvAvatar.setText(initials.toUpperCase());

            // Timestamp
            String sentAt = conv.getLastSentAt();
            if (sentAt != null && sentAt.length() >= 16) {
                b.tvTime.setText(sentAt.substring(11, 16));
            } else {
                b.tvTime.setText("");
            }

            String lastMsg = conv.getLastMessage();

            // 🚨 urgent styling only for controllers receiving unread alert messages
            boolean isUrgent = isController
                    && lastMsg != null
                    && lastMsg.startsWith("🚨")
                    && conv.getUnreadCount() > 0;

            if (isUrgent) {
                b.getRoot().setBackgroundColor(Color.parseColor("#FFF0F0"));
                b.tvName.setTextColor(Color.parseColor("#CC0000"));
                b.tvLastMessage.setText(lastMsg);
                b.tvLastMessage.setTextColor(Color.parseColor("#CC0000"));
                b.tvUnreadBadge.setVisibility(android.view.View.VISIBLE);
                b.tvUnreadBadge.setText("🚨");
                b.tvUnreadBadge.setBackgroundColor(Color.TRANSPARENT);
                b.tvUnreadBadge.setTextColor(Color.parseColor("#CC0000"));
                b.tvUnreadBadge.setTextSize(18f);
            } else {
                b.getRoot().setBackgroundColor(Color.WHITE);
                b.tvName.setTextColor(Color.parseColor("#1A1A1A"));
                b.tvLastMessage.setText(lastMsg != null ? lastMsg : "Aucun message");
                b.tvLastMessage.setTextColor(Color.parseColor("#888888"));

                if (conv.getUnreadCount() > 0) {
                    b.tvUnreadBadge.setVisibility(android.view.View.VISIBLE);
                    b.tvUnreadBadge.setText(String.valueOf(conv.getUnreadCount()));
                    b.tvUnreadBadge.setTextColor(Color.WHITE);
                    b.tvUnreadBadge.setTextSize(12f);
                    b.tvUnreadBadge.setBackgroundResource(
                            com.deliverpro.app.R.drawable.bg_badge);
                } else {
                    b.tvUnreadBadge.setVisibility(android.view.View.GONE);
                }
            }

            b.getRoot().setOnClickListener(v -> listener.onClick(conv));
        }
    }
}
/*package com.deliverpro.app.ui.controller;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.deliverpro.app.databinding.ItemConversationBinding;
import com.deliverpro.app.model.Message;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter
        extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener { void onClick(Message conv); }

    private List<Message> data = new ArrayList<>();
    private final OnConversationClickListener listener;

    public ConversationAdapter(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Message> newData) {
        this.data = newData != null ? newData : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemConversationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() { return data.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemConversationBinding b;
        ViewHolder(ItemConversationBinding b) { super(b.getRoot()); this.b = b; }

        void bind(Message conv) {
            // Avatar initials
            String name = conv.getFullName() != null ? conv.getFullName() : "—";
            b.tvName.setText(name);
            String[] parts = name.split(" ");
            String initials = parts.length >= 2
                    ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                    : name.substring(0, Math.min(2, name.length()));
            b.tvAvatar.setText(initials.toUpperCase());

            // Timestamp
            String sentAt = conv.getLastSentAt();
            if (sentAt != null && sentAt.length() >= 16) {
                b.tvTime.setText(sentAt.substring(11, 16));
            } else {
                b.tvTime.setText("");
            }

            // Determine if this is an urgent alert conversation:
            // Either flagged by MessagingActivity (deliverer side)
            // OR the last message starts with 🚨 and is unread (controller side)
            String lastMsg   = conv.getLastMessage();
            boolean isUrgent = conv.isUrgent()
                    || (lastMsg != null && lastMsg.startsWith("🚨") && conv.getUnreadCount() > 0);

            if (isUrgent) {
                // ── URGENT ──────────────────────────────────────────
                b.getRoot().setBackgroundColor(Color.parseColor("#FFF0F0"));
                b.tvName.setTextColor(Color.parseColor("#CC0000"));
                b.tvLastMessage.setText(lastMsg != null ? lastMsg : "🚨 Alerte urgence");
                b.tvLastMessage.setTextColor(Color.parseColor("#CC0000"));
                // Show 🚨 emoji instead of unread count
                b.tvUnreadBadge.setVisibility(android.view.View.VISIBLE);
                b.tvUnreadBadge.setText("🚨");
                b.tvUnreadBadge.setBackgroundColor(Color.TRANSPARENT);
                b.tvUnreadBadge.setTextColor(Color.parseColor("#CC0000"));
                b.tvUnreadBadge.setTextSize(18f);

            } else {
                // ── NORMAL ───────────────────────────────────────────
                b.getRoot().setBackgroundColor(Color.WHITE);
                b.tvLastMessage.setText(lastMsg != null ? lastMsg : "Aucun message");
                b.tvLastMessage.setTextColor(Color.parseColor("#888888"));
                b.tvName.setTextColor(Color.parseColor("#1A1A1A"));

                if (conv.getUnreadCount() > 0) {
                    b.tvUnreadBadge.setVisibility(android.view.View.VISIBLE);
                    b.tvUnreadBadge.setText(String.valueOf(conv.getUnreadCount()));
                    b.tvUnreadBadge.setTextColor(Color.WHITE);
                    b.tvUnreadBadge.setTextSize(12f);
                    b.tvUnreadBadge.setBackgroundResource(
                            com.deliverpro.app.R.drawable.bg_badge);
                } else {
                    b.tvUnreadBadge.setVisibility(android.view.View.GONE);
                }
            }

            b.getRoot().setOnClickListener(v -> listener.onClick(conv));
        }
    }
}/*package com.deliverpro.app.ui.controller;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.deliverpro.app.databinding.ItemConversationBinding;
import com.deliverpro.app.model.Message;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter
        extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onClick(Message conv);
    }

    private List<Message> data = new ArrayList<>();
    private final OnConversationClickListener listener;

    public ConversationAdapter(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Message> newData) {
        this.data = newData != null ? newData : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemConversationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() { return data.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemConversationBinding b;

        ViewHolder(ItemConversationBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(Message conv) {
            String name = conv.getFullName() != null ? conv.getFullName() : "—";
            b.tvName.setText(name);

            // Initiales pour l'avatar
            String[] parts = name.split(" ");
            String initials = parts.length >= 2
                    ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                    : name.substring(0, Math.min(2, name.length()));
            b.tvAvatar.setText(initials.toUpperCase());

            // FIX: use getLastMessage() not getContent()
            // getContent() is the field for individual messages, not conversations.
            // getLastMessage() maps to "last_message" from the conversations API.
            String lastMsg = conv.getLastMessage();
            b.tvLastMessage.setText(lastMsg != null ? lastMsg : "Aucun message");

            // Heure du dernier message
            String sentAt = conv.getLastSentAt();
            if (sentAt != null && sentAt.length() >= 16) {
                b.tvTime.setText(sentAt.substring(11, 16));
            } else {
                b.tvTime.setText("");
            }

            // Badge non lus
            if (conv.getUnreadCount() > 0) {
                b.tvUnreadBadge.setVisibility(android.view.View.VISIBLE);
                b.tvUnreadBadge.setText(String.valueOf(conv.getUnreadCount()));
                b.tvName.setTextColor(android.graphics.Color.parseColor("#1A1A1A"));
            } else {
                b.tvUnreadBadge.setVisibility(android.view.View.GONE);
            }

            b.getRoot().setOnClickListener(v -> listener.onClick(conv));
        }
    }
}*/
/*// ── ConversationAdapter.java ─────────────────────────────────
package com.deliverpro.app.ui.controller;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.deliverpro.app.databinding.ItemConversationBinding;
import com.deliverpro.app.model.Message;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter
        extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onClick(Message conv);
    }

    private List<Message> data = new ArrayList<>();
    private final OnConversationClickListener listener;

    public ConversationAdapter(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Message> newData) {
        this.data = newData != null ? newData : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemConversationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() { return data.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemConversationBinding b;

        ViewHolder(ItemConversationBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(Message conv) {
            String name = conv.getFullName() != null ? conv.getFullName() : "—";
            b.tvName.setText(name);

            // Initiales pour l'avatar
            String[] parts = name.split(" ");
            String initials = parts.length >= 2
                    ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                    : name.substring(0, Math.min(2, name.length()));
            b.tvAvatar.setText(initials.toUpperCase());

            // Dernier message
            b.tvLastMessage.setText(conv.getContent() != null
                    ? conv.getContent() : "Aucun message");

            // Heure
            if (conv.getSentAt() != null && conv.getSentAt().length() >= 16) {
                b.tvTime.setText(conv.getSentAt().substring(11, 16));
            }

            // Badge non lus
            if (conv.getUnreadCount() > 0) {
                b.tvUnreadBadge.setVisibility(android.view.View.VISIBLE);
                b.tvUnreadBadge.setText(String.valueOf(conv.getUnreadCount()));
                b.tvName.setTextColor(android.graphics.Color.parseColor("#1A1A1A"));
            } else {
                b.tvUnreadBadge.setVisibility(android.view.View.GONE);
            }

            b.getRoot().setOnClickListener(v -> listener.onClick(conv));
        }
    }
}*/



