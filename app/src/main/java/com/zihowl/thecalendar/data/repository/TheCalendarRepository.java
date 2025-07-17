package com.zihowl.thecalendar.data.repository;

import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.data.source.local.RealmDataSource;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TheCalendarRepository {

    private final RealmDataSource localDataSource;
    private static volatile TheCalendarRepository INSTANCE;

    private TheCalendarRepository(RealmDataSource localDataSource) {
        this.localDataSource = localDataSource;
    }

    public static TheCalendarRepository getInstance(RealmDataSource localDataSource) {
        if (INSTANCE == null) {
            synchronized (TheCalendarRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TheCalendarRepository(localDataSource);
                }
            }
        }
        return INSTANCE;
    }

    // --- LÓGICA DE INICIALIZACIÓN ---
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

    // --- MÉTODOS "GET" CORREGIDOS ---

    /**
     * CORRECCIÓN: Este método ahora solo lee y calcula los contadores sobre los datos leídos,
     * sin escribir de nuevo en la base de datos. Es más rápido y seguro.
     */
    public List<Subject> getSubjects() {
        List<Subject> subjects = localDataSource.getAllSubjects();
        List<Task> allTasks = localDataSource.getAllTasks();
        List<Note> allNotes = localDataSource.getAllNotes();

        // Asigna los contadores a los objetos en memoria, sin modificar la BD.
        for (Subject subject : subjects) {
            long pendingTaskCount = allTasks.stream()
                    .filter(task -> !task.isCompleted() && subject.getName().equals(task.getSubjectName()))
                    .count();
            long noteCount = allNotes.stream()
                    .filter(note -> subject.getName().equals(note.getSubjectName()))
                    .count();
            subject.setTasksPending((int) pendingTaskCount);
            subject.setNotesCount((int) noteCount);
        }
        return subjects;
    }

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

    // --- MÉTODOS DE ESCRITURA / BORRADO

    public void disassociateAndDeleteSubject(int subjectId) {
        localDataSource.disassociateAndDeleteSubject(subjectId);
    }

    public void cascadeDeleteSubjects(List<Integer> subjectIds) {
        localDataSource.cascadeDeleteSubjects(subjectIds);
    }

    public void addSubject(Subject subject) { localDataSource.saveSubject(subject); }
    public void addTask(Task task) { localDataSource.saveTask(task); }
    public void addNote(Note note) { localDataSource.saveNote(note); }
    public void updateTask(Task task) { localDataSource.saveTask(task); }
    public void updateNote(Note note) { localDataSource.saveNote(note); }
    public void deleteTasks(List<Task> tasks) { localDataSource.deleteTasks(tasks); }
    public void deleteNotes(List<Note> notes) { localDataSource.deleteNotes(notes); }

    // --- DATOS DUMMY (sin cambios) ---
    private List<Subject> createDummySubjects() {
        ArrayList<Subject> dummyList = new ArrayList<>();
        dummyList.add(new Subject("Cálculo Diferencial", "Dr. Alan Turing", "Lunes 07:00 - 08:40\nMiércoles 07:00 - 08:40"));
        dummyList.add(new Subject("Programación Móvil", "Dra. Ada Lovelace", "Martes 09:00 - 11:00\nJueves 09:00 - 11:00"));
        dummyList.add(new Subject("Bases de Datos", "Prof. Edgar F. Codd", "Viernes 11:00 - 13:00"));
        dummyList.add(new Subject("Redes de Computadoras", "Vint Cerf", "Lunes 09:00 - 10:30\nMiércoles 09:00 - 10:30"));
        dummyList.add(new Subject("Inteligencia Artificial", "Geoffrey Hinton", "Martes 11:30 - 13:00\nJueves 11:30 - 13:00"));
        dummyList.add(new Subject("Taller de Liderazgo", "Grace Hopper", "Viernes 08:00 - 10:00"));
        return dummyList;
    }

    private List<Task> createDummyTasks() {
        ArrayList<Task> dummyList = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        Task t1 = new Task("Hacer resumen cap 3", "Resumen del capítulo 3 sobre 'Activity Lifecycle'.", new Date(), "Programación Móvil");
        Task t2 = new Task("Resolver ejercicios pag. 50", "Ejercicios de la página 50, sección de derivadas.", new Date(), "Cálculo Diferencial");
        t2.setCompleted(true);
        Task t3 = new Task("Investigar SQL Injection", "Preparar una presentación breve sobre qué es y cómo prevenirlo.", null, "Bases de Datos");
        cal.add(Calendar.DAY_OF_YEAR, 3);
        Task t4 = new Task("Configurar Subnetting", "Práctica de laboratorio sobre máscaras de subred.", cal.getTime(), "Redes de Computadoras");
        cal.add(Calendar.DAY_OF_YEAR, 5);
        Task t5 = new Task("Leer paper sobre Redes Neuronales", "Paper 'Attention is All You Need'.", cal.getTime(), "Inteligencia Artificial");
        Task t6 = new Task("Entregar ensayo final", "Ensayo sobre estilos de liderazgo.", new Date(), "Taller de Liderazgo");
        t6.setCompleted(true);
        dummyList.add(t1);
        dummyList.add(t2);
        dummyList.add(t3);
        dummyList.add(t4);
        dummyList.add(t5);
        dummyList.add(t6);
        return dummyList;
    }

    private List<Note> createDummyNotes() {
        ArrayList<Note> dummyList = new ArrayList<>();
        dummyList.add(new Note("Apunte de Cálculo", "Recordar la regla de la cadena: (f(g(x)))' = f'(g(x)) * g'(x).", "Cálculo Diferencial"));
        dummyList.add(new Note("Recordatorio Examen BD", "Estudiar para el examen parcial de Bases de Datos el próximo lunes.", "Bases de Datos"));
        dummyList.add(new Note("Idea para App", "Crear una app de calendario para organizar materias. Usar Realm para la base de datos local.", "Programación Móvil"));
        dummyList.add(new Note("Comandos útiles Linux", "ls -la, grep, awk, sed", null));
        dummyList.add(new Note("Definición de IA", "La IA es la simulación de procesos de inteligencia humana por parte de máquinas.", "Inteligencia Artificial"));
        return dummyList;
    }
}