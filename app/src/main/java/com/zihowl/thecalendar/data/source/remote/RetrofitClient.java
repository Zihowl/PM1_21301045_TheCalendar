package com.zihowl.thecalendar.data.source.remote;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit = null;

    // URL base para las peticiones. Cuando tengas tu backend, la cambiarás.
    // Usamos una URL pública de prueba por ahora.
    private static final String BASE_URL = "https://jsonplaceholder.typicode.com/";

    public static Retrofit getClient() {
        if (retrofit == null) {
            // Interceptor para ver los logs de las llamadas a la API en el Logcat.
            // Es extremadamente útil para depurar.
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}