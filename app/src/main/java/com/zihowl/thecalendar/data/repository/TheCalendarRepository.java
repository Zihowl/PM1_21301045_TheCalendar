package com.zihowl.thecalendar.data.repository;

import android.util.Log;
import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.data.source.local.RealmDataSource;
import com.zihowl.thecalendar.data.source.remote.ApiService;
import com.zihowl.thecalendar.data.source.remote.RetrofitClient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TheCalendarRepository {

    private final RealmDataSource localDataSource;
    private final ApiService remoteDataSource;
    private static volatile TheCalendarRepository INSTANCE;

    private TheCalendarRepository(RealmDataSource localDataSource, ApiService remoteDataSource) {
        this.localDataSource = localDataSource;
        this.remoteDataSource = remoteDataSource;
    }

    public static TheCalendarRepository getInstance(RealmDataSource localDataSource) {
        if (INSTANCE == null) {
            synchronized (TheCalendarRepository.class) {
                if (INSTANCE == null) {
                    ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
                    INSTANCE = new TheCalendarRepository(localDataSource, apiService);
                }
            }
        }
        return INSTANCE;
    }

    // --- LÓGICA DE INICIALIZACIÓN (SIN CAMBIOS) ---
    public void initializeDummyData() {
        if (localDataSource.getAllSubjects().isEmpty()) {
            createDummySubjects().forEach(this::addSubject);
        }
        if (localDataSource.getAllTasks().isEmpty()) {
            createDummyTasks().forEach(this::addTask);
        }
        if (localDataSource.getAllNotes().isEmpty()) {
            createDummyNotes().forEach(this::addNote);
        }
    }

    public List<Subject> getSubjects() {
        Log.d("Repo", "Intentando obtener materias de la API...");
        // Hacemos la llamada a la API
        remoteDataSource.getSubjects().enqueue(new Callback<List<Subject>>() {
            @Override
            public void onResponse(Call<List<Subject>> call, Response<List<Subject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("Repo", "Materias obtenidas de la API. Actualizando BD local.");
                    // Guardamos las materias recibidas en la base de datos local (Realm)
                    response.body().forEach(localDataSource::saveSubject);
                } else {
                    Log.e("Repo", "Error al obtener materias: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Subject>> call, Throwable t) {
                // Si falla la conexión, la app seguirá funcionando con los datos locales
                Log.e("Repo", "Fallo de red al obtener materias: " + t.getMessage());
            }
        });

        // La app siempre devuelve los datos locales para funcionar offline (RQF-10)
        return localDataSource.getAllSubjects();
    }

    // --- MÉTODOS "ADD" MODIFICADOS ---
    public void addSubject(Subject subject) {
        // Primero guardamos en la base de datos local para una respuesta rápida en la UI
        localDataSource.saveSubject(subject);

        // Luego, intentamos crear la materia en el servidor
        remoteDataSource.createSubject(subject).enqueue(new Callback<Subject>() {
            @Override
            public void onResponse(Call<Subject> call, Response<Subject> response) {
                if (response.isSuccessful()) {
                    Log.d("Repo", "Materia creada en el servidor con éxito.");
                    // Opcional: podrías actualizar el objeto local con datos del servidor si es necesario
                } else {
                    Log.e("Repo", "Error al crear la materia en el servidor: " + response.code());
                    // Aquí podrías implementar una lógica para reintentar más tarde
                }
            }

            @Override
            public void onFailure(Call<Subject> call, Throwable t) {
                Log.e("Repo", "Fallo de red al crear la materia: " + t.getMessage());
            }
        });
    }

    public void addTask(Task task) {
        localDataSource.saveTask(task); // Guardado local primero
        remoteDataSource.createTask(task).enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call, Response<Task> response) {
                if(response.isSuccessful()) {
                    Log.d("Repo", "Tarea creada en el servidor.");
                } else {
                    Log.e("Repo", "Error al crear la tarea: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<Task> call, Throwable t) {
                Log.e("Repo", "Fallo de red al crear tarea: " + t.getMessage());
            }
        });
    }

    public void addNote(Note note) {
        localDataSource.saveNote(note); // Guardado local primero
        remoteDataSource.createNote(note).enqueue(new Callback<Note>() {
            @Override
            public void onResponse(Call<Note> call, Response<Note> response) {
                if(response.isSuccessful()) {
                    Log.d("Repo", "Nota creada en el servidor.");
                } else {
                    Log.e("Repo", "Error al crear la nota: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<Note> call, Throwable t) {
                Log.e("Repo", "Fallo de red al crear nota: " + t.getMessage());
            }
        });
    }

    // --- RESTO DE MÉTODOS (SIN CAMBIOS) ---
    public List<Task> getPendingTasks() {
        return localDataSource.getAllTasks().stream().filter(t -> !t.isCompleted()).collect(Collectors.toList());
    }

    public List<Task> getCompletedTasks() {
        return localDataSource.getAllTasks().stream().filter(Task::isCompleted).collect(Collectors.toList());
    }

    public List<Note> getNotes() {
        return localDataSource.getAllNotes();
    }

    public List<Task> getTasksForSubject(String subjectName) {
        return localDataSource.getTasksForSubject(subjectName);
    }

    public List<Note> getNotesForSubject(String subjectName) {
        return localDataSource.getNotesForSubject(subjectName);
    }

    public void disassociateAndDeleteSubject(int subjectId) {
        localDataSource.disassociateAndDeleteSubject(subjectId);
    }

    public void cascadeDeleteSubjects(List<Integer> subjectIds) {
        localDataSource.cascadeDeleteSubjects(subjectIds);
    }

    public void updateTask(Task task) { localDataSource.saveTask(task); }
    public void updateNote(Note note) { localDataSource.saveNote(note); }
    public void deleteTasks(List<Task> tasks) { localDataSource.deleteTasks(tasks); }
    public void deleteNotes(List<Note> notes) { localDataSource.deleteNotes(notes); }

    // --- DATOS DUMMY (SIN CAMBIOS) ---
    private List<Subject> createDummySubjects() {
        ArrayList<Subject> dummyList = new ArrayList<>();
        dummyList.add(new Subject("Cálculo Diferencial", "Dr. Alan Turing", "Lunes 07:00 - 08:40\nMiércoles 07:00 - 08:40"));
        dummyList.add(new Subject("Programación Móvil", "Dra. Ada Lovelace", "Martes 09:00 - 11:00\nJueves 09:00 - 11:00"));
        return dummyList;
    }

    private List<Task> createDummyTasks() {
        ArrayList<Task> dummyList = new ArrayList<>();
        Task t1 = new Task("Hacer resumen cap 3", "Resumen del capítulo 3 sobre 'Activity Lifecycle'.", new Date(), "Programación Móvil");
        Task t2 = new Task("Resolver ejercicios pag. 50", "Ejercicios de la página 50, sección de derivadas.", new Date(), "Cálculo Diferencial");
        dummyList.add(t1);
        dummyList.add(t2);
        return dummyList;
    }

    private List<Note> createDummyNotes() {
        ArrayList<Note> dummyList = new ArrayList<>();
        dummyList.add(new Note("Apunte de Cálculo", "Recordar la regla de la cadena.", "Cálculo Diferencial"));
        dummyList.add(new Note("Idea para App", "Usar Realm para la base de datos local.", "Programación Móvil"));
        return dummyList;
    }
}