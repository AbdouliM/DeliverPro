package com.deliverpro.app.model;

import com.google.gson.annotations.SerializedName;

public class Alert {
    @SerializedName("alert_id")    private int    alertId;
    @SerializedName("alert_type")  private String alertType;
    @SerializedName("description") private String description;
    @SerializedName("sent_at")     private String sentAt;
    @SerializedName("sender_id")   private int    senderId;
    @SerializedName("sender_name") private String senderName;

    public int    getAlertId()    { return alertId;    }
    public String getAlertType()  { return alertType;  }
    public String getDescription(){ return description; }
    public String getSentAt()     { return sentAt;     }
    public int    getSenderId()   { return senderId;   }
    public String getSenderName() { return senderName; }
}