package com.zihowl.thecalendar.data.model;

import com.google.gson.annotations.SerializedName;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

import java.io.Serializable;
import java.util.Objects;

public class Note extends RealmObject implements Serializable {
    @PrimaryKey
    private int id;

    @SerializedName("titulo")
    private String title;

    @SerializedName("contenido")
    private String content;

    // Se mantiene para la lógica local y la interfaz de usuario
    private String subjectName;

    // Se usa para enviar y recibir el ID de la materia desde la API
    @SerializedName("id_materia")
    private Integer subjectId;

    private String owner;

    public Note() {
        // Constructor vacío requerido por Realm
    }

    public Note(String title, String content, String subjectName) {
        this.title = title;
        this.content = content;
        this.subjectName = subjectName;
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getSubjectName() { return subjectName; }
    public Integer getSubjectId() { return subjectId; }
    public String getOwner() { return owner; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    public void setOwner(String owner) { this.owner = owner; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return id == note.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}