package com.zihowl.thecalendar.data.source.remote;

import com.zihowl.thecalendar.data.model.auth.AuthToken;
import com.zihowl.thecalendar.data.model.auth.LoginRequest;
import com.zihowl.thecalendar.data.model.auth.RegisterRequest;
import com.zihowl.thecalendar.data.source.remote.graphql.GraphQLRequest;
import com.zihowl.thecalendar.data.source.remote.graphql.GraphQLResponse;
import com.zihowl.thecalendar.data.source.remote.graphql.SubjectsData;
import com.zihowl.thecalendar.data.source.remote.graphql.TasksData;
import com.zihowl.thecalendar.data.source.remote.graphql.NotesData;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Endpoints para autenticación REST y operaciones GraphQL.
 */
public interface ApiService {

    // --- Autenticación REST ---
    @POST("api/login")
    Call<AuthToken> login(@Body LoginRequest request);

    @POST("api/register")
    Call<Void> register(@Body RegisterRequest request);

    // --- Consultas GraphQL ---
    @POST("graphql")
    Call<GraphQLResponse<SubjectsData>> getSubjects(@Body GraphQLRequest body);

    @POST("graphql")
    Call<GraphQLResponse<TasksData>> getTasks(@Body GraphQLRequest body);

    @POST("graphql")
    Call<GraphQLResponse<NotesData>> getNotes(@Body GraphQLRequest body);

    // --- Mutaciones GraphQL ---
    @POST("graphql")
    Call<GraphQLResponse<Object>> mutate(@Body GraphQLRequest body);
}