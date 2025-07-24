package com.zihowl.thecalendar.data.source.remote;

import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.data.model.auth.AuthToken;
import com.zihowl.thecalendar.data.model.auth.LoginRequest;
import com.zihowl.thecalendar.data.model.auth.RegisterRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Define todos los endpoints de la API para la comunicación con el servidor.
 * Utiliza Retrofit para las operaciones HTTP.
 */
public interface ApiService {

    // --- Endpoints de Autenticación ---
    @POST("api/login")
    Call<AuthToken> login(@Body LoginRequest request);

    @POST("api/register")
    Call<Void> register(@Body RegisterRequest request);

    // --- Endpoints para Materias (Subjects) ---

    @GET("materias")
    Call<List<Subject>> getSubjects();

    @GET("materias/{id}")
    Call<Subject> getSubject(@Path("id") int subjectId);

    @POST("materias")
    Call<Subject> createSubject(@Body Subject subject);

    @PUT("materias/{id}")
    Call<Subject> updateSubject(@Path("id") int subjectId, @Body Subject subject);

    @DELETE("materias/{id}")
    Call<Void> deleteSubject(@Path("id") int subjectId);


    // --- Endpoints para Tareas (Tasks) ---

    @GET("tasks")
    Call<List<Task>> getTasks();

    @GET("tasks/{id}")
    Call<Task> getTask(@Path("id") int taskId);

    @POST("tasks")
    Call<Task> createTask(@Body Task task);

    @PUT("tasks/{id}")
    Call<Task> updateTask(@Path("id") int taskId, @Body Task task);

    @DELETE("tasks/{id}")
    Call<Void> deleteTask(@Path("id") int taskId);


    // --- Endpoints para Notas (Notes) ---

    @GET("notes")
    Call<List<Note>> getNotes();

    @GET("notes/{id}")
    Call<Note> getNote(@Path("id") int noteId);

    @POST("notes")
    Call<Note> createNote(@Body Note note);

    @PUT("notes/{id}")
    Call<Note> updateNote(@Path("id") int noteId, @Body Note note);

    @DELETE("notes/{id}")
    Call<Void> deleteNote(@Path("id") int noteId);
}