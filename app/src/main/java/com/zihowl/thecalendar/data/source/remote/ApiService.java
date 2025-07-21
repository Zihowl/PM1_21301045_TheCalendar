package com.zihowl.thecalendar.data.source.remote;

import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    // --- Endpoints para Materias (Subjects) ---

    @GET("materias") // El endpoint que creamos en app.py
    Call<List<Subject>> getSubjects();

    @POST("materias")
    Call<Subject> createSubject(@Body Subject subject);

    @PUT("materias/{id}")
    Call<Subject> updateSubject(@Path("id") int subjectId, @Body Subject subject);

    @DELETE("materias/{id}")
    Call<Void> deleteSubject(@Path("id") int subjectId); // Void porque no esperamos respuesta


    // --- Endpoints para Tareas (Tasks) ---

    @GET("tasks")
    Call<List<Task>> getTasks();

    @POST("tasks")
    Call<Task> createTask(@Body Task task);


    // --- Endpoints para Notas (Notes) ---

    @GET("notes")
    Call<List<Note>> getNotes();

    @POST("notes")
    Call<Note> createNote(@Body Note note);
}