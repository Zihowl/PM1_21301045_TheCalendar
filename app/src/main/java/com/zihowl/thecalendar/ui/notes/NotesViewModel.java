package com.zihowl.thecalendar.ui.notes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zihowl.thecalendar.data.model.Note;

import java.util.ArrayList;
import java.util.List;

public class NotesViewModel extends ViewModel {

    private final MutableLiveData<List<Note>> _notes = new MutableLiveData<>();
    public final LiveData<List<Note>> notes = _notes;

    public void loadNotes() {
        if (_notes.getValue() == null) {
            ArrayList<Note> dummyList = new ArrayList<>();
            dummyList.add(new Note("Apunte de Cálculo", "Las derivadas son importantes...", "Cálculo Diferencial"));
            dummyList.add(new Note("Recordatorio", "Estudiar para el examen de Bases de Datos.", "Bases de Datos"));
            _notes.setValue(dummyList);
        }
    }

    public void addNote(Note note) {
        List<Note> currentList = new ArrayList<>(_notes.getValue() != null ? _notes.getValue() : new ArrayList<>());
        currentList.add(note);
        _notes.setValue(currentList);
    }
}