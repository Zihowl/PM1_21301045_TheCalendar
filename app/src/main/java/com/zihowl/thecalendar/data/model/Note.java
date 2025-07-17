package com.zihowl.thecalendar.data.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Note extends RealmObject {
    @PrimaryKey
    private int id;
    private String title;
    private String content;
    private String subjectName;

    // --- CONSTRUCTOR VACÍO (REQUERIDO POR REALM) ---
    public Note() {}

    public Note(int id, String title, String content, String subjectName) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.subjectName = subjectName;
    }

    // Getters y Setters (sin cambios)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
}