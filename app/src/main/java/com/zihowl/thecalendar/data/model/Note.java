package com.zihowl.thecalendar.data.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

import java.io.Serializable;
import java.util.Objects;

public class Note extends RealmObject implements Serializable {
    @PrimaryKey
    private int id;
    private String title;
    private String content;
    private String subjectName;

    public Note() {}

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

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

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