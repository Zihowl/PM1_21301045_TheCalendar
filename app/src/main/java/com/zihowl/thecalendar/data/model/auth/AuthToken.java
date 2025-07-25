package com.zihowl.thecalendar.data.model.auth;

import com.google.gson.annotations.SerializedName;

public class AuthToken {
    private String token;
    @SerializedName("foto_perfil")
    private String fotoPerfil;

    public String getToken() {
        return token;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }
}
