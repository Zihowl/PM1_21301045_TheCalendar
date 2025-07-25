package com.zihowl.thecalendar.data.model;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

import java.io.Serializable;
import java.util.Objects;

public class Subject extends RealmObject implements Serializable {
    @PrimaryKey
    private int id;
    @SerializedName("nombre") // Mapea el campo 'nombre' del JSON a la variable 'name'
    private String name;
    @SerializedName("profesor") // Mapea 'profesor' a 'professorName'
    private String professorName;
    @SerializedName("horario")
    private String schedule;
    private int tasksPending;
    private int notesCount;

    // Indica si el registro se marcó como eliminado pero aún no se ha
    // sincronizado con el servidor (tombstone).
    private boolean deleted;

    // Almacena el usuario dueño del registro para separar los datos locales
    private String owner;

    // Constructor vacío requerido por Realm
    public Subject() {}

    // Constructor simplificado (preferido)
    public Subject(String name, String professorName, String schedule) {
        this(name, professorName, schedule, 0, 0);
    }

    // --- ¡CONSTRUCTOR AÑADIDO PARA COMPATIBILIDAD! ---
    // Este constructor se necesita para que los UseCase no fallen.
    @Deprecated
    public Subject(String name, String professorName, String schedule, int tasksPending, int notesCount) {
        this.name = name;
        this.professorName = professorName;
        this.schedule = schedule;
        this.tasksPending = tasksPending;
        this.notesCount = notesCount;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getProfessorName() { return professorName; }
    public String getSchedule() { return schedule; }
    public int getTasksPending() { return tasksPending; }
    public int getNotesCount() { return notesCount; }
    public String getOwner() { return owner; }
    public boolean isDeleted() { return deleted; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setProfessorName(String professorName) { this.professorName = professorName; }
    public void setSchedule(String schedule) { this.schedule = schedule; }
    public void setTasksPending(int tasksPending) { this.tasksPending = tasksPending; }
    public void setNotesCount(int notesCount) { this.notesCount = notesCount; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subject subject = (Subject) o;
        return id == subject.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}