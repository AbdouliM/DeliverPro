

// ── Delivery.java ────────────────────────────────────────────
package com.deliverpro.app.model;
import com.google.gson.annotations.SerializedName;
public class Delivery {
    @SerializedName("delivery_id")      private int     deliveryId;
    @SerializedName("order_number")     private String  orderNumber;
    @SerializedName("status")           private String  status;
    @SerializedName("failure_reason")   private String  failureReason;
    @SerializedName("amount")           private double  amount;
    @SerializedName("payment_method")   private String  paymentMethod;
    @SerializedName("nb_articles")      private int     nbArticles;
    @SerializedName("scheduled_date")   private String  scheduledDate;
    @SerializedName("delivery_address") private String  deliveryAddress;
    @SerializedName("delivery_city")    private String  deliveryCity;
    @SerializedName("stop_order")       private int     stopOrder;
    @SerializedName("client_name")      private String  clientName;
    @SerializedName("client_phone")     private String  clientPhone;
    @SerializedName("deliverer_name")   private String  delivererName;
    @SerializedName("deliverer_id")     private int     delivererId;
    @SerializedName("client")           private Client  client;
    @SerializedName("deliverer")        private Deliverer deliverer;

    // Getters
    public int       getDeliveryId()      { return deliveryId;      }
    public String    getOrderNumber()     { return orderNumber;     }
    public String    getStatus()          { return status;          }
    public String    getFailureReason()   { return failureReason;   }
    public double    getAmount()          { return amount;          }
    public String    getPaymentMethod()   { return paymentMethod;   }
    public int       getNbArticles()      { return nbArticles;      }
    public String    getScheduledDate()   { return scheduledDate;   }
    public String    getDeliveryAddress() { return deliveryAddress; }
    public String    getDeliveryCity()    { return deliveryCity;    }
    public int       getStopOrder()       { return stopOrder;       }
    public String    getClientName()      { return clientName;      }
    public String    getClientPhone()     { return clientPhone;     }
    public String    getDelivererName()   { return delivererName;   }
    public int       getDelivererId()     { return delivererId;     }
    public Client    getClient()          { return client;          }
    public Deliverer getDeliverer()       { return deliverer;       }

    // Helpers statut
    public boolean isPending()   { return "PENDING".equals(status);   }
    public boolean isDelivered() { return "DELIVERED".equals(status); }
    public boolean isFailed()    { return "FAILED".equals(status);    }

    public static class Client {
        private String name, phone;
        public String getName()  { return name;  }
        public String getPhone() { return phone; }
    }

    public static class Deliverer {
        @SerializedName("user_id") private int    userId;
        private String name, phone;
        public int    getUserId() { return userId; }
        public String getName()   { return name;   }
        public String getPhone()  { return phone;  }
    }
}

