
// ── LoginRequest.java ────────────────────────────────────────
package com.deliverpro.app.model;
public class LoginRequest {
    private String username;
    private String password;
    private String email; // pour forgot-password

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    public LoginRequest(String email) {
        this.email = email;
    }
}