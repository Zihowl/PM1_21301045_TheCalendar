package com.zihowl.thecalendar.data.model;

public class Note {
    private final String title;
    private final String content;
    private final String subjectName;

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
}