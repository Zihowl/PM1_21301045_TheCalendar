package com.zihowl.thecalendar.data.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Subject extends RealmObject {
    @PrimaryKey
    private int id;
    private String name;
    private String professorName;
    private String schedule;
    private int tasksPending;
    private int notesCount;

    // --- CONSTRUCTOR VACÍO (REQUERIDO POR REALM) ---
    public Subject() {}

    // Constructor original
    public Subject(String name, String professorName, String schedule, int tasksPending, int notesCount) {
        this.name = name;
        this.professorName = professorName;
        this.schedule = schedule;
        this.tasksPending = tasksPending;
        this.notesCount = notesCount;
    }

    // Getters y Setters (sin cambios)
    public String getName() { return name; }
    public String getProfessorName() { return professorName; }
    public String getSchedule() { return schedule; }
    public int getTasksPending() { return tasksPending; }
    public int getNotesCount() { return notesCount; }

    public void setTasksPending(int tasksPending) { this.tasksPending = tasksPending; }
    public void setNotesCount(int notesCount) { this.notesCount = notesCount; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}