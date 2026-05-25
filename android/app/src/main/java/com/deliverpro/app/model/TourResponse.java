
// ── TourResponse.java ────────────────────────────────────────
package com.deliverpro.app.model;
import java.util.List;
public class TourResponse {
    private Summary        summary;
    private List<Delivery> stops;

    public Summary        getSummary() { return summary; }
    public List<Delivery> getStops()   { return stops;   }

    public static class Summary {
        private int total, done, remaining;
        public int getTotal()     { return total;     }
        public int getDone()      { return done;      }
        public int getRemaining() { return remaining; }
    }
}