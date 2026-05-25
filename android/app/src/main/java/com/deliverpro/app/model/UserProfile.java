// model/UserProfile.java — Nouveau modèle pour le profil complet
package com.deliverpro.app.model;

import com.google.gson.annotations.SerializedName;

public class UserProfile {
    @SerializedName("user_id")      private int    userId;
    @SerializedName("username")     private String username;
    @SerializedName("full_name")    private String fullName;
    @SerializedName("role")         private String role;
    @SerializedName("phone")        private String phone;
    @SerializedName("address")      private String address;
    @SerializedName("hire_date")    private String hireDate;
    @SerializedName("email")        private String email;
    @SerializedName("profile_photo")private String profilePhoto;

    public int    getUserId()     { return userId;       }
    public String getUsername()   { return username;     }
    public String getFullName()   { return fullName;     }
    public String getRole()       { return role;         }
    public String getPhone()      { return phone;        }
    public String getAddress()    { return address;      }
    public String getHireDate()   { return hireDate;     }
    public String getEmail()      { return email;        }
    public String getProfilePhoto(){ return profilePhoto; }
}