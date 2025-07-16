package com.zihowl.thecalendar.data.model;

public class Subject {
    private String name;
    private String professorName;
    private String schedule;
    private int tasksPending;
    private int notesCount;

    // Constructor
    public Subject(String name, String professorName, String schedule, int tasksPending, int notesCount) {
        this.name = name;
        this.professorName = professorName;
        this.schedule = schedule;
        this.tasksPending = tasksPending;
        this.notesCount = notesCount;
    }

    // Getters
    public String getName() { return name; }
    public String getProfessorName() { return professorName; }
    public String getSchedule() { return schedule; }
    public int getTasksPending() { return tasksPending; }
    public int getNotesCount() { return notesCount; }

    // Setters (NUEVOS)
    public void setTasksPending(int tasksPending) { this.tasksPending = tasksPending; }
    public void setNotesCount(int notesCount) { this.notesCount = notesCount; }
}