package com.zihowl.thecalendar.data.repository;

import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.data.source.local.RealmDataSource;

import java.util.ArrayList;
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

    // --- LÓGICA DE INICIALIZACIÓN UNIFICADA ---
    private void initializeDummyDataIfNeeded() {
        if (localDataSource.getAllSubjects().isEmpty()) {
            createDummySubjects().forEach(localDataSource::saveSubject);
        }
        if (localDataSource.getAllTasks().isEmpty()) {
            createDummyTasks().forEach(localDataSource::saveTask);
        }
        if (localDataSource.getAllNotes().isEmpty()) {
            createDummyNotes().forEach(localDataSource::saveNote);
        }
    }

    // --- MÉTODOS PÚBLICOS ---

    public List<Subject> getSubjects() {
        initializeDummyDataIfNeeded();
        List<Subject> subjects = localDataSource.getAllSubjects();
        List<Task> allTasks = localDataSource.getAllTasks();
        List<Note> allNotes = localDataSource.getAllNotes();

        for (Subject subject : subjects) {
            long taskCount = allTasks.stream().filter(task -> !task.isCompleted() && subject.getName().equals(task.getSubjectName())).count();
            long noteCount = allNotes.stream().filter(note -> subject.getName().equals(note.getSubjectName())).count();
            subject.setTasksPending((int) taskCount);
            subject.setNotesCount((int) noteCount);
        }
        return subjects;
    }

    public List<Task> getPendingTasks() {
        initializeDummyDataIfNeeded();
        return localDataSource.getAllTasks().stream().filter(t -> !t.isCompleted()).collect(Collectors.toList());
    }

    public List<Task> getCompletedTasks() {
        initializeDummyDataIfNeeded();
        return localDataSource.getAllTasks().stream().filter(Task::isCompleted).collect(Collectors.toList());
    }

    public List<Note> getNotes() {
        initializeDummyDataIfNeeded();
        return localDataSource.getAllNotes();
    }

    public void addSubject(Subject subject) {
        localDataSource.saveSubject(subject);
    }

    public void addTask(Task task) {
        localDataSource.saveTask(task);
    }

    public void updateTask(Task task) {
        localDataSource.saveTask(task);
    }

    public void addNote(Note note) {
        localDataSource.saveNote(note);
    }

    public void deleteSubjects(List<Subject> subjects) {
        localDataSource.deleteSubjects(subjects);
    }

    // --- DATOS DUMMY ---

    private List<Subject> createDummySubjects() {
        ArrayList<Subject> dummyList = new ArrayList<>();
        dummyList.add(new Subject("Cálculo Diferencial", "Dr. Alan Turing", "Lunes 07:00 - 08:40\nMiércoles 07:00 - 08:40", 1, 1));
        dummyList.add(new Subject("Programación Móvil", "Dra. Ada Lovelace", "Martes 09:00 - 11:00\nJueves 09:00 - 11:00", 1, 0));
        dummyList.add(new Subject("Bases de Datos", "Prof. Edgar F. Codd", "Viernes 11:00 - 13:00", 1, 1));
        return dummyList;
    }

    private List<Task> createDummyTasks() {
        ArrayList<Task> dummyList = new ArrayList<>();
        dummyList.add(new Task("Hacer resumen", "Resumen del capítulo 3.", new Date(), false, "Programación Móvil"));
        dummyList.add(new Task("Resolver ejercicios", "Ejercicios de la página 50.", new Date(), true, "Cálculo Diferencial"));
        dummyList.add(new Task("Tarea de BD", "Investigar sobre SQL Injection.", null, false, "Bases de Datos"));
        return dummyList;
    }

    private List<Note> createDummyNotes() {
        ArrayList<Note> dummyList = new ArrayList<>();
        dummyList.add(new Note(1, "Apunte de Cálculo", "Las derivadas son importantes...", "Cálculo Diferencial"));
        dummyList.add(new Note(2, "Recordatorio", "Estudiar para el examen de Bases de Datos.", "Bases de Datos"));
        return dummyList;
    }
}