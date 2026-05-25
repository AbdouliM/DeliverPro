
// ── LoginResponse.java ───────────────────────────────────────
package com.deliverpro.app.model;
import com.google.gson.annotations.SerializedName;
public class LoginResponse {
    private String token;
    private User user;

    public String getToken() { return token; }
    public User   getUser()  { return user;  }

    public static class User {
        @SerializedName("user_id")   private int    userId;
        @SerializedName("username")  private String username;
        @SerializedName("full_name") private String fullName;
        @SerializedName("role")      private String role;
        @SerializedName("phone")     private String phone;

        public int    getUserId()  { return userId;   }
        public String getUsername(){ return username; }
        public String getFullName(){ return fullName; }
        public String getRole()    { return role;     }
        public String getPhone()   { return phone;    }
    }
}


