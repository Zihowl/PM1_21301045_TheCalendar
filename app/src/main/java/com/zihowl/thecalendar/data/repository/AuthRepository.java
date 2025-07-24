package com.zihowl.thecalendar.data.repository;

import android.content.Context;

import com.zihowl.thecalendar.data.model.auth.AuthToken;
import com.zihowl.thecalendar.data.model.auth.LoginRequest;
import com.zihowl.thecalendar.data.model.auth.RegisterRequest;
import com.zihowl.thecalendar.data.session.SessionManager;
import com.zihowl.thecalendar.data.source.remote.ApiService;
import com.zihowl.thecalendar.data.source.remote.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {
    private final ApiService apiService;
    private final SessionManager sessionManager;

    public AuthRepository(Context context) {
        this.sessionManager = new SessionManager(context.getApplicationContext());
        this.apiService = RetrofitClient.getClient(sessionManager).create(ApiService.class);
    }

    public void login(String username, String password, Callback<Boolean> callback) {
        apiService.login(new LoginRequest(username, password)).enqueue(new Callback<AuthToken>() {
            @Override
            public void onResponse(Call<AuthToken> call, Response<AuthToken> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionManager.saveSession(username, response.body().getToken());
                    callback.onResponse(null, Response.success(true));
                } else {
                    callback.onResponse(null, Response.success(false));
                }
            }

            @Override
            public void onFailure(Call<AuthToken> call, Throwable t) {
                callback.onFailure(null, t);
            }
        });
    }

    public void register(String username, String password, Callback<Boolean> callback) {
        apiService.register(new RegisterRequest(username, password)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                callback.onResponse(null, Response.success(response.isSuccessful()));
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onFailure(null, t);
            }
        });
    }

    public void logout() {
        sessionManager.clear();
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
}
