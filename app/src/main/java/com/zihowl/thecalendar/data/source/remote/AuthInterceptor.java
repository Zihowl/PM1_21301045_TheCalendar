package com.zihowl.thecalendar.data.source.remote;

import com.zihowl.thecalendar.data.session.SessionManager;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private final SessionManager sessionManager;

    public AuthInterceptor(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String token = sessionManager.getToken();
        if (token != null && !token.isEmpty()) {
            request = request.newBuilder()
                    .addHeader("x-access-tokens", token)
                    .build();
        }
        return chain.proceed(request);
    }
}
