package com.zihowl.thecalendar.data.repository;

import android.util.Log;
import com.google.gson.Gson;
import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.data.model.PendingOperation;
import com.zihowl.thecalendar.data.session.SessionManager;
import com.zihowl.thecalendar.data.source.local.RealmDataSource;
import com.zihowl.thecalendar.data.source.remote.ApiService;
import com.zihowl.thecalendar.data.source.remote.RetrofitClient;
import com.zihowl.thecalendar.data.source.remote.graphql.GraphQLRequest;
import com.zihowl.thecalendar.data.source.remote.graphql.GraphQLResponse;
import com.zihowl.thecalendar.data.source.remote.graphql.SubjectsData;
import com.zihowl.thecalendar.data.source.remote.graphql.TasksData;
import com.zihowl.thecalendar.data.source.remote.graphql.NotesData;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TheCalendarRepository {

    private final RealmDataSource localDataSource;
    private final ApiService remoteDataSource;
    private final SessionManager sessionManager;
    private final Gson gson = new Gson();
    private static volatile TheCalendarRepository INSTANCE;

    /**
     * Ensure tasks and notes reference the correct subject ID.
     */
    private void updateSubjectReferences(Subject subject) {
        String owner = sessionManager.getUsername();
        for (Task t : localDataSource.getAllTasksForOwner(owner)) {
            if (subject.getName().equals(t.getSubjectName()) &&
                    (t.getSubjectId() == null || t.getSubjectId() != subject.getId())) {
                t.setSubjectId(subject.getId());
                localDataSource.saveTask(t);
            }
        }
        for (Note n : localDataSource.getAllNotesForOwner(owner)) {
            if (subject.getName().equals(n.getSubjectName()) &&
                    (n.getSubjectId() == null || n.getSubjectId() != subject.getId())) {
                n.setSubjectId(subject.getId());
                localDataSource.saveNote(n);
            }
        }
    }

    private boolean isLoggedIn() {
        String token = sessionManager.getToken();
        return token != null && !token.isEmpty();
    }

    private long queueOperation(String entity, String action, Object obj) {
        String json = gson.toJson(obj);
        PendingOperation op = new PendingOperation();
        op.setEntity(entity);
        op.setAction(action);
        op.setPayload(json);
        op.setTimestamp(System.currentTimeMillis());
        localDataSource.savePendingOperation(op);
        return op.getId();
    }

    /**
     * Busca una tarea por su ID en la base local.
     */
    private Task getTaskById(int id) {
        String owner = sessionManager.getUsername();
        return localDataSource.getAllTasksForOwner(owner).stream()
                .filter(t -> t.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /**
     * Busca una nota por su ID en la base local.
     */
    private Note getNoteById(int id) {
        String owner = sessionManager.getUsername();
        return localDataSource.getAllNotesForOwner(owner).stream()
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

    /**
     * Returns true if a subject with the given name already exists for the current user.
     */
    public boolean subjectExists(String name) {
        String owner = sessionManager.getUsername();
        return localDataSource.subjectExists(owner, name);
    }



    // --- MÉTODOS GET CON SINCRONIZACIÓN ---

    /**
     * Obtiene las materias. Primero intenta sincronizar desde la API
     * y siempre devuelve la lista desde la base de datos local.
     */
    public List<Subject> getSubjects() {
        Log.d("Repo", "Intentando obtener materias de la API...");
        String q = "query{misMaterias{ id: dbId nombre profesor horario tareasCount notasCount }}";
        if (isLoggedIn()) {
            remoteDataSource.getSubjects(new GraphQLRequest(q)).enqueue(new Callback<GraphQLResponse<SubjectsData>>() {
            @Override
            public void onResponse(Call<GraphQLResponse<SubjectsData>> call, Response<GraphQLResponse<SubjectsData>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Subject> list = response.body().getData().getMisMaterias();
                    if (list != null) {
                        String owner = sessionManager.getUsername();
                        java.util.Set<Integer> remoteIds = new java.util.HashSet<>();
                        for (Subject s : list) {
                            s.setOwner(owner);
                            s.setDeleted(false);
                            remoteIds.add(s.getId());
                            localDataSource.saveSubject(s);
                            updateSubjectReferences(s);
                        }
                        for (Subject local : localDataSource.getAllSubjectsForOwner(owner)) {
                            if (!remoteIds.contains(local.getId())) {
                                localDataSource.removeCascadeSubject(local.getId());
                            }
                        }
                    }
                } else if (response.errorBody() != null) {
                    Log.e("Repo", "Error al obtener materias: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<GraphQLResponse<SubjectsData>> call, Throwable t) {
                Log.e("Repo", "Fallo de red al obtener materias: " + t.getMessage());
            }
            });
        }
        String owner = sessionManager.getUsername();
        return localDataSource.getAllSubjectsForOwner(owner);
    }

    /**
     * Obtiene todas las tareas. Intenta sincronizar desde la API
     * y siempre devuelve la lista local.
     */
    public List<Task> getAllTasks() {
        String q = "query{todasMisTareas{ id: dbId titulo descripcion fecha_entrega: fechaEntrega completada id_materia: idMateria }}";
        if (isLoggedIn()) {
        remoteDataSource.getTasks(new GraphQLRequest(q)).enqueue(new Callback<GraphQLResponse<TasksData>>() {
            @Override
            public void onResponse(Call<GraphQLResponse<TasksData>> call, Response<GraphQLResponse<TasksData>> response) {
                if(response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Task> list = response.body().getData().getTodasMisTareas();
                    if (list != null) {
                        String owner = sessionManager.getUsername();
                        for (Task t : list) {
                            t.setOwner(owner);
                            t.setDeleted(false);
                            if (t.getSubjectId() != null) {
                                Subject s = localDataSource.getSubjectById(t.getSubjectId());
                                if (s != null) {
                                    t.setSubjectName(s.getName());
                                }
                            }
                            localDataSource.saveTask(t);
                        }
                    }
                } else if (response.errorBody() != null) {
                    Log.e("Repo", "Error al obtener tareas de la API: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<GraphQLResponse<TasksData>> call, Throwable t) {
                Log.e("Repo", "Fallo de red al obtener tareas: " + t.getMessage());
            }
        });
        }
        String owner = sessionManager.getUsername();
        return localDataSource.getAllTasksForOwner(owner);
    }

    /**
     * Obtiene todas las notas. Intenta sincronizar desde la API
     * y siempre devuelve la lista local.
     */
    public List<Note> getNotes() {
        String q = "query{todasMisNotas{ id: dbId titulo contenido id_materia: idMateria }}";
        if (isLoggedIn()) {
        remoteDataSource.getNotes(new GraphQLRequest(q)).enqueue(new Callback<GraphQLResponse<NotesData>>() {
            @Override
            public void onResponse(Call<GraphQLResponse<NotesData>> call, Response<GraphQLResponse<NotesData>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Note> list = response.body().getData().getTodasMisNotas();
                    if (list != null) {
                        String owner = sessionManager.getUsername();
                        for (Note n : list) {
                            n.setOwner(owner);
                            n.setDeleted(false);
                            if (n.getSubjectId() != null) {
                                Subject s = localDataSource.getSubjectById(n.getSubjectId());
                                if (s != null) {
                                    n.setSubjectName(s.getName());
                                }
                            }
                            localDataSource.saveNote(n);
                        }
                    }
                } else if (response.errorBody() != null) {
                    Log.e("Repo", "Error al obtener notas de la API: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<GraphQLResponse<NotesData>> call, Throwable t) {
                Log.e("Repo", "Fallo de red al obtener notas: " + t.getMessage());
            }
        });
        }
        String owner = sessionManager.getUsername();
        return localDataSource.getAllNotesForOwner(owner);
    }


    // --- MÉTODOS "ADD" MODIFICADOS PARA LA API ---

    public void addSubject(Subject subject) {
        subject.setOwner(sessionManager.getUsername());
        subject.setDeleted(false);
        localDataSource.saveSubject(subject); // Respuesta rápida en UI
        updateSubjectReferences(subject);
        long opId = queueOperation("subject", "CREATE", subject);
        String q = "mutation($nombre:String!,$profesor:String,$horario:String){ crearMateria(nombre:$nombre, profesor:$profesor, horario:$horario){ materia{ id: dbId nombre profesor horario } } }";
        Map<String,Object> vars = new java.util.HashMap<>();
        vars.put("nombre", subject.getName());
        vars.put("profesor", subject.getProfessorName());
        vars.put("horario", subject.getSchedule());
        if (isLoggedIn()) {
            remoteDataSource.mutate(new GraphQLRequest(q, vars)).enqueue(new Callback<GraphQLResponse<Object>>() {
                @Override
                public void onResponse(Call<GraphQLResponse<Object>> call, Response<GraphQLResponse<Object>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        try {
                            Map<?,?> data = (Map<?,?>) response.body().getData();
                            Map<?,?> crearMateria = (Map<?,?>) data.get("crearMateria");
                            Map<?,?> materia = (Map<?,?>) crearMateria.get("materia");
                            Number newId = (Number) materia.get("id");
                            if (newId != null) {
                                subject.setId(newId.intValue());
                                localDataSource.saveSubject(subject);
                                updateSubjectReferences(subject);
                            }
                        } catch (Exception e) {
                            Log.e("Repo", "Error parsing create subject response", e);
                        }
                        localDataSource.deletePendingOperation(opId);
                    } else {
                        Log.e("Repo", "Error al crear la materia en el servidor: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<GraphQLResponse<Object>> call, Throwable t) {
                    Log.e("Repo", "Fallo de red al crear la materia: " + t.getMessage());
                }
            });
        }
    }

    /**
     * Actualiza una materia tanto localmente como en el servidor.
     */
    public void updateSubject(Subject subject) {
        subject.setOwner(sessionManager.getUsername());
        subject.setDeleted(false);
        Subject original = localDataSource.getSubjectById(subject.getId());
        String oldName = original != null ? original.getName() : null;
        localDataSource.saveSubject(subject);
        if (oldName != null && !oldName.equals(subject.getName())) {
            for (Task t : localDataSource.getTasksForSubject(oldName)) {
                t.setSubjectName(subject.getName());
                localDataSource.saveTask(t);
            }
            for (Note n : localDataSource.getNotesForSubject(oldName)) {
                n.setSubjectName(subject.getName());
                localDataSource.saveNote(n);
            }
        }
        updateSubjectReferences(subject);
        long opId = queueOperation("subject", "UPDATE", subject);
        String q = "mutation($id:ID!,$nombre:String,$profesor:String,$horario:String){ actualizarMateria(id:$id, nombre:$nombre, profesor:$profesor, horario:$horario){ materia{ id: dbId nombre profesor horario } } }";
        Map<String,Object> vars = new HashMap<>();
        vars.put("id", subject.getId());
        vars.put("nombre", subject.getName());
        vars.put("profesor", subject.getProfessorName());
        vars.put("horario", subject.getSchedule());
        if (isLoggedIn()) {
            remoteDataSource.mutate(new GraphQLRequest(q, vars)).enqueue(new Callback<GraphQLResponse<Object>>() {
                @Override
                public void onResponse(Call<GraphQLResponse<Object>> call, Response<GraphQLResponse<Object>> response) {
                    if (response.isSuccessful()) {
                        localDataSource.deletePendingOperation(opId);
                    } else {
                        Log.e("Repo", "Error al actualizar la materia: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<GraphQLResponse<Object>> call, Throwable t) {
                    Log.e("Repo", "Fallo de red al actualizar la materia: " + t.getMessage());
                }
            });
        }
    }

    public void addTask(Task task) {
        task.setOwner(sessionManager.getUsername());
        task.setDeleted(false);
        task.setDeleted(false);
        Subject s = localDataSource.getSubjectByName(task.getSubjectName());
        if (s != null) {
            task.setSubjectId(s.getId());
        } else {
            task.setSubjectId(null);
        }
        localDataSource.saveTask(task); // Guardado local primero
        recalculateSubjectCounters(task.getSubjectName());
        long opId = queueOperation("task", "CREATE", task);
        String q = "mutation($titulo:String!,$descripcion:String,$fecha:DateTime,$idMateria:ID!){ crearTarea(titulo:$titulo,idMateria:$idMateria,descripcion:$descripcion,fechaEntrega:$fecha){ tarea{ id: dbId } } }";
        Map<String,Object> vars = new HashMap<>();
        vars.put("titulo", task.getTitle());
        vars.put("descripcion", task.getDescription());
        vars.put("fecha", task.getDueDate());
        vars.put("idMateria", task.getSubjectId());
        if (isLoggedIn()) {
            remoteDataSource.mutate(new GraphQLRequest(q, vars)).enqueue(new Callback<GraphQLResponse<Object>>() {
                @Override
                public void onResponse(Call<GraphQLResponse<Object>> call, Response<GraphQLResponse<Object>> response) {
                    if(response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        try {
                            Map<?,?> data = (Map<?,?>) response.body().getData();
                            Map<?,?> crearTarea = (Map<?,?>) data.get("crearTarea");
                            Map<?,?> tarea = (Map<?,?>) crearTarea.get("tarea");
                            Number newId = (Number) tarea.get("id");
                            if (newId != null) {
                                task.setId(newId.intValue());
                                localDataSource.saveTask(task);
                            }
                        } catch (Exception e) {
                            Log.e("Repo", "Error parsing create task response", e);
                        }
                        localDataSource.deletePendingOperation(opId);
                    } else {
                        Log.e("Repo", "Error al crear la tarea: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<GraphQLResponse<Object>> call, Throwable t) {
                    Log.e("Repo", "Fallo de red al crear tarea: " + t.getMessage());
                }
            });
        }
    }

    public void addNote(Note note) {
        note.setOwner(sessionManager.getUsername());
        note.setDeleted(false);
        note.setDeleted(false);
        note.setDeleted(false);
        note.setDeleted(false);
        Subject s = localDataSource.getSubjectByName(note.getSubjectName());
        if (s != null) {
            note.setSubjectId(s.getId());
        } else {
            note.setSubjectId(null);
        }
        localDataSource.saveNote(note); // Guardado local primero
        recalculateSubjectCounters(note.getSubjectName());
        long opId = queueOperation("note", "CREATE", note);
        String qn = "mutation($titulo:String!,$contenido:String,$idMateria:ID!){ crearNota(titulo:$titulo,idMateria:$idMateria,contenido:$contenido){ nota{ id: dbId } } }";
        Map<String,Object> varsN = new HashMap<>();
        varsN.put("titulo", note.getTitle());
        varsN.put("contenido", note.getContent());
        varsN.put("idMateria", note.getSubjectId());
        if (isLoggedIn()) {
            remoteDataSource.mutate(new GraphQLRequest(qn, varsN)).enqueue(new Callback<GraphQLResponse<Object>>() {
                @Override
                public void onResponse(Call<GraphQLResponse<Object>> call, Response<GraphQLResponse<Object>> response) {
                    if(response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        try {
                            Map<?,?> data = (Map<?,?>) response.body().getData();
                            Map<?,?> crearNota = (Map<?,?>) data.get("crearNota");
                            Map<?,?> nota = (Map<?,?>) crearNota.get("nota");
                            Number newId = (Number) nota.get("id");
                            if (newId != null) {
                                note.setId(newId.intValue());
                                localDataSource.saveNote(note);
                            }
                        } catch (Exception e) {
                            Log.e("Repo", "Error parsing create note response", e);
                        }
                        localDataSource.deletePendingOperation(opId);
                    } else {
                        Log.e("Repo", "Error al crear la nota: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<GraphQLResponse<Object>> call, Throwable t) {
                    Log.e("Repo", "Fallo de red al crear nota: " + t.getMessage());
                }
            });
        }
    }

    // --- MÉTODOS DE LÓGICA LOCAL (SIN CAMBIOS) ---

    public List<Task> getPendingTasks() {
        String owner = sessionManager.getUsername();
        return localDataSource.getAllTasksForOwner(owner).stream()
                .filter(t -> !t.isCompleted())
                .collect(Collectors.toList());
    }

    public List<Task> getCompletedTasks() {
        String owner = sessionManager.getUsername();
        return localDataSource.getAllTasksForOwner(owner).stream()
                .filter(Task::isCompleted)
                .collect(Collectors.toList());
    }

    public List<Task> getTasksForSubject(String subjectName) {
        return localDataSource.getTasksForSubject(subjectName);
    }

    public List<Note> getNotesForSubject(String subjectName) {
        return localDataSource.getNotesForSubject(subjectName);
    }

    public void disassociateAndDeleteSubject(int subjectId) {
        Subject subject = localDataSource.getSubjectById(subjectId);
        if (subject == null) return;

        localDataSource.disassociateAndDeleteSubject(subjectId);

        long opId = queueOperation("subject", "UNLINK_DELETE", subject);
        String q = "mutation($id:ID!){ desvincularYEliminarMateria(id:$id){ ok }}";
        Map<String,Object> vars = new HashMap<>();
        vars.put("id", subjectId);
        if (isLoggedIn()) {
            remoteDataSource.mutate(new GraphQLRequest(q, vars)).enqueue(new Callback<GraphQLResponse<Object>>() {
                @Override
                public void onResponse(Call<GraphQLResponse<Object>> call, Response<GraphQLResponse<Object>> response) {
                    if (!response.isSuccessful()) {
                        Log.e("Repo", "Error al desvincular/eliminar materia: " + response.code());
                    } else {
                        localDataSource.deletePendingOperation(opId);
                        localDataSource.removeSubject(subjectId);
                    }
                }

                @Override
                public void onFailure(Call<GraphQLResponse<Object>> call, Throwable t) {
                    Log.e("Repo", "Fallo de red al desvincular/eliminar materia: " + t.getMessage());
                }
            });
        }
    }

    public void cascadeDeleteSubjects(List<Integer> subjectIds) {
        localDataSource.cascadeDeleteSubjects(subjectIds);
        for (Integer id : subjectIds) {
            Subject stub = new Subject();
            stub.setId(id);
            long opId = queueOperation("subject", "DELETE", stub);
            String q = "mutation($id:ID!){ eliminarMateria(id:$id){ ok }}";
            Map<String,Object> vars = new HashMap<>();
            vars.put("id", id);
            if (isLoggedIn()) {
                remoteDataSource.mutate(new GraphQLRequest(q, vars)).enqueue(new Callback<GraphQLResponse<Object>>() {
                    @Override
                    public void onResponse(Call<GraphQLResponse<Object>> call, Response<GraphQLResponse<Object>> response) {
                        if (!response.isSuccessful()) {
                            Log.e("Repo", "Error al eliminar materia: " + response.code());
                        } else {
                        localDataSource.deletePendingOperation(opId);
                    }
                    }

                    @Override
                    public void onFailure(Call<GraphQLResponse<Object>> call, Throwable t) {
                        Log.e("Repo", "Fallo de red al eliminar materia: " + t.getMessage());
                    }
                });
            }
        }
    }

    public void updateTask(Task task) {
        String originalSubjectName = null;
        Task existing = getTaskById(task.getId());
        if (existing != null) {
            originalSubjectName = existing.getSubjectName();
        }

        task.setOwner(sessionManager.getUsername());
        Subject s = localDataSource.getSubjectByName(task.getSubjectName());
        if (s != null) {
            task.setSubjectId(s.getId());
        } else {
            task.setSubjectId(null);
        }
        localDataSource.saveTask(task);
        long opId = queueOperation("task", "UPDATE", task);
        String q = "mutation($id:ID!,$titulo:String,$descripcion:String,$completada:Boolean,$idMateria:ID){ actualizarTarea(id:$id,titulo:$titulo,descripcion:$descripcion,completada:$completada){ tarea{ id: dbId } } }";
        Map<String,Object> vars = new HashMap<>();
        vars.put("id", task.getId());
        vars.put("titulo", task.getTitle());
        vars.put("descripcion", task.getDescription());
        vars.put("completada", task.isCompleted());
        vars.put("idMateria", task.getSubjectId());
        if (isLoggedIn()) {
            remoteDataSource.mutate(new GraphQLRequest(q, vars)).enqueue(new Callback<GraphQLResponse<Object>>() {
                @Override
                public void onResponse(Call<GraphQLResponse<Object>> call, Response<GraphQLResponse<Object>> response) {
                    if (!response.isSuccessful()) {
                        Log.e("Repo", "Error al actualizar tarea: " + response.code());
                    } else {
                        localDataSource.deletePendingOperation(opId);
                    }
                }

                @Override
                public void onFailure(Call<GraphQLResponse<Object>> call, Throwable t) {
                    Log.e("Repo", "Fallo de red al actualizar tarea: " + t.getMessage());
                }
            });
        }

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

        note.setOwner(sessionManager.getUsername());
        Subject s = localDataSource.getSubjectByName(note.getSubjectName());
        if (s != null) {
            note.setSubjectId(s.getId());
        } else {
            note.setSubjectId(null);
        }
        localDataSource.saveNote(note);
        long opId = queueOperation("note", "UPDATE", note);
        String q = "mutation($id:ID!,$titulo:String,$contenido:String,$idMateria:ID){ actualizarNota(id:$id,titulo:$titulo,contenido:$contenido){ nota{ id: dbId } } }";
        Map<String,Object> vars = new HashMap<>();
        vars.put("id", note.getId());
        vars.put("titulo", note.getTitle());
        vars.put("contenido", note.getContent());
        vars.put("idMateria", note.getSubjectId());
        if (isLoggedIn()) {
            remoteDataSource.mutate(new GraphQLRequest(q, vars)).enqueue(new Callback<GraphQLResponse<Object>>() {
                @Override
                public void onResponse(Call<GraphQLResponse<Object>> call, Response<GraphQLResponse<Object>> response) {
                    if (!response.isSuccessful()) {
                        Log.e("Repo", "Error al actualizar nota: " + response.code());
                    } else {
                        localDataSource.deletePendingOperation(opId);
                    }
                }

                @Override
                public void onFailure(Call<GraphQLResponse<Object>> call, Throwable t) {
                    Log.e("Repo", "Fallo de red al actualizar nota: " + t.getMessage());
                }
            });
        }

        if (originalSubjectName != null && !originalSubjectName.equals(note.getSubjectName())) {
            recalculateSubjectCounters(originalSubjectName);
        }

        recalculateSubjectCounters(note.getSubjectName());
    }

    public void deleteTasks(List<Task> tasks) {
        localDataSource.markTasksDeleted(tasks);
        for (Task t : tasks) {
            long opId = queueOperation("task", "DELETE", t);
            String q = "mutation($id:ID!){ eliminarTarea(id:$id){ ok }}";
            Map<String,Object> vars = new HashMap<>();
            vars.put("id", t.getId());
            if (isLoggedIn()) {
                remoteDataSource.mutate(new GraphQLRequest(q, vars)).enqueue(new Callback<GraphQLResponse<Object>>() {
                    @Override
                    public void onResponse(Call<GraphQLResponse<Object>> call, Response<GraphQLResponse<Object>> response) {
                        if (!response.isSuccessful()) {
                            Log.e("Repo", "Error al eliminar tarea: " + response.code());
                        } else {
                            localDataSource.deletePendingOperation(opId);
                            localDataSource.deleteTasks(Collections.singletonList(t));
                        }
                    }

                    @Override
                    public void onFailure(Call<GraphQLResponse<Object>> call, Throwable t) {
                        Log.e("Repo", "Fallo de red al eliminar tarea: " + t.getMessage());
                    }
                });
            }
        }
        tasks.stream()
                .map(Task::getSubjectName)
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .forEach(this::recalculateSubjectCounters);
    }

    public void deleteNotes(List<Note> notes) {
        localDataSource.markNotesDeleted(notes);
        for (Note n : notes) {
            long opId = queueOperation("note", "DELETE", n);
            String qn = "mutation($id:ID!){ eliminarNota(id:$id){ ok }}";
            Map<String,Object> varsN = new HashMap<>();
            varsN.put("id", n.getId());
            if (isLoggedIn()) {
                remoteDataSource.mutate(new GraphQLRequest(qn, varsN)).enqueue(new Callback<GraphQLResponse<Object>>() {
                    @Override
                    public void onResponse(Call<GraphQLResponse<Object>> call, Response<GraphQLResponse<Object>> response) {
                        if (!response.isSuccessful()) {
                            Log.e("Repo", "Error al eliminar nota: " + response.code());
                        } else {
                            localDataSource.deletePendingOperation(opId);
                            localDataSource.deleteNotes(Collections.singletonList(n));
                        }
                    }

                    @Override
                    public void onFailure(Call<GraphQLResponse<Object>> call, Throwable t) {
                        Log.e("Repo", "Fallo de red al eliminar nota: " + t.getMessage());
                    }
                });
            }
        }
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
        int pendingTasks = (int) localDataSource.getAllTasksForOwner(sessionManager.getUsername()).stream()
                .filter(t -> !t.isCompleted() && subjectName.equals(t.getSubjectName()))
                .count();
        int notesCount = (int) localDataSource.getAllNotesForOwner(sessionManager.getUsername()).stream()
                .filter(n -> subjectName.equals(n.getSubjectName()))
                .count();

        localDataSource.updateSubjectCounters(subject.getId(), pendingTasks, notesCount);
    }

    /**
     * Recalculates the counters for a single subject based on current tasks and notes.
     */
    public void recalculateSubjectCounters(Subject subject) {
        int pendingTasks = (int) localDataSource.getAllTasksForOwner(sessionManager.getUsername()).stream()
                .filter(t -> !t.isCompleted() && subject.getName().equals(t.getSubjectName()))
                .count();
        int noteCount = (int) localDataSource.getAllNotesForOwner(sessionManager.getUsername()).stream()
                .filter(n -> subject.getName().equals(n.getSubjectName()))
                .count();

        localDataSource.updateSubjectCounters(subject.getId(), pendingTasks, noteCount);
    }

    /**
     * Iterates all subjects and updates their counters.
     */
    public void recalculateAllSubjectCounters() {
        for (Subject subject : localDataSource.getAllSubjectsForOwner(sessionManager.getUsername())) {
            recalculateSubjectCounters(subject);
        }
    }

    public void clearCurrentUserData() {
        localDataSource.clearUserData(sessionManager.getUsername());
    }

    /**
     * Returns true if there are local changes pending synchronization.
     */
    public boolean hasPendingOperations() {
        return !localDataSource.getAllPendingOperations().isEmpty();
    }

    /**
     * Sincroniza con el servidor aplicando estrategia "last-write-wins".
     * Este es un ejemplo simple de subida y descarga de datos.
     */
    public void syncWithRemote(ApiService api) throws Exception {
        if (!isLoggedIn()) return;
        // Subir operaciones pendientes
        for (PendingOperation op : localDataSource.getAllPendingOperations()) {
            switch (op.getEntity()) {
                case "subject":
                    Subject s = gson.fromJson(op.getPayload(), Subject.class);
                    if ("CREATE".equals(op.getAction())) {
                        String m = "mutation($nombre:String!,$profesor:String,$horario:String){ crearMateria(nombre:$nombre,profesor:$profesor,horario:$horario){ materia{ id: dbId } } }";
                        Map<String,Object> v = new HashMap<>();
                        v.put("nombre", s.getName());
                        v.put("profesor", s.getProfessorName());
                        v.put("horario", s.getSchedule());
                        api.mutate(new GraphQLRequest(m, v)).execute();
                    } else if ("UPDATE".equals(op.getAction())) {
                        String m = "mutation($id:ID!,$nombre:String,$profesor:String,$horario:String){ actualizarMateria(id:$id,nombre:$nombre,profesor:$profesor,horario:$horario){ materia{ id: dbId } } }";
                        Map<String,Object> v = new HashMap<>();
                        v.put("id", s.getId());
                        v.put("nombre", s.getName());
                        v.put("profesor", s.getProfessorName());
                        v.put("horario", s.getSchedule());
                        api.mutate(new GraphQLRequest(m, v)).execute();
                    } else if ("DELETE".equals(op.getAction())) {
                        String m = "mutation($id:ID!){ eliminarMateria(id:$id){ ok }}"; Map<String,Object> v=new HashMap<>(); v.put("id", s.getId());
                        api.mutate(new GraphQLRequest(m,v)).execute();
                    } else if ("UNLINK_DELETE".equals(op.getAction())) {
                        String m = "mutation($id:ID!){ desvincularYEliminarMateria(id:$id){ ok }}"; Map<String,Object> v = new HashMap<>();
                        v.put("id", s.getId());
                        api.mutate(new GraphQLRequest(m, v)).execute();
                    }
                    break;
                case "task":
                    Task t = gson.fromJson(op.getPayload(), Task.class);
                    if ("CREATE".equals(op.getAction())) {
                        String m="mutation($titulo:String!,$descripcion:String,$fecha:DateTime,$idMateria:ID!){ crearTarea(titulo:$titulo,idMateria:$idMateria,descripcion:$descripcion,fechaEntrega:$fecha){ tarea{ id: dbId } } }";
                        Map<String,Object> v=new HashMap<>(); v.put("titulo",t.getTitle()); v.put("descripcion",t.getDescription()); v.put("fecha",t.getDueDate()); v.put("idMateria",t.getSubjectId());
                        api.mutate(new GraphQLRequest(m,v)).execute();
                    } else if ("UPDATE".equals(op.getAction())) {
                        String m="mutation($id:ID!,$titulo:String,$descripcion:String,$completada:Boolean,$idMateria:ID){ actualizarTarea(id:$id,titulo:$titulo,descripcion:$descripcion,completada:$completada){ tarea{ id: dbId } } }";
                        Map<String,Object> v=new HashMap<>(); v.put("id",t.getId()); v.put("titulo",t.getTitle()); v.put("descripcion",t.getDescription()); v.put("completada",t.isCompleted()); v.put("idMateria",t.getSubjectId());
                        api.mutate(new GraphQLRequest(m,v)).execute();
                    } else if ("DELETE".equals(op.getAction())) {
                        String m="mutation($id:ID!){ eliminarTarea(id:$id){ ok }}"; Map<String,Object> v=new HashMap<>(); v.put("id",t.getId());
                        api.mutate(new GraphQLRequest(m,v)).execute();
                    }
                    break;
                case "note":
                    Note n = gson.fromJson(op.getPayload(), Note.class);
                    if ("CREATE".equals(op.getAction())) {
                        String m="mutation($titulo:String!,$contenido:String,$idMateria:ID!){ crearNota(titulo:$titulo,idMateria:$idMateria,contenido:$contenido){ nota{ id: dbId } } }";
                        Map<String,Object> v=new HashMap<>(); v.put("titulo",n.getTitle()); v.put("contenido",n.getContent()); v.put("idMateria",n.getSubjectId());
                        api.mutate(new GraphQLRequest(m,v)).execute();
                    } else if ("UPDATE".equals(op.getAction())) {
                        String m="mutation($id:ID!,$titulo:String,$contenido:String,$idMateria:ID){ actualizarNota(id:$id,titulo:$titulo,contenido:$contenido){ nota{ id: dbId } } }";
                        Map<String,Object> v=new HashMap<>(); v.put("id",n.getId()); v.put("titulo",n.getTitle()); v.put("contenido",n.getContent()); v.put("idMateria",n.getSubjectId());
                        api.mutate(new GraphQLRequest(m,v)).execute();
                    } else if ("DELETE".equals(op.getAction())) {
                        String m="mutation($id:ID!){ eliminarNota(id:$id){ ok }}"; Map<String,Object> v=new HashMap<>(); v.put("id",n.getId());
                        api.mutate(new GraphQLRequest(m,v)).execute();
                    }
                    break;
            }
            localDataSource.deletePendingOperation(op.getId());
        }

        // Descargar datos remotos y guardarlos localmente
        Response<GraphQLResponse<SubjectsData>> subjectsRes = api.getSubjects(new GraphQLRequest("query{misMaterias{ id: dbId nombre profesor horario tareasCount notasCount }}")).execute();
        if (subjectsRes.isSuccessful() && subjectsRes.body() != null && subjectsRes.body().getData() != null) {
            String owner = sessionManager.getUsername();
            java.util.Set<Integer> remoteIds = new java.util.HashSet<>();
            for (Subject s : subjectsRes.body().getData().getMisMaterias()) {
                s.setOwner(owner);
                s.setDeleted(false);
                remoteIds.add(s.getId());
                localDataSource.saveSubject(s);
                updateSubjectReferences(s);
            }
            for (Subject local : localDataSource.getAllSubjectsForOwner(owner)) {
                if (!remoteIds.contains(local.getId())) {
                    localDataSource.removeCascadeSubject(local.getId());
                }
            }
        }

        Response<GraphQLResponse<TasksData>> tasksRes = api.getTasks(new GraphQLRequest("query{todasMisTareas{ id: dbId titulo descripcion fecha_entrega: fechaEntrega completada id_materia: idMateria }}")).execute();
        if (tasksRes.isSuccessful() && tasksRes.body() != null && tasksRes.body().getData() != null) {
            String owner = sessionManager.getUsername();
            for (Task t : tasksRes.body().getData().getTodasMisTareas()) {
                t.setOwner(owner);
                t.setDeleted(false);
                if (t.getSubjectId() != null) {
                    Subject s = localDataSource.getSubjectById(t.getSubjectId());
                    if (s != null) {
                        t.setSubjectName(s.getName());
                    }
                }
                localDataSource.saveTask(t);
            }
        }

        Response<GraphQLResponse<NotesData>> notesRes = api.getNotes(new GraphQLRequest("query{todasMisNotas{ id: dbId titulo contenido id_materia: idMateria }}")).execute();
        if (notesRes.isSuccessful() && notesRes.body() != null && notesRes.body().getData() != null) {
            String owner = sessionManager.getUsername();
            for (Note n : notesRes.body().getData().getTodasMisNotas()) {
                n.setOwner(owner);
                n.setDeleted(false);
                if (n.getSubjectId() != null) {
                    Subject s = localDataSource.getSubjectById(n.getSubjectId());
                    if (s != null) {
                        n.setSubjectName(s.getName());
                    }
                }
                localDataSource.saveNote(n);
            }
        }

        recalculateAllSubjectCounters();
    }

}