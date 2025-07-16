package com.zihowl.thecalendar.data.database;

import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.data.model.Note;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class DbNote {
public void setNote(Note note) {
    Realm.getDefaultInstance().executeTransaction(realm1 -> realm1.insertOrUpdate(note));
}

public void deleteNote(Note note) {
    Realm.getDefaultInstance().executeTransaction(realm1 -> {
        RealmResults<Note> realmResults = realm1.where(Note.class).equalTo("id", note.getId()).findAll();
        realmResults.deleteAllFromRealm();
    });
}

public List<Note> getNotes(int noteId) {
    Realm realm = Realm.getDefaultInstance();
    RealmResults<Note> notes = realm.where(Note.class).equalTo("id", noteId).findAll();
    return notes != null ? new ArrayList<>(realm.copyFromRealm(notes)) : null;
}





}