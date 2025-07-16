package com.zihowl.thecalendar.data.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Task extends RealmObject implements Serializable {
    @PrimaryKey
    private int id;
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

    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Date getDueDate() { return dueDate; }
    public boolean isCompleted() { return isCompleted; }
    public String getSubjectName() { return subjectName; }

    // Setters
    public void setCompleted(boolean completed) { isCompleted = completed; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return title.equals(task.title);
    }

    @Override
    public int hashCode() {
        // El hashCode también debe basarse solo en el título.
        return Objects.hash(title);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
