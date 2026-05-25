
// ── DashboardResponse.java ───────────────────────────────────
package com.deliverpro.app.model;
import com.google.gson.annotations.SerializedName;
import java.util.List;
public class DashboardResponse {
    private Summary summary;
    private List<DelivererStats> deliverers;

    public Summary            getSummary()    { return summary;    }
    public List<DelivererStats> getDeliverers() { return deliverers; }

    public static class Summary {
        private int total, pending, failed, delivered;
        public int getTotal()     { return total;     }
        public int getPending()   { return pending;   }
        public int getFailed()    { return failed;    }
        public int getDelivered() { return delivered; }
    }

    public static class DelivererStats {
        @SerializedName("deliverer_id")   private int    delivererId;
        @SerializedName("deliverer_name") private String delivererName;
        @SerializedName("total")          private int    total;
        @SerializedName("pending")        private int    pending;
        @SerializedName("failed")         private int    failed;
        @SerializedName("delivered")      private int    delivered;

        public int    getDelivererId()   { return delivererId;   }
        public String getDelivererName() { return delivererName; }
        public int    getTotal()         { return total;         }
        public int    getPending()       { return pending;       }
        public int    getFailed()        { return failed;        }
        public int    getDelivered()     { return delivered;     }
    }
}

