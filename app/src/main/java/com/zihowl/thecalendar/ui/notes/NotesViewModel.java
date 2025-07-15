package com.zihowl.thecalendar.ui.notes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zihowl.thecalendar.data.model.Note;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NotesViewModel extends ViewModel {

    private final MutableLiveData<List<Note>> _notes = new MutableLiveData<>();
    public final LiveData<List<Note>> notes = _notes;

    private final MutableLiveData<Boolean> _isSelectionMode = new MutableLiveData<>(false);
    public final LiveData<Boolean> isSelectionMode = _isSelectionMode;

    private final MutableLiveData<Set<Note>> _selectedNotes = new MutableLiveData<>(new LinkedHashSet<>());
    public final LiveData<Set<Note>> selectedNotes = _selectedNotes;

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

    public void updateNote(int position, String title, String content, String subjectName) {
        List<Note> currentList = new ArrayList<>(_notes.getValue() != null ? _notes.getValue() : Collections.emptyList());
        if (position >= 0 && position < currentList.size()) {
            Note noteToUpdate = currentList.get(position);
            noteToUpdate.setTitle(title);
            noteToUpdate.setContent(content);
            noteToUpdate.setSubjectName(subjectName);
            _notes.setValue(currentList); // Notificar cambio
        }
    }

    public void deleteSelectedNotes() {
        if (_notes.getValue() == null || _selectedNotes.getValue() == null) return;

        List<Note> currentList = new ArrayList<>(_notes.getValue());
        currentList.removeAll(_selectedNotes.getValue());
        _notes.setValue(currentList);
        finishSelectionMode();
    }

    // --- Lógica de Selección ---

    public void toggleSelection(Note note) {
        Set<Note> selected = new LinkedHashSet<>(_selectedNotes.getValue() != null ? _selectedNotes.getValue() : Collections.emptySet());
        if (selected.contains(note)) {
            selected.remove(note);
        } else {
            selected.add(note);
        }
        _selectedNotes.setValue(selected);

        if (selected.isEmpty() && Boolean.TRUE.equals(_isSelectionMode.getValue())) {
            finishSelectionMode();
        } else if (!selected.isEmpty() && !Boolean.TRUE.equals(_isSelectionMode.getValue())) {
            _isSelectionMode.setValue(true);
        }
    }

    public void finishSelectionMode() {
        _selectedNotes.setValue(new LinkedHashSet<>());
        _isSelectionMode.setValue(false);
    }
}