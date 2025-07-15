package com.zihowl.thecalendar.data.model;

public class Note {
    private String title;
    private String content;
    private String subjectName;

    public Note(String title, String content, String subjectName) {
        this.title = title;
        this.content = content;
        this.subjectName = subjectName;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSubjectName() {
        return subjectName;
    }

    // Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
}