package com.zihowl.thecalendar.data.source.local;

import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class RealmDataSource {

    // --- Métodos de Subject ---
    public List<Subject> getAllSubjects() {
        try (Realm realm = Realm.getDefaultInstance()) {
            RealmResults<Subject> results = realm.where(Subject.class).findAll();
            return realm.copyFromRealm(results);
        }
    }

    public void saveSubject(Subject subject) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> r.insertOrUpdate(subject));
        }
    }

    public void deleteSubjects(List<Subject> subjects) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> {
                for (Subject subject : subjects) {
                    // Volver a buscar el objeto en el realm actual antes de eliminarlo
                    Subject managedSubject = r.where(Subject.class).equalTo("name", subject.getName()).findFirst();
                    if (managedSubject != null) {
                        managedSubject.deleteFromRealm();
                    }
                }
            });
        }
    }


    // --- Métodos de Task ---
    public List<Task> getAllTasks() {
        try (Realm realm = Realm.getDefaultInstance()) {
            RealmResults<Task> results = realm.where(Task.class).findAll();
            return realm.copyFromRealm(results);
        }
    }

    public void saveTask(Task task) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> r.insertOrUpdate(task));
        }
    }

    // --- Métodos de Note ---
    public List<Note> getAllNotes() {
        try (Realm realm = Realm.getDefaultInstance()) {
            RealmResults<Note> results = realm.where(Note.class).findAll();
            return realm.copyFromRealm(results);
        }
    }

    public void saveNote(Note note) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> r.insertOrUpdate(note));
        }
    }
}