// ── StatusUpdateRequest.java ─────────────────────────────────
package com.deliverpro.app.model;
import com.google.gson.annotations.SerializedName;
public class StatusUpdateRequest {
    @SerializedName("status")         private String status;
    @SerializedName("failure_reason") private String failureReason;

    public StatusUpdateRequest(String status, String failureReason) {
        this.status        = status;
        this.failureReason = failureReason;
    }
}
