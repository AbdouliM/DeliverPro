



// ── AlertRequest.java ────────────────────────────────────────
package com.deliverpro.app.model;
import com.google.gson.annotations.SerializedName;
public class AlertRequest {
    @SerializedName("delivery_id")    private int    deliveryId;
    @SerializedName("alert_type")     private String alertType;
    @SerializedName("description")    private String description;
    @SerializedName("latitude")       private double latitude;
    @SerializedName("longitude")      private double longitude;
    @SerializedName("location_name")  private String locationName;

    public AlertRequest(int deliveryId, String alertType, String description,
                        double latitude, double longitude, String locationName) {
        this.deliveryId   = deliveryId;
        this.alertType    = alertType;
        this.description  = description;
        this.latitude     = latitude;
        this.longitude    = longitude;
        this.locationName = locationName;
    }
}