package com.zihowl.thecalendar.data.model;

public class Subject {
    private String name;
    private String schedule;
    private int tasksPending;
    private int notesCount;

    // Constructor
    public Subject(String name, String schedule, int tasksPending, int notesCount) {
        this.name = name;
        this.schedule = schedule;
        this.tasksPending = tasksPending;
        this.notesCount = notesCount;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getSchedule() {
        return schedule;
    }

    public int getTasksPending() {
        return tasksPending;
    }

    public int getNotesCount() {
        return notesCount;
    }
}