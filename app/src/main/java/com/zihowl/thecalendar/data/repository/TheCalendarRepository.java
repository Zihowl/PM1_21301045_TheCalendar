package com.zihowl.thecalendar.data.repository;

import android.util.Log;
import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.data.session.SessionManager;
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
    private final SessionManager sessionManager;
    private static volatile TheCalendarRepository INSTANCE;

    /**
     * Busca una tarea por su ID en la base local.
     */
    private Task getTaskById(int id) {
        return localDataSource.getAllTasks().stream()
                .filter(t -> t.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /**
     * Busca una nota por su ID en la base local.
     */
    private Note getNoteById(int id) {
        return localDataSource.getAllNotes().stream()
                .filter(n -> n.getId() == id)
                .findFirst()
                .orElse(null);
    }

    private TheCalendarRepository(RealmDataSource localDataSource, SessionManager sessionManager) {
        this.localDataSource = localDataSource;
        this.sessionManager = sessionManager;
        this.remoteDataSource = RetrofitClient.getClient(sessionManager).create(ApiService.class);
    }

    public static TheCalendarRepository getInstance(RealmDataSource localDataSource, SessionManager sessionManager) {
        if (INSTANCE == null) {
            synchronized (TheCalendarRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TheCalendarRepository(localDataSource, sessionManager);
                }
            }
        }
        return INSTANCE;
    }

    // --- LÓGICA DE INICIALIZACIÓN (DATOS DUMMY) ---
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
        // Once all dummy data is inserted, ensure the counters of each subject
        // reflect the current amount of pending tasks and notes.
        recalculateAllSubjectCounters();
    }

    // --- MÉTODOS GET CON SINCRONIZACIÓN ---

    /**
     * Obtiene las materias. Primero intenta sincronizar desde la API
     * y siempre devuelve la lista desde la base de datos local.
     */
    public List<Subject> getSubjects() {
        Log.d("Repo", "Intentando obtener materias de la API...");
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
                Log.e("Repo", "Fallo de red al obtener materias: " + t.getMessage());
            }
        });
        // La app siempre devuelve los datos locales para funcionar offline (RQF-10)
        return localDataSource.getAllSubjects();
    }

    /**
     * Obtiene todas las tareas. Intenta sincronizar desde la API
     * y siempre devuelve la lista local.
     */
    public List<Task> getAllTasks() {
        remoteDataSource.getTasks().enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if(response.isSuccessful() && response.body() != null) {
                    Log.d("Repo", "Tareas obtenidas de la API.");
                    response.body().forEach(localDataSource::saveTask);
                } else {
                    Log.e("Repo", "Error al obtener tareas de la API: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                Log.e("Repo", "Fallo de red al obtener tareas: " + t.getMessage());
            }
        });
        return localDataSource.getAllTasks();
    }

    /**
     * Obtiene todas las notas. Intenta sincronizar desde la API
     * y siempre devuelve la lista local.
     */
    public List<Note> getNotes() {
        remoteDataSource.getNotes().enqueue(new Callback<List<Note>>() {
            @Override
            public void onResponse(Call<List<Note>> call, Response<List<Note>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("Repo", "Notas obtenidas de la API.");
                    response.body().forEach(localDataSource::saveNote);
                } else {
                    Log.e("Repo", "Error al obtener notas de la API: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<List<Note>> call, Throwable t) {
                Log.e("Repo", "Fallo de red al obtener notas: " + t.getMessage());
            }
        });
        return localDataSource.getAllNotes();
    }


    // --- MÉTODOS "ADD" MODIFICADOS PARA LA API ---

    public void addSubject(Subject subject) {
        localDataSource.saveSubject(subject); // Respuesta rápida en UI
        remoteDataSource.createSubject(subject).enqueue(new Callback<Subject>() {
            @Override
            public void onResponse(Call<Subject> call, Response<Subject> response) {
                if (response.isSuccessful()) {
                    Log.d("Repo", "Materia creada en el servidor con éxito.");
                } else {
                    Log.e("Repo", "Error al crear la materia en el servidor: " + response.code());
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
        recalculateSubjectCounters(task.getSubjectName());
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
        recalculateSubjectCounters(note.getSubjectName());
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

    // --- MÉTODOS DE LÓGICA LOCAL (SIN CAMBIOS) ---

    public List<Task> getPendingTasks() {
        return localDataSource.getAllTasks().stream().filter(t -> !t.isCompleted()).collect(Collectors.toList());
    }

    public List<Task> getCompletedTasks() {
        return localDataSource.getAllTasks().stream().filter(Task::isCompleted).collect(Collectors.toList());
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

    public void updateTask(Task task) {
        String originalSubjectName = null;
        Task existing = getTaskById(task.getId());
        if (existing != null) {
            originalSubjectName = existing.getSubjectName();
        }

        localDataSource.saveTask(task);

        if (originalSubjectName != null && !originalSubjectName.equals(task.getSubjectName())) {
            recalculateSubjectCounters(originalSubjectName);
        }

        recalculateSubjectCounters(task.getSubjectName());
    }

    public void updateNote(Note note) {
        String originalSubjectName = null;
        Note existing = getNoteById(note.getId());
        if (existing != null) {
            originalSubjectName = existing.getSubjectName();
        }

        localDataSource.saveNote(note);

        if (originalSubjectName != null && !originalSubjectName.equals(note.getSubjectName())) {
            recalculateSubjectCounters(originalSubjectName);
        }

        recalculateSubjectCounters(note.getSubjectName());
    }

    public void deleteTasks(List<Task> tasks) {
        localDataSource.deleteTasks(tasks);
        tasks.stream()
                .map(Task::getSubjectName)
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .forEach(this::recalculateSubjectCounters);
    }

    public void deleteNotes(List<Note> notes) {
        localDataSource.deleteNotes(notes);
        notes.stream()
                .map(Note::getSubjectName)
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .forEach(this::recalculateSubjectCounters);
    }

    /**
     * Recalcula los contadores para una materia a partir de su nombre.
     */
    private void recalculateSubjectCounters(String subjectName) {
        if (subjectName == null || subjectName.isEmpty()) {
            return;
        }
        Subject subject = localDataSource.getSubjectByName(subjectName);
        if (subject == null) {
            return;
        }
        int pendingTasks = (int) localDataSource.getAllTasks().stream()
                .filter(t -> !t.isCompleted() && subjectName.equals(t.getSubjectName()))
                .count();
        int notesCount = (int) localDataSource.getAllNotes().stream()
                .filter(n -> subjectName.equals(n.getSubjectName()))
                .count();

        localDataSource.updateSubjectCounters(subject.getId(), pendingTasks, notesCount);
    }

    /**
     * Recalculates the counters for a single subject based on current tasks and notes.
     */
    public void recalculateSubjectCounters(Subject subject) {
        int pendingTasks = (int) localDataSource.getAllTasks().stream()
                .filter(t -> !t.isCompleted() && subject.getName().equals(t.getSubjectName()))
                .count();
        int noteCount = (int) localDataSource.getAllNotes().stream()
                .filter(n -> subject.getName().equals(n.getSubjectName()))
                .count();

        localDataSource.updateSubjectCounters(subject.getId(), pendingTasks, noteCount);
    }

    /**
     * Iterates all subjects and updates their counters.
     */
    public void recalculateAllSubjectCounters() {
        for (Subject subject : localDataSource.getAllSubjects()) {
            recalculateSubjectCounters(subject);
        }
    }

    /**
     * Sincroniza con el servidor aplicando estrategia "last-write-wins".
     * Este es un ejemplo simple de subida y descarga de datos.
     */
    public void syncWithRemote(ApiService api) throws Exception {
        // Descargar datos remotos y guardarlos localmente
        Response<List<Subject>> subjectsRes = api.getSubjects().execute();
        if (subjectsRes.isSuccessful() && subjectsRes.body() != null) {
            for (Subject s : subjectsRes.body()) {
                localDataSource.saveSubject(s);
            }
        }

        Response<List<Task>> tasksRes = api.getTasks().execute();
        if (tasksRes.isSuccessful() && tasksRes.body() != null) {
            for (Task t : tasksRes.body()) {
                localDataSource.saveTask(t);
            }
        }

        Response<List<Note>> notesRes = api.getNotes().execute();
        if (notesRes.isSuccessful() && notesRes.body() != null) {
            for (Note n : notesRes.body()) {
                localDataSource.saveNote(n);
            }
        }

        recalculateAllSubjectCounters();
    }

    // --- DATOS DUMMY (PARA LA PRIMERA EJECUCIÓN) ---
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