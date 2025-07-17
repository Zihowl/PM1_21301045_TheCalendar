package com.zihowl.thecalendar.data.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Task extends RealmObject implements Serializable {
    @PrimaryKey
    private int id;
    private String title;
    private String description;
    private Date dueDate;
    private boolean isCompleted;
    private String subjectName;

    public Task() {}

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
    public int getId() { return id; }

    // Setters (LOS MÉTODOS QUE FALTABAN)
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    // No necesitamos un setter para dueDate por ahora

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}