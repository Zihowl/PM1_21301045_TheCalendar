package com.zihowl.thecalendar.data.model;

import java.util.Date;

public class Task {
    private final String title;
    private final String description;
    private final Date dueDate;
    private boolean isCompleted;
    private final String subjectName;

    public Task(String title, String description, Date dueDate, boolean isCompleted, String subjectName) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.isCompleted = isCompleted;
        this.subjectName = subjectName;
    }

    // Getters y Setters
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getSubjectName() {
        return subjectName;
    }
}