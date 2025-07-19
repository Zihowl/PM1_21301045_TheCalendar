package com.zihowl.thecalendar.data.source.remote;

import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    // --- Endpoints para Materias (Subjects) ---

    @GET("subjects")
    Call<List<Subject>> getSubjects();

    @POST("subjects")
    Call<Subject> createSubject(@Body Subject subject);


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