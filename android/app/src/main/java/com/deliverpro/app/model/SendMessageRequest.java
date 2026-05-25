

// ── SendMessageRequest.java ──────────────────────────────────
package com.deliverpro.app.model;
import com.google.gson.annotations.SerializedName;
public class SendMessageRequest {
    @SerializedName("receiver_id") private int    receiverId;
    @SerializedName("content")     private String content;

    public SendMessageRequest(int receiverId, String content) {
        this.receiverId = receiverId;
        this.content    = content;
    }
}

