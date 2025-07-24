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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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

    // --- LÓGICA DE INICIALIZACIÓN (DATOS DUMMY) ---
    public void initializeDummyData() {
        String demoOwner = "demo";
        if (localDataSource.getAllSubjectsForOwner(demoOwner).isEmpty()) {
            createDummySubjects().forEach(s -> { s.setOwner(demoOwner); addSubject(s); });
        }
        if (localDataSource.getAllTasksForOwner(demoOwner).isEmpty()) {
            createDummyTasks().forEach(t -> { t.setOwner(demoOwner); addTask(t); });
        }
        if (localDataSource.getAllNotesForOwner(demoOwner).isEmpty()) {
            createDummyNotes().forEach(n -> { n.setOwner(demoOwner); addNote(n); });
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
        String q = "query{misMaterias{ id: dbId nombre profesor tareasCount notasCount }}";
        remoteDataSource.getSubjects(new GraphQLRequest(q)).enqueue(new Callback<GraphQLResponse<SubjectsData>>() {
            @Override
            public void onResponse(Call<GraphQLResponse<SubjectsData>> call, Response<GraphQLResponse<SubjectsData>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Subject> list = response.body().getData().getMisMaterias();
                    if (list != null) {
                        list.forEach(localDataSource::saveSubject);
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
        String owner = sessionManager.getUsername();
        return localDataSource.getAllSubjectsForOwner(owner);
    }

    /**
     * Obtiene todas las tareas. Intenta sincronizar desde la API
     * y siempre devuelve la lista local.
     */
    public List<Task> getAllTasks() {
        String q = "query{todasMisTareas{ id: dbId titulo descripcion fecha_entrega: fechaEntrega completada id_materia: idMateria }}";
        remoteDataSource.getTasks(new GraphQLRequest(q)).enqueue(new Callback<GraphQLResponse<TasksData>>() {
            @Override
            public void onResponse(Call<GraphQLResponse<TasksData>> call, Response<GraphQLResponse<TasksData>> response) {
                if(response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Task> list = response.body().getData().getTodasMisTareas();
                    if (list != null) {
                        list.forEach(localDataSource::saveTask);
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
        String owner = sessionManager.getUsername();
        return localDataSource.getAllTasksForOwner(owner);
    }

    /**
     * Obtiene todas las notas. Intenta sincronizar desde la API
     * y siempre devuelve la lista local.
     */
    public List<Note> getNotes() {
        String q = "query{todasMisNotas{ id: dbId titulo contenido id_materia: idMateria }}";
        remoteDataSource.getNotes(new GraphQLRequest(q)).enqueue(new Callback<GraphQLResponse<NotesData>>() {
            @Override
            public void onResponse(Call<GraphQLResponse<NotesData>> call, Response<GraphQLResponse<NotesData>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Note> list = response.body().getData().getTodasMisNotas();
                    if (list != null) list.forEach(localDataSource::saveNote);
                } else if (response.errorBody() != null) {
                    Log.e("Repo", "Error al obtener notas de la API: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<GraphQLResponse<NotesData>> call, Throwable t) {
                Log.e("Repo", "Fallo de red al obtener notas: " + t.getMessage());
            }
        });
        String owner = sessionManager.getUsername();
        return localDataSource.getAllNotesForOwner(owner);
    }


    // --- MÉTODOS "ADD" MODIFICADOS PARA LA API ---

    public void addSubject(Subject subject) {
        subject.setOwner(sessionManager.getUsername());
        localDataSource.saveSubject(subject); // Respuesta rápida en UI
        long opId = queueOperation("subject", "CREATE", subject);
        String q = "mutation($nombre:String!,$profesor:String){ crearMateria(nombre:$nombre, profesor:$profesor){ materia{ id: dbId nombre profesor } } }";
        Map<String,Object> vars = new java.util.HashMap<>();
        vars.put("nombre", subject.getName());
        vars.put("profesor", subject.getProfessorName());
        remoteDataSource.mutate(new GraphQLRequest(q, vars)).enqueue(new Callback<GraphQLResponse<Object>>() {
            @Override
            public void onResponse(Call<GraphQLResponse<Object>> call, Response<GraphQLResponse<Object>> response) {
                if (response.isSuccessful()) {
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

    /**
     * Actualiza una materia tanto localmente como en el servidor.
     */
    public void updateSubject(Subject subject) {
        subject.setOwner(sessionManager.getUsername());
        localDataSource.saveSubject(subject);
        long opId = queueOperation("subject", "UPDATE", subject);
        String q = "mutation($id:ID!,$nombre:String,$profesor:String){ actualizarMateria(id:$id, nombre:$nombre, profesor:$profesor){ materia{ id: dbId nombre profesor } } }";
        Map<String,Object> vars = new HashMap<>();
        vars.put("id", subject.getId());
        vars.put("nombre", subject.getName());
        vars.put("profesor", subject.getProfessorName());
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

    public void addTask(Task task) {
        task.setOwner(sessionManager.getUsername());
        Subject s = localDataSource.getSubjectByName(task.getSubjectName());
        if (s != null) task.setSubjectId(s.getId());
        localDataSource.saveTask(task); // Guardado local primero
        recalculateSubjectCounters(task.getSubjectName());
        long opId = queueOperation("task", "CREATE", task);
        String q = "mutation($titulo:String!,$descripcion:String,$fecha:DateTime,$idMateria:ID!){ crearTarea(titulo:$titulo,id_materia:$idMateria,descripcion:$descripcion,fecha_entrega:$fecha){ tarea{ id: dbId } } }";
        Map<String,Object> vars = new HashMap<>();
        vars.put("titulo", task.getTitle());
        vars.put("descripcion", task.getDescription());
        vars.put("fecha", task.getDueDate());
        vars.put("idMateria", task.getSubjectId());
        remoteDataSource.mutate(new GraphQLRequest(q, vars)).enqueue(new Callback<GraphQLResponse<Object>>() {
            @Override
            public void onResponse(Call<GraphQLResponse<Object>> call, Response<GraphQLResponse<Object>> response) {
                if(response.isSuccessful()) {;
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

    public void addNote(Note note) {
        note.setOwner(sessionManager.getUsername());
        Subject s = localDataSource.getSubjectByName(note.getSubjectName());
        if (s != null) note.setSubjectId(s.getId());
        localDataSource.saveNote(note); // Guardado local primero
        recalculateSubjectCounters(note.getSubjectName());
        long opId = queueOperation("note", "CREATE", note);
        String qn = "mutation($titulo:String!,$contenido:String,$idMateria:ID!){ crearNota(titulo:$titulo,id_materia:$idMateria,contenido:$contenido){ nota{ id: dbId } } }";
        Map<String,Object> varsN = new HashMap<>();
        varsN.put("titulo", note.getTitle());
        varsN.put("contenido", note.getContent());
        varsN.put("idMateria", note.getSubjectId());
        remoteDataSource.mutate(new GraphQLRequest(qn, varsN)).enqueue(new Callback<GraphQLResponse<Object>>() {
            @Override
            public void onResponse(Call<GraphQLResponse<Object>> call, Response<GraphQLResponse<Object>> response) {
                if(response.isSuccessful()) {
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
        localDataSource.disassociateAndDeleteSubject(subjectId);
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

    public void updateTask(Task task) {
        String originalSubjectName = null;
        Task existing = getTaskById(task.getId());
        if (existing != null) {
            originalSubjectName = existing.getSubjectName();
        }

        task.setOwner(sessionManager.getUsername());
        Subject s = localDataSource.getSubjectByName(task.getSubjectName());
        if (s != null) task.setSubjectId(s.getId());
        localDataSource.saveTask(task);
        long opId = queueOperation("task", "UPDATE", task);
        String q = "mutation($id:ID!,$titulo:String,$descripcion:String,$completada:Boolean,$idMateria:ID){ actualizarTarea(id:$id,titulo:$titulo,descripcion:$descripcion,completada:$completada){ tarea{ id: dbId } } }";
        Map<String,Object> vars = new HashMap<>();
        vars.put("id", task.getId());
        vars.put("titulo", task.getTitle());
        vars.put("descripcion", task.getDescription());
        vars.put("completada", task.isCompleted());
        vars.put("idMateria", task.getSubjectId());
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
        if (s != null) note.setSubjectId(s.getId());
        localDataSource.saveNote(note);
        long opId = queueOperation("note", "UPDATE", note);localDataSource.saveNote(note);
        String q = "mutation($id:ID!,$titulo:String,$contenido:String,$idMateria:ID){ actualizarNota(id:$id,titulo:$titulo,contenido:$contenido){ nota{ id: dbId } } }";
        Map<String,Object> vars = new HashMap<>();
        vars.put("id", note.getId());
        vars.put("titulo", note.getTitle());
        vars.put("contenido", note.getContent());
        vars.put("idMateria", note.getSubjectId());
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

        if (originalSubjectName != null && !originalSubjectName.equals(note.getSubjectName())) {
            recalculateSubjectCounters(originalSubjectName);
        }

        recalculateSubjectCounters(note.getSubjectName());
    }

    public void deleteTasks(List<Task> tasks) {
        localDataSource.deleteTasks(tasks);
        for (Task t : tasks) {
            long opId = queueOperation("task", "DELETE", t);
            String q = "mutation($id:ID!){ eliminarTarea(id:$id){ ok }}";
            Map<String,Object> vars = new HashMap<>();
            vars.put("id", t.getId());
            remoteDataSource.mutate(new GraphQLRequest(q, vars)).enqueue(new Callback<GraphQLResponse<Object>>() {
                @Override
                public void onResponse(Call<GraphQLResponse<Object>> call, Response<GraphQLResponse<Object>> response) {
                    if (!response.isSuccessful()) {
                        Log.e("Repo", "Error al eliminar tarea: " + response.code());
                    } else {
                        localDataSource.deletePendingOperation(opId);
                    }
                }

                @Override
                public void onFailure(Call<GraphQLResponse<Object>> call, Throwable t) {
                    Log.e("Repo", "Fallo de red al eliminar tarea: " + t.getMessage());
                }
            });
        }
        tasks.stream()
                .map(Task::getSubjectName)
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .forEach(this::recalculateSubjectCounters);
    }

    public void deleteNotes(List<Note> notes) {
        localDataSource.deleteNotes(notes);
        for (Note n : notes) {
            long opId = queueOperation("note", "DELETE", n);
            String qn = "mutation($id:ID!){ eliminarNota(id:$id){ ok }}";
            Map<String,Object> varsN = new HashMap<>();
            varsN.put("id", n.getId());
            remoteDataSource.mutate(new GraphQLRequest(qn, varsN)).enqueue(new Callback<GraphQLResponse<Object>>() {
                @Override
                public void onResponse(Call<GraphQLResponse<Object>> call, Response<GraphQLResponse<Object>> response) {
                    if (!response.isSuccessful()) {
                        Log.e("Repo", "Error al eliminar nota: " + response.code());
                    } else {
                        localDataSource.deletePendingOperation(opId);
                    }
                }

                @Override
                public void onFailure(Call<GraphQLResponse<Object>> call, Throwable t) {
                    Log.e("Repo", "Fallo de red al eliminar nota: " + t.getMessage());
                }
            });
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
     * Sincroniza con el servidor aplicando estrategia "last-write-wins".
     * Este es un ejemplo simple de subida y descarga de datos.
     */
    public void syncWithRemote(ApiService api) throws Exception {
        // Subir operaciones pendientes
        for (PendingOperation op : localDataSource.getAllPendingOperations()) {
            switch (op.getEntity()) {
                case "subject":
                    Subject s = gson.fromJson(op.getPayload(), Subject.class);
                    if ("CREATE".equals(op.getAction())) {
                        String m = "mutation($nombre:String!,$profesor:String){ crearMateria(nombre:$nombre,profesor:$profesor){ materia{ id: dbId } } }";
                        Map<String,Object> v = new HashMap<>(); v.put("nombre", s.getName()); v.put("profesor", s.getProfessorName());
                        api.mutate(new GraphQLRequest(m,v)).execute();
                    } else if ("UPDATE".equals(op.getAction())) {
                        String m = "mutation($id:ID!,$nombre:String,$profesor:String){ actualizarMateria(id:$id,nombre:$nombre,profesor:$profesor){ materia{ id: dbId } } }";
                        Map<String,Object> v = new HashMap<>(); v.put("id", s.getId()); v.put("nombre", s.getName()); v.put("profesor", s.getProfessorName());
                        api.mutate(new GraphQLRequest(m,v)).execute();
                    } else if ("DELETE".equals(op.getAction())) {
                        String m = "mutation($id:ID!){ eliminarMateria(id:$id){ ok }}"; Map<String,Object> v=new HashMap<>(); v.put("id", s.getId());
                        api.mutate(new GraphQLRequest(m,v)).execute();
                    }
                    break;
                case "task":
                    Task t = gson.fromJson(op.getPayload(), Task.class);
                    if ("CREATE".equals(op.getAction())) {
                        String m="mutation($titulo:String!,$descripcion:String,$fecha:DateTime,$idMateria:ID!){ crearTarea(titulo:$titulo,id_materia:$idMateria,descripcion:$descripcion,fecha_entrega:$fecha){ tarea{ id: dbId } } }";
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
                        String m="mutation($titulo:String!,$contenido:String,$idMateria:ID!){ crearNota(titulo:$titulo,id_materia:$idMateria,contenido:$contenido){ nota{ id: dbId } } }";
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
        Response<GraphQLResponse<SubjectsData>> subjectsRes = api.getSubjects(new GraphQLRequest("query{misMaterias{ id: dbId nombre profesor tareasCount notasCount }}")).execute();
        if (subjectsRes.isSuccessful() && subjectsRes.body() != null && subjectsRes.body().getData() != null) {
            for (Subject s : subjectsRes.body().getData().getMisMaterias()) {
                localDataSource.saveSubject(s);
            }
        }

        Response<GraphQLResponse<TasksData>> tasksRes = api.getTasks(new GraphQLRequest("query{todasMisTareas{ id: dbId titulo descripcion fecha_entrega: fechaEntrega completada id_materia: idMateria }}")).execute();
        if (tasksRes.isSuccessful() && tasksRes.body() != null && tasksRes.body().getData() != null) {
            for (Task t : tasksRes.body().getData().getTodasMisTareas()) {
                localDataSource.saveTask(t);
            }
        }

        Response<GraphQLResponse<NotesData>> notesRes = api.getNotes(new GraphQLRequest("query{todasMisNotas{ id: dbId titulo contenido id_materia: idMateria }}")).execute();
        if (notesRes.isSuccessful() && notesRes.body() != null && notesRes.body().getData() != null) {
            for (Note n : notesRes.body().getData().getTodasMisNotas()) {
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