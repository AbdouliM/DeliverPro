package com.deliverpro.app.model;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("user_id")   private int    userId;
    @SerializedName("full_name") private String fullName;
    @SerializedName("role")      private String role;
    @SerializedName("phone")     private String phone;

    public int    getUserId()   { return userId;   }
    public String getFullName() { return fullName; }
    public String getRole()     { return role;     }
    public String getPhone()    { return phone;    }
}