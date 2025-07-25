package com.zihowl.thecalendar.data.source.remote;

import com.zihowl.thecalendar.BuildConfig;
import com.zihowl.thecalendar.data.session.SessionManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RetrofitClient {

    private static Retrofit retrofit = null;
    
    // URL base para las peticiones. Cuando tengas tu backend, la cambiarás.
    // Usamos una URL pública de prueba por ahora.
    private static final String BASE_URL = BuildConfig.API_BASE_URL;

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static Retrofit getClient(SessionManager sessionManager) {
        if (retrofit == null) {
            // Interceptor para ver los logs de las llamadas a la API en el Logcat.
            // Es extremadamente útil para depurar.
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(sessionManager))
                    .addInterceptor(logging)
                    .build();

            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }
}