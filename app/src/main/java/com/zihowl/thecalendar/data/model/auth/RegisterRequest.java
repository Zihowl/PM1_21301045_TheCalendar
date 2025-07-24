package com.zihowl.thecalendar.data.model.auth;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("nombre_usuario")
    private String username;
    @SerializedName("contrasena")
    private String password;

    public RegisterRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
